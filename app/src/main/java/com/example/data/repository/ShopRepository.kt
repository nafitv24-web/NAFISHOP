package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
import com.example.util.CalculationHelper
import com.example.util.CalculationHelper.round2
import com.example.ui.components.toIntOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ShopRepository(private val database: AppDatabase) {

    fun getDatabase(): AppDatabase = database

    private val productDao = database.productDao()
    private val transactionDao = database.transactionDao()
    private val customerDao = database.customerDao()
    private val dueLogDao = database.dueLogDao()
    private val expenseDao = database.expenseDao()
    private val cashLogDao = database.cashLogDao()

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val lowStockProducts: Flow<List<Product>> = productDao.getLowStockProducts()
    val allTransactions: Flow<List<TransactionRecord>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionRecord>> = transactionDao.getRecentTransactions(30)
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allDueLogs: Flow<List<DueLog>> = dueLogDao.getAllDueLogs()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allCashLogs: Flow<List<CashLog>> = cashLogDao.getAllCashLogs()

    suspend fun isDatabaseEmpty(): Boolean = withContext(Dispatchers.IO) {
        val prodCount = productDao.getCount()
        val txCount = transactionDao.getCount()
        val custCount = customerDao.getCount()
        val dueCount = dueLogDao.getCount()
        (prodCount == 0 && txCount == 0 && custCount == 0 && dueCount == 0)
    }

    suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        val sanitized = product.copy(
            buyPrice = round2(product.buyPrice),
            sellPrice = round2(product.sellPrice),
            stockQuantity = round2(product.stockQuantity),
            minStockAlert = round2(product.minStockAlert)
        )
        if (sanitized.id == 0L) {
            productDao.insertProduct(sanitized)
        } else {
            productDao.updateProduct(sanitized)
        }
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.IO) {
        productDao.deleteProduct(product)
    }

    suspend fun recordStockIn(
        productId: Long,
        quantity: Double,
        buyPrice: Double?,
        sellPrice: Double?,
        note: String = "স্টক ইন"
    ) = withContext(Dispatchers.IO) {
        val existing = productDao.getProductById(productId) ?: return@withContext
        val updatedBuy = round2(buyPrice ?: existing.buyPrice)
        val updatedSell = round2(sellPrice ?: existing.sellPrice)
        val cleanQty = round2(quantity)
        val updatedProduct = existing.copy(
            stockQuantity = round2(existing.stockQuantity + cleanQty),
            buyPrice = updatedBuy,
            sellPrice = updatedSell
        )
        productDao.updateProduct(updatedProduct)

        val tx = TransactionRecord(
            type = "STOCK_IN",
            invoiceNumber = "STK-${System.currentTimeMillis() % 100000}",
            productId = productId,
            productName = existing.name,
            quantity = cleanQty,
            unit = existing.unit,
            unitPrice = updatedBuy,
            costPrice = updatedBuy,
            totalAmount = round2(cleanQty * updatedBuy),
            profitAmount = 0.0,
            paymentMethod = "CASH",
            note = note,
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(tx)
    }

    suspend fun recordStockOutManual(
        productId: Long,
        quantity: Double,
        reason: String
    ) = withContext(Dispatchers.IO) {
        val existing = productDao.getProductById(productId) ?: return@withContext
        val cleanQty = round2(quantity)
        val newStock = round2((existing.stockQuantity - cleanQty).coerceAtLeast(0.0))
        productDao.updateStock(productId, newStock)

        val tx = TransactionRecord(
            type = "STOCK_OUT_DAMAGE",
            invoiceNumber = "DMG-${System.currentTimeMillis() % 100000}",
            productId = productId,
            productName = existing.name,
            quantity = cleanQty,
            unit = existing.unit,
            unitPrice = existing.buyPrice,
            costPrice = existing.buyPrice,
            totalAmount = round2(cleanQty * existing.buyPrice),
            profitAmount = -round2(cleanQty * existing.buyPrice),
            paymentMethod = "ADJUST",
            note = reason,
            timestamp = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(tx)
    }

    suspend fun findCustomerByNameOrPhone(name: String, phone: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.findExistingCustomer(name, phone)
    }

    suspend fun deduplicateAndMergeCustomers() = withContext(Dispatchers.IO) {
        val all = customerDao.getAllCustomersList()
        if (all.isEmpty()) return@withContext

        // Group customers by normalized phone if available, or normalized name
        val groups = all.groupBy { c ->
            val cleanPhone = c.phone.trim()
            val cleanName = c.name.trim().lowercase()
            if (cleanPhone.isNotBlank()) "PHONE:$cleanPhone" else "NAME:$cleanName"
        }

        for ((_, group) in groups) {
            if (group.size > 1) {
                val survivor = group.first()
                val totalCombinedDue = round2(group.sumOf { it.totalDue })
                val totalCombinedPurchased = round2(group.sumOf { it.totalPurchased })
                val bestPhone = group.firstOrNull { it.phone.isNotBlank() }?.phone ?: survivor.phone
                val bestAddress = group.firstOrNull { it.address.isNotBlank() }?.address ?: survivor.address
                val bestImage = group.firstOrNull { it.imageUri.isNotBlank() }?.imageUri ?: survivor.imageUri

                val mergedSurvivor = survivor.copy(
                    totalDue = totalCombinedDue,
                    totalPurchased = totalCombinedPurchased,
                    phone = bestPhone,
                    address = bestAddress,
                    imageUri = bestImage,
                    lastTransactionDate = group.maxOf { it.lastTransactionDate }
                )
                customerDao.updateCustomer(mergedSurvivor)

                // Reassign due logs and delete duplicate customer rows
                for (duplicate in group) {
                    if (duplicate.id != survivor.id) {
                        customerDao.reassignDueLogs(duplicate.id, survivor.id, survivor.name)
                        customerDao.reassignTransactionsByName(duplicate.name, survivor.name)
                        customerDao.deleteCustomer(duplicate)
                    }
                }
            }
        }
        reconcileCustomerLedgers()
    }

    /**
     * Complete Ledger Reconciliation:
     * Audits and re-calculates every customer's total due by matching with their DueLogs.
     * Automatically links any orphaned logs and creates missing opening balance records.
     * Prevents any mathematical discrepancies or calculation errors.
     */
    suspend fun reconcileCustomerLedgers() = withContext(Dispatchers.IO) {
        val customers = customerDao.getAllCustomersList()
        val allDueLogs = dueLogDao.getAllDueLogsList()

        for (cust in customers) {
            val cleanPhone = cust.phone.trim()
            val cleanName = cust.name.trim().lowercase()

            // 1. Link any unlinked/orphaned due logs matching customer name or phone
            val matchingLogs = allDueLogs.filter { log ->
                log.customerId == cust.id ||
                (cleanPhone.isNotBlank() && log.customerPhone.isNotBlank() && log.customerPhone.trim() == cleanPhone) ||
                (cleanName.isNotBlank() && log.customerName.isNotBlank() && log.customerName.trim().lowercase() == cleanName)
            }

            for (log in matchingLogs) {
                if (log.customerId != cust.id) {
                    dueLogDao.updateDueLog(log.copy(customerId = cust.id))
                }
            }

            // 2. Fetch all logs for this customer
            val logs: List<DueLog> = dueLogDao.getDueLogsListForCustomer(cust.id)
            var sumGiven = 0.0
            var sumCollected = 0.0

            for (log in logs) {
                if (log.type == "DUE_GIVEN") {
                    sumGiven += log.amount
                } else if (log.type == "DUE_COLLECTED") {
                    sumCollected += log.amount
                }
            }
            sumGiven = round2(sumGiven)
            sumCollected = round2(sumCollected)
            val netFromLogs = round2((sumGiven - sumCollected).coerceAtLeast(0.0))
            val cleanCustDue = round2(cust.totalDue)

            // 3. If customer's total due is higher than the sum of history (due to opening balance)
            val missingOpeningDue = round2(cleanCustDue - netFromLogs)
            if (missingOpeningDue > 0.01) {
                val earliestTime = logs.minOfOrNull { it.timestamp }?.let { it - 60000L }
                    ?: (cust.lastTransactionDate.takeIf { it > 0 } ?: (System.currentTimeMillis() - 86400000L))
                dueLogDao.insertDueLog(
                    DueLog(
                        customerId = cust.id,
                        customerName = cust.name,
                        customerPhone = cust.phone,
                        type = "DUE_GIVEN",
                        amount = missingOpeningDue,
                        note = "প্রারম্ভিক বাকি / পূর্বের হিসাব",
                        timestamp = earliestTime
                    )
                )
            } else if (netFromLogs > cleanCustDue + 0.01) {
                // If sum of history is higher, update customer totalDue to match history exactly
                customerDao.setCustomerDue(cust.id, netFromLogs, System.currentTimeMillis())
            }
        }
    }

    private fun Double.formatQty(): String = if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()

    suspend fun processSale(
        cartItems: List<CartItem>,
        customerName: String,
        customerPhone: String,
        discount: Double,
        paidAmount: Double,
        paymentMethod: String,
        note: String
    ): String = withContext(Dispatchers.IO) {
        val invoiceNumber = "INV-${SimpleDateFormat("yyMMddHHmm", Locale.getDefault()).format(Date())}"
        val now = System.currentTimeMillis()

        var grossTotal = 0.0
        for (item in cartItems) {
            grossTotal += round2(item.total)
        }
        grossTotal = round2(grossTotal)
        val cleanDiscount = round2(discount)
        val netTotal = round2((grossTotal - cleanDiscount).coerceAtLeast(0.0))
        val cleanPaid = round2(paidAmount)
        val calculatedDue = round2((netTotal - cleanPaid).coerceAtLeast(0.0))

        // 1. Process each cart item & decrease stock
        for (item in cartItems) {
            val product = productDao.getProductById(item.product.id) ?: item.product
            productDao.decreaseStock(product.id, item.quantity)

            val itemRatio = if (grossTotal > 0) item.total / grossTotal else 0.0
            val itemDiscount = round2(cleanDiscount * itemRatio)
            val finalItemTotal = round2((item.total - itemDiscount).coerceAtLeast(0.0))
            val itemCost = round2(product.buyPrice * item.quantity)
            val itemProfit = round2(finalItemTotal - itemCost)

            val itemPaid = round2(cleanPaid * (if (netTotal > 0) finalItemTotal / netTotal else 1.0))
            val itemDue = round2(calculatedDue * (if (netTotal > 0) finalItemTotal / netTotal else 1.0))

            val tx = TransactionRecord(
                type = "SALE",
                invoiceNumber = invoiceNumber,
                productId = product.id,
                productName = product.name,
                quantity = round2(item.quantity),
                unit = product.unit,
                unitPrice = round2(item.customPrice),
                costPrice = round2(product.buyPrice),
                totalAmount = finalItemTotal,
                profitAmount = itemProfit,
                customerName = customerName.ifBlank { "ক্যাশ কাস্টমার" },
                customerPhone = customerPhone,
                paidAmount = itemPaid,
                dueAmount = itemDue,
                paymentMethod = paymentMethod,
                note = note,
                timestamp = now
            )
            transactionDao.insertTransaction(tx)
        }

        // 2. If customer has due or is identified, update/create customer ledger (No Duplicates!)
        val cleanName = customerName.trim()
        val cleanPhone = customerPhone.trim()
        if (cleanName.isNotBlank() && cleanName != "ক্যাশ কাস্টমার" || cleanPhone.isNotBlank() || calculatedDue > 0) {
            var existingCustomer = customerDao.findExistingCustomer(cleanName, cleanPhone)

            if (existingCustomer != null) {
                customerDao.updateCustomerBalance(
                    customerId = existingCustomer.id,
                    dueDiff = calculatedDue,
                    purchaseAdd = netTotal,
                    time = now
                )
            } else {
                val newCustomerId = customerDao.insertCustomer(
                    Customer(
                        name = if (cleanName.isNotBlank()) cleanName else "কাস্টমার",
                        phone = cleanPhone,
                        address = "",
                        totalDue = calculatedDue,
                        totalPurchased = netTotal,
                        lastTransactionDate = now
                    )
                )
                existingCustomer = Customer(
                    id = newCustomerId,
                    name = if (cleanName.isNotBlank()) cleanName else "কাস্টমার",
                    phone = cleanPhone,
                    totalDue = calculatedDue
                )
            }

            if (calculatedDue > 0) {
                val itemsSummary = cartItems.joinToString(", ") { "${it.product.name} (${it.quantity.formatQty()} ${it.product.unit})" }
                val dueNote = if (note.isNotBlank()) "$itemsSummary • $note • মেমো #$invoiceNumber" else "$itemsSummary • মেমো #$invoiceNumber"
                dueLogDao.insertDueLog(
                    DueLog(
                        customerId = existingCustomer.id,
                        customerName = existingCustomer.name,
                        customerPhone = existingCustomer.phone,
                        type = "DUE_GIVEN",
                        amount = calculatedDue,
                        note = dueNote,
                        timestamp = now
                    )
                )
            }
        }

        invoiceNumber
    }

    suspend fun addCustomer(name: String, phone: String, address: String, initialDue: Double, imageUri: String = "") = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanDue = round2(initialDue)

        // Check if customer with this name or phone already exists
        val existing = customerDao.findExistingCustomer(cleanName, cleanPhone)
        if (existing != null) {
            val updatedDue = round2(existing.totalDue + cleanDue)
            val updatedPurchased = round2(existing.totalPurchased + cleanDue)
            customerDao.updateCustomer(
                existing.copy(
                    name = if (cleanName.isNotBlank()) cleanName else existing.name,
                    phone = if (cleanPhone.isNotBlank()) cleanPhone else existing.phone,
                    address = if (address.isNotBlank()) address else existing.address,
                    totalDue = updatedDue,
                    totalPurchased = updatedPurchased,
                    imageUri = if (imageUri.isNotBlank()) imageUri else existing.imageUri,
                    lastTransactionDate = now
                )
            )
            if (cleanDue > 0) {
                dueLogDao.insertDueLog(
                    DueLog(
                        customerId = existing.id,
                        customerName = existing.name,
                        customerPhone = existing.phone,
                        type = "DUE_GIVEN",
                        amount = cleanDue,
                        note = "বাকি যুক্ত করা হয়েছে",
                        timestamp = now
                    )
                )
            }
            return@withContext
        }

        val customerId = customerDao.insertCustomer(
            Customer(
                name = cleanName,
                phone = cleanPhone,
                address = address,
                totalDue = cleanDue,
                totalPurchased = cleanDue,
                imageUri = imageUri,
                lastTransactionDate = now
            )
        )
        if (cleanDue > 0) {
            dueLogDao.insertDueLog(
                DueLog(
                    customerId = customerId,
                    customerName = cleanName,
                    customerPhone = cleanPhone,
                    type = "DUE_GIVEN",
                    amount = cleanDue,
                    note = "পূর্বের বাকি হিসাব শুরু",
                    timestamp = now
                )
            )
        }
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        val sanitized = customer.copy(
            totalDue = round2(customer.totalDue),
            totalPurchased = round2(customer.totalPurchased)
        )
        customerDao.updateCustomer(sanitized)
    }

    suspend fun collectCustomerDuePayment(customer: Customer, amountPaid: Double, note: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cleanPaid = round2(amountPaid)
        val updatedDue = round2((customer.totalDue - cleanPaid).coerceAtLeast(0.0))
        customerDao.setCustomerDue(customer.id, updatedDue, now)

        dueLogDao.insertDueLog(
            DueLog(
                customerId = customer.id,
                customerName = customer.name,
                customerPhone = customer.phone,
                type = "DUE_COLLECTED",
                amount = cleanPaid,
                note = note.ifBlank { "বাকি আদায়" },
                timestamp = now
            )
        )
    }

    suspend fun giveCustomerAdditionalDue(
        customer: Customer,
        amountDue: Double,
        note: String,
        selectedProducts: List<Pair<Product, Double>> = emptyList(),
        customTimestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val now = if (customTimestamp > 0) customTimestamp else System.currentTimeMillis()
        val cleanDue = round2(amountDue)
        val invoiceNo = "INV-${SimpleDateFormat("yyMMddHHmm", Locale.getDefault()).format(Date(now))}"

        if (selectedProducts.isNotEmpty()) {
            val itemsSummary = selectedProducts.joinToString(", ") { "${it.first.name} (${it.second.toIntOrNull() ?: it.second} ${it.first.unit})" }
            val fullNote = if (note.isNotBlank() && note != itemsSummary) "$itemsSummary • $note • মেমো #$invoiceNo" else "$itemsSummary • মেমো #$invoiceNo"

            for (pair in selectedProducts) {
                val prod = pair.first
                val qty = pair.second
                productDao.decreaseStock(prod.id, qty)
                val itemTotal = round2(prod.sellPrice * qty)
                val itemCost = round2(prod.buyPrice * qty)
                val itemProfit = round2(itemTotal - itemCost)

                transactionDao.insertTransaction(
                    TransactionRecord(
                        type = "SALE",
                        invoiceNumber = invoiceNo,
                        productId = prod.id,
                        productName = prod.name,
                        quantity = qty,
                        unit = prod.unit,
                        unitPrice = prod.sellPrice,
                        costPrice = prod.buyPrice,
                        totalAmount = itemTotal,
                        profitAmount = itemProfit,
                        customerName = customer.name,
                        customerPhone = customer.phone,
                        paidAmount = 0.0,
                        dueAmount = itemTotal,
                        paymentMethod = "বাকি (Due)",
                        note = fullNote,
                        timestamp = now
                    )
                )
            }

            customerDao.updateCustomerBalance(customer.id, cleanDue, cleanDue, now)
            dueLogDao.insertDueLog(
                DueLog(
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    type = "DUE_GIVEN",
                    amount = cleanDue,
                    note = fullNote,
                    timestamp = now
                )
            )
        } else {
            val fullNote = if (note.isNotBlank()) "$note • মেমো #$invoiceNo" else "বাকি বিক্রি • মেমো #$invoiceNo"
            val prodName = if (note.isNotBlank()) note else "বাকি পণ্য বিক্রয়"

            transactionDao.insertTransaction(
                TransactionRecord(
                    type = "SALE",
                    invoiceNumber = invoiceNo,
                    productId = 0L,
                    productName = prodName,
                    quantity = 1.0,
                    unit = "টি",
                    unitPrice = cleanDue,
                    costPrice = 0.0,
                    totalAmount = cleanDue,
                    profitAmount = 0.0,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    paidAmount = 0.0,
                    dueAmount = cleanDue,
                    paymentMethod = "বাকি (Due)",
                    note = fullNote,
                    timestamp = now
                )
            )

            customerDao.updateCustomerBalance(customer.id, cleanDue, cleanDue, now)
            dueLogDao.insertDueLog(
                DueLog(
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    type = "DUE_GIVEN",
                    amount = cleanDue,
                    note = fullNote,
                    timestamp = now
                )
            )
        }
    }

    suspend fun updateDueLog(dueLog: DueLog) = withContext(Dispatchers.IO) {
        dueLogDao.updateDueLog(dueLog.copy(amount = round2(dueLog.amount)))
    }

    suspend fun deleteDueLog(dueLog: DueLog) = withContext(Dispatchers.IO) {
        // 1. Revert customer balance
        val customer = customerDao.getCustomerById(dueLog.customerId)
            ?: customerDao.findExistingCustomer(dueLog.customerName, dueLog.customerPhone)

        if (customer != null) {
            val updatedDue = if (dueLog.type == "DUE_GIVEN") {
                round2((customer.totalDue - dueLog.amount).coerceAtLeast(0.0))
            } else {
                round2(customer.totalDue + dueLog.amount)
            }
            customerDao.setCustomerDue(customer.id, updatedDue, System.currentTimeMillis())
        }

        // 2. If this due log was tied to an invoice, restore the product stocks!
        if (dueLog.note.contains("INV-")) {
            val regex = "INV-\\d+".toRegex()
            val match = regex.find(dueLog.note)
            if (match != null) {
                val invoiceNo = match.value
                val invoiceTxs = transactionDao.getTransactionsByInvoice(invoiceNo)
                for (tx in invoiceTxs) {
                    if (tx.type == "SALE" && tx.productId > 0) {
                        // Restore product stock
                        productDao.increaseStock(tx.productId, tx.quantity)
                    }
                }
                transactionDao.deleteTransactionsByInvoice(invoiceNo)
            }
        }

        dueLogDao.deleteDueLog(dueLog)
    }

    suspend fun updateTransaction(tx: TransactionRecord) = withContext(Dispatchers.IO) {
        transactionDao.updateTransaction(
            tx.copy(
                quantity = round2(tx.quantity),
                unitPrice = round2(tx.unitPrice),
                costPrice = round2(tx.costPrice),
                totalAmount = round2(tx.totalAmount),
                profitAmount = round2(tx.profitAmount),
                paidAmount = round2(tx.paidAmount),
                dueAmount = round2(tx.dueAmount)
            )
        )
    }

    suspend fun deleteTransaction(tx: TransactionRecord) = withContext(Dispatchers.IO) {
        // 1. Restore product stock
        if (tx.type == "SALE" && tx.productId > 0) {
            productDao.increaseStock(tx.productId, tx.quantity)
        } else if (tx.type == "STOCK_IN" && tx.productId > 0) {
            productDao.decreaseStock(tx.productId, tx.quantity)
        } else if (tx.type == "STOCK_OUT_DAMAGE" && tx.productId > 0) {
            productDao.increaseStock(tx.productId, tx.quantity)
        }

        // 2. Adjust customer due if this transaction had due
        if (tx.dueAmount > 0) {
            val customer = customerDao.findExistingCustomer(tx.customerName, tx.customerPhone)
            if (customer != null) {
                val updatedDue = round2((customer.totalDue - tx.dueAmount).coerceAtLeast(0.0))
                customerDao.setCustomerDue(customer.id, updatedDue, System.currentTimeMillis())
            }
            // Remove associated DueLog if matching invoice
            if (tx.invoiceNumber.isNotBlank()) {
                dueLogDao.deleteDueLogsByNotePattern("%${tx.invoiceNumber}%")
            }
        }

        transactionDao.deleteTransaction(tx)
    }

    suspend fun returnSaleItem(tx: TransactionRecord, returnQty: Double, note: String) = withContext(Dispatchers.IO) {
        val qtyToReturn = round2(returnQty.coerceIn(0.1, tx.quantity))
        val now = System.currentTimeMillis()

        // 1. Restock product into inventory
        if (tx.productId > 0) {
            productDao.increaseStock(tx.productId, qtyToReturn)
        }

        val returnedAmount = round2(tx.unitPrice * qtyToReturn)

        // 2. Adjust customer due if transaction had due
        if (tx.dueAmount > 0) {
            val customer = customerDao.findExistingCustomer(tx.customerName, tx.customerPhone)
            if (customer != null) {
                val dueReduction = round2(returnedAmount.coerceAtMost(tx.dueAmount))
                val updatedDue = round2((customer.totalDue - dueReduction).coerceAtLeast(0.0))
                customerDao.setCustomerDue(customer.id, updatedDue, now)

                dueLogDao.insertDueLog(
                    DueLog(
                        customerId = customer.id,
                        customerName = customer.name,
                        customerPhone = customer.phone,
                        type = "DUE_COLLECTED",
                        amount = dueReduction,
                        note = "পণ্য ফেরত: ${tx.productName} (${qtyToReturn.formatQty()} ${tx.unit}) • ${note.ifBlank { "মেমো #${tx.invoiceNumber}" }}",
                        timestamp = now
                    )
                )
            }
        }

        // 3. Update or delete transaction record
        if (qtyToReturn >= tx.quantity) {
            transactionDao.deleteTransaction(tx)
        } else {
            val remainingQty = round2(tx.quantity - qtyToReturn)
            val remainingTotal = round2((tx.unitPrice * remainingQty).coerceAtLeast(0.0))
            val remainingCost = round2(tx.costPrice * remainingQty)
            val remainingProfit = round2(remainingTotal - remainingCost)
            val remainingPaid = round2((tx.paidAmount - returnedAmount).coerceAtLeast(0.0))
            val remainingDue = round2((tx.dueAmount - returnedAmount).coerceAtLeast(0.0))

            transactionDao.updateTransaction(
                tx.copy(
                    quantity = remainingQty,
                    totalAmount = remainingTotal,
                    profitAmount = remainingProfit,
                    paidAmount = remainingPaid,
                    dueAmount = remainingDue,
                    note = "${tx.note} (ফেরত: $qtyToReturn ${tx.unit})".trim()
                )
            )
        }
    }

    suspend fun editSaleTransaction(
        oldTx: TransactionRecord,
        newQuantity: Double,
        newUnitPrice: Double,
        newCustomerName: String,
        newNote: String
    ) = editSaleTransaction(
        oldTx = oldTx,
        newQuantity = newQuantity,
        newUnitPrice = newUnitPrice,
        newPaidAmount = if (oldTx.dueAmount == 0.0) round2(newQuantity * newUnitPrice) else oldTx.paidAmount,
        newCustomerName = newCustomerName,
        newCustomerPhone = oldTx.customerPhone,
        newNote = newNote
    )

    suspend fun editSaleTransaction(
        oldTx: TransactionRecord,
        newQuantity: Double,
        newUnitPrice: Double,
        newPaidAmount: Double,
        newCustomerName: String,
        newCustomerPhone: String,
        newNote: String
    ) = withContext(Dispatchers.IO) {
        val cleanQty = round2(newQuantity.coerceAtLeast(0.01))
        val cleanPrice = round2(newUnitPrice.coerceAtLeast(0.0))
        val isSale = oldTx.type.equals("SALE", ignoreCase = true)
        val isStockIn = oldTx.type.equals("STOCK_IN", ignoreCase = true) || oldTx.type.equals("PURCHASE", ignoreCase = true)

        // 1. Stock adjustment
        val qtyDiff = round2(oldTx.quantity - cleanQty)
        if (oldTx.productId > 0 && qtyDiff != 0.0) {
            if (isSale) {
                productDao.increaseStock(oldTx.productId, qtyDiff) // if cleanQty < oldTx.quantity, qtyDiff > 0 (return to stock)
            } else if (isStockIn) {
                productDao.decreaseStock(oldTx.productId, qtyDiff) // if cleanQty < oldTx.quantity, qtyDiff > 0 (decrease stock)
            }
        }

        // 2. Financial calculation
        val newTotal = round2(cleanQty * cleanPrice)
        val newCost = round2(oldTx.costPrice * cleanQty)
        val newProfit = round2(newTotal - newCost)
        val cleanPaid = round2(newPaidAmount.coerceIn(0.0, newTotal))
        val newDue = round2((newTotal - cleanPaid).coerceAtLeast(0.0))
        val finalCustName = newCustomerName.trim().ifBlank { oldTx.customerName }
        val finalCustPhone = newCustomerPhone.trim().ifBlank { oldTx.customerPhone }

        // 3. Customer ledger & due adjustment if this was a customer transaction
        val dueDiff = round2(newDue - oldTx.dueAmount)
        if (dueDiff != 0.0 && finalCustName.isNotBlank() && finalCustName != "ক্যাশ কাস্টমার") {
            val customer = customerDao.findExistingCustomer(finalCustName, finalCustPhone)
                ?: customerDao.findExistingCustomer(oldTx.customerName, oldTx.customerPhone)
            if (customer != null) {
                val updatedDue = round2((customer.totalDue + dueDiff).coerceAtLeast(0.0))
                customerDao.setCustomerDue(customer.id, updatedDue, System.currentTimeMillis())
            }
        }

        // 4. Update transaction
        transactionDao.updateTransaction(
            oldTx.copy(
                quantity = cleanQty,
                unitPrice = cleanPrice,
                totalAmount = newTotal,
                profitAmount = if (isSale) newProfit else 0.0,
                paidAmount = if (isSale) cleanPaid else newTotal,
                dueAmount = if (isSale) newDue else 0.0,
                customerName = finalCustName,
                customerPhone = finalCustPhone,
                note = newNote
            )
        )
    }

    suspend fun updateCashLog(cashLog: CashLog) = withContext(Dispatchers.IO) {
        cashLogDao.updateCashLog(
            cashLog.copy(
                amount = round2(cashLog.amount),
                balanceAfter = round2(cashLog.balanceAfter)
            )
        )
    }

    suspend fun deleteCashLog(cashLog: CashLog) = withContext(Dispatchers.IO) {
        cashLogDao.deleteCashLog(cashLog)
    }

    suspend fun updateExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense.copy(amount = round2(expense.amount)))
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun addExpense(title: String, category: String, amount: Double, note: String) = withContext(Dispatchers.IO) {
        val cleanAmount = round2(amount)
        expenseDao.insertExpense(
            Expense(
                title = title,
                category = category,
                amount = cleanAmount,
                timestamp = System.currentTimeMillis(),
                note = note
            )
        )
    }

    suspend fun deleteExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun recordCashLog(
        type: String,
        amount: Double,
        balanceAfter: Double,
        note: String,
        timestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        cashLogDao.insertCashLog(
            CashLog(
                type = type,
                amount = amount,
                balanceAfter = balanceAfter,
                note = note,
                timestamp = timestamp
            )
        )
    }

    suspend fun exportDataAsJson(
        productsList: List<Product>,
        customersList: List<Customer>,
        expensesList: List<Expense>,
        transactionsList: List<TransactionRecord>,
        dueLogsList: List<DueLog> = emptyList(),
        cashLogsList: List<CashLog> = emptyList(),
        shopInfo: ShopInfo? = null
    ): String = withContext(Dispatchers.Default) {
        val root = JSONObject()
        root.put("appName", "ShopKhata")
        root.put("version", "2.0")
        root.put("exportTime", System.currentTimeMillis())

        shopInfo?.let { s ->
            val sObj = JSONObject().apply {
                put("shopName", s.shopName)
                put("ownerName", s.ownerName)
                put("phone", s.phone)
                put("address", s.address)
                put("currency", s.currency)
                put("mainBalance", s.mainBalance)
                put("userEmail", s.userEmail)
            }
            root.put("shopInfo", sObj)
            root.put("mainBalance", s.mainBalance)
            root.put("cashBalance", s.mainBalance)
        } ?: run {
            root.put("mainBalance", 0.0)
            root.put("cashBalance", 0.0)
        }

        val pArray = JSONArray()
        productsList.forEach { p ->
            val o = JSONObject()
            o.put("name", p.name)
            o.put("barcode", p.barcode)
            o.put("category", p.category)
            o.put("buyPrice", p.buyPrice)
            o.put("sellPrice", p.sellPrice)
            o.put("stockQuantity", p.stockQuantity)
            o.put("unit", p.unit)
            o.put("minStockAlert", p.minStockAlert)
            o.put("imageUri", p.imageUri)
            o.put("expiryDate", p.expiryDate)
            o.put("createdAt", p.createdAt)
            pArray.put(o)
        }
        root.put("products", pArray)

        val cArray = JSONArray()
        customersList.forEach { c ->
            val o = JSONObject()
            o.put("name", c.name)
            o.put("phone", c.phone)
            o.put("address", c.address)
            o.put("totalDue", c.totalDue)
            o.put("totalPurchased", c.totalPurchased)
            o.put("imageUri", c.imageUri)
            o.put("lastTransactionDate", c.lastTransactionDate)
            cArray.put(o)
        }
        root.put("customers", cArray)

        val dArray = JSONArray()
        dueLogsList.forEach { d ->
            val o = JSONObject()
            o.put("customerId", d.customerId)
            o.put("customerName", d.customerName)
            o.put("customerPhone", d.customerPhone)
            o.put("type", d.type)
            o.put("amount", d.amount)
            o.put("note", d.note)
            o.put("timestamp", d.timestamp)
            dArray.put(o)
        }
        root.put("dueLogs", dArray)

        val eArray = JSONArray()
        expensesList.forEach { e ->
            val o = JSONObject()
            o.put("title", e.title)
            o.put("category", e.category)
            o.put("amount", e.amount)
            o.put("timestamp", e.timestamp)
            o.put("note", e.note)
            eArray.put(o)
        }
        root.put("expenses", eArray)

        val tArray = JSONArray()
        transactionsList.forEach { t ->
            val o = JSONObject()
            o.put("type", t.type)
            o.put("invoiceNumber", t.invoiceNumber)
            o.put("productId", t.productId)
            o.put("productName", t.productName)
            o.put("quantity", t.quantity)
            o.put("unit", t.unit)
            o.put("unitPrice", t.unitPrice)
            o.put("costPrice", t.costPrice)
            o.put("totalAmount", t.totalAmount)
            o.put("profitAmount", t.profitAmount)
            o.put("customerName", t.customerName)
            o.put("customerPhone", t.customerPhone)
            o.put("paidAmount", t.paidAmount)
            o.put("dueAmount", t.dueAmount)
            o.put("paymentMethod", t.paymentMethod)
            o.put("note", t.note)
            o.put("timestamp", t.timestamp)
            tArray.put(o)
        }
        root.put("transactions", tArray)

        val clArray = JSONArray()
        cashLogsList.forEach { cl ->
            val o = JSONObject()
            o.put("type", cl.type)
            o.put("amount", cl.amount)
            o.put("balanceAfter", cl.balanceAfter)
            o.put("note", cl.note)
            o.put("timestamp", cl.timestamp)
            clArray.put(o)
        }
        root.put("cashLogs", clArray)

        root.toString(2)
    }

    private fun extractJsonObjects(root: JSONObject, vararg candidateKeys: String): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        for (k in candidateKeys) {
            if (root.has(k)) {
                val arrayVal = root.optJSONArray(k)
                if (arrayVal != null) {
                    for (i in 0 until arrayVal.length()) {
                        val obj = arrayVal.optJSONObject(i)
                        if (obj != null) result.add(obj)
                    }
                    if (result.isNotEmpty()) return result
                }

                val objVal = root.optJSONObject(k)
                if (objVal != null) {
                    val keys = objVal.keys()
                    while (keys.hasNext()) {
                        val subKey = keys.next()
                        val subObj = objVal.optJSONObject(subKey)
                        if (subObj != null) result.add(subObj)
                    }
                    if (result.isNotEmpty()) return result
                }
            }
        }
        return result
    }

    suspend fun importDataFromJson(jsonStr: String, cleanSlate: Boolean = true): RestoreResult = withContext(Dispatchers.IO) {
        try {
            if (jsonStr.isBlank() || jsonStr.trim() == "null" || jsonStr.trim() == "{}") {
                return@withContext RestoreResult(
                    success = false,
                    message = "রিস্টোর করার মতো কোনো ডাটা পাওয়া যায়নি।"
                )
            }

            var root: JSONObject
            try {
                root = JSONObject(jsonStr.trim())
            } catch (e: Exception) {
                // If wrapped in quotes or escaped JSON
                val unescaped = if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                    jsonStr.substring(1, jsonStr.length - 1).replace("\\\"", "\"").replace("\\\\", "\\")
                } else jsonStr
                root = JSONObject(unescaped)
            }

            // Unpack nested root wrappers
            var unwrapped = true
            while (unwrapped) {
                unwrapped = false
                for (wrapperKey in listOf("backup", "data", "records", "shopData", "dataMap", "payload")) {
                    if (root.has(wrapperKey)) {
                        val subObj = root.optJSONObject(wrapperKey)
                        if (subObj != null) {
                            root = subObj
                            unwrapped = true
                            break
                        }
                        val subStr = root.optString(wrapperKey)
                        if (subStr.isNotBlank() && subStr.startsWith("{")) {
                            try {
                                root = JSONObject(subStr)
                                unwrapped = true
                                break
                            } catch (e: Exception) {}
                        }
                    }
                }
            }

            // Also check if root contains a single top-level user key (like "nafitv24_at_gmail_dot_com")
            if (!root.has("products") && !root.has("transactions") && !root.has("customers")) {
                val topKeys = root.keys()
                while (topKeys.hasNext()) {
                    val k = topKeys.next()
                    val childObj = root.optJSONObject(k)
                    if (childObj != null && (childObj.has("products") || childObj.has("transactions") || childObj.has("customers") || childObj.has("backup") || childObj.has("data"))) {
                        if (childObj.has("backup") && childObj.optJSONObject("backup") != null) {
                            root = childObj.getJSONObject("backup")
                        } else if (childObj.has("data") && childObj.optJSONObject("data") != null) {
                            root = childObj.getJSONObject("data")
                        } else {
                            root = childObj
                        }
                        break
                    }
                }
            }

            // Parse shop info
            var restoredShopInfo: ShopInfo? = null
            val rootCash = if (root.has("mainBalance")) root.optDouble("mainBalance", 0.0)
            else if (root.has("cashBalance")) root.optDouble("cashBalance", 0.0)
            else root.optDouble("cash_balance", 0.0)

            val shopInfoObj = root.optJSONObject("shopInfo") ?: root.optJSONObject("shop_info") ?: root.optJSONObject("shop")
            if (shopInfoObj != null) {
                val finalCash = if (shopInfoObj.has("mainBalance")) shopInfoObj.optDouble("mainBalance", rootCash)
                else if (shopInfoObj.has("cashBalance")) shopInfoObj.optDouble("cashBalance", rootCash)
                else rootCash

                restoredShopInfo = ShopInfo(
                    shopName = shopInfoObj.optString("shopName").ifBlank { shopInfoObj.optString("shop_name", "আমার দোকান") },
                    ownerName = shopInfoObj.optString("ownerName").ifBlank { shopInfoObj.optString("owner_name", "দোকানদার") },
                    phone = shopInfoObj.optString("phone").ifBlank { shopInfoObj.optString("mobile", "") },
                    address = shopInfoObj.optString("address", ""),
                    currency = shopInfoObj.optString("currency", "৳"),
                    mainBalance = finalCash,
                    userEmail = shopInfoObj.optString("userEmail").ifBlank { shopInfoObj.optString("user_email", "") },
                    isGoogleLinked = (shopInfoObj.optString("userEmail").ifBlank { shopInfoObj.optString("user_email", "") }).isNotBlank()
                )
            } else if (root.has("mainBalance") || root.has("cashBalance") || root.has("cash_balance")) {
                restoredShopInfo = ShopInfo(
                    mainBalance = rootCash
                )
            }

            // 1. Extract Products
            val productObjs = extractJsonObjects(root, "products", "productList", "product_list", "items", "itemList", "item_list", "stock", "inventory")
            val products = mutableListOf<Product>()
            for (o in productObjs) {
                try {
                    val name = o.optString("name").ifBlank { o.optString("productName").ifBlank { o.optString("product_name").ifBlank { o.optString("title", "পণ্য") } } }
                    val barcode = o.optString("barcode").ifBlank { o.optString("bar_code", "") }
                    val category = o.optString("category").ifBlank { o.optString("categoryName").ifBlank { o.optString("category_name", "সাধারণ") } }
                    val buyPrice = if (o.has("buyPrice")) o.optDouble("buyPrice") else if (o.has("buy_price")) o.optDouble("buy_price") else if (o.has("purchasePrice")) o.optDouble("purchasePrice") else if (o.has("purchase_price")) o.optDouble("purchase_price") else if (o.has("costPrice")) o.optDouble("costPrice") else o.optDouble("cost_price", 0.0)
                    val sellPrice = if (o.has("sellPrice")) o.optDouble("sellPrice") else if (o.has("sell_price")) o.optDouble("sell_price") else if (o.has("salePrice")) o.optDouble("salePrice") else if (o.has("sale_price")) o.optDouble("sale_price") else if (o.has("price")) o.optDouble("price", 0.0) else o.optDouble("rate", 0.0)
                    val stockQty = if (o.has("stockQuantity")) o.optDouble("stockQuantity") else if (o.has("stock_quantity")) o.optDouble("stock_quantity") else if (o.has("stock")) o.optDouble("stock") else if (o.has("quantity")) o.optDouble("quantity") else o.optDouble("qty", 0.0)
                    val unit = o.optString("unit").ifBlank { o.optString("unit_name", "পিস") }
                    val minAlert = if (o.has("minStockAlert")) o.optDouble("minStockAlert") else if (o.has("min_stock_alert")) o.optDouble("min_stock_alert") else o.optDouble("minStock", 5.0)
                    val image = o.optString("imageUri").ifBlank { o.optString("image_uri").ifBlank { o.optString("image", "") } }
                    val expiry = if (o.has("expiryDate")) o.optLong("expiryDate") else if (o.has("expiry_date")) o.optLong("expiry_date") else o.optLong("expireDate", 0L)
                    val created = if (o.has("createdAt")) o.optLong("createdAt") else if (o.has("created_at")) o.optLong("created_at") else o.optLong("timestamp", System.currentTimeMillis())

                    products.add(
                        Product(
                            name = name,
                            barcode = barcode,
                            category = category,
                            buyPrice = round2(buyPrice),
                            sellPrice = round2(sellPrice),
                            stockQuantity = round2(stockQty),
                            unit = unit,
                            minStockAlert = minAlert,
                            imageUri = image,
                            expiryDate = expiry,
                            createdAt = created
                        )
                    )
                } catch (e: Exception) {}
            }

            // 2. Extract Customers
            val customerObjs = extractJsonObjects(root, "customers", "customerList", "customer_list", "clients", "buyers")
            val customers = mutableListOf<Customer>()
            for (o in customerObjs) {
                try {
                    val name = o.optString("name").ifBlank { o.optString("customerName").ifBlank { o.optString("customer_name", "কাস্টমার") } }
                    val phone = o.optString("phone").ifBlank { o.optString("customerPhone").ifBlank { o.optString("mobile", "") } }
                    val address = o.optString("address", "")
                    val totalDue = if (o.has("totalDue")) o.optDouble("totalDue") else if (o.has("total_due")) o.optDouble("total_due") else if (o.has("due")) o.optDouble("due") else o.optDouble("dueAmount", 0.0)
                    val totalPurchased = if (o.has("totalPurchased")) o.optDouble("totalPurchased") else if (o.has("total_purchased")) o.optDouble("total_purchased") else if (o.has("total_buy")) o.optDouble("total_buy") else o.optDouble("totalPurchase", 0.0)
                    val image = o.optString("imageUri").ifBlank { o.optString("image_uri", "") }
                    val lastTx = if (o.has("lastTransactionDate")) o.optLong("lastTransactionDate") else if (o.has("last_transaction_date")) o.optLong("last_transaction_date") else o.optLong("timestamp", System.currentTimeMillis())

                    customers.add(
                        Customer(
                            name = name,
                            phone = phone,
                            address = address,
                            totalDue = round2(totalDue),
                            totalPurchased = round2(totalPurchased),
                            imageUri = image,
                            lastTransactionDate = lastTx
                        )
                    )
                } catch (e: Exception) {}
            }

            // 3. Extract Due Logs
            val dueObjs = extractJsonObjects(root, "dueLogs", "due_logs", "dueLogList", "due_list", "dues", "customerDueLogs")
            val dueLogs = mutableListOf<DueLog>()
            for (o in dueObjs) {
                try {
                    val cId = if (o.has("customerId")) o.optLong("customerId") else o.optLong("customer_id", 0L)
                    val cName = o.optString("customerName").ifBlank { o.optString("customer_name", "") }
                    val cPhone = o.optString("customerPhone").ifBlank { o.optString("customer_phone", "") }
                    val type = o.optString("type").ifBlank { o.optString("due_type", "DUE_GIVEN") }
                    val amount = if (o.has("amount")) o.optDouble("amount") else if (o.has("due_amount")) o.optDouble("due_amount") else o.optDouble("total", 0.0)
                    val note = o.optString("note", "")
                    val time = if (o.has("timestamp")) o.optLong("timestamp") else if (o.has("date")) o.optLong("date") else System.currentTimeMillis()

                    dueLogs.add(
                        DueLog(
                            customerId = cId,
                            customerName = cName,
                            customerPhone = cPhone,
                            type = type,
                            amount = round2(amount),
                            note = note,
                            timestamp = time
                        )
                    )
                } catch (e: Exception) {}
            }

            // 4. Extract Expenses
            val expenseObjs = extractJsonObjects(root, "expenses", "expense_list", "expenseList", "costs", "costList")
            val expenses = mutableListOf<Expense>()
            for (o in expenseObjs) {
                try {
                    val title = o.optString("title").ifBlank { o.optString("name", "খরচ") }
                    val category = o.optString("category", "অন্যান্য")
                    val amount = if (o.has("amount")) o.optDouble("amount") else o.optDouble("cost", 0.0)
                    val note = o.optString("note", "")
                    val time = if (o.has("timestamp")) o.optLong("timestamp") else System.currentTimeMillis()

                    expenses.add(
                        Expense(
                            title = title,
                            category = category,
                            amount = round2(amount),
                            timestamp = time,
                            note = note
                        )
                    )
                } catch (e: Exception) {}
            }

            // 5. Extract Transactions
            val txObjs = extractJsonObjects(root, "transactions", "transactionList", "transaction_list", "sales", "saleList", "sale_list", "orders", "orderList")
            val transactions = mutableListOf<TransactionRecord>()
            for (o in txObjs) {
                try {
                    val rawType = o.optString("type").ifBlank { o.optString("transactionType", "SALE") }
                    val type = if (rawType.equals("STOCK_IN", ignoreCase = true) || rawType.equals("PURCHASE", ignoreCase = true)) "STOCK_IN"
                    else if (rawType.equals("STOCK_OUT_DAMAGE", ignoreCase = true) || rawType.equals("DAMAGE", ignoreCase = true)) "STOCK_OUT_DAMAGE"
                    else "SALE"

                    val invoice = o.optString("invoiceNumber").ifBlank { o.optString("invoice_number").ifBlank { o.optString("invoice").ifBlank { o.optString("memoNo", "") } } }
                    val pId = if (o.has("productId")) o.optLong("productId") else o.optLong("product_id", 0L)
                    val pName = o.optString("productName").ifBlank { o.optString("product_name").ifBlank { o.optString("name").ifBlank { o.optString("title", "পণ্য") } } }
                    val qty = if (o.has("quantity")) o.optDouble("quantity") else if (o.has("qty")) o.optDouble("qty") else o.optDouble("count", 1.0)
                    val unit = o.optString("unit").ifBlank { o.optString("unit_name", "পিস") }
                    val unitPrice = if (o.has("unitPrice")) o.optDouble("unitPrice") else if (o.has("unit_price")) o.optDouble("unit_price") else if (o.has("rate")) o.optDouble("rate") else o.optDouble("price", 0.0)
                    val costPrice = if (o.has("costPrice")) o.optDouble("costPrice") else if (o.has("cost_price")) o.optDouble("cost_price") else if (o.has("buyPrice")) o.optDouble("buyPrice") else o.optDouble("buy_price", 0.0)
                    val totalAmount = if (o.has("totalAmount")) o.optDouble("totalAmount") else if (o.has("total_amount")) o.optDouble("total_amount") else if (o.has("total")) o.optDouble("total") else round2(qty * unitPrice)
                    val profitAmount = if (o.has("profitAmount")) o.optDouble("profitAmount") else if (o.has("profit_amount")) o.optDouble("profit_amount") else if (o.has("profit")) o.optDouble("profit") else round2(totalAmount - (costPrice * qty))
                    val cName = o.optString("customerName").ifBlank { o.optString("customer_name", "") }
                    val cPhone = o.optString("customerPhone").ifBlank { o.optString("customer_phone").ifBlank { o.optString("phone", "") } }
                    val paidAmount = if (o.has("paidAmount")) o.optDouble("paidAmount") else if (o.has("paid_amount")) o.optDouble("paid_amount") else if (o.has("paid")) o.optDouble("paid") else totalAmount
                    val dueAmount = if (o.has("dueAmount")) o.optDouble("dueAmount") else if (o.has("due_amount")) o.optDouble("due_amount") else if (o.has("due")) o.optDouble("due") else (totalAmount - paidAmount).coerceAtLeast(0.0)
                    val paymentMethod = o.optString("paymentMethod").ifBlank { o.optString("payment_method").ifBlank { o.optString("paymentType", "CASH") } }
                    val note = o.optString("note").ifBlank { o.optString("remarks", "") }
                    val time = if (o.has("timestamp")) o.optLong("timestamp") else if (o.has("date")) o.optLong("date") else if (o.has("createdAt")) o.optLong("createdAt") else System.currentTimeMillis()

                    transactions.add(
                        TransactionRecord(
                            type = type,
                            invoiceNumber = invoice,
                            productId = pId,
                            productName = pName,
                            quantity = round2(qty),
                            unit = unit,
                            unitPrice = round2(unitPrice),
                            costPrice = round2(costPrice),
                            totalAmount = round2(totalAmount),
                            profitAmount = round2(profitAmount),
                            customerName = cName,
                            customerPhone = cPhone,
                            paidAmount = round2(paidAmount),
                            dueAmount = round2(dueAmount),
                            paymentMethod = paymentMethod,
                            note = note,
                            timestamp = time
                        )
                    )
                } catch (e: Exception) {}
            }

            // 6. Extract Cash Logs
            val clObjs = extractJsonObjects(root, "cashLogs", "cash_logs", "cashLogList", "closings")
            val cashLogs = mutableListOf<CashLog>()
            for (o in clObjs) {
                try {
                    val type = o.optString("type", "DAY_END_CLOSING")
                    val amount = if (o.has("amount")) o.optDouble("amount") else 0.0
                    val bal = if (o.has("balanceAfter")) o.optDouble("balanceAfter") else if (o.has("balance_after")) o.optDouble("balance_after") else 0.0
                    val note = o.optString("note", "")
                    val time = if (o.has("timestamp")) o.optLong("timestamp") else System.currentTimeMillis()

                    cashLogs.add(
                        CashLog(
                            type = type,
                            amount = round2(amount),
                            balanceAfter = round2(bal),
                            note = note,
                            timestamp = time
                        )
                    )
                } catch (e: Exception) {}
            }

            val totalItemsFound = products.size + customers.size + transactions.size + expenses.size + dueLogs.size + cashLogs.size

            if (totalItemsFound == 0 && restoredShopInfo == null) {
                return@withContext RestoreResult(
                    success = false,
                    message = "ফাইলে কোনো পণ্য, কাস্টমার বা লেনদেনের তথ্য পাওয়া যায়নি।"
                )
            }

            // Now safely write to database
            if (cleanSlate) {
                if (products.isNotEmpty()) productDao.clearAll()
                if (customers.isNotEmpty()) customerDao.clearAll()
                if (dueLogs.isNotEmpty()) dueLogDao.clearAll()
                if (expenses.isNotEmpty()) expenseDao.clearAll()
                if (transactions.isNotEmpty()) transactionDao.clearAll()
                if (cashLogs.isNotEmpty()) cashLogDao.clearAll()
            }

            var pCount = 0
            var cCount = 0
            var tCount = 0
            var eCount = 0
            var dCount = 0
            var clCount = 0

            if (products.isNotEmpty()) {
                productDao.insertAll(products)
                pCount = products.size
            }
            if (customers.isNotEmpty()) {
                customerDao.insertAll(customers)
                cCount = customers.size
            }
            if (dueLogs.isNotEmpty()) {
                dueLogDao.insertAll(dueLogs)
                dCount = dueLogs.size
            }
            if (expenses.isNotEmpty()) {
                expenseDao.insertAll(expenses)
                eCount = expenses.size
            }
            if (transactions.isNotEmpty()) {
                transactionDao.insertAllTransactions(transactions)
                tCount = transactions.size
            }
            if (cashLogs.isNotEmpty()) {
                cashLogDao.insertAll(cashLogs)
                clCount = cashLogs.size
            }

            RestoreResult(
                success = true,
                message = "সফলভাবে রিস্টোর হয়েছে!",
                productCount = pCount,
                customerCount = cCount,
                transactionCount = tCount,
                expenseCount = eCount,
                dueLogCount = dCount,
                cashLogCount = clCount,
                restoredShopInfo = restoredShopInfo
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(
                success = false,
                message = "রিস্টোর ত্রুটি: ${e.localizedMessage ?: "ফাইলের তথ্য প্রক্রিয়াকরণে সমস্যা"}"
            )
        }
    }
}
