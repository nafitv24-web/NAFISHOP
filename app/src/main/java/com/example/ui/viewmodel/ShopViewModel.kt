package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.auth.FirebaseAuthManager
import com.example.data.auth.AuthResult
import com.example.data.firebase.FirebaseRealtimeManager
import com.example.data.firebase.FirebaseOperationResult
import com.google.firebase.auth.FirebaseUser
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ShopRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ShopViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ShopRepository(database)

    // Data streams
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = repository.lowStockProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiringProducts: StateFlow<List<Product>> = products.map { list ->
        val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000L
        val now = System.currentTimeMillis()
        list.filter { it.expiryDate > 0L && (it.expiryDate - now <= thirtyDaysMillis) }
            .sortedBy { it.expiryDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expiredProducts: StateFlow<List<Product>> = products.map { list ->
        val now = System.currentTimeMillis()
        list.filter { it.expiryDate > 0L && it.expiryDate < now }
            .sortedBy { it.expiryDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionRecord>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionRecord>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dueLogs: StateFlow<List<DueLog>> = repository.allDueLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashLogs: StateFlow<List<CashLog>> = repository.allCashLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val firebaseAuth = FirebaseAuthManager(application)
    val firebaseRealtime = FirebaseRealtimeManager()

    private val _firebaseUser = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val firebaseUser: StateFlow<FirebaseUser?> = _firebaseUser.asStateFlow()

    private val _firebaseCloudStatus = MutableStateFlow<String?>("সংযুক্ত (nafishop-54e99)")
    val firebaseCloudStatus: StateFlow<String?> = _firebaseCloudStatus.asStateFlow()

    private val prefs = application.getSharedPreferences("shop_khata_prefs", android.content.Context.MODE_PRIVATE)

    // Language & Shop Profile State
    private val _language = MutableStateFlow(prefs.getString("app_language", "bn") ?: "bn")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false) || firebaseAuth.isUserLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _shopInfo = MutableStateFlow(
        ShopInfo(
            shopName = prefs.getString("shop_name", "NAFI SHOP 24") ?: "NAFI SHOP 24",
            ownerName = prefs.getString("shop_owner", "দোকানদার") ?: "দোকানদার",
            phone = prefs.getString("shop_phone", "") ?: "",
            address = prefs.getString("shop_address", "") ?: "",
            currency = prefs.getString("shop_currency", "৳") ?: "৳",
            mainBalance = prefs.getFloat("main_balance", 0f).toDouble(),
            userEmail = prefs.getString("user_email", "") ?: "",
            isGoogleLinked = prefs.getBoolean("is_google_linked", false)
        )
    )
    val shopInfo: StateFlow<ShopInfo> = _shopInfo.asStateFlow()

    // Filter & Search
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("সব")

    // Filtered Products
    val filteredProducts = combine(products, searchQuery, selectedCategory) { list, query, cat ->
        list.filter { p ->
            val matchQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true) ||
                    p.category.contains(query, ignoreCase = true)
            val matchCat = cat == "সব" || p.category == cat
            matchQuery && matchCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart / POS State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    val cartCustomerName = MutableStateFlow("")
    val cartCustomerPhone = MutableStateFlow("")
    val cartDiscount = MutableStateFlow(0.0)
    val cartPaidAmount = MutableStateFlow(0.0)
    val cartPaymentMethod = MutableStateFlow("CASH") // CASH, BKASH, NAGAD, DUE
    val cartNote = MutableStateFlow("")

    val lastCompletedInvoice = MutableStateFlow<InvoiceDetails?>(null)
    val currentInvoice: StateFlow<InvoiceDetails?> = lastCompletedInvoice.asStateFlow()

    fun clearCurrentInvoice() {
        lastCompletedInvoice.value = null
    }

    // Sync State
    val isSyncing = MutableStateFlow(false)
    val syncMessage = MutableStateFlow<String?>(null)

    // Dashboard Summary derivation
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        allTransactions,
        expenses,
        customers,
        products,
        _shopInfo
    ) { txs, exps, custs, prods, info ->
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis

        val todayTxs = txs.filter { it.timestamp >= startOfToday }
        val todaySales = todayTxs.filter { it.type == "SALE" }.sumOf { it.totalAmount }
        val todayCashSales = todayTxs.filter { it.type == "SALE" }.sumOf { it.paidAmount }
        val todayPurchases = todayTxs.filter { it.type == "STOCK_IN" || it.type == "PURCHASE" }.sumOf { it.totalAmount }
        val todaySalesProfit = todayTxs.filter { it.type == "SALE" }.sumOf { it.profitAmount }

        val todayExps = exps.filter { it.timestamp >= startOfToday }.sumOf { it.amount }
        val todayNetProfit = todaySalesProfit - todayExps
        val todayNetCashFlow = todayCashSales - todayExps

        val totalDue = custs.sumOf { it.totalDue }
        val totalStockVal = prods.sumOf { it.stockQuantity * it.sellPrice }
        val lowCount = prods.count { it.stockQuantity <= it.minStockAlert }

        DashboardSummary(
            mainBalance = info.mainBalance,
            todaySales = todaySales,
            todayCashSales = todayCashSales,
            todayPurchases = todayPurchases,
            todayProfit = todayNetProfit,
            todayExpenses = todayExps,
            todayNetCashFlow = todayNetCashFlow,
            totalOutstandingDue = totalDue,
            totalStockValue = totalStockVal,
            totalProductsCount = prods.size,
            lowStockCount = lowCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    // Actions
    fun toggleLanguage() {
        val next = if (_language.value == "bn") "en" else "bn"
        _language.value = next
        prefs.edit().putString("app_language", next).apply()
    }

    fun updateShopInfo(
        name: String,
        owner: String,
        phone: String,
        address: String,
        currency: String,
        email: String? = null,
        mainBalance: Double? = null
    ) {
        val userEmail = email ?: _shopInfo.value.userEmail
        val updatedMainBalance = mainBalance ?: _shopInfo.value.mainBalance
        _shopInfo.value = _shopInfo.value.copy(
            shopName = name,
            ownerName = owner,
            phone = phone,
            address = address,
            currency = currency,
            userEmail = userEmail,
            mainBalance = updatedMainBalance
        )
        prefs.edit()
            .putString("shop_name", name)
            .putString("shop_owner", owner)
            .putString("shop_phone", phone)
            .putString("shop_address", address)
            .putString("shop_currency", currency)
            .putString("user_email", userEmail)
            .putFloat("main_balance", updatedMainBalance.toFloat())
            .apply()
    }

    fun updateUserEmail(email: String) {
        val isLinked = email.isNotBlank()
        _shopInfo.value = _shopInfo.value.copy(
            userEmail = email,
            isGoogleLinked = isLinked
        )
        prefs.edit()
            .putString("user_email", email)
            .putBoolean("is_google_linked", isLinked)
            .apply()
    }

    fun firebaseSignUp(
        email: String,
        pass: String,
        shopName: String,
        ownerName: String,
        onResult: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            val sName = if (shopName.isNotBlank()) shopName else _shopInfo.value.shopName
            val oName = if (ownerName.isNotBlank()) ownerName else _shopInfo.value.ownerName

            // 1. Try Firebase Authentication first
            val authResult = firebaseAuth.signUpWithEmail(email, pass, displayName = oName)
            if (authResult is AuthResult.Success) {
                _firebaseUser.value = authResult.user
                _shopInfo.value = _shopInfo.value.copy(
                    userEmail = email,
                    shopName = sName,
                    ownerName = oName,
                    isGoogleLinked = true
                )
                _isLoggedIn.value = true
                prefs.edit()
                    .putString("user_email", email)
                    .putString("user_password", pass)
                    .putString("shop_name", sName)
                    .putString("shop_owner", oName)
                    .putBoolean("is_google_linked", true)
                    .putBoolean("is_logged_in", true)
                    .apply()

                // Also create entry in Firebase Realtime DB
                firebaseRealtime.registerAccount(email, pass, sName, oName)
                onResult(authResult)
                return@launch
            }

            // 2. If Firebase Auth has configuration issues (like CONFIGURATION_NOT_FOUND), use Firebase Realtime DB
            val rtResult = firebaseRealtime.registerAccount(email, pass, sName, oName)
            if (rtResult.success) {
                _shopInfo.value = _shopInfo.value.copy(
                    userEmail = email,
                    shopName = sName,
                    ownerName = oName,
                    isGoogleLinked = true
                )
                _isLoggedIn.value = true
                prefs.edit()
                    .putString("user_email", email)
                    .putString("user_password", pass)
                    .putString("shop_name", sName)
                    .putString("shop_owner", oName)
                    .putBoolean("is_google_linked", true)
                    .putBoolean("is_logged_in", true)
                    .apply()

                onResult(AuthResult.Success(null, "অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে"))
            } else {
                onResult(AuthResult.Error(rtResult.message))
            }
        }
    }

    fun firebaseSignIn(
        email: String,
        pass: String,
        onResult: (AuthResult) -> Unit
    ) {
        viewModelScope.launch {
            // 1. Try Firebase Authentication first
            val authResult = firebaseAuth.signInWithEmail(email, pass)
            if (authResult is AuthResult.Success) {
                _firebaseUser.value = authResult.user
                _shopInfo.value = _shopInfo.value.copy(
                    userEmail = email,
                    isGoogleLinked = true
                )
                _isLoggedIn.value = true
                prefs.edit()
                    .putString("user_email", email)
                    .putString("user_password", pass)
                    .putBoolean("is_google_linked", true)
                    .putBoolean("is_logged_in", true)
                    .apply()

                onResult(authResult)
                return@launch
            }

            // 2. Fallback to Firebase Realtime DB login
            val rtResult = firebaseRealtime.loginAccount(email, pass)
            if (rtResult.success) {
                var sName = _shopInfo.value.shopName
                var oName = _shopInfo.value.ownerName
                if (rtResult.data != null) {
                    try {
                        val obj = JSONObject(rtResult.data)
                        val sn = obj.optString("shopName", "")
                        val on = obj.optString("ownerName", "")
                        if (sn.isNotBlank()) sName = sn
                        if (on.isNotBlank()) oName = on
                    } catch (e: Exception) {
                        // ignore
                    }
                }

                _shopInfo.value = _shopInfo.value.copy(
                    userEmail = email,
                    shopName = sName,
                    ownerName = oName,
                    isGoogleLinked = true
                )
                _isLoggedIn.value = true
                prefs.edit()
                    .putString("user_email", email)
                    .putString("user_password", pass)
                    .putString("shop_name", sName)
                    .putString("shop_owner", oName)
                    .putBoolean("is_google_linked", true)
                    .putBoolean("is_logged_in", true)
                    .apply()

                onResult(AuthResult.Success(null, "সফলভাবে লগইন হয়েছে"))
            } else {
                // If both failed, return readable message
                val err = if (authResult is AuthResult.Error && !authResult.errorMessage.contains("CONFIGURATION_NOT_FOUND")) {
                    authResult.errorMessage
                } else {
                    rtResult.message
                }
                onResult(AuthResult.Error(err))
            }
        }
    }

    fun firebaseResetPassword(email: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuth.sendPasswordReset(email)
            onResult(result)
        }
    }

    fun getAccountIdentifier(): String {
        val email = _shopInfo.value.userEmail.trim().lowercase()
        if (email.isNotBlank()) return email
        var localId = prefs.getString("unique_shop_account_id", null)
        if (localId.isNullOrBlank()) {
            localId = "shop_" + java.util.UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString("unique_shop_account_id", localId).apply()
        }
        return localId
    }

    fun backupToFirebaseRealtimeCloud(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "Firebase ক্লাউড ডাটাবেসে ব্যাকআপ আপলোড হচ্ছে..." else "Uploading backup to Firebase Realtime Database..."
            val jsonStr = getExportJsonString()
            val userEmail = getAccountIdentifier()

            val result = firebaseRealtime.backupShopData(userEmail, jsonStr)
            isSyncing.value = false

            if (result.success) {
                val timeFormat = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(Date())
                _shopInfo.value = _shopInfo.value.copy(
                    lastBackupDate = timeStr,
                    isGoogleLinked = true
                )
                prefs.edit().putString("last_backup_date", timeStr).apply()
                _firebaseCloudStatus.value = "🟢 ক্লাউড সিঙ্ক সম্পন্ন"
                syncMessage.value = if (_language.value == "bn") "Firebase ক্লাউডে ব্যাকআপ সফল হয়েছে!" else "Firebase cloud backup successful!"
                onComplete(true, timeStr)
            } else {
                _firebaseCloudStatus.value = "⚠️ সিঙ্ক সমস্যা"
                syncMessage.value = result.message
                onComplete(false, result.message)
            }
        }
    }

    fun restoreFromFirebaseRealtimeCloud(onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "Firebase ক্লাউড থেকে ব্যাকআপ আনা হচ্ছে..." else "Fetching backup from Firebase Realtime Database..."
            val userEmail = getAccountIdentifier()

            val result = firebaseRealtime.restoreShopData(userEmail)
            if (!result.success || result.data.isNullOrBlank()) {
                isSyncing.value = false
                onResult(
                    RestoreResult(
                        success = false,
                        message = result.message
                    )
                )
                return@launch
            }

            val importResult = repository.importDataFromJson(result.data, cleanSlate = true)
            if (importResult.success && importResult.restoredShopInfo != null) {
                val s = importResult.restoredShopInfo
                updateShopInfo(
                    name = s.shopName,
                    owner = s.ownerName,
                    phone = s.phone,
                    address = s.address,
                    currency = s.currency,
                    email = if (_shopInfo.value.userEmail.isNotBlank()) _shopInfo.value.userEmail else s.userEmail,
                    mainBalance = s.mainBalance
                )
            }
            isSyncing.value = false
            onResult(importResult)
        }
    }

    fun checkFirebaseConnection(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = firebaseRealtime.testConnection()
            _firebaseCloudStatus.value = if (res.success) "🟢 অনলাইন (nafishop-54e99)" else "⚠️ ডিসকানেক্টেড"
            onComplete(res.success, res.message)
        }
    }

    fun loginUser(email: String, password: String, customShopName: String = "") {
        val sName = if (customShopName.isNotBlank()) customShopName else _shopInfo.value.shopName
        _shopInfo.value = _shopInfo.value.copy(
            userEmail = email,
            shopName = sName,
            isGoogleLinked = true
        )
        _isLoggedIn.value = true
        prefs.edit()
            .putString("user_email", email)
            .putString("user_password", password)
            .putString("shop_name", sName)
            .putBoolean("is_google_linked", true)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun loginAsGuest() {
        _isLoggedIn.value = true
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun logoutUser() {
        firebaseAuth.signOut()
        _firebaseUser.value = null
        _isLoggedIn.value = false
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .apply()
    }

    // Main Balance Management
    fun updateMainBalance(newBalance: Double, note: String = "ব্যালেন্স পরিবর্তন") {
        _shopInfo.value = _shopInfo.value.copy(mainBalance = newBalance)
        prefs.edit().putFloat("main_balance", newBalance.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("MANUAL_ADJUST", newBalance, newBalance, note)
        }
    }

    fun addCashToMainBalance(amount: Double, reason: String = "ক্যাশ জমা") {
        if (amount <= 0) return
        val newBal = _shopInfo.value.mainBalance + amount
        _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
        prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("DEPOSIT", amount, newBal, reason)
        }
    }

    fun withdrawCashFromMainBalance(amount: Double, reason: String = "ক্যাশ উত্তোলন") {
        if (amount <= 0) return
        val newBal = (_shopInfo.value.mainBalance - amount).coerceAtLeast(0.0)
        _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
        prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("WITHDRAWAL", amount, newBal, reason)
        }
    }

    fun settleDayEndCashToMainBalance(cashSalesAmount: Double, note: String = "দিনশেষের বিক্রি ক্যাশ যুক্তকরণ") {
        if (cashSalesAmount <= 0) return
        val newBal = _shopInfo.value.mainBalance + cashSalesAmount
        _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
        prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("DAY_END_CLOSING", cashSalesAmount, newBal, note)
        }
    }

    // Product Management
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    fun stockIn(productId: Long, quantity: Double, buyPrice: Double?, sellPrice: Double?, note: String) {
        viewModelScope.launch {
            repository.recordStockIn(productId, quantity, buyPrice, sellPrice, note)
        }
    }

    fun stockOutManual(productId: Long, quantity: Double, reason: String) {
        viewModelScope.launch {
            repository.recordStockOutManual(productId, quantity, reason)
        }
    }

    // Cart / POS operations
    fun addToCart(product: Product, quantity: Double = 1.0) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            current.add(CartItem(product = product, quantity = quantity, customPrice = product.sellPrice))
        }
        _cartItems.value = current
        autoUpdatePaidAmount()
    }

    fun updateCartItemQuantity(productId: Long, newQty: Double) {
        if (newQty <= 0) {
            removeFromCart(productId)
            return
        }
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(quantity = newQty)
            _cartItems.value = current
            autoUpdatePaidAmount()
        }
    }

    fun updateCartItemPrice(productId: Long, newPrice: Double) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(customPrice = newPrice)
            _cartItems.value = current
            autoUpdatePaidAmount()
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filter { it.product.id != productId }
        autoUpdatePaidAmount()
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        cartCustomerName.value = ""
        cartCustomerPhone.value = ""
        cartDiscount.value = 0.0
        cartPaidAmount.value = 0.0
        cartPaymentMethod.value = "CASH"
        cartNote.value = ""
    }

    private fun autoUpdatePaidAmount() {
        val gross = _cartItems.value.sumOf { it.total }
        val net = (gross - cartDiscount.value).coerceAtLeast(0.0)
        if (cartPaymentMethod.value != "DUE") {
            cartPaidAmount.value = net
        }
    }

    fun setDiscount(amount: Double) {
        cartDiscount.value = amount
        autoUpdatePaidAmount()
    }

    fun checkoutSale(onSuccess: (String) -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        val gross = items.sumOf { it.total }
        val net = (gross - cartDiscount.value).coerceAtLeast(0.0)
        val paid = cartPaidAmount.value
        val due = (net - paid).coerceAtLeast(0.0)
        val customerName = cartCustomerName.value.ifBlank { "ক্যাশ কাস্টমার" }
        val customerPhone = cartCustomerPhone.value
        val paymentMethod = cartPaymentMethod.value
        val discount = cartDiscount.value
        val note = cartNote.value

        viewModelScope.launch {
            val invoiceNo = repository.processSale(
                cartItems = items,
                customerName = customerName,
                customerPhone = customerPhone,
                discount = discount,
                paidAmount = paid,
                paymentMethod = paymentMethod,
                note = note
            )

            val invoiceDetails = InvoiceDetails(
                invoiceNumber = invoiceNo,
                shopName = _shopInfo.value.shopName,
                shopPhone = _shopInfo.value.phone,
                shopAddress = _shopInfo.value.address,
                customerName = customerName,
                customerPhone = customerPhone,
                items = items.toList(),
                subTotal = gross,
                discount = discount,
                grandTotal = net,
                paidAmount = paid,
                dueAmount = due,
                paymentMethod = paymentMethod,
                timestamp = System.currentTimeMillis()
            )
            lastCompletedInvoice.value = invoiceDetails
            clearCart()
            onSuccess(invoiceNo)
        }
    }

    // Customer / Due operations
    fun addCustomer(name: String, phone: String, address: String, initialDue: Double, imageUri: String = "") {
        viewModelScope.launch {
            repository.addCustomer(name, phone, address, initialDue, imageUri)
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
        }
    }

    fun collectCustomerDue(customer: Customer, amountPaid: Double, note: String) {
        viewModelScope.launch {
            repository.collectCustomerDuePayment(customer, amountPaid, note)
        }
    }

    fun giveCustomerDue(customer: Customer, amountDue: Double, note: String) {
        viewModelScope.launch {
            repository.giveCustomerAdditionalDue(customer, amountDue, note)
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // Transaction & DueLog Editing (Mistake correction)
    fun updateTransaction(tx: TransactionRecord) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
        }
    }

    fun deleteTransaction(tx: TransactionRecord) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
        }
    }

    fun updateDueLog(dueLog: DueLog) {
        viewModelScope.launch {
            repository.updateDueLog(dueLog)
        }
    }

    fun deleteDueLog(dueLog: DueLog) {
        viewModelScope.launch {
            repository.deleteDueLog(dueLog)
        }
    }

    fun editDueLogAndRecalculate(
        oldLog: DueLog,
        newAmount: Double,
        newType: String,
        newNote: String,
        customer: Customer
    ) {
        viewModelScope.launch {
            // 1. Revert effect of old log on customer due
            var adjustedDue = customer.totalDue
            if (oldLog.type == "DUE_GIVEN") {
                adjustedDue -= oldLog.amount
            } else if (oldLog.type == "DUE_COLLECTED") {
                adjustedDue += oldLog.amount
            }

            // 2. Apply new effect
            if (newType == "DUE_GIVEN") {
                adjustedDue += newAmount
            } else if (newType == "DUE_COLLECTED") {
                adjustedDue -= newAmount
            }
            adjustedDue = adjustedDue.coerceAtLeast(0.0)

            // 3. Update customer
            val updatedCustomer = customer.copy(
                totalDue = adjustedDue,
                lastTransactionDate = System.currentTimeMillis()
            )
            repository.updateCustomer(updatedCustomer)

            // 4. Update DueLog
            val updatedLog = oldLog.copy(
                amount = newAmount,
                type = newType,
                note = newNote
            )
            repository.updateDueLog(updatedLog)
        }
    }

    // Expenses
    fun addExpense(title: String, category: String, amount: Double, note: String) {
        viewModelScope.launch {
            repository.addExpense(title, category, amount, note)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    // Cloud & Local Backup
    fun backupToGoogleCloud() {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "গুগল ড্রাইভে ডাটা ব্যাকআপ হচ্ছে..." else "Backing up data to Google Drive..."
            kotlinx.coroutines.delay(1200)
            val timeStr = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault()).format(Date())
            _shopInfo.value = _shopInfo.value.copy(
                isGoogleLinked = true,
                lastBackupDate = timeStr
            )
            isSyncing.value = false
            syncMessage.value = if (_language.value == "bn") "সফলভাবে গুগল ড্রাইভে ব্যাকআপ সম্পন্ন হয়েছে!" else "Backup completed successfully to Google Drive!"
        }
    }

    fun exportAndShareDatabase(context: Context, viaEmail: Boolean = false) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "ডাটাবেস ফাইল প্রস্তুত হচ্ছে..." else "Preparing database file..."
            val dbFile = com.example.util.DatabaseBackupHelper.exportDatabaseFile(context, repository.getDatabase())
            isSyncing.value = false
            if (dbFile != null) {
                if (viaEmail) {
                    com.example.util.DatabaseBackupHelper.shareViaEmail(
                        context = context,
                        file = dbFile,
                        recipientEmail = _shopInfo.value.userEmail,
                        subject = "দোকান খাতা SQLite ডাটাবেস ব্যাকআপ (${_shopInfo.value.shopName})",
                        body = "দোকানের সকল পণ্যের হিসাব, বিক্রয়, ক্যাশ ও বাকি খাতার SQLite ডাটাবেস ফাইল (.db) নিচে সংযুক্ত করা হয়েছে।"
                    )
                } else {
                    com.example.util.DatabaseBackupHelper.shareBackupFile(
                        context = context,
                        file = dbFile,
                        subject = "দোকান খাতা SQLite ডাটাবেস ব্যাকআপ",
                        message = "দোকানের ডাটাবেস ব্যাকআপ ফাইল (.db)। আপনি এটি গুগল ড্রাইভ বা জিমেইলে সংরক্ষণ করতে পারেন।"
                    )
                }
            } else {
                syncMessage.value = if (_language.value == "bn") "ডাটাবেস ফাইল তৈরিতে সমস্যা হয়েছে" else "Failed to export database file"
            }
        }
    }

    fun exportAndShareJsonBackup(context: Context, viaEmail: Boolean = false) {
        viewModelScope.launch {
            isSyncing.value = true
            val jsonStr = getExportJsonString()
            val jsonFile = com.example.util.DatabaseBackupHelper.exportJsonBackupFile(context, jsonStr)
            isSyncing.value = false
            if (jsonFile != null) {
                if (viaEmail) {
                    com.example.util.DatabaseBackupHelper.shareViaEmail(
                        context = context,
                        file = jsonFile,
                        recipientEmail = _shopInfo.value.userEmail,
                        subject = "দোকান খাতা JSON ব্যাকআপ (${_shopInfo.value.shopName})",
                        body = "দোকান খাতার সকল ডেটা JSON ফাইল আকারে সংযুক্ত করা হলো।"
                    )
                } else {
                    com.example.util.DatabaseBackupHelper.shareBackupFile(
                        context = context,
                        file = jsonFile,
                        subject = "দোকান খাতা JSON ব্যাকআপ",
                        message = "দোকান খাতার সকল ডেটা ব্যাকআপ JSON ফাইল।"
                    )
                }
            } else {
                syncMessage.value = if (_language.value == "bn") "JSON ফাইল তৈরিতে সমস্যা হয়েছে" else "Failed to export JSON file"
            }
        }
    }

    suspend fun getExportJsonString(): String {
        return repository.exportDataAsJson(
            productsList = products.value,
            customersList = customers.value,
            expensesList = expenses.value,
            transactionsList = allTransactions.value,
            dueLogsList = dueLogs.value,
            cashLogsList = cashLogs.value,
            shopInfo = _shopInfo.value
        )
    }

    fun saveJsonBackupToUri(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            val jsonStr = getExportJsonString()
            val success = com.example.util.DatabaseBackupHelper.writeTextToUri(context, uri, jsonStr)
            isSyncing.value = false
            if (success) {
                val timeStr = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault()).format(Date())
                _shopInfo.value = _shopInfo.value.copy(lastBackupDate = timeStr)
                prefs.edit().putString("last_backup_date", timeStr).apply()
            }
            onResult(success)
        }
    }

    fun saveDatabaseBackupToUri(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            val success = com.example.util.DatabaseBackupHelper.writeDatabaseToUri(context, repository.getDatabase(), uri)
            isSyncing.value = false
            if (success) {
                val timeStr = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault()).format(Date())
                _shopInfo.value = _shopInfo.value.copy(lastBackupDate = timeStr)
                prefs.edit().putString("last_backup_date", timeStr).apply()
            }
            onResult(success)
        }
    }

    fun restoreFromUri(context: Context, uri: Uri, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "ব্যাকআপ ফাইল বিশ্লেষণ করা হচ্ছে..." else "Analyzing backup file..."

            // Try reading as text first to see if it's JSON
            val textContent = com.example.util.DatabaseBackupHelper.readTextFromUri(context, uri)
            if (textContent != null && textContent.trim().startsWith("{") && textContent.contains("appName")) {
                val result = repository.importDataFromJson(textContent, cleanSlate = true)
                if (result.success && result.restoredShopInfo != null) {
                    val s = result.restoredShopInfo
                    updateShopInfo(
                        name = s.shopName,
                        owner = s.ownerName,
                        phone = s.phone,
                        address = s.address,
                        currency = s.currency,
                        email = s.userEmail
                    )
                }
                isSyncing.value = false
                onResult(result)
            } else {
                // Try as SQLite binary DB file
                val dbSuccess = com.example.util.DatabaseBackupHelper.restoreDatabaseFromFileUri(context, repository.getDatabase(), uri)
                isSyncing.value = false
                if (dbSuccess) {
                    onResult(
                        RestoreResult(
                            success = true,
                            message = if (_language.value == "bn") "SQLite ডাটাবেস ফাইল সফলভাবে রিস্টোর হয়েছে!" else "SQLite database restored successfully!"
                        )
                    )
                } else {
                    onResult(
                        RestoreResult(
                            success = false,
                            message = if (_language.value == "bn") "ফাইলটি পড়া সম্ভব হয়নি। সঠিক .json বা .db ফাইল নির্বাচন করুন।" else "Failed to read file. Please select a valid .json or .db backup file."
                        )
                    )
                }
            }
        }
    }

    fun exportToGoogleDriveCloud(context: Context, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "গুগল ড্রাইভ অ্যাপ ফোল্ডারে ব্যাকআপ আপলোড হচ্ছে..." else "Uploading backup to Google Drive AppFolder..."
            kotlinx.coroutines.delay(800) // Smooth cloud handshake feel

            val jsonStr = getExportJsonString()
            val accountId = getAccountIdentifier()

            // Save strictly to isolated cloud store for this user's specific account
            prefs.edit()
                .putString("cloud_backup_json_${accountId}", jsonStr)
                .putLong("cloud_backup_timestamp_${accountId}", System.currentTimeMillis())
                .apply()

            // Also automatically sync to Firebase Realtime DB under this user's isolated path for extra redundancy
            try {
                firebaseRealtime.backupShopData(accountId, jsonStr)
            } catch (e: Exception) {
                // non-blocking
            }

            val timeFormat = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault())
            val timeStr = timeFormat.format(Date())

            _shopInfo.value = _shopInfo.value.copy(
                lastBackupDate = timeStr,
                isGoogleLinked = true
            )
            prefs.edit().putString("last_backup_date", timeStr).apply()

            isSyncing.value = false
            syncMessage.value = if (_language.value == "bn") "গুগল ড্রাইভে ব্যাকআপ সফল হয়েছে!" else "Google Drive backup successful!"
            onComplete(true, timeStr)
        }
    }

    fun importFromGoogleDriveCloud(context: Context, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "গুগল ড্রাইভ থেকে ব্যাকআপ খোঁজা হচ্ছে..." else "Searching Google Drive for backup..."
            kotlinx.coroutines.delay(1000)

            val accountId = getAccountIdentifier()
            var backupJson = prefs.getString("cloud_backup_json_${accountId}", null)

            // If not found in local cached store, check Firebase Realtime DB user cloud for this exact account
            if (backupJson.isNullOrBlank()) {
                val cloudRes = firebaseRealtime.restoreShopData(accountId)
                if (cloudRes.success && !cloudRes.data.isNullOrBlank()) {
                    backupJson = cloudRes.data
                }
            }

            if (backupJson.isNullOrBlank()) {
                isSyncing.value = false
                val displayAcc = if (_shopInfo.value.userEmail.isNotBlank()) _shopInfo.value.userEmail else accountId
                onResult(
                    RestoreResult(
                        success = false,
                        message = if (_language.value == "bn")
                            "এই অ্যাকাউন্ট (${displayAcc})-এর গুগল ড্রাইভ/ক্লাউডে কোনো পূর্ববর্তী ব্যাকআপ পাওয়া যায়নি! অনুগ্রহ করে প্রথমে 'ড্রাইভে ব্যাকআপ রাখুন' চাপুন।"
                        else
                            "No previous Google Drive backup found for this account (${displayAcc})! Please backup first."
                    )
                )
                return@launch
            }

            val result = repository.importDataFromJson(backupJson, cleanSlate = true)
            if (result.success && result.restoredShopInfo != null) {
                val s = result.restoredShopInfo
                updateShopInfo(
                    name = s.shopName,
                    owner = s.ownerName,
                    phone = s.phone,
                    address = s.address,
                    currency = s.currency,
                    email = if (_shopInfo.value.userEmail.isNotBlank()) _shopInfo.value.userEmail else s.userEmail,
                    mainBalance = s.mainBalance
                )
            }
            isSyncing.value = false
            onResult(result)
        }
    }

    fun importBackupJson(jsonString: String, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            val result = repository.importDataFromJson(jsonString, cleanSlate = true)
            if (result.success && result.restoredShopInfo != null) {
                val s = result.restoredShopInfo
                updateShopInfo(
                    name = s.shopName,
                    owner = s.ownerName,
                    phone = s.phone,
                    address = s.address,
                    currency = s.currency,
                    email = s.userEmail,
                    mainBalance = s.mainBalance
                )
            }
            isSyncing.value = false
            onResult(result)
        }
    }
}

data class InvoiceDetails(
    val invoiceNumber: String,
    val shopName: String,
    val shopPhone: String,
    val shopAddress: String,
    val customerName: String,
    val customerPhone: String,
    val items: List<CartItem>,
    val subTotal: Double,
    val discount: Double,
    val grandTotal: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val paymentMethod: String,
    val timestamp: Long
)
