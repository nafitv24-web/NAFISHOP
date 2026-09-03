package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.components.AdminPanelDialog
import com.example.ui.components.AppNoticeDialog
import com.example.ui.components.AppPermissionDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.InvoiceDialog
import com.example.ui.components.NafiShopSmallLogo
import com.example.ui.components.NewsNoticeTickerBar
import com.example.ui.components.PermissionHelper
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel

enum class ShopScreen(
    val bnTitle: String,
    val enTitle: String,
    val icon: ImageVector
) {
    DASHBOARD("ড্যাশবোর্ড", "Dashboard", Icons.Default.Dashboard),
    POS("বিক্রয় POS", "POS Sale", Icons.Default.PointOfSale),
    ACCOUNTS("হিসাব", "Accounts", Icons.Default.Calculate),
    INVENTORY("স্টক ও পণ্য", "Stock & Inventory", Icons.Default.Inventory2),
    DUE_KHATA("বাকি খাতা", "Due Khata", Icons.Default.AccountBalanceWallet),
    EXPENSES("খরচ হিসাব", "Expenses", Icons.Default.ReceiptLong),
    REPORTS("রিপোর্ট", "Reports", Icons.Default.Assessment),
    SETTINGS("সেটিংস", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    viewModel: ShopViewModel
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    if (!isLoggedIn) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = { /* Logged in */ }
        )
        return
    }

    var currentScreen by remember { mutableStateOf(ShopScreen.DASHBOARD) }
    val language by viewModel.language.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val invoiceDetails by viewModel.currentInvoice.collectAsState()

    var showQuickStockInDialog by remember { mutableStateOf(false) }
    var showQuickAddExpenseDialog by remember { mutableStateOf(false) }
    val products by viewModel.products.collectAsState()

    val appUpdateInfo by viewModel.appUpdateInfo.collectAsState()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsState()
    val activeNotice by viewModel.activeNotice.collectAsState()

    var userDismissedUpdateDialog by remember { mutableStateOf(false) }
    var userDismissedNoticeDialog by remember { mutableStateOf(false) }
    var showNoticeDetailsDialog by remember { mutableStateOf<com.example.data.model.AppNotice?>(null) }
    var showAdminPanelFromTop by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var showPermissionDialog by remember {
        mutableStateOf(!PermissionHelper.areAllPermissionsGranted(context))
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            NafiShopSmallLogo(size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") currentScreen.bnTitle else currentScreen.enTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = shopInfo.shopName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        // Dark / Light Mode Toggle Button
                        IconButton(onClick = { viewModel.toggleDarkMode() }) {
                            Icon(
                                imageVector = if (themeMode == "DARK") Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark/Light Mode",
                                tint = if (themeMode == "DARK") AmberTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // News / Notice action icon
                        IconButton(onClick = {
                            if (activeNotice != null) {
                                showNoticeDetailsDialog = activeNotice
                            } else {
                                showAdminPanelFromTop = true
                            }
                        }) {
                            BadgedBox(
                                badge = {
                                    if (activeNotice != null && activeNotice!!.isActive) {
                                        Badge(containerColor = LossRed)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Campaign,
                                    contentDescription = "News & Notice",
                                    tint = if (activeNotice != null && activeNotice!!.isActive) LossRed else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (currentScreen != ShopScreen.POS) {
                            FilledTonalButton(
                                onClick = { currentScreen = ShopScreen.POS },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                    contentColor = EmeraldPrimary
                                )
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == "bn") "বিক্রি" else "Sale", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Top Live Breaking News & Notice Ticker Bar
                NewsNoticeTickerBar(
                    activeNotice = activeNotice,
                    language = language,
                    onNoticeClick = { notice ->
                        showNoticeDetailsDialog = notice
                    },
                    onOpenAdminPanel = {
                        showAdminPanelFromTop = true
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Dashboard / Home
                    val isHome = currentScreen == ShopScreen.DASHBOARD
                    IconButton(
                        onClick = { currentScreen = ShopScreen.DASHBOARD },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isHome) Icons.Default.Home else Icons.Default.Home,
                                contentDescription = "Home",
                                tint = if (isHome) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == "bn") "হোম" else "Home",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isHome) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHome) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // 2. Accounts / হিসাব
                    val isAccounts = currentScreen == ShopScreen.ACCOUNTS
                    IconButton(
                        onClick = { currentScreen = ShopScreen.ACCOUNTS },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Accounts",
                                tint = if (isAccounts) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == "bn") "হিসাব" else "Accounts",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isAccounts) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAccounts) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // 3. Center Elevated Quick Sale / Scanner Button (Iconic Circle)
                    Box(
                        modifier = Modifier
                            .weight(1.1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = { currentScreen = ShopScreen.POS },
                            shape = CircleShape,
                            color = TealDarkHeader,
                            shadowElevation = 6.dp,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PointOfSale,
                                    contentDescription = "POS Sale",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // 4. Due Khata / বাকি খাতা
                    val isDue = currentScreen == ShopScreen.DUE_KHATA
                    IconButton(
                        onClick = { currentScreen = ShopScreen.DUE_KHATA },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = "Due",
                                tint = if (isDue) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == "bn") "বাকি খাতা" else "Due",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isDue) FontWeight.Bold else FontWeight.Normal,
                                color = if (isDue) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // 5. Settings & More / মেনু
                    val isSettings = currentScreen == ShopScreen.SETTINGS
                    IconButton(
                        onClick = { currentScreen = ShopScreen.SETTINGS },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = if (isSettings) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == "bn") "মেনু" else "More",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSettings) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ShopScreen.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToPos = { currentScreen = ShopScreen.POS },
                        onNavigateToStock = { currentScreen = ShopScreen.INVENTORY },
                        onNavigateToDue = { currentScreen = ShopScreen.DUE_KHATA },
                        onNavigateToExpenses = { currentScreen = ShopScreen.EXPENSES },
                        onOpenStockInDialog = { showQuickStockInDialog = true },
                        onOpenAddExpenseDialog = { showQuickAddExpenseDialog = true },
                        onNavigateToAccounts = { currentScreen = ShopScreen.ACCOUNTS }
                    )
                }
                ShopScreen.POS -> {
                    PosSaleScreen(
                        viewModel = viewModel,
                        onSaleCompleted = {
                            // Automatically triggers invoiceDetails modal
                        }
                    )
                }
                ShopScreen.ACCOUNTS -> {
                    AccountsScreen(
                        viewModel = viewModel,
                        onNavigateToDue = { currentScreen = ShopScreen.DUE_KHATA },
                        onNavigateToPos = { currentScreen = ShopScreen.POS }
                    )
                }
                ShopScreen.INVENTORY -> {
                    InventoryScreen(
                        viewModel = viewModel,
                        onNavigateToPos = { currentScreen = ShopScreen.POS }
                    )
                }
                ShopScreen.DUE_KHATA -> {
                    DueKhataScreen(
                        viewModel = viewModel
                    )
                }
                ShopScreen.EXPENSES -> {
                    ExpenseScreen(
                        viewModel = viewModel
                    )
                }
                ShopScreen.REPORTS -> {
                    ReportsScreen(
                        viewModel = viewModel
                    )
                }
                ShopScreen.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateToAccounts = { currentScreen = ShopScreen.ACCOUNTS },
                        onNavigateToReports = { currentScreen = ShopScreen.REPORTS }
                    )
                }
            }
        }
    }

    // Global Digital Cash Memo Dialog (if invoice generated)
    invoiceDetails?.let { invoice ->
        InvoiceDialog(
            invoice = invoice,
            currency = shopInfo.currency,
            language = language,
            onDismiss = { viewModel.clearCurrentInvoice() }
        )
    }

    // Quick Stock In Dialog for Dashboard shortcut
    if (showQuickStockInDialog) {
        if (products.isNotEmpty()) {
            StockInDialog(
                product = products.first(),
                allProducts = products,
                currency = shopInfo.currency,
                language = language,
                onDismiss = { showQuickStockInDialog = false },
                onConfirm = { targetProd, qty, buyP, sellP, note ->
                    viewModel.stockIn(targetProd.id, qty, buyP, sellP, note)
                    showQuickStockInDialog = false
                }
            )
        } else {
            AddEditProductDialog(
                product = null,
                currency = shopInfo.currency,
                language = language,
                savedCategories = customCategories,
                onDismiss = { showQuickStockInDialog = false },
                onSave = { newProd ->
                    viewModel.saveProduct(newProd)
                    showQuickStockInDialog = false
                }
            )
        }
    }

    // Quick Expense Dialog for Dashboard shortcut
    if (showQuickAddExpenseDialog) {
        AddExpenseDialog(
            currency = shopInfo.currency,
            language = language,
            onDismiss = { showQuickAddExpenseDialog = false },
            onSave = { title, category, amount, note ->
                viewModel.addExpense(title, category, amount, note)
                showQuickAddExpenseDialog = false
            }
        )
    }

    // App Permissions Dialog on install / launch
    if (showPermissionDialog) {
        AppPermissionDialog(
            language = language,
            onDismiss = { showPermissionDialog = false },
            onPermissionsResult = { granted ->
                showPermissionDialog = false
            }
        )
    }

    // Automatic App Update Dialog (if new version detected and not yet dismissed)
    if (isUpdateAvailable && !userDismissedUpdateDialog && appUpdateInfo != null) {
        AppUpdateDialog(
            updateInfo = appUpdateInfo,
            currentVersion = viewModel.currentAppVersion,
            language = language,
            onDismiss = { userDismissedUpdateDialog = true }
        )
    }

    // Automatic Admin Notice Dialog (if active broadcast notice and not yet dismissed)
    if (activeNotice != null && !userDismissedNoticeDialog) {
        AppNoticeDialog(
            notice = activeNotice!!,
            language = language,
            onDismiss = { userDismissedNoticeDialog = true }
        )
    }

    // Manual Notice Detail Dialog (when user taps on the top News / Notice Bar or Action Icon)
    if (showNoticeDetailsDialog != null) {
        AppNoticeDialog(
            notice = showNoticeDetailsDialog!!,
            language = language,
            onDismiss = { showNoticeDetailsDialog = null }
        )
    }

    // Admin Panel Dialog opened from top bar or news banner
    if (showAdminPanelFromTop) {
        AdminPanelDialog(
            viewModel = viewModel,
            onDismiss = { showAdminPanelFromTop = false }
        )
    }
}
