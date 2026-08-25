package com.example.data.repository

import com.example.data.local.*
import com.example.data.model.*
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

    suspend fun saveProduct(product: Product) = withContext(Dispatchers.IO) {
        if (product.id == 0L) {
            productDao.insertProduct(product)
        } else {
            productDao.updateProduct(product)
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
        val updatedBuy = buyPrice ?: existing.buyPrice
        val updatedSell = sellPrice ?: existing.sellPrice
        val updatedProduct = existing.copy(
            stockQuantity = existing.stockQuantity + quantity,
            buyPrice = updatedBuy,
            sellPrice = updatedSell
        )
        productDao.updateProduct(updatedProduct)

        val tx = TransactionRecord(
            type = "STOCK_IN",
            invoiceNumber = "STK-${System.currentTimeMillis() % 100000}",
            productId = productId,
            productName = existing.name,
            quantity = quantity,
            unit = existing.unit,
            unitPrice = updatedBuy,
            costPrice = updatedBuy,
            totalAmount = quantity * updatedBuy,
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
        val newStock = (existing.stockQuantity - quantity).coerceAtLeast(0.0)
        productDao.updateStock(productId, newStock)

        val tx = TransactionRecord(
            type = "STOCK_OUT_DAMAGE",
            invoiceNumber = "DMG-${System.currentTimeMillis() % 100000}",
            productId = productId,
            productName = existing.name,
            quantity = quantity,
            unit = existing.unit,
            unitPrice = existing.buyPrice,
            costPrice = existing.buyPrice,
            totalAmount = quantity * existing.buyPrice,
            profitAmount = -(quantity * existing.buyPrice),
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
                val totalCombinedDue = group.sumOf { it.totalDue }
                val totalCombinedPurchased = group.sumOf { it.totalPurchased }
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
    }

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
            grossTotal += item.total
        }
        val netTotal = (grossTotal - discount).coerceAtLeast(0.0)
        val calculatedDue = (netTotal - paidAmount).coerceAtLeast(0.0)

        // 1. Process each cart item & decrease stock
        for (item in cartItems) {
            val product = productDao.getProductById(item.product.id) ?: item.product
            productDao.decreaseStock(product.id, item.quantity)

            val itemRatio = if (grossTotal > 0) item.total / grossTotal else 0.0
            val itemDiscount = discount * itemRatio
            val finalItemTotal = item.total - itemDiscount
            val itemCost = product.buyPrice * item.quantity
            val itemProfit = finalItemTotal - itemCost

            val tx = TransactionRecord(
                type = "SALE",
                invoiceNumber = invoiceNumber,
                productId = product.id,
                productName = product.name,
                quantity = item.quantity,
                unit = product.unit,
                unitPrice = item.customPrice,
                costPrice = product.buyPrice,
                totalAmount = finalItemTotal,
                profitAmount = itemProfit,
                customerName = customerName.ifBlank { "ক্যাশ কাস্টমার" },
                customerPhone = customerPhone,
                paidAmount = paidAmount * (if (netTotal > 0) finalItemTotal / netTotal else 1.0),
                dueAmount = calculatedDue * (if (netTotal > 0) finalItemTotal / netTotal else 1.0),
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
                dueLogDao.insertDueLog(
                    DueLog(
                        customerId = existingCustomer.id,
                        customerName = existingCustomer.name,
                        customerPhone = existingCustomer.phone,
                        type = "DUE_GIVEN",
                        amount = calculatedDue,
                        note = "মেমো #$invoiceNumber বাবদ বাকি",
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

        // Check if customer with this name or phone already exists
        val existing = customerDao.findExistingCustomer(cleanName, cleanPhone)
        if (existing != null) {
            val updatedDue = existing.totalDue + initialDue
            val updatedPurchased = existing.totalPurchased + initialDue
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
            if (initialDue > 0) {
                dueLogDao.insertDueLog(
                    DueLog(
                        customerId = existing.id,
                        customerName = existing.name,
                        customerPhone = existing.phone,
                        type = "DUE_GIVEN",
                        amount = initialDue,
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
                totalDue = initialDue,
                totalPurchased = initialDue,
                imageUri = imageUri,
                lastTransactionDate = now
            )
        )
        if (initialDue > 0) {
            dueLogDao.insertDueLog(
                DueLog(
                    customerId = customerId,
                    customerName = cleanName,
                    customerPhone = cleanPhone,
                    type = "DUE_GIVEN",
                    amount = initialDue,
                    note = "পূর্বের বাকি হিসাব শুরু",
                    timestamp = now
                )
            )
        }
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer)
    }

    suspend fun collectCustomerDuePayment(customer: Customer, amountPaid: Double, note: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updatedDue = (customer.totalDue - amountPaid).coerceAtLeast(0.0)
        customerDao.setCustomerDue(customer.id, updatedDue, now)

        dueLogDao.insertDueLog(
            DueLog(
                customerId = customer.id,
                customerName = customer.name,
                customerPhone = customer.phone,
                type = "DUE_COLLECTED",
                amount = amountPaid,
                note = note.ifBlank { "বাকি আদায়" },
                timestamp = now
            )
        )
    }

    suspend fun giveCustomerAdditionalDue(customer: Customer, amountDue: Double, note: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val updatedDue = customer.totalDue + amountDue
        val updatedPurchased = customer.totalPurchased + amountDue
        customerDao.updateCustomerBalance(customer.id, amountDue, amountDue, now)

        dueLogDao.insertDueLog(
            DueLog(
                customerId = customer.id,
                customerName = customer.name,
                customerPhone = customer.phone,
                type = "DUE_GIVEN",
                amount = amountDue,
                note = note.ifBlank { "বাকি প্রদান" },
                timestamp = now
            )
        )
    }

    suspend fun updateDueLog(dueLog: DueLog) = withContext(Dispatchers.IO) {
        dueLogDao.updateDueLog(dueLog)
    }

    suspend fun deleteDueLog(dueLog: DueLog) = withContext(Dispatchers.IO) {
        // 1. Revert customer balance
        val customer = customerDao.getCustomerById(dueLog.customerId)
            ?: customerDao.findExistingCustomer(dueLog.customerName, dueLog.customerPhone)

        if (customer != null) {
            val updatedDue = if (dueLog.type == "DUE_GIVEN") {
                (customer.totalDue - dueLog.amount).coerceAtLeast(0.0)
            } else {
                customer.totalDue + dueLog.amount
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
        transactionDao.updateTransaction(tx)
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
                val updatedDue = (customer.totalDue - tx.dueAmount).coerceAtLeast(0.0)
                customerDao.setCustomerDue(customer.id, updatedDue, System.currentTimeMillis())
            }
            // Remove associated DueLog if matching invoice
            if (tx.invoiceNumber.isNotBlank()) {
                dueLogDao.deleteDueLogsByNotePattern("%${tx.invoiceNumber}%")
            }
        }

        transactionDao.deleteTransaction(tx)
    }

    suspend fun updateExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.deleteCustomer(customer)
    }

    suspend fun addExpense(title: String, category: String, amount: Double, note: String) = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(
            Expense(
                title = title,
                category = category,
                amount = amount,
                timestamp = System.currentTimeMillis(),
                note = note
            )
        )
    }

    suspend fun deleteExpense(expense: Expense) = withContext(Dispatchers.IO) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun recordCashLog(type: String, amount: Double, balanceAfter: Double, note: String) = withContext(Dispatchers.IO) {
        cashLogDao.insertCashLog(
            CashLog(
                type = type,
                amount = amount,
                balanceAfter = balanceAfter,
                note = note,
                timestamp = System.currentTimeMillis()
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

    suspend fun importDataFromJson(jsonStr: String, cleanSlate: Boolean = true): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            var pCount = 0
            var cCount = 0
            var tCount = 0
            var eCount = 0
            var dCount = 0
            var clCount = 0

            var restoredShopInfo: ShopInfo? = null
            val rootCash = if (root.has("mainBalance")) root.optDouble("mainBalance", 0.0) else root.optDouble("cashBalance", 0.0)
            if (root.has("shopInfo")) {
                val sObj = root.getJSONObject("shopInfo")
                val finalCash = if (sObj.has("mainBalance")) sObj.optDouble("mainBalance", rootCash) else rootCash
                restoredShopInfo = ShopInfo(
                    shopName = sObj.optString("shopName", "আমার দোকান"),
                    ownerName = sObj.optString("ownerName", "দোকানদার"),
                    phone = sObj.optString("phone", ""),
                    address = sObj.optString("address", ""),
                    currency = sObj.optString("currency", "৳"),
                    mainBalance = finalCash,
                    userEmail = sObj.optString("userEmail", ""),
                    isGoogleLinked = sObj.optString("userEmail", "").isNotBlank()
                )
            } else if (root.has("mainBalance") || root.has("cashBalance")) {
                restoredShopInfo = ShopInfo(
                    mainBalance = rootCash
                )
            }

            if (cleanSlate) {
                productDao.clearAll()
                customerDao.clearAll()
                dueLogDao.clearAll()
                expenseDao.clearAll()
                transactionDao.clearAll()
                cashLogDao.clearAll()
            }

            if (root.has("products")) {
                val pArray = root.getJSONArray("products")
                val products = mutableListOf<Product>()
                for (i in 0 until pArray.length()) {
                    val o = pArray.getJSONObject(i)
                    products.add(
                        Product(
                            name = o.optString("name", "Product"),
                            barcode = o.optString("barcode", ""),
                            category = o.optString("category", "সাধারণ"),
                            buyPrice = o.optDouble("buyPrice", 0.0),
                            sellPrice = o.optDouble("sellPrice", 0.0),
                            stockQuantity = o.optDouble("stockQuantity", 0.0),
                            unit = o.optString("unit", "পিস"),
                            minStockAlert = o.optDouble("minStockAlert", 5.0),
                            imageUri = o.optString("imageUri", ""),
                            expiryDate = o.optLong("expiryDate", 0L),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (products.isNotEmpty()) {
                    productDao.insertAll(products)
                    pCount = products.size
                }
            }

            if (root.has("customers")) {
                val cArray = root.getJSONArray("customers")
                val customers = mutableListOf<Customer>()
                for (i in 0 until cArray.length()) {
                    val o = cArray.getJSONObject(i)
                    customers.add(
                        Customer(
                            name = o.optString("name", "Customer"),
                            phone = o.optString("phone", ""),
                            address = o.optString("address", ""),
                            totalDue = o.optDouble("totalDue", 0.0),
                            totalPurchased = o.optDouble("totalPurchased", 0.0),
                            imageUri = o.optString("imageUri", ""),
                            lastTransactionDate = o.optLong("lastTransactionDate", System.currentTimeMillis())
                        )
                    )
                }
                if (customers.isNotEmpty()) {
                    customerDao.insertAll(customers)
                    cCount = customers.size
                }
            }

            if (root.has("dueLogs")) {
                val dArray = root.getJSONArray("dueLogs")
                val dueLogs = mutableListOf<DueLog>()
                for (i in 0 until dArray.length()) {
                    val o = dArray.getJSONObject(i)
                    dueLogs.add(
                        DueLog(
                            customerId = o.optLong("customerId", 0L),
                            customerName = o.optString("customerName", ""),
                            customerPhone = o.optString("customerPhone", ""),
                            type = o.optString("type", "DUE_GIVEN"),
                            amount = o.optDouble("amount", 0.0),
                            note = o.optString("note", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (dueLogs.isNotEmpty()) {
                    dueLogDao.insertAll(dueLogs)
                    dCount = dueLogs.size
                }
            }

            if (root.has("expenses")) {
                val eArray = root.getJSONArray("expenses")
                val expenses = mutableListOf<Expense>()
                for (i in 0 until eArray.length()) {
                    val o = eArray.getJSONObject(i)
                    expenses.add(
                        Expense(
                            title = o.optString("title", "Expense"),
                            category = o.optString("category", "অন্যান্য"),
                            amount = o.optDouble("amount", 0.0),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                            note = o.optString("note", "")
                        )
                    )
                }
                if (expenses.isNotEmpty()) {
                    expenseDao.insertAll(expenses)
                    eCount = expenses.size
                }
            }

            if (root.has("transactions")) {
                val tArray = root.getJSONArray("transactions")
                val transactions = mutableListOf<TransactionRecord>()
                for (i in 0 until tArray.length()) {
                    val o = tArray.getJSONObject(i)
                    transactions.add(
                        TransactionRecord(
                            type = o.optString("type", "SALE"),
                            invoiceNumber = o.optString("invoiceNumber", ""),
                            productId = o.optLong("productId", 0L),
                            productName = o.optString("productName", ""),
                            quantity = o.optDouble("quantity", 1.0),
                            unit = o.optString("unit", "পিস"),
                            unitPrice = o.optDouble("unitPrice", 0.0),
                            costPrice = o.optDouble("costPrice", 0.0),
                            totalAmount = o.optDouble("totalAmount", 0.0),
                            profitAmount = o.optDouble("profitAmount", 0.0),
                            customerName = o.optString("customerName", ""),
                            customerPhone = o.optString("customerPhone", ""),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            dueAmount = o.optDouble("dueAmount", 0.0),
                            paymentMethod = o.optString("paymentMethod", "CASH"),
                            note = o.optString("note", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (transactions.isNotEmpty()) {
                    transactionDao.insertAllTransactions(transactions)
                    tCount = transactions.size
                }
            }

            if (root.has("cashLogs")) {
                val clArray = root.getJSONArray("cashLogs")
                val cashLogs = mutableListOf<CashLog>()
                for (i in 0 until clArray.length()) {
                    val o = clArray.getJSONObject(i)
                    cashLogs.add(
                        CashLog(
                            type = o.optString("type", "DAY_END_CLOSING"),
                            amount = o.optDouble("amount", 0.0),
                            balanceAfter = o.optDouble("balanceAfter", 0.0),
                            note = o.optString("note", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (cashLogs.isNotEmpty()) {
                    cashLogDao.insertAll(cashLogs)
                    clCount = cashLogs.size
                }
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
                message = "রিস্টোর ব্যর্থ: ${e.localizedMessage ?: "ফাইলের তথ্য সঠিক নয়"}"
            )
        }
    }
}
