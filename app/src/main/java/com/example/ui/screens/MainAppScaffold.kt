package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
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
    TAILOR_KHATA("দর্জি ও জামার মাপ", "Tailor & Measurements", Icons.Default.Straighten),
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = currentScreen != ShopScreen.DASHBOARD || drawerState.isOpen) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentScreen = ShopScreen.DASHBOARD
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.widthIn(max = 320.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(EmeraldPrimary, TealDarkHeader)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        NafiShopSmallLogo(size = 32.dp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = shopInfo.shopName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = shopInfo.ownerName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "স্মার্ট দোকান ও দর্জি খাতা",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "মেনু ও ফিচারসমূহ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )

                    // 1. ড্যাশবোর্ড
                    NavigationDrawerItem(
                        label = { Text(if (language == "bn") "ড্যাশবোর্ড (হোম)" else "Dashboard") },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                        selected = currentScreen == ShopScreen.DASHBOARD,
                        onClick = {
                            currentScreen = ShopScreen.DASHBOARD
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // 2. ✂️ দর্জি ও জামার মাপ খাতা (Highlighted)
                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (language == "bn") "দর্জি ও জামার মাপ খাতা" else "Tailor & Measurements",
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentScreen == ShopScreen.TAILOR_KHATA) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "কামিজ, সেলোয়ার, ফ্রক, পেটিকোট, ব্লাউজ",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = EmeraldPrimary
                                ) {
                                    Text(
                                        text = "নতুন",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Straighten,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        selected = currentScreen == ShopScreen.TAILOR_KHATA,
                        onClick = {
                            currentScreen = ShopScreen.TAILOR_KHATA
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                            unselectedContainerColor = EmeraldPrimary.copy(alpha = 0.06f)
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))

                    // 3. বিক্রয় POS
                    NavigationDrawerItem(
                        label = { Text(if (language == "bn") "বিক্রয় POS (ক্যাশ মেমো)" else "POS Sale") },
                        icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
                        selected = currentScreen == ShopScreen.POS,
                        onClick = {
                            currentScreen = ShopScreen.POS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // 4. হিসাব ও ক্যাশ
                    NavigationDrawerItem(
                        label = { Text(if (language == "bn") "হিসাব ও ক্যাশ খাতা" else "Accounts") },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                        selected = currentScreen == ShopScreen.ACCOUNTS,
                        onClick = {
                            currentScreen = ShopScreen.ACCOUNTS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // 5. স্টক ও পণ্য
                    NavigationDrawerItem(
                        label = { Text(if (language == "bn") "স্টক ও পণ্য তালিকা" else "Stock & Inventory") },
                        icon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                        selected = currentScreen == ShopScreen.INVENTORY,
                        onClick = {
                            currentScreen = ShopScreen.INVENTORY
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // 6. বাকি খাতা
                    NavigationDrawerItem(
                        label = { Text(if (language == "bn") "বাকি খাতা (কাস্টমার লেজার)" else "Due Khata") },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                        selected = currentScreen == ShopScreen.DUE_KHATA,
                        onClick = {
                            currentScreen = ShopScreen.DUE_KHATA
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // 7. খরচ হিসাব
                    NavigationDrawerItem(
                        label = { Text(if (language == "bn") "দোকান খরচ হিসাব" else "Expenses") },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                        selected = currentScreen == ShopScreen.EXPENSES,
                        onClick = {
                            currentScreen = ShopScreen.EXPENSES
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // 8. রিপোর্ট
                    NavigationDrawerItem(
                        label = { Text(if (language == "bn") "রিপোর্ট ও লাভ-ক্ষতি" else "Reports") },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                        selected = currentScreen == ShopScreen.REPORTS,
                        onClick = {
                            currentScreen = ShopScreen.REPORTS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    // 9. সেটিংস
                    NavigationDrawerItem(
                        label = { Text(if (language == "bn") "সেটিংস ও ব্যাকআপ" else "Settings") },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        selected = currentScreen == ShopScreen.SETTINGS,
                        onClick = {
                            currentScreen = ShopScreen.SETTINGS
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "নাফি খাতা v2.4.0 • স্মার্ট দোকান ও দর্জি খাতা",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Open Side Menu",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
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
                        onNavigateToAccounts = { currentScreen = ShopScreen.ACCOUNTS },
                        onNavigateToReports = { currentScreen = ShopScreen.REPORTS },
                        onNavigateToTailor = { currentScreen = ShopScreen.TAILOR_KHATA }
                    )
                }
                ShopScreen.TAILOR_KHATA -> {
                    TailorKhataScreen(
                        viewModel = viewModel
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
                        onNavigateToReports = { currentScreen = ShopScreen.REPORTS },
                        onNavigateToTailor = { currentScreen = ShopScreen.TAILOR_KHATA }
                    )
                }
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
