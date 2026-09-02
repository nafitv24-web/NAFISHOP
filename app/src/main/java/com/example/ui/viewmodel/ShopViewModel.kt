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
import com.example.data.firebase.FirebaseUserAccount
import com.google.firebase.auth.FirebaseUser
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ShopRepository
import com.example.util.CalculationHelper.round2
import com.example.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    // Language, Theme & Shop Profile State
    private val _language = MutableStateFlow(prefs.getString("app_language", "bn") ?: "bn")
    val language: StateFlow<String> = _language.asStateFlow()

    // App Theme Mode: "SYSTEM", "LIGHT", "DARK"
    private val _themeMode = MutableStateFlow(prefs.getString("app_theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // User-Defined Custom Categories (no hardcoded sample categories)
    private val _customCategories = MutableStateFlow<List<String>>(
        prefs.getStringSet("custom_user_categories", emptySet())?.toList()?.filter { it.isNotBlank() && it != "সব" && it != "All" }?.sorted() ?: emptyList()
    )
    val customCategories: StateFlow<List<String>> = combine(_customCategories, products) { userCats, prodList ->
        val prodCats = prodList.map { it.category.trim() }.filter { it.isNotBlank() && it != "সব" && it != "All" }
        (userCats + prodCats).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addCustomCategory(newCategory: String) {
        val trimmed = newCategory.trim()
        if (trimmed.isNotBlank() && trimmed != "সব" && trimmed != "All") {
            val updated = (_customCategories.value + trimmed).distinct().sorted()
            _customCategories.value = updated
            prefs.edit().putStringSet("custom_user_categories", updated.toSet()).apply()
        }
    }

    fun removeCustomCategory(categoryToRemove: String) {
        val updated = _customCategories.value.filter { it != categoryToRemove }
        _customCategories.value = updated
        prefs.edit().putStringSet("custom_user_categories", updated.toSet()).apply()
    }

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
            isGoogleLinked = prefs.getBoolean("is_google_linked", true)
        )
    )
    val shopInfo: StateFlow<ShopInfo> = _shopInfo.asStateFlow()

    // Network & Real-time Connectivity State
    private val _isOnline = MutableStateFlow(NetworkMonitor.isOnline(application))
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _hasPendingSync = MutableStateFlow(prefs.getBoolean("has_pending_cloud_sync", false))
    val hasPendingSync: StateFlow<Boolean> = _hasPendingSync.asStateFlow()

    // Real-time Automatic Cloud Drive Backup State
    private val _autoBackupStatus = MutableStateFlow<String?>(
        if (NetworkMonitor.isOnline(application)) "🟢 ক্লাউড ব্যাকআপ সক্রিয়" else "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)"
    )
    val autoBackupStatus: StateFlow<String?> = _autoBackupStatus.asStateFlow()

    private var autoBackupJob: Job? = null

    /**
     * Synchronizes all local offline changes to Firebase Realtime Cloud immediately.
     */
    fun syncPendingOfflineDataToCloud(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val online = NetworkMonitor.isOnline(getApplication())
            _isOnline.value = online
            if (!online) {
                _hasPendingSync.value = true
                prefs.edit().putBoolean("has_pending_cloud_sync", true).apply()
                _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
                onComplete?.invoke(false)
                return@launch
            }

            try {
                _autoBackupStatus.value = if (_language.value == "bn") "⏳ ক্লাউড ড্রাইভে ব্যাকআপ হচ্ছে..." else "⏳ Backing up to Cloud..."
                val jsonStr = getExportJsonString()
                val accountId = getAccountIdentifier()
                val userEmail = _shopInfo.value.userEmail.trim().lowercase()

                // Save locally in isolated cloud store for instant local recovery
                prefs.edit()
                    .putString("cloud_backup_json_${accountId}", jsonStr)
                    .putString("cloud_backup_json_latest", jsonStr)
                    .putLong("cloud_backup_timestamp_${accountId}", System.currentTimeMillis())
                    .apply()

                val res1 = firebaseRealtime.backupShopData(accountId, jsonStr)
                val res2 = if (userEmail.isNotBlank() && userEmail != accountId) {
                    firebaseRealtime.backupShopData(userEmail, jsonStr)
                } else res1

                if (res1.success || res2.success) {
                    _hasPendingSync.value = false
                    prefs.edit().putBoolean("has_pending_cloud_sync", false).apply()
                    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    val fullTimeStr = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault()).format(Date())
                    _shopInfo.value = _shopInfo.value.copy(
                        lastBackupDate = fullTimeStr,
                        isGoogleLinked = true
                    )
                    prefs.edit().putString("last_backup_date", fullTimeStr).apply()
                    _autoBackupStatus.value = "🟢 ক্লাউডে ব্যাকআপ সম্পন্ন ($timeStr)"
                    onComplete?.invoke(true)
                } else {
                    _hasPendingSync.value = true
                    prefs.edit().putBoolean("has_pending_cloud_sync", true).apply()
                    _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
                    onComplete?.invoke(false)
                }
            } catch (e: Exception) {
                _hasPendingSync.value = true
                prefs.edit().putBoolean("has_pending_cloud_sync", true).apply()
                _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
                onComplete?.invoke(false)
            }
        }
    }

    /**
     * Automatic Google Drive & Cloud Backup:
     * Triggers automatically immediately whenever ANY new entry or change is made by the user.
     * Preserves offline data and syncs to cloud whenever online.
     */
    fun triggerInstantDriveBackup(reason: String = "এন্ট্রি") {
        autoBackupJob?.cancel()
        autoBackupJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Short debounce to batch rapid typing if any, but execute quickly in 350ms
                kotlinx.coroutines.delay(350)
                val online = NetworkMonitor.isOnline(getApplication())
                _isOnline.value = online

                val jsonStr = getExportJsonString()
                val accountId = getAccountIdentifier()

                // 1. Save locally in isolated cloud store for this user (Offline safe)
                prefs.edit()
                    .putString("cloud_backup_json_${accountId}", jsonStr)
                    .putString("cloud_backup_json_latest", jsonStr)
                    .putLong("cloud_backup_timestamp_${accountId}", System.currentTimeMillis())
                    .apply()

                if (!online) {
                    // OFFLINE: Mark pending sync and do NOT show fake backup!
                    _hasPendingSync.value = true
                    prefs.edit().putBoolean("has_pending_cloud_sync", true).apply()
                    _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
                    return@launch
                }

                // 2. Online: Sync to Cloud under isolated account ID and email
                _autoBackupStatus.value = if (_language.value == "bn") "⏳ ক্লাউড ড্রাইভে ব্যাকআপ হচ্ছে..." else "⏳ Backing up to Cloud..."
                val res1 = firebaseRealtime.backupShopData(accountId, jsonStr)
                val userEmail = _shopInfo.value.userEmail.trim().lowercase()
                val res2 = if (userEmail.isNotBlank() && userEmail != accountId) {
                    firebaseRealtime.backupShopData(userEmail, jsonStr)
                } else res1

                if (res1.success || res2.success) {
                    _hasPendingSync.value = false
                    prefs.edit().putBoolean("has_pending_cloud_sync", false).apply()
                    val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                    val fullTimeStr = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault()).format(Date())
                    _shopInfo.value = _shopInfo.value.copy(
                        lastBackupDate = fullTimeStr,
                        isGoogleLinked = true
                    )
                    prefs.edit().putString("last_backup_date", fullTimeStr).apply()
                    _autoBackupStatus.value = "🟢 ক্লাউডে ব্যাকআপ সম্পন্ন ($timeStr)"
                } else {
                    _hasPendingSync.value = true
                    prefs.edit().putBoolean("has_pending_cloud_sync", true).apply()
                    _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
                }
            } catch (e: Exception) {
                _hasPendingSync.value = true
                prefs.edit().putBoolean("has_pending_cloud_sync", true).apply()
                _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
            }
        }
    }

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

    // Current App Version Info
    val currentAppVersion = "2.4.0"
    val currentVersionCode = 24

    // Admin & App Update State
    private val _appUpdateInfo = MutableStateFlow(
        AppUpdateInfo(
            versionName = prefs.getString("app_update_version_name", "2.4.0") ?: "2.4.0",
            versionCode = prefs.getInt("app_update_version_code", 24),
            downloadUrl = prefs.getString("app_update_download_url", "https://drive.google.com") ?: "https://drive.google.com",
            releaseNotes = prefs.getString("app_update_notes", "নতুন দ্রুত বিক্রয় (POS), কাস্টমার ট্রানজেকশন হিস্ট্রি ও মেয়াদ ট্র্যাকিং সুবিধা যুক্ত করা হয়েছে।") ?: "",
            isForceUpdate = prefs.getBoolean("app_update_force", false),
            isUpdateActive = prefs.getBoolean("app_update_active", true),
            releaseDate = prefs.getLong("app_update_date", System.currentTimeMillis())
        )
    )
    val appUpdateInfo: StateFlow<AppUpdateInfo> = _appUpdateInfo.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _dismissedUpdate = MutableStateFlow(false)
    val dismissedUpdate: StateFlow<Boolean> = _dismissedUpdate.asStateFlow()

    // Admin User Notices
    private val _activeNotice = MutableStateFlow<AppNotice?>(null)
    val activeNotice: StateFlow<AppNotice?> = _activeNotice.asStateFlow()

    private val _noticeHistory = MutableStateFlow<List<AppNotice>>(emptyList())
    val noticeHistory: StateFlow<List<AppNotice>> = _noticeHistory.asStateFlow()

    // Registered Users state (Admin Panel)
    private val _registeredUsers = MutableStateFlow<List<FirebaseUserAccount>>(emptyList())
    val registeredUsers: StateFlow<List<FirebaseUserAccount>> = _registeredUsers.asStateFlow()

    private val _isLoadingUsers = MutableStateFlow(false)
    val isLoadingUsers: StateFlow<Boolean> = _isLoadingUsers.asStateFlow()

    private val _usersErrorMessage = MutableStateFlow<String?>(null)
    val usersErrorMessage: StateFlow<String?> = _usersErrorMessage.asStateFlow()

    private val _adminPin: MutableStateFlow<String>
    val adminPin: StateFlow<String>

    init {
        val savedPin = prefs.getString("admin_pin", null)
        val initialPin = if (savedPin == null || savedPin == "1234") {
            prefs.edit().putString("admin_pin", "40541273").apply()
            "40541273"
        } else {
            savedPin
        }
        _adminPin = MutableStateFlow(initialPin)
        adminPin = _adminPin.asStateFlow()

        loadSavedNotices()
        checkForUpdates(manualCheck = false)
        viewModelScope.launch {
            repository.deduplicateAndMergeCustomers()
            // Check if local database is empty; only restore if local db is empty!
            val isDbEmpty = repository.isDatabaseEmpty()
            val savedEmail = prefs.getString("user_email", "") ?: ""
            val emailToUse = if (savedEmail.isNotBlank()) savedEmail else _shopInfo.value.userEmail
            if (isDbEmpty && emailToUse.isNotBlank()) {
                autoRestoreOnLogin(emailToUse, forceOverwrite = true)
            } else if (!isDbEmpty && _hasPendingSync.value) {
                syncPendingOfflineDataToCloud()
            }
        }

        // Real-time network connectivity listener
        viewModelScope.launch {
            NetworkMonitor.observeConnectivity(application).collect { online ->
                _isOnline.value = online
                if (online) {
                    // Back online! Auto-sync all offline transactions/entries to Cloud immediately!
                    if (_hasPendingSync.value || _autoBackupStatus.value?.contains("অফলাইন") == true) {
                        syncPendingOfflineDataToCloud()
                    }
                } else {
                    _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
                }
            }
        }
    }

    // Dashboard Summary derivation
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        allTransactions,
        expenses,
        customers,
        products,
        combine(dueLogs, _shopInfo, cashLogs) { dues, info, cLogs -> Triple(dues, info, cLogs) }
    ) { txs, exps, custs, prods, extra ->
        val dues = extra.first
        val info = extra.second
        val cLogs = extra.third
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = calendar.timeInMillis

        val todayTxs = txs.filter { it.timestamp >= startOfToday }
        val todaySalesTxs = todayTxs.filter { it.type == "SALE" }
        val todaySales = round2(todaySalesTxs.sumOf { it.totalAmount })
        val todayCashSales = round2(todaySalesTxs.sumOf { it.paidAmount })
        val todayDueSales = round2(todaySalesTxs.sumOf { it.dueAmount })

        val todayCollectedDue = round2(dues.filter { it.timestamp >= startOfToday && it.type == "DUE_COLLECTED" }.sumOf { it.amount })
        val todayNewDueGiven = round2(dues.filter { it.timestamp >= startOfToday && it.type == "DUE_GIVEN" }.sumOf { it.amount })

        val todayClosedCash = round2(cLogs.filter { it.timestamp >= startOfToday && it.type == "DAY_END_CLOSING" }.sumOf { it.amount })
        val todayUnclosedCash = round2(((todayCashSales + todayCollectedDue) - todayClosedCash).coerceAtLeast(0.0))

        val todayPurchases = round2(todayTxs.filter { it.type == "STOCK_IN" || it.type == "PURCHASE" }.sumOf { it.totalAmount })
        // 1. Pure Product Sales Profit (লাভ শুধুমাত্র পণ্য বিক্রি থেকে)
        val todaySalesProfit = round2(todaySalesTxs.sumOf { it.profitAmount })
        val todayProfitMarginRate = if (todaySales > 0) round2((todaySalesProfit / todaySales) * 100.0) else 0.0

        // Calculate Realized Cash Profit vs Unrealized Due Profit from sold products
        var todayRealizedGrossProfit = 0.0
        var todayDueGrossProfit = 0.0
        for (sale in todaySalesTxs) {
            if (sale.totalAmount > 0) {
                val cashRatio = (sale.paidAmount / sale.totalAmount).coerceIn(0.0, 1.0)
                todayRealizedGrossProfit += sale.profitAmount * cashRatio
                todayDueGrossProfit += sale.profitAmount * (1.0 - cashRatio)
            } else {
                todayRealizedGrossProfit += sale.profitAmount
            }
        }
        todayRealizedGrossProfit = round2(todayRealizedGrossProfit)
        todayDueGrossProfit = round2(todayDueGrossProfit)

        val todayExps = round2(exps.filter { it.timestamp >= startOfToday }.sumOf { it.amount })
        val todayNetCashFlow = round2((todayCashSales + todayCollectedDue) - todayExps)

        val totalDue = round2(custs.sumOf { it.totalDue })
        val totalStockVal = round2(prods.sumOf { it.stockQuantity * it.sellPrice })
        val lowCount = prods.count { it.stockQuantity <= it.minStockAlert }

        val todayEstimatedDrawerCash = round2((todayUnclosedCash - todayExps).coerceAtLeast(0.0))

        DashboardSummary(
            mainBalance = round2(info.mainBalance),
            todaySales = todaySales,
            todayCashSales = todayCashSales,
            todayDueSales = todayDueSales,
            todayCollectedDue = todayCollectedDue,
            todayNewDueGiven = todayNewDueGiven,
            todayEstimatedDrawerCash = todayEstimatedDrawerCash,
            todayPurchases = todayPurchases,
            todayProfit = todaySalesProfit,
            todayRealizedProfit = todayRealizedGrossProfit,
            todayDueProfit = todayDueGrossProfit,
            todayProfitMargin = todayProfitMarginRate,
            todayExpenses = todayExps,
            todayNetCashFlow = todayNetCashFlow,
            totalOutstandingDue = totalDue,
            totalStockValue = totalStockVal,
            totalProductsCount = prods.size,
            lowStockCount = lowCount,
            todayClosedCash = todayClosedCash,
            todayUnclosedCash = todayUnclosedCash
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    // Actions
    fun toggleLanguage() {
        val next = if (_language.value == "bn") "en" else "bn"
        _language.value = next
        prefs.edit().putString("app_language", next).apply()
    }

    fun setThemeMode(mode: String) {
        // "LIGHT", "DARK", "SYSTEM"
        _themeMode.value = mode
        prefs.edit().putString("app_theme_mode", mode).apply()
    }

    fun toggleDarkMode() {
        val next = if (_themeMode.value == "DARK") "LIGHT" else "DARK"
        setThemeMode(next)
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
        triggerInstantDriveBackup("দোকানের তথ্য আপডেট")
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
        triggerInstantDriveBackup("ইমেইল লিঙ্ক")
    }

    /**
     * Automatically restore user's previous transaction history, products, dues, expenses,
     * cash logs and shop info immediately upon login from Google Drive / Cloud storage.
     * If local database already has offline data and not forced, it preserves local data and syncs to cloud!
     */
    fun autoRestoreOnLogin(accountId: String, forceOverwrite: Boolean = false, onRestored: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // If local database already has items and this is not a forced user action, don't overwrite offline work!
                val isDbEmpty = repository.isDatabaseEmpty()
                if (!isDbEmpty && !forceOverwrite) {
                    syncPendingOfflineDataToCloud { success ->
                        onRestored?.invoke(success)
                    }
                    return@launch
                }

                _autoBackupStatus.value = "⏳ ক্লাউড থেকে পূর্বের খাতা লোড হচ্ছে..."
                val cleanAccountId = accountId.trim().lowercase().ifBlank { getAccountIdentifier() }

                var backupJson: String? = null

                // 1. Query Firebase Realtime DB Cloud under this account ID or email first
                val cloudRes = firebaseRealtime.restoreShopData(cleanAccountId)
                if (cloudRes.success && !cloudRes.data.isNullOrBlank()) {
                    backupJson = cloudRes.data
                }

                // If user email differs from cleanAccountId, also query with user email
                val userEmail = _shopInfo.value.userEmail.trim().lowercase()
                if (backupJson.isNullOrBlank() && userEmail.isNotBlank() && userEmail != cleanAccountId) {
                    val cloudRes2 = firebaseRealtime.restoreShopData(userEmail)
                    if (cloudRes2.success && !cloudRes2.data.isNullOrBlank()) {
                        backupJson = cloudRes2.data
                    }
                }

                // 2. Try local isolated cloud store for this user
                if (backupJson.isNullOrBlank()) {
                    val localUserJson = prefs.getString("cloud_backup_json_${cleanAccountId}", null)
                    if (!localUserJson.isNullOrBlank()) {
                        backupJson = localUserJson
                    }
                }

                // 3. Fallback to latest local cache if available
                if (backupJson.isNullOrBlank()) {
                    val latestBackup = prefs.getString("cloud_backup_json_latest", null)
                    if (!latestBackup.isNullOrBlank()) {
                        backupJson = latestBackup
                    }
                }

                if (!backupJson.isNullOrBlank()) {
                    val result = repository.importDataFromJson(backupJson, cleanSlate = true)
                    if (result.success) {
                        if (result.restoredShopInfo != null) {
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
                        _autoBackupStatus.value = "🟢 ক্লাউড থেকে পূর্বের সকল লেনদেন ও খাতা রিস্টোর হয়েছে"
                        onRestored?.invoke(true)
                        return@launch
                    }
                }
                _autoBackupStatus.value = if (NetworkMonitor.isOnline(getApplication())) "🟢 ড্রাইভে অটো ব্যাকআপ সক্রিয়" else "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)"
                onRestored?.invoke(false)
            } catch (e: Exception) {
                _autoBackupStatus.value = if (NetworkMonitor.isOnline(getApplication())) "🟢 ড্রাইভে ব্যাকআপ সক্রিয়" else "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)"
                onRestored?.invoke(false)
            }
        }
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
                autoRestoreOnLogin(email)
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

                autoRestoreOnLogin(email)
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

                // Auto-restore previous transactions and shop database on login
                autoRestoreOnLogin(email)
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

                // Auto-restore previous transactions and shop database on login
                autoRestoreOnLogin(email)
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
        autoRestoreOnLogin(email)
    }

    fun loginAsGuest() {
        _isLoggedIn.value = true
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .apply()
        autoRestoreOnLogin(getAccountIdentifier())
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
        val cleanBalance = round2(newBalance)
        _shopInfo.value = _shopInfo.value.copy(mainBalance = cleanBalance)
        prefs.edit().putFloat("main_balance", cleanBalance.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("MANUAL_ADJUST", cleanBalance, cleanBalance, note)
            triggerInstantDriveBackup("ব্যালেন্স আপডেট")
        }
    }

    fun addCashToMainBalance(amount: Double, reason: String = "ক্যাশ জমা", timestamp: Long = System.currentTimeMillis()) {
        if (amount <= 0) return
        val cleanAmount = round2(amount)
        val newBal = round2(_shopInfo.value.mainBalance + cleanAmount)
        _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
        prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("DEPOSIT", cleanAmount, newBal, reason, timestamp)
            triggerInstantDriveBackup("ক্যাশ জমা")
        }
    }

    fun addCashIncome(amount: Double, reason: String = "ক্যাশ আয়", timestamp: Long = System.currentTimeMillis()) {
        addCashToMainBalance(amount, reason, timestamp)
    }

    fun withdrawCashFromMainBalance(amount: Double, reason: String = "ক্যাশ উত্তোলন", timestamp: Long = System.currentTimeMillis()) {
        if (amount <= 0) return
        val cleanAmount = round2(amount)
        val newBal = round2(_shopInfo.value.mainBalance - cleanAmount)
        _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
        prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("WITHDRAWAL", cleanAmount, newBal, reason, timestamp)
            triggerInstantDriveBackup("ক্যাশ উত্তোলন")
        }
    }

    fun addCashExpense(amount: Double, reason: String = "ক্যাশ খরচ", category: String = "অন্যান্য", timestamp: Long = System.currentTimeMillis()) {
        if (amount <= 0) return
        val cleanAmount = round2(amount)
        val newBal = round2(_shopInfo.value.mainBalance - cleanAmount)
        _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
        prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("WITHDRAWAL", cleanAmount, newBal, reason, timestamp)
            triggerInstantDriveBackup("ক্যাশ খরচ")
        }
    }

    fun settleDayEndCashToMainBalance(cashSalesAmount: Double, note: String = "দিনশেষের বিক্রি ক্যাশ যুক্তকরণ") {
        if (cashSalesAmount <= 0) return
        val cleanAmount = round2(cashSalesAmount)
        val newBal = round2(_shopInfo.value.mainBalance + cleanAmount)
        _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
        prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
        viewModelScope.launch {
            repository.recordCashLog("DAY_END_CLOSING", cleanAmount, newBal, note)
            triggerInstantDriveBackup("দিনশেষের ক্যাশ ক্লোজিং")
        }
    }

    // Product Management
    fun saveProduct(product: Product) {
        val cat = product.category.trim()
        if (cat.isNotBlank() && cat != "সব" && cat != "All") {
            addCustomCategory(cat)
        }
        viewModelScope.launch {
            val initialStockCost = repository.saveProduct(product)
            if (initialStockCost > 0) {
                withdrawCashFromMainBalance(initialStockCost, "পণ্য ক্রয়: ${product.name}")
            }
            triggerInstantDriveBackup("পণ্য সংরক্ষণ")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            triggerInstantDriveBackup("পণ্য মুছে ফেলা")
        }
    }

    fun stockIn(productId: Long, quantity: Double, buyPrice: Double?, sellPrice: Double?, note: String) {
        viewModelScope.launch {
            val totalCost = repository.recordStockIn(productId, quantity, buyPrice, sellPrice, note)
            if (totalCost > 0) {
                val label = note.ifBlank { "পণ্য স্টক ইন" }
                withdrawCashFromMainBalance(totalCost, "পণ্য ক্রয়: $label")
            }
            triggerInstantDriveBackup("স্টক ইন")
        }
    }

    fun stockOutManual(productId: Long, quantity: Double, reason: String) {
        viewModelScope.launch {
            repository.recordStockOutManual(productId, quantity, reason)
            triggerInstantDriveBackup("স্টক আউট")
        }
    }

    // Cart / POS operations
    fun addToCart(product: Product, quantity: Double = 1.0) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            val existing = current[index]
            current[index] = existing.copy(quantity = round2(existing.quantity + quantity))
        } else {
            current.add(CartItem(product = product, quantity = round2(quantity), customPrice = round2(product.sellPrice)))
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
            current[index] = current[index].copy(quantity = round2(newQty))
            _cartItems.value = current
            autoUpdatePaidAmount()
        }
    }

    fun updateCartItemPrice(productId: Long, newPrice: Double) {
        val current = _cartItems.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            current[index] = current[index].copy(customPrice = round2(newPrice))
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
        val gross = round2(_cartItems.value.sumOf { it.total })
        val net = round2((gross - cartDiscount.value).coerceAtLeast(0.0))
        if (cartPaymentMethod.value != "DUE") {
            cartPaidAmount.value = net
        }
    }

    fun setDiscount(amount: Double) {
        cartDiscount.value = round2(amount)
        autoUpdatePaidAmount()
    }

    fun checkoutSale(onSuccess: (String) -> Unit) {
        val items = _cartItems.value
        if (items.isEmpty()) return

        val gross = round2(items.sumOf { it.total })
        val discount = round2(cartDiscount.value)
        val net = round2((gross - discount).coerceAtLeast(0.0))
        val paid = round2(cartPaidAmount.value)
        val due = round2((net - paid).coerceAtLeast(0.0))
        val customerName = cartCustomerName.value.ifBlank { "ক্যাশ কাস্টমার" }
        val customerPhone = cartCustomerPhone.value
        val paymentMethod = cartPaymentMethod.value
        val note = cartNote.value

        val cleanName = customerName.trim()
        val cleanPhone = customerPhone.trim()
        val existingCust = customers.value.find {
            (cleanPhone.isNotBlank() && it.phone == cleanPhone) ||
            (cleanName.isNotBlank() && cleanName != "ক্যাশ কাস্টমার" && it.name.equals(cleanName, ignoreCase = true))
        }
        val prevDue = round2(existingCust?.totalDue ?: 0.0)
        val updatedTotalDue = round2(prevDue + due)

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
                previousDue = prevDue,
                totalCurrentDue = updatedTotalDue,
                paymentMethod = paymentMethod,
                timestamp = System.currentTimeMillis()
            )
            lastCompletedInvoice.value = invoiceDetails
            clearCart()
            triggerInstantDriveBackup("বিক্রয় সম্পন্ন #$invoiceNo")
            onSuccess(invoiceNo)
        }
    }

    // Customer / Due operations
    fun addCustomer(name: String, phone: String, address: String, initialDue: Double, imageUri: String = "") {
        viewModelScope.launch {
            repository.addCustomer(name, phone, address, initialDue, imageUri)
            triggerInstantDriveBackup("কাস্টমার যুক্তকরণ")
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            triggerInstantDriveBackup("কাস্টমার আপডেট")
        }
    }

    fun collectCustomerDue(customer: Customer, amountPaid: Double, note: String) {
        viewModelScope.launch {
            val cleanPaid = round2(amountPaid)
            if (cleanPaid > 0) {
                repository.collectCustomerDuePayment(customer, cleanPaid, note)
                addCashToMainBalance(cleanPaid, "বাকি আদায়: ${customer.name}")
                triggerInstantDriveBackup("বাকি আদায় (${customer.name})")
            }
        }
    }

    fun giveCustomerDue(
        customer: Customer,
        amountDue: Double,
        note: String,
        selectedProducts: List<Pair<Product, Double>> = emptyList(),
        customTimestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val cleanDue = round2(amountDue)
            repository.giveCustomerAdditionalDue(customer, cleanDue, note, selectedProducts, customTimestamp)
            triggerInstantDriveBackup("বাকি প্রদান (${customer.name})")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            triggerInstantDriveBackup("কাস্টমার মুছে ফেলা")
        }
    }

    fun reconcileCustomerLedgers() {
        viewModelScope.launch {
            repository.reconcileCustomerLedgers()
        }
    }

    // Transaction & DueLog Editing (Mistake correction / Return handling)
    fun returnProductSale(tx: TransactionRecord, returnQuantity: Double, note: String) {
        viewModelScope.launch {
            repository.returnSaleItem(tx, returnQuantity, note)
            triggerInstantDriveBackup("পণ্য ফেরত (${tx.productName})")
        }
    }

    fun editSaleTransaction(
        oldTx: TransactionRecord,
        newQuantity: Double,
        newUnitPrice: Double,
        newCustomerName: String,
        newNote: String
    ) {
        viewModelScope.launch {
            repository.editSaleTransaction(oldTx, newQuantity, newUnitPrice, newCustomerName, newNote)
            triggerInstantDriveBackup("বিক্রয় সংশোধন")
        }
    }

    fun editSaleTransaction(
        oldTx: TransactionRecord,
        newQuantity: Double,
        newUnitPrice: Double,
        newPaidAmount: Double,
        newCustomerName: String,
        newCustomerPhone: String,
        newNote: String
    ) {
        viewModelScope.launch {
            repository.editSaleTransaction(oldTx, newQuantity, newUnitPrice, newPaidAmount, newCustomerName, newCustomerPhone, newNote)
            triggerInstantDriveBackup("লেনদেন সংশোধন (${oldTx.productName})")
        }
    }

    fun deleteSaleAndRestock(tx: TransactionRecord) {
        viewModelScope.launch {
            if (tx.paidAmount > 0) {
                withdrawCashFromMainBalance(tx.paidAmount, "বিক্রি বাতিল/টাকা ফেরত: ${tx.productName}")
            }
            repository.deleteTransaction(tx)
            triggerInstantDriveBackup("ট্রানজেকশন বাতিল ও রিস্টক")
        }
    }

    fun updateTransaction(tx: TransactionRecord) {
        viewModelScope.launch {
            repository.updateTransaction(tx)
            triggerInstantDriveBackup("ট্রানজেকশন আপডেট")
        }
    }

    fun deleteTransaction(tx: TransactionRecord) {
        viewModelScope.launch {
            if ((tx.type == "STOCK_IN" || tx.type == "PURCHASE") && tx.totalAmount > 0) {
                addCashToMainBalance(tx.totalAmount, "পণ্য ক্রয় বাতিল/ক্যাশ ফেরত: ${tx.productName}")
            } else if (tx.type == "SALE" && tx.paidAmount > 0) {
                withdrawCashFromMainBalance(tx.paidAmount, "বিক্রি বাতিল/টাকা ফেরত: ${tx.productName}")
            }
            repository.deleteTransaction(tx)
            triggerInstantDriveBackup("ট্রানজেকশন মুছে ফেলা")
        }
    }

    fun editCashLog(
        oldLog: CashLog,
        newAmount: Double,
        newNote: String
    ) {
        viewModelScope.launch {
            val cleanNewAmount = round2(newAmount)
            val diff = cleanNewAmount - oldLog.amount
            
            // Adjust current main balance based on entry type
            val curBal = _shopInfo.value.mainBalance
            val newBal = when (oldLog.type) {
                "DEPOSIT", "INCOME", "DAY_END_CLOSING" -> round2(curBal + diff)
                "WITHDRAWAL", "EXPENSE" -> round2(curBal - diff)
                "MANUAL_ADJUST" -> cleanNewAmount
                else -> curBal
            }
            
            if (newBal != curBal) {
                _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
                prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
            }

            val updated = oldLog.copy(amount = cleanNewAmount, note = newNote, balanceAfter = newBal)
            repository.updateCashLog(updated)
            triggerInstantDriveBackup("ক্যাশ লগ সংশোধন")
        }
    }

    fun deleteCashLog(cashLog: CashLog) {
        viewModelScope.launch {
            // Revert balance impact upon deletion
            val curBal = _shopInfo.value.mainBalance
            val newBal = when (cashLog.type) {
                "DEPOSIT", "INCOME", "DAY_END_CLOSING" -> round2(curBal - cashLog.amount)
                "WITHDRAWAL", "EXPENSE" -> round2(curBal + cashLog.amount)
                else -> curBal
            }

            if (newBal != curBal) {
                _shopInfo.value = _shopInfo.value.copy(mainBalance = newBal)
                prefs.edit().putFloat("main_balance", newBal.toFloat()).apply()
            }

            repository.deleteCashLog(cashLog)
            triggerInstantDriveBackup("ক্যাশ লগ মুছে ফেলা")
        }
    }

    fun updateDueLog(dueLog: DueLog) {
        viewModelScope.launch {
            repository.updateDueLog(dueLog)
            triggerInstantDriveBackup("বাকি লগ আপডেট")
        }
    }

    fun deleteDueLog(dueLog: DueLog) {
        viewModelScope.launch {
            if (dueLog.type == "DUE_COLLECTED" && dueLog.amount > 0) {
                withdrawCashFromMainBalance(dueLog.amount, "বাকি আদায় বাতিল (${dueLog.customerName})")
            }
            repository.deleteDueLog(dueLog)
            triggerInstantDriveBackup("বাকি লগ মুছে ফেলা")
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
            val cleanNewAmount = round2(newAmount)
            // Cash balance adjustment if collected amount changed
            if (oldLog.type == "DUE_COLLECTED" && newType == "DUE_COLLECTED") {
                val diff = round2(cleanNewAmount - oldLog.amount)
                if (diff > 0) addCashToMainBalance(diff, "বাকি আদায় সংশোধন (${customer.name})")
                else if (diff < 0) withdrawCashFromMainBalance(-diff, "বাকি আদায় সংশোধন (${customer.name})")
            } else if (oldLog.type == "DUE_COLLECTED" && newType != "DUE_COLLECTED") {
                withdrawCashFromMainBalance(oldLog.amount, "বাকি আদায় বাতিল (${customer.name})")
            } else if (oldLog.type != "DUE_COLLECTED" && newType == "DUE_COLLECTED") {
                addCashToMainBalance(cleanNewAmount, "বাকি আদায় (${customer.name})")
            }

            // 1. Revert effect of old log on customer due
            var adjustedDue = customer.totalDue
            if (oldLog.type == "DUE_GIVEN") {
                adjustedDue -= oldLog.amount
            } else if (oldLog.type == "DUE_COLLECTED") {
                adjustedDue += oldLog.amount
            }

            // 2. Apply new effect
            if (newType == "DUE_GIVEN") {
                adjustedDue += cleanNewAmount
            } else if (newType == "DUE_COLLECTED") {
                adjustedDue -= cleanNewAmount
            }
            adjustedDue = round2(adjustedDue.coerceAtLeast(0.0))

            // 3. Update customer
            val updatedCustomer = customer.copy(
                totalDue = adjustedDue,
                lastTransactionDate = System.currentTimeMillis()
            )
            repository.updateCustomer(updatedCustomer)

            // 4. Update DueLog
            val updatedLog = oldLog.copy(
                amount = cleanNewAmount,
                type = newType,
                note = newNote
            )
            repository.updateDueLog(updatedLog)
            triggerInstantDriveBackup("বাকি খাতা সংশোধন")
        }
    }

    // Expenses
    fun addExpense(title: String, category: String, amount: Double, note: String) {
        val cleanAmount = round2(amount)
        viewModelScope.launch {
            repository.addExpense(title, category, cleanAmount, note)
            if (cleanAmount > 0) {
                withdrawCashFromMainBalance(cleanAmount, "খরচ: $title ($category)")
            }
            triggerInstantDriveBackup("খরচ এন্ট্রি")
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
            triggerInstantDriveBackup("খরচ আপডেট")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            if (expense.amount > 0) {
                addCashToMainBalance(expense.amount, "খরচ বাতিল/ক্যাশ ফেরত: ${expense.title}")
            }
            triggerInstantDriveBackup("খরচ মুছে ফেলা")
        }
    }

    // Cloud & Local Backup
    fun backupToGoogleCloud() {
        viewModelScope.launch {
            if (!NetworkMonitor.isOnline(getApplication())) {
                _hasPendingSync.value = true
                prefs.edit().putBoolean("has_pending_cloud_sync", true).apply()
                _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
                syncMessage.value = if (_language.value == "bn") "ইন্টারনেট সংযোগ নেই! ইন্টারনেট চালু হলে স্বয়ংক্রিয়ভাবে ব্যাকআপ হবে।" else "No internet connection! Will auto-backup once online."
                return@launch
            }
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "গুগল ড্রাইভে ডাটা ব্যাকআপ হচ্ছে..." else "Backing up data to Google Drive..."
            syncPendingOfflineDataToCloud { success ->
                isSyncing.value = false
                if (success) {
                    syncMessage.value = if (_language.value == "bn") "সফলভাবে গুগল ড্রাইভে ব্যাকআপ সম্পন্ন হয়েছে!" else "Backup completed successfully to Google Drive!"
                } else {
                    syncMessage.value = if (_language.value == "bn") "ব্যাকআপ সম্পন্ন হতে ব্যর্থ হয়েছে" else "Backup failed"
                }
            }
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
            if (!NetworkMonitor.isOnline(context)) {
                _hasPendingSync.value = true
                prefs.edit().putBoolean("has_pending_cloud_sync", true).apply()
                _autoBackupStatus.value = if (_language.value == "bn") "🟠 অফলাইন (ইন্টারনেট পেলে ব্যাকআপ হবে)" else "🟠 Offline (Will backup when online)"
                val errorMsg = if (_language.value == "bn") "ইন্টারনেট সংযোগ নেই! ইন্টারনেট চালু হলে স্বয়ংক্রিয়ভাবে ব্যাকআপ হবে।" else "No internet connection! Will auto-backup once online."
                syncMessage.value = errorMsg
                onComplete(false, errorMsg)
                return@launch
            }

            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "গুগল ড্রাইভ অ্যাপ ফোল্ডারে ব্যাকআপ আপলোড হচ্ছে..." else "Uploading backup to Google Drive AppFolder..."

            val jsonStr = getExportJsonString()
            val accountId = getAccountIdentifier()

            // Save strictly to isolated cloud store for this user's specific account
            prefs.edit()
                .putString("cloud_backup_json_${accountId}", jsonStr)
                .putString("cloud_backup_json_latest", jsonStr)
                .putLong("cloud_backup_timestamp_${accountId}", System.currentTimeMillis())
                .apply()

            var uploadSuccess = false
            var resMessage = ""
            try {
                val res = firebaseRealtime.backupShopData(accountId, jsonStr)
                val userEmail = _shopInfo.value.userEmail.trim().lowercase()
                if (userEmail.isNotBlank() && userEmail != accountId) {
                    firebaseRealtime.backupShopData(userEmail, jsonStr)
                }
                uploadSuccess = res.success
                resMessage = res.message
            } catch (e: Exception) {
                uploadSuccess = false
                resMessage = e.localizedMessage ?: "Unknown error"
            }

            isSyncing.value = false
            if (uploadSuccess) {
                _hasPendingSync.value = false
                prefs.edit().putBoolean("has_pending_cloud_sync", false).apply()
                val timeFormat = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(Date())

                _shopInfo.value = _shopInfo.value.copy(
                    lastBackupDate = timeStr,
                    isGoogleLinked = true
                )
                prefs.edit().putString("last_backup_date", timeStr).apply()
                _autoBackupStatus.value = "🟢 ক্লাউডে ব্যাকআপ সম্পন্ন (${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())})"
                syncMessage.value = if (_language.value == "bn") "গুগল ড্রাইভে ব্যাকআপ সফল হয়েছে!" else "Google Drive backup successful!"
                onComplete(true, timeStr)
            } else {
                _hasPendingSync.value = true
                _autoBackupStatus.value = if (_language.value == "bn") "🟠 ব্যাকআপ ব্যর্থ হয়েছে" else "🟠 Backup failed"
                val errorMsg = if (_language.value == "bn") "ব্যাকআপ আপলোড ব্যর্থ হয়েছে: $resMessage" else "Backup failed: $resMessage"
                syncMessage.value = errorMsg
                onComplete(false, errorMsg)
            }
        }
    }

    fun backupToLocalFile(context: Context, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "ব্যাকআপ ফাইল তৈরি হচ্ছে..." else "Creating backup file..."
            try {
                val jsonStr = getExportJsonString()
                val fileName = "nafishop_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.json"
                val file = java.io.File(context.getExternalFilesDir(null) ?: context.filesDir, fileName)
                file.writeText(jsonStr)

                val timeFormat = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault())
                val timeStr = timeFormat.format(Date())
                _shopInfo.value = _shopInfo.value.copy(lastBackupDate = timeStr)
                prefs.edit().putString("last_backup_date", timeStr).apply()
                isSyncing.value = false
                syncMessage.value = if (_language.value == "bn") "ফাইলে ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে!" else "Backup saved to file successfully!"
                onComplete?.invoke(true, file.absolutePath)
            } catch (e: Exception) {
                isSyncing.value = false
                syncMessage.value = "Backup failed: ${e.localizedMessage}"
                onComplete?.invoke(false, e.localizedMessage ?: "Error")
            }
        }
    }

    fun importFromGoogleDriveCloud(context: Context, customEmail: String? = null, onResult: (RestoreResult) -> Unit) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = if (_language.value == "bn") "ক্লাউড থেকে ব্যাকআপ খোঁজা হচ্ছে..." else "Searching cloud for backup..."
            kotlinx.coroutines.delay(400)

            val emailToUse = customEmail?.trim()?.lowercase() ?: _shopInfo.value.userEmail.trim().lowercase()
            val accountId = if (emailToUse.isNotBlank()) emailToUse else getAccountIdentifier()
            var backupJson: String? = null

            // 1. Query Firebase Realtime DB Cloud under this account ID / email first
            val cloudRes = firebaseRealtime.restoreShopData(accountId)
            if (cloudRes.success && !cloudRes.data.isNullOrBlank()) {
                backupJson = cloudRes.data
            }

            // 2. If not found and shopInfo email exists, try that too
            if (backupJson.isNullOrBlank() && _shopInfo.value.userEmail.isNotBlank() && _shopInfo.value.userEmail != accountId) {
                val cloudRes2 = firebaseRealtime.restoreShopData(_shopInfo.value.userEmail)
                if (cloudRes2.success && !cloudRes2.data.isNullOrBlank()) {
                    backupJson = cloudRes2.data
                }
            }

            // 3. Try local cache
            if (backupJson.isNullOrBlank()) {
                val localUser = prefs.getString("cloud_backup_json_${accountId}", null)
                if (!localUser.isNullOrBlank()) backupJson = localUser
            }
            if (backupJson.isNullOrBlank()) {
                val latest = prefs.getString("cloud_backup_json_latest", null)
                if (!latest.isNullOrBlank()) backupJson = latest
            }

            // 4. Try local backup file from external / internal storage
            if (backupJson.isNullOrBlank()) {
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val files = dir.listFiles { _, name -> name.startsWith("nafishop_backup_") && name.endsWith(".json") }
                val newest = files?.maxByOrNull { it.lastModified() }
                if (newest != null && newest.exists()) {
                    try {
                        backupJson = newest.readText()
                    } catch (e: Exception) {}
                }
            }

            if (backupJson.isNullOrBlank()) {
                isSyncing.value = false
                val displayAcc = if (emailToUse.isNotBlank()) emailToUse else accountId
                onResult(
                    RestoreResult(
                        success = false,
                        message = if (_language.value == "bn")
                            "এই অ্যাকাউন্ট (${displayAcc})-এর গুগল ড্রাইভ/ক্লাউডে কোনো পূর্ববর্তী ব্যাকআপ পাওয়া যায়নি! অনুগ্রহ করে সঠিক জিমেইল দিয়ে চেষ্টা করুন।"
                        else
                            "No previous backup found for this account (${displayAcc})! Please check your Gmail."
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
                    email = if (emailToUse.isNotBlank()) emailToUse else s.userEmail,
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

    // -------------------------------------------------------------
    // ADMIN PANEL: APP UPDATES & USER NOTIFICATIONS
    // -------------------------------------------------------------

    private fun loadSavedNotices() {
        val noticesJson = prefs.getString("saved_notices_json", null)
        if (!noticesJson.isNullOrBlank()) {
            try {
                val arr = org.json.JSONArray(noticesJson)
                val list = mutableListOf<AppNotice>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        AppNotice(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            title = obj.optString("title", ""),
                            message = obj.optString("message", ""),
                            type = obj.optString("type", "INFO"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            isActive = obj.optBoolean("isActive", true),
                            actionUrl = obj.optString("actionUrl", "")
                        )
                    )
                }
                _noticeHistory.value = list
                _activeNotice.value = list.firstOrNull { it.isActive }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveNoticesToPrefs(list: List<AppNotice>) {
        try {
            val arr = org.json.JSONArray()
            for (n in list) {
                val obj = JSONObject().apply {
                    put("id", n.id)
                    put("title", n.title)
                    put("message", n.message)
                    put("type", n.type)
                    put("timestamp", n.timestamp)
                    put("isActive", n.isActive)
                    put("actionUrl", n.actionUrl)
                }
                arr.put(obj)
            }
            prefs.edit().putString("saved_notices_json", arr.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkForUpdates(manualCheck: Boolean = false, onResult: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            if (manualCheck) {
                isSyncing.value = true
                syncMessage.value = if (_language.value == "bn") "আপডেট চেক করা হচ্ছে..." else "Checking for updates..."
            }

            try {
                // 1. Try fetching latest update metadata from cloud (Firebase Realtime DB)
                val cloudUpdate = firebaseRealtime.fetchAppUpdate()
                val activeCloudNotice = firebaseRealtime.fetchActiveNotice()

                if (cloudUpdate != null) {
                    _appUpdateInfo.value = cloudUpdate
                    prefs.edit()
                        .putString("app_update_version_name", cloudUpdate.versionName)
                        .putInt("app_update_version_code", cloudUpdate.versionCode)
                        .putString("app_update_download_url", cloudUpdate.downloadUrl)
                        .putString("app_update_notes", cloudUpdate.releaseNotes)
                        .putBoolean("app_update_force", cloudUpdate.isForceUpdate)
                        .putBoolean("app_update_active", cloudUpdate.isUpdateActive)
                        .putLong("app_update_date", cloudUpdate.releaseDate)
                        .apply()
                }

                if (activeCloudNotice != null) {
                    _activeNotice.value = activeCloudNotice
                }

                val currentUpdate = _appUpdateInfo.value
                val hasNewVersion = currentUpdate.isUpdateActive && (
                        currentUpdate.versionCode > currentVersionCode ||
                                (!currentUpdate.versionName.equals(currentAppVersion, ignoreCase = true) && currentUpdate.downloadUrl.isNotBlank())
                        )

                _isUpdateAvailable.value = hasNewVersion

                if (manualCheck) {
                    isSyncing.value = false
                    if (hasNewVersion) {
                        onResult?.invoke(true, if (_language.value == "bn") "নতুন ভার্সন (${currentUpdate.versionName}) পাওয়া গেছে!" else "New version (${currentUpdate.versionName}) is available!")
                    } else {
                        onResult?.invoke(false, if (_language.value == "bn") "আপনি সর্বশেষ ভার্সন (${currentAppVersion}) ব্যবহার করছেন।" else "You are using the latest version (${currentAppVersion}).")
                    }
                }
            } catch (e: Exception) {
                if (manualCheck) {
                    isSyncing.value = false
                    onResult?.invoke(false, "চেক করতে সমস্যা হয়েছে: ${e.localizedMessage}")
                }
            }
        }
    }

    fun publishAppUpdate(
        updateInfo: AppUpdateInfo,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            _appUpdateInfo.value = updateInfo
            prefs.edit()
                .putString("app_update_version_name", updateInfo.versionName)
                .putInt("app_update_version_code", updateInfo.versionCode)
                .putString("app_update_download_url", updateInfo.downloadUrl)
                .putString("app_update_notes", updateInfo.releaseNotes)
                .putBoolean("app_update_force", updateInfo.isForceUpdate)
                .putBoolean("app_update_active", updateInfo.isUpdateActive)
                .putLong("app_update_date", updateInfo.releaseDate)
                .apply()

            val hasNewVersion = updateInfo.isUpdateActive && (
                    updateInfo.versionCode > currentVersionCode ||
                            (!updateInfo.versionName.equals(currentAppVersion, ignoreCase = true) && updateInfo.downloadUrl.isNotBlank())
                    )
            _isUpdateAvailable.value = hasNewVersion
            _dismissedUpdate.value = false

            // Publish to Cloud so all users receive it
            val res = firebaseRealtime.publishAppUpdate(updateInfo)
            onComplete(true, if (res.success) "অ্যাপ আপডেট সফলভাবে ক্লাউড ও লোকাল সিস্টেমে প্রকাশিত হয়েছে!" else "আপডেট সংরক্ষিত হয়েছে (অফলাইন মোড)")
        }
    }

    fun publishNotice(
        notice: AppNotice,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val updatedList = listOf(notice) + _noticeHistory.value.filter { it.id != notice.id }
            _noticeHistory.value = updatedList
            _activeNotice.value = notice
            saveNoticesToPrefs(updatedList)

            val res = firebaseRealtime.publishAppNotice(notice)
            onComplete(true, if (res.success) "ইউজারদের কাছে নোটিফিকেশন সফলভাবে পাঠানো হয়েছে!" else "নোটিশ সংরক্ষিত হয়েছে")
        }
    }

    fun deleteNotice(noticeId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val updatedList = _noticeHistory.value.filter { it.id != noticeId }
            _noticeHistory.value = updatedList
            if (_activeNotice.value?.id == noticeId) {
                _activeNotice.value = updatedList.firstOrNull { it.isActive }
                firebaseRealtime.clearActiveNotice()
            }
            saveNoticesToPrefs(updatedList)
            onComplete(true)
        }
    }

    fun dismissActiveNotice() {
        _activeNotice.value = null
    }

    fun dismissUpdateAlert() {
        _dismissedUpdate.value = true
    }

    fun verifyAdminPin(pin: String): Boolean {
        return pin.trim() == _adminPin.value.trim()
    }

    fun changeAdminPin(oldPin: String, newPin: String): Boolean {
        if (oldPin.trim() == _adminPin.value.trim()) {
            _adminPin.value = newPin.trim()
            prefs.edit().putString("admin_pin", newPin.trim()).apply()
            return true
        }
        return false
    }

    /**
     * Load all registered user accounts for Admin Dashboard
     */
    fun loadAllRegisteredUsers(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            _isLoadingUsers.value = true
            _usersErrorMessage.value = null
            try {
                val cloudUsers = firebaseRealtime.fetchAllUsers()
                if (cloudUsers.isNotEmpty()) {
                    _registeredUsers.value = cloudUsers
                    _isLoadingUsers.value = false
                    onComplete?.invoke(cloudUsers.size)
                } else {
                    // Fallback to local accounts if any
                    val currentEmail = _shopInfo.value.userEmail
                    val fallbackList = if (currentEmail.isNotBlank()) {
                        listOf(
                            FirebaseUserAccount(
                                email = currentEmail,
                                passwordHash = "",
                                shopName = _shopInfo.value.shopName,
                                ownerName = _shopInfo.value.ownerName,
                                createdAt = System.currentTimeMillis() - 86400000L,
                                lastLoginAt = System.currentTimeMillis()
                            )
                        )
                    } else emptyList()
                    _registeredUsers.value = fallbackList
                    _isLoadingUsers.value = false
                    onComplete?.invoke(fallbackList.size)
                }
            } catch (e: Exception) {
                _isLoadingUsers.value = false
                _usersErrorMessage.value = "ইউজার তালিকা লোড করতে সমস্যা: ${e.localizedMessage}"
                onComplete?.invoke(_registeredUsers.value.size)
            }
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
    val previousDue: Double = 0.0,
    val totalCurrentDue: Double = 0.0,
    val paymentMethod: String,
    val timestamp: Long
)
