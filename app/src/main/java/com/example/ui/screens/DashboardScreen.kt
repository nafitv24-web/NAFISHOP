package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CashLog
import com.example.data.model.Product
import com.example.data.model.TransactionRecord
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: ShopViewModel,
    onNavigateToPos: () -> Unit,
    onNavigateToStock: () -> Unit,
    onNavigateToDue: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onOpenStockInDialog: () -> Unit,
    onOpenAddExpenseDialog: () -> Unit
) {
    val summary by viewModel.dashboardSummary.collectAsState()
    val lowStockItems by viewModel.lowStockProducts.collectAsState()
    val recentTxs by viewModel.recentTransactions.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val cashLogs by viewModel.cashLogs.collectAsState()
    val mainBalance = shopInfo.mainBalance
    val currency = shopInfo.currency

    var showAddCashDialog by remember { mutableStateOf(false) }
    var showWithdrawCashDialog by remember { mutableStateOf(false) }
    var showSetBalanceDialog by remember { mutableStateOf(false) }
    var showDayEndSettleDialog by remember { mutableStateOf(false) }
    var showCashHistoryDialog by remember { mutableStateOf(false) }

    val todayDateFormatted = remember {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH)
        sdf.format(Date())
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Shop Greeting Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, Color(0xFF047857))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = shopInfo.shopName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${shopInfo.ownerName} • $todayDateFormatted",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD1FAE5)
                                )
                            }
                            // Google Cloud Status Pill
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0x33FFFFFF),
                                modifier = Modifier.clip(CircleShape)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = "Cloud",
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == "bn") "ব্যাকআপ সক্রিয়" else "Cloud Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Net Profit & Sales Overview banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x26000000), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "আজকের বিক্রি" else "Today's Sales",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD1FAE5)
                                )
                                Text(
                                    text = "$currency${summary.todaySales.toIntOrNull() ?: summary.todaySales}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(Color(0x40FFFFFF))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "আজকের লাভ" else "Today's Profit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFDE68A)
                                )
                                Text(
                                    text = "$currency${summary.todayProfit.toIntOrNull() ?: summary.todayProfit}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (summary.todayProfit >= 0) Color(0xFFFDE68A) else Color(0xFFFCA5A5)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(Color(0x40FFFFFF))
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "মোট বাকি" else "Total Due",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFED7AA)
                                )
                                Text(
                                    text = "$currency${summary.totalOutstandingDue.toIntOrNull() ?: summary.totalOutstandingDue}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDBA74)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Main Cash Balance & Day-End Cash Settlement Card (দোকানদারের মূল ক্যাশ / হাতে নগদ টাকা)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Main Cash",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "দোকানের মূল ক্যাশ (হাতে নগদ)" else "Main Cash in Hand",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (language == "bn") "বর্তমান নগদ তহবিল ব্যালেন্স" else "Current Cash Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        IconButton(
                            onClick = { showCashHistoryDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Cash History",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big Current Balance Amount Display
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldPrimary.copy(alpha = 0.4f), Color(0xFF10B981)))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (language == "bn") "হাতে মোট নগদ ক্যাশ:" else "Total Cash In Hand:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = "$currency${mainBalance.toIntOrNull() ?: String.format(Locale.US, "%.2f", mainBalance)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857)
                                )
                            }

                            // Quick Balance Edit Button
                            OutlinedButton(
                                onClick = { showSetBalanceDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Balance", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == "bn") "সংশোধন" else "Set", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Cash In / Out Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddCashDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfitGreen)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "ক্যাশ জমা" else "Add Cash", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showWithdrawCashDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed)
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "ক্যাশ উত্তোলন" else "Withdraw", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Day-End Settle Action Button
                    Button(
                        onClick = { showDayEndSettleDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "দিনশেষের বিক্রি ক্যাশ মেন ব্যালেন্সে যুক্ত করুন" else "Add Day-End Sales to Main Cash",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Quick Action Buttons (পণ্য বিক্রি, স্টক ইন, খরচ যোগ, বাকি খাতা)
        item {
            Text(
                text = if (language == "bn") "দ্রুত শর্টকাট" else "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    title = if (language == "bn") "পণ্য বিক্রি" else "New Sale",
                    icon = Icons.Default.PointOfSale,
                    containerColor = Color(0xFFECFDF5),
                    contentColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToPos
                )
                QuickActionButton(
                    title = if (language == "bn") "স্টক ইন" else "Stock In",
                    icon = Icons.Default.AddBox,
                    containerColor = Color(0xFFEFF6FF),
                    contentColor = StockBlue,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStockInDialog
                )
                QuickActionButton(
                    title = if (language == "bn") "খরচ যোগ" else "Add Expense",
                    icon = Icons.Default.ReceiptLong,
                    containerColor = Color(0xFFFEF2F2),
                    contentColor = LossRed,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenAddExpenseDialog
                )
                QuickActionButton(
                    title = if (language == "bn") "বাকি খাতা" else "Due Khata",
                    icon = Icons.Default.AccountBalanceWallet,
                    containerColor = Color(0xFFFFFBEB),
                    contentColor = DueOrange,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToDue
                )
            }
        }

        // 4. Financial Metrics Cards Grid (আজকের ক্রয়, আজকের খরচ, মোট স্টক মূল্য, মোট পণ্য)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = if (language == "bn") "আজকের কেনা/ক্রয়" else "Today's Purchase",
                    value = "$currency${summary.todayPurchases.toIntOrNull() ?: summary.todayPurchases}",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    iconColor = StockBlue,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (language == "bn") "আজকের খরচ" else "Today's Expense",
                    value = "$currency${summary.todayExpenses.toIntOrNull() ?: summary.todayExpenses}",
                    icon = Icons.AutoMirrored.Filled.TrendingDown,
                    iconColor = LossRed,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = if (language == "bn") "দোকানের মোট স্টক মূল্য" else "Stock Inventory Value",
                    value = "$currency${summary.totalStockValue.toIntOrNull() ?: summary.totalStockValue}",
                    icon = Icons.Default.Inventory2,
                    iconColor = EmeraldPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = if (language == "bn") "মোট পণ্য আইটেম" else "Total Products",
                    value = "${summary.totalProductsCount} ${if (language == "bn") "টি" else "Items"}",
                    icon = Icons.Default.Category,
                    iconColor = AmberTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Low Stock Alert Banner (যদি কোনো পণ্যের স্টক কম থাকে)
        if (lowStockItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFEA580C))))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    contentDescription = "Warning",
                                    tint = Color(0xFFB45309)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "bn") "জরুরী কম স্টক সতর্কতা (${lowStockItems.size})" else "Low Stock Alert (${lowStockItems.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            TextButton(onClick = onNavigateToStock) {
                                Text(
                                    text = if (language == "bn") "সব দেখুন" else "View All",
                                    color = Color(0xFFB45309),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            items(lowStockItems) { prod ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = CardDefaults.outlinedCardBorder()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = prod.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "${if (language == "bn") "বাকি আছে: " else "Left: "}${prod.stockQuantity.toIntOrNull() ?: prod.stockQuantity} ${prod.unit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = LossRed,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Recent Transactions Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == "bn") "সাম্প্রতিক লেনদেন" else "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${recentTxs.size} ${if (language == "bn") "টি" else "items"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (recentTxs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == "bn") "এখনও কোনো লেনদেন রেকর্ড হয়নি" else "No transactions recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(recentTxs.take(10)) { tx ->
                TransactionFeedItem(tx = tx, currency = currency, language = language)
            }
        }
    }

    // Cash Management Dialogs
    if (showAddCashDialog) {
        CashInputDialog(
            title = if (language == "bn") "মূল ক্যাশে টাকা জমা" else "Add Cash into Drawer",
            currency = currency,
            language = language,
            isPositive = true,
            onDismiss = { showAddCashDialog = false },
            onConfirm = { amount, note ->
                viewModel.addCashToMainBalance(amount, note)
                showAddCashDialog = false
            }
        )
    }

    if (showWithdrawCashDialog) {
        CashInputDialog(
            title = if (language == "bn") "মূল ক্যাশ থেকে উত্তোলন" else "Withdraw Cash",
            currency = currency,
            language = language,
            isPositive = false,
            onDismiss = { showWithdrawCashDialog = false },
            onConfirm = { amount, note ->
                viewModel.withdrawCashFromMainBalance(amount, note)
                showWithdrawCashDialog = false
            }
        )
    }

    if (showSetBalanceDialog) {
        SetMainBalanceDialog(
            currentBalance = mainBalance,
            currency = currency,
            language = language,
            onDismiss = { showSetBalanceDialog = false },
            onConfirm = { amount, note ->
                viewModel.updateMainBalance(amount, note)
                showSetBalanceDialog = false
            }
        )
    }

    if (showDayEndSettleDialog) {
        DayEndSettlementDialog(
            summary = summary,
            currentMainBalance = mainBalance,
            currency = currency,
            language = language,
            onDismiss = { showDayEndSettleDialog = false },
            onConfirm = { note ->
                viewModel.settleDayEndCashToMainBalance(summary.todaySales, note)
                showDayEndSettleDialog = false
            }
        )
    }

    if (showCashHistoryDialog) {
        CashHistoryDialog(
            cashLogs = cashLogs,
            currency = currency,
            language = language,
            onDismiss = { showCashHistoryDialog = false }
        )
    }
}

@Composable
fun CashInputDialog(
    title: String,
    currency: String,
    language: String,
    isPositive: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) ProfitGreen else LossRed
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (language == "bn") "টাকার পরিমাণ ($currency)" else "Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "কারণ / বিবরণ (যেমন: ব্যক্তিগত জমা, মহাজন পরিশোধ)" else "Note / Reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (language == "bn") "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                onConfirm(amount, note.ifBlank { if (isPositive) "ক্যাশ জমা" else "ক্যাশ উত্তোলন" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPositive) ProfitGreen else LossRed
                        ),
                        enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text(if (language == "bn") "নিশ্চিত করুন" else "Confirm")
                    }
                }
            }
        }
    }
}

@Composable
fun SetMainBalanceDialog(
    currentBalance: Double,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountStr by remember { mutableStateOf(currentBalance.toString().replace(".0", "")) }
    var note by remember { mutableStateOf("সরাসরি ক্যাশ ব্যালেন্স সেট/সংশোধন") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "মূল ক্যাশ ব্যালেন্স পরিবর্তন" else "Set Main Balance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (language == "bn") "আপনার ক্যাশ ড্রয়ারে এখন সর্বমোট কত টাকা আছে তা লিখুন" else "Enter current exact cash in your cash register/drawer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (language == "bn") "নতুন মূল ক্যাশ ($currency)" else "New Balance") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "সংশোধনের বিবরণ" else "Reason / Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (language == "bn") "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            onConfirm(amount, note)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(if (language == "bn") "সেট করুন" else "Set Balance")
                    }
                }
            }
        }
    }
}

@Composable
fun DayEndSettlementDialog(
    summary: com.example.data.model.DashboardSummary,
    currentMainBalance: Double,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var note by remember { mutableStateOf("আজকের দিনের মোট বিক্রির ক্যাশ ক্লোজিং") }
    val newExpectedBalance = currentMainBalance + summary.todaySales

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == "bn") "দিনশেষের বিক্রি ক্যাশ ক্লোজিং" else "Day-End Cash Settle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (language == "bn") "আজ সারাদিনের মোট বিক্রি মূল ক্যাশ ব্যালেন্সে যোগ করা হবে।" else "Today's sales will be added directly into your main cash balance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (language == "bn") "বর্তমান মূল ক্যাশ:" else "Current Cash:", style = MaterialTheme.typography.bodyMedium)
                            Text("$currency${currentMainBalance.toIntOrNull() ?: currentMainBalance}", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (language == "bn") "আজকের মোট বিক্রি (+):" else "Today's Total Sales (+):", style = MaterialTheme.typography.bodyMedium, color = ProfitGreen)
                            Text("+$currency${summary.todaySales.toIntOrNull() ?: summary.todaySales}", fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (language == "bn") "ক্লোজিং এর পর নতুন ক্যাশ:" else "New Total Cash Balance:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            Text("$currency${newExpectedBalance.toIntOrNull() ?: newExpectedBalance}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "নোট / মন্তব্য" else "Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (language == "bn") "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(note) },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(if (language == "bn") "ক্যাশে যুক্ত করুন" else "Add to Cash")
                    }
                }
            }
        }
    }
}

@Composable
fun CashHistoryDialog(
    cashLogs: List<CashLog>,
    currency: String,
    language: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "ক্যাশ লেজার / হিস্টোরি" else "Cash Drawer Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (cashLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (language == "bn") "কোনো ক্যাশ লেনদেনের ইতিহাস পাওয়া যায়নি" else "No cash log records found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(cashLogs) { log ->
                            val dateStr = remember(log.timestamp) {
                                SimpleDateFormat("hh:mm a, d MMM yyyy", Locale.getDefault()).format(Date(log.timestamp))
                            }
                            val isAddition = log.type in listOf("DEPOSIT", "DAY_END_CLOSING") || (log.type == "MANUAL_ADJUST" && log.amount >= 0)

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = when (log.type) {
                                                "DEPOSIT" -> if (language == "bn") "ক্যাশ জমা" else "Cash Deposit"
                                                "WITHDRAWAL" -> if (language == "bn") "ক্যাশ উত্তোলন" else "Cash Withdrawal"
                                                "DAY_END_CLOSING" -> if (language == "bn") "দিনশেষের বিক্রি ক্যাশ" else "Day-End Sales Added"
                                                "MANUAL_ADJUST" -> if (language == "bn") "ব্যালেন্স সংশোধন" else "Balance Set"
                                                else -> log.type
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAddition) ProfitGreen else LossRed
                                        )
                                        Text(
                                            text = "${log.note} • $dateStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "${if (language == "bn") "নতুন ব্যালেন্স: " else "New Bal: "}$currency${log.balanceAfter.toIntOrNull() ?: log.balanceAfter}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "${if (isAddition) "+" else "-"}$currency${log.amount.toIntOrNull() ?: log.amount}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isAddition) ProfitGreen else LossRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TransactionFeedItem(
    tx: TransactionRecord,
    currency: String,
    language: String
) {
    val dateStr = remember(tx.timestamp) {
        SimpleDateFormat("hh:mm a, d MMM", Locale.getDefault()).format(Date(tx.timestamp))
    }

    val isSale = tx.type == "SALE"
    val isStockIn = tx.type == "STOCK_IN"
    val isDamage = tx.type == "STOCK_OUT_DAMAGE"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when {
                            isSale -> ProfitGreen.copy(alpha = 0.12f)
                            isStockIn -> StockBlue.copy(alpha = 0.12f)
                            else -> LossRed.copy(alpha = 0.12f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        isSale -> Icons.Default.ShoppingCart
                        isStockIn -> Icons.Default.AddBusiness
                        else -> Icons.Default.DeleteOutline
                    },
                    contentDescription = null,
                    tint = when {
                        isSale -> ProfitGreen
                        isStockIn -> StockBlue
                        else -> LossRed
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${if (isSale) "বিক্রি" else if (isStockIn) "স্টক ইন" else "সমন্বয়"} • ${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit} • $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                if (tx.customerName.isNotBlank() && tx.customerName != "ক্যাশ কাস্টমার") {
                    Text(
                        text = "ক্রেতা: ${tx.customerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isSale) "+" else "-"}$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isSale) ProfitGreen else Color(0xFF1E293B)
                )
                if (isSale && tx.profitAmount != 0.0) {
                    Text(
                        text = "${if (language == "bn") "লাভ: " else "Profit: "}$currency${tx.profitAmount.toIntOrNull() ?: tx.profitAmount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tx.profitAmount >= 0) ProfitGreen else LossRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
