package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CashLog
import com.example.data.model.Product
import com.example.data.model.TransactionRecord
import com.example.ui.components.EditOrReturnSaleDialog
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
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
    val context = LocalContext.current
    val summary by viewModel.dashboardSummary.collectAsState()
    val lowStockItems by viewModel.lowStockProducts.collectAsState()
    val recentTxs by viewModel.recentTransactions.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val dueLogs by viewModel.dueLogs.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val cashLogs by viewModel.cashLogs.collectAsState()
    val mainBalance = shopInfo.mainBalance
    val currency = shopInfo.currency

    val calendar = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val startOfToday = calendar.timeInMillis

    val todayDueSalesList = remember(allTransactions) {
        allTransactions.filter { it.timestamp >= startOfToday && it.type == "SALE" && it.dueAmount > 0 }
    }

    var showAddCashDialog by remember { mutableStateOf(false) }
    var showWithdrawCashDialog by remember { mutableStateOf(false) }
    var showSetBalanceDialog by remember { mutableStateOf(false) }
    var showDayEndSettleDialog by remember { mutableStateOf(false) }
    var showCashHistoryDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }

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
                                if (summary.todayDueSales > 0) {
                                    Text(
                                        text = "${if (language == "bn") "নগদ:" else "Cash:"} $currency${summary.todayCashSales.toIntOrNull() ?: summary.todayCashSales}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
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
                                    text = if (language == "bn") "মোট বকেয়া বাকি" else "Total Due",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFED7AA)
                                )
                                Text(
                                    text = "$currency${summary.totalOutstandingDue.toIntOrNull() ?: summary.totalOutstandingDue}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDBA74)
                                )
                                if (summary.todayDueSales > 0) {
                                    Text(
                                        text = "+$currency${summary.todayDueSales.toIntOrNull() ?: summary.todayDueSales} ${if (language == "bn") "আজকে" else "today"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFED7AA)
                                    )
                                }
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

        // 5.5. Today's Due Sales & Customers (আজকে কার কতো টাকার কোন পণ্যের বাকি নিয়েছে)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, Color(0xFFFDBA74).copy(alpha = 0.6f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFFF7ED), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = DueOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "আজকের বাকিতে বিক্রি ও কাস্টমার তালিকা" else "Today's Credit Sales & Due",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (language == "bn") "আজকের মোট বাকি: $currency${summary.todayDueSales.toIntOrNull() ?: summary.todayDueSales} (ক্যাশে যোগ হয়নি)" else "Today's Due: $currency${summary.todayDueSales}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DueOrange,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        TextButton(onClick = onNavigateToDue) {
                            Text(
                                text = if (language == "bn") "বাকি খাতা" else "Due Khata",
                                color = DueOrange,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (todayDueSalesList.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAFAFA),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "bn") "আজকে নতুন কোনো বাকি দেওয়া হয়নি।" else "No credit sales recorded today.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            todayDueSalesList.forEach { tx ->
                                val matchingCust = customers.find {
                                    (tx.customerPhone.isNotBlank() && it.phone == tx.customerPhone) ||
                                    (tx.customerName.isNotBlank() && it.name.equals(tx.customerName, ignoreCase = true))
                                }
                                val custTotalDue = matchingCust?.totalDue ?: tx.dueAmount
                                val prevDue = (custTotalDue - tx.dueAmount).coerceAtLeast(0.0)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.customerName.ifBlank { "কাস্টমার" },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF78350F)
                                                )
                                                if (tx.customerPhone.isNotBlank()) {
                                                    Text(
                                                        text = "📞 ${tx.customerPhone}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF92400E)
                                                    )
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "আজকের বাকি: $currency${tx.dueAmount.toIntOrNull() ?: tx.dueAmount}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = LossRed
                                                )
                                                Text(
                                                    text = "মোট বর্তমান বকেয়া: $currency${custTotalDue.toIntOrNull() ?: custTotalDue}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Product detail
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = CardDefaults.outlinedCardBorder(),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = StockBlue, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "পণ্য: ${tx.productName} (${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}) • মোট বিল: $currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount} (জমা: $currency${tx.paidAmount.toIntOrNull() ?: tx.paidAmount})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF334155),
                                                    maxLines = 2
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // SMS and WhatsApp Actions for this specific customer
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            val smsMsg = com.example.util.CustomerSmsHelper.buildDueSaleMessage(
                                                shopName = shopInfo.shopName,
                                                shopPhone = shopInfo.phone,
                                                customerName = tx.customerName.ifBlank { "সম্মানিত ক্রেতা" },
                                                invoiceNo = tx.invoiceNumber,
                                                purchasedItemsSummary = "• ${tx.productName} (${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}) = $currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}",
                                                saleTotal = tx.totalAmount,
                                                paidAmount = tx.paidAmount,
                                                todayNewDue = tx.dueAmount,
                                                previousDue = prevDue,
                                                totalCurrentDue = custTotalDue,
                                                currency = currency
                                            )

                                            Button(
                                                onClick = {
                                                    com.example.util.CustomerSmsHelper.sendDirectSms(
                                                        context = context,
                                                        phone = tx.customerPhone,
                                                        message = smsMsg
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (language == "bn") "এসএমএস পাঠান" else "Send SMS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    com.example.util.CustomerSmsHelper.sendWhatsAppMessage(
                                                        context = context,
                                                        phone = tx.customerPhone,
                                                        message = smsMsg
                                                    )
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfitGreen),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (language == "bn") "হোয়াটসঅ্যাপ" else "WhatsApp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            }
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

                if (recentTxs.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            val pdfFile = PdfGenerator.generateTransactionsListPdf(
                                context = context,
                                shopName = shopInfo.shopName,
                                title = if (language == "bn") "দোকানের লেনদেন রিপোর্ট (Transactions Record)" else "Shop Transactions Record",
                                transactions = recentTxs,
                                currency = currency
                            )
                            if (pdfFile != null) {
                                PdfGenerator.openOrSharePdf(
                                    context = context,
                                    file = pdfFile,
                                    chooserTitle = if (language == "bn") "লেনদেন PDF ডাউনলোড / শেয়ার" else "Download / Share Transactions PDF"
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "লেনদেন PDF" else "Transactions PDF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "${recentTxs.size} ${if (language == "bn") "টি" else "items"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
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
                TransactionFeedItem(
                    tx = tx,
                    currency = currency,
                    language = language,
                    onClick = {
                        if (tx.type == "SALE") {
                            editingTransaction = tx
                        }
                    }
                )
            }
        }
    }

    // Edit / Return Transaction Dialog
    if (editingTransaction != null) {
        val txToEdit = editingTransaction!!
        EditOrReturnSaleDialog(
            transaction = txToEdit,
            currency = currency,
            language = language,
            onDismiss = { editingTransaction = null },
            onReturnItem = { returnQty, note ->
                viewModel.returnProductSale(txToEdit, returnQty, note)
                editingTransaction = null
            },
            onEditSale = { newQty, newPrice, newCustomerName, newNote ->
                viewModel.editSaleTransaction(txToEdit, newQty, newPrice, newCustomerName, newNote)
                editingTransaction = null
            },
            onDeleteSale = {
                viewModel.deleteSaleAndRestock(txToEdit)
                editingTransaction = null
            }
        )
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
        EditMainCashBalanceDialog(
            currentBalance = mainBalance,
            currency = currency,
            language = language,
            onDismiss = { showSetBalanceDialog = false },
            onConfirm = { newBalance, note ->
                viewModel.updateMainBalance(newBalance, note)
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
            onConfirm = { settledAmount, note ->
                viewModel.settleDayEndCashToMainBalance(settledAmount, note)
                showDayEndSettleDialog = false
            }
        )
    }

    if (showCashHistoryDialog) {
        CashHistoryDialog(
            cashLogs = cashLogs,
            currency = currency,
            language = language,
            onDismiss = { showCashHistoryDialog = false },
            onEditLog = { log, newAmount, newNote ->
                viewModel.editCashLog(log, newAmount, newNote)
            },
            onDeleteLog = { log ->
                viewModel.deleteCashLog(log)
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMainCashBalanceDialog(
    currentBalance: Double,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    // Mode: "ADD", "SUBTRACT", "SET"
    var mode by remember { mutableStateOf("SET") }
    var amountStr by remember {
        mutableStateOf(currentBalance.toIntOrNull()?.toString() ?: currentBalance.toString())
    }
    var note by remember { mutableStateOf("") }

    val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
    val resultingBalance = remember(mode, enteredAmount, currentBalance) {
        when (mode) {
            "ADD" -> currentBalance + enteredAmount
            "SUBTRACT" -> (currentBalance - enteredAmount).coerceAtLeast(0.0)
            else -> enteredAmount
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "দোকানের মূল ক্যাশ এডিট" else "Edit Main Cash Balance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${if (language == "bn") "বর্তমান মূল ক্যাশ:" else "Current:"} $currency${currentBalance.toIntOrNull() ?: currentBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = mode == "SET",
                        onClick = {
                            mode = "SET"
                            amountStr = currentBalance.toIntOrNull()?.toString() ?: currentBalance.toString()
                        },
                        label = { Text(if (language == "bn") "সরাসরি সেট (=)" else "Set Exact (=)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == "ADD",
                        onClick = {
                            mode = "ADD"
                            amountStr = ""
                        },
                        label = { Text(if (language == "bn") "ক্যাশ বৃদ্ধি (+)" else "Add (+)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == "SUBTRACT",
                        onClick = {
                            mode = "SUBTRACT"
                            amountStr = ""
                        },
                        label = { Text(if (language == "bn") "ক্যাশ হ্রাস (-)" else "Subtract (-)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = {
                        Text(
                            when (mode) {
                                "ADD" -> if (language == "bn") "কত টাকা যোগ করবেন ($currency)" else "Amount to Add ($currency)"
                                "SUBTRACT" -> if (language == "bn") "কত টাকা বিয়োগ করবেন ($currency)" else "Amount to Subtract ($currency)"
                                else -> if (language == "bn") "নতুন মূল ক্যাশ ব্যালেন্স ($currency)" else "New Total Balance ($currency)"
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "সংশোধনের বিবরণ / কারণ" else "Reason / Note") },
                    placeholder = { Text(if (language == "bn") "যেমন: ক্যাশ ড্রয়ার হিসাব সমন্বয়" else "e.g. Cash drawer reconciliation") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Result Preview Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "আপডেটের পর নতুন মূল ক্যাশ:" else "New Resulting Cash:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$currency${resultingBalance.toIntOrNull() ?: resultingBalance}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

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
                            val defaultNote = when (mode) {
                                "ADD" -> "ক্যাশ বৃদ্ধি (+ $currency$enteredAmount)"
                                "SUBTRACT" -> "ক্যাশ হ্রাস (- $currency$enteredAmount)"
                                else -> "সরাসরি ক্যাশ ব্যালেন্স সংশোধন"
                            }
                            onConfirm(resultingBalance, note.ifBlank { defaultNote })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == "bn") "আপডেট করুন" else "Update Balance")
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
    onConfirm: (settledAmount: Double, note: String) -> Unit
) {
    // The actual cash received today from sales & due collections (excluding unpaid due sales):
    val defaultClosingAmount = summary.todayCashSales + summary.todayCollectedDue
    var customAmountStr by remember { mutableStateOf(defaultClosingAmount.toIntOrNull()?.toString() ?: defaultClosingAmount.toString()) }
    var note by remember { mutableStateOf("আজকের দিনের নগদ বিক্রি ও আদায় ক্যাশ ক্লোজিং") }

    val settledAmount = customAmountStr.toDoubleOrNull() ?: 0.0
    val newExpectedBalance = currentMainBalance + settledAmount

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "দিনশেষের বিক্রি ক্যাশ ক্লোজিং" else "Day-End Cash Settle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = if (language == "bn") "নগদ বিক্রি ও আদায় মূল ক্যাশে যুক্তকরণ" else "Reconcile register with main cash",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (language == "bn") "বাকিতে বিক্রি সরাসরি মোট বাকিতে জমা হয়। দিনশেষে ড্রয়ারে থাকা আজকের নগদ বিক্রি ও বাকি আদায়ের আসল ক্যাশ গুনে মূল ক্যাশে যুক্ত করুন।" else "Due sales are recorded in Total Due. Only actual cash collected should be added to main balance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Breakdown Card with opening and closing details
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (language == "bn") "১. পূর্বে দোকানের মূল ক্যাশ:" else "1. Previous Main Cash:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$currency${currentMainBalance.toIntOrNull() ?: currentMainBalance}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (language == "bn") "২. আজকের নগদ বিক্রি (+):" else "2. Today's Cash Sales (+):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ProfitGreen
                            )
                            Text(
                                text = "+$currency${summary.todayCashSales.toIntOrNull() ?: summary.todayCashSales}",
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }
                        if (summary.todayCollectedDue > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (language == "bn") "৩. আজকের বাকি আদায় (+):" else "3. Today's Due Collected (+):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "+$currency${summary.todayCollectedDue.toIntOrNull() ?: summary.todayCollectedDue}",
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                        if (summary.todayDueSales > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (language == "bn") "৪. আজকের বাকিতে বিক্রি (বকেয়া):" else "4. Today's Due Sales (Credit):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DueOrange
                                )
                                Text(
                                    text = "$currency${summary.todayDueSales.toIntOrNull() ?: summary.todayDueSales} (ক্যাশে যোগ হবে না)",
                                    fontWeight = FontWeight.Bold,
                                    color = DueOrange
                                )
                            }
                        }
                        if (summary.todayExpenses > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = if (language == "bn") "৫. আজকের খরচ (-):" else "5. Today's Expenses (-):",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LossRed
                                )
                                Text(
                                    text = "-$currency${summary.todayExpenses.toIntOrNull() ?: summary.todayExpenses}",
                                    fontWeight = FontWeight.Bold,
                                    color = LossRed
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = if (language == "bn") "যুক্ত করার পর নতুন মূল ক্যাশ:" else "New Total Main Cash:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "$currency${newExpectedBalance.toIntOrNull() ?: newExpectedBalance}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                if (summary.todayDueSales > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "বাকিতে বিক্রি ৳${summary.todayDueSales.toIntOrNull() ?: summary.todayDueSales} টাকা বকেয়া থাকায় ক্যাশে যোগ হবে না, মোট বাকিতে আছে।" else "Due sales are tracked in Total Due and not in cash drawer.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = customAmountStr,
                    onValueChange = { customAmountStr = it },
                    label = { Text(if (language == "bn") "মূল ক্যাশে যুক্ত করার টাকার পরিমাণ ($currency) *" else "Amount to Add to Main Cash ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "নোট / মন্তব্য" else "Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
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
                            if (settledAmount > 0) {
                                onConfirm(settledAmount, note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        enabled = settledAmount > 0
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == "bn") "ক্যাশে যুক্ত ও ক্লোজ করুন" else "Add to Cash & Close")
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
    onDismiss: () -> Unit,
    onEditLog: (CashLog, Double, String) -> Unit,
    onDeleteLog: (CashLog) -> Unit
) {
    var editingLog by remember { mutableStateOf<CashLog?>(null) }
    var editAmountStr by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }

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

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isAddition) "+" else "-"}$currency${log.amount.toIntOrNull() ?: log.amount}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isAddition) ProfitGreen else LossRed
                                        )
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    editingLog = log
                                                    editAmountStr = log.amount.toIntOrNull()?.toString() ?: log.amount.toString()
                                                    editNote = log.note
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = { onDeleteLog(log) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Sub-dialog for editing a cash log
    if (editingLog != null) {
        val targetLog = editingLog!!
        Dialog(onDismissRequest = { editingLog = null }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = if (language == "bn") "ক্যাশ লেনদেন সংশোধন" else "Edit Cash Entry",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editAmountStr,
                        onValueChange = { editAmountStr = it },
                        label = { Text(if (language == "bn") "টাকার পরিমাণ ($currency)" else "Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text(if (language == "bn") "নোট / বিবরণ" else "Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingLog = null }) {
                            Text(if (language == "bn") "বাতিল" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = editAmountStr.toDoubleOrNull() ?: targetLog.amount
                                onEditLog(targetLog, amount, editNote)
                                editingLog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text(if (language == "bn") "সংরক্ষণ" else "Save")
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
    language: String,
    onClick: (() -> Unit)? = null
) {
    val dateStr = remember(tx.timestamp) {
        SimpleDateFormat("hh:mm a, d MMM", Locale.getDefault()).format(Date(tx.timestamp))
    }

    val isSale = tx.type == "SALE"
    val isStockIn = tx.type == "STOCK_IN"
    val isDamage = tx.type == "STOCK_OUT_DAMAGE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tx.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isSale) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit or Return",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
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
