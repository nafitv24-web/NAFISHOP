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

        // 2. If customer has due or is identified, update/create customer ledger
        if (customerName.isNotBlank() && (calculatedDue > 0 || customerPhone.isNotBlank())) {
            var existingCustomer = if (customerPhone.isNotBlank()) {
                customerDao.getCustomerByPhone(customerPhone)
            } else null

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
                        name = customerName,
                        phone = customerPhone,
                        address = "",
                        totalDue = calculatedDue,
                        totalPurchased = netTotal,
                        lastTransactionDate = now
                    )
                )
                existingCustomer = Customer(id = newCustomerId, name = customerName, phone = customerPhone, totalDue = calculatedDue)
            }

            if (calculatedDue > 0) {
                dueLogDao.insertDueLog(
                    DueLog(
                        customerId = existingCustomer.id,
                        customerName = customerName,
                        customerPhone = customerPhone,
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

    suspend fun addCustomer(name: String, phone: String, address: String, initialDue: Double) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val customerId = customerDao.insertCustomer(
            Customer(
                name = name,
                phone = phone,
                address = address,
                totalDue = initialDue,
                totalPurchased = initialDue,
                lastTransactionDate = now
            )
        )
        if (initialDue > 0) {
            dueLogDao.insertDueLog(
                DueLog(
                    customerId = customerId,
                    customerName = name,
                    customerPhone = phone,
                    type = "DUE_GIVEN",
                    amount = initialDue,
                    note = "পূর্বের বাকি হিসাব শুরু",
                    timestamp = now
                )
            )
        }
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
        customerDao.setCustomerDue(customer.id, updatedDue, now)

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
        transactionsList: List<TransactionRecord>
    ): String = withContext(Dispatchers.Default) {
        val root = JSONObject()
        root.put("appName", "ShopKhata")
        root.put("version", "1.0")
        root.put("exportTime", System.currentTimeMillis())

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
            cArray.put(o)
        }
        root.put("customers", cArray)

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

        root.toString(2)
    }

    suspend fun importDataFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
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
                            imageUri = o.optString("imageUri", "")
                        )
                    )
                }
                if (products.isNotEmpty()) {
                    productDao.insertAll(products)
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
                            totalPurchased = o.optDouble("totalPurchased", 0.0)
                        )
                    )
                }
                if (customers.isNotEmpty()) {
                    customerDao.insertAll(customers)
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
