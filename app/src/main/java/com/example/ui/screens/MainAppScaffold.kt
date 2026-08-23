package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.ui.components.InvoiceDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel

enum class ShopScreen(
    val bnTitle: String,
    val enTitle: String,
    val icon: ImageVector
) {
    DASHBOARD("ড্যাশবোর্ড", "Dashboard", Icons.Default.Dashboard),
    POS("বিক্রয় POS", "POS Sale", Icons.Default.PointOfSale),
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
    var currentScreen by remember { mutableStateOf(ShopScreen.DASHBOARD) }
    val language by viewModel.language.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val invoiceDetails by viewModel.currentInvoice.collectAsState()

    var showQuickStockInDialog by remember { mutableStateOf(false) }
    var showQuickAddExpenseDialog by remember { mutableStateOf(false) }
    val products by viewModel.products.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                },
                actions = {
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
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                listOf(
                    ShopScreen.DASHBOARD,
                    ShopScreen.POS,
                    ShopScreen.INVENTORY,
                    ShopScreen.DUE_KHATA,
                    ShopScreen.EXPENSES,
                    ShopScreen.REPORTS,
                    ShopScreen.SETTINGS
                ).forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        selected = selected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = if (language == "bn") screen.bnTitle else screen.enTitle
                            )
                        },
                        label = {
                            Text(
                                text = if (language == "bn") screen.bnTitle else screen.enTitle,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldPrimary,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                        )
                    )
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
                        onOpenAddExpenseDialog = { showQuickAddExpenseDialog = true }
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
                        viewModel = viewModel
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
}
