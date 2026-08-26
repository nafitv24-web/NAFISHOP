package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.window.DialogProperties
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

                        // Big Net Profit & Sales Overview banner (Cash on top, Due below, Cash profit on top, Due profit below)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x26000000), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // 1. আজকের বিক্রি (নগদ টাকা উপরে, বাকি টাকা নিচে)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "আজকের বিক্রি" else "Today's Sales",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFD1FAE5)
                                )
                                Text(
                                    text = "$currency${summary.todayCashSales.toIntOrNull() ?: summary.todayCashSales}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == "bn") "নগদ বিক্রি (উপরে)" else "Cash Sales",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 10.sp
                                )
                                if (summary.todayDueSales > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (language == "bn") "বাকি:" else "Due:"} $currency${summary.todayDueSales.toIntOrNull() ?: summary.todayDueSales}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFED7AA),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(Color(0x40FFFFFF))
                            )
                            // 2. আজকের লাভ ও লাভের হার (শুধুমাত্র পণ্য বিক্রি থেকে লাভ)
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
                                Text(
                                    text = if (language == "bn") "লাভের হার: ${String.format(Locale.US, "%.1f", summary.todayProfitMargin)}%" else "Margin: ${String.format(Locale.US, "%.1f", summary.todayProfitMargin)}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFEF08A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                if (summary.todayDueProfit > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (language == "bn") "নগদ লাভ:" else "Cash Profit:"} $currency${summary.todayRealizedProfit.toIntOrNull() ?: summary.todayRealizedProfit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFED7AA),
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(44.dp)
                                    .background(Color(0x40FFFFFF))
                            )
                            // 3. মোট বকেয়া বাকি
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
                                Text(
                                    text = if (summary.todayDueSales > 0) "+$currency${summary.todayDueSales.toIntOrNull() ?: summary.todayDueSales} ${if (language == "bn") "আজকে" else "today"}" else (if (language == "bn") "কাস্টমার বাকি" else "Outstanding"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFED7AA)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Main Cash Balance & Day-End Cash Settlement Card (দোকানদারের মূল ক্যাশ / হাতে নগদ টাকা - Compact & Sleek)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.5.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0xFFDCFCE7), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = "Main Cash",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "দোকানের মূল ক্যাশ (হাতে নগদ)" else "Main Cash in Hand",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (language == "bn") "বর্তমান নগদ তহবিল ব্যালেন্স" else "Current Cash Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showCashHistoryDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Cash History",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Current Balance Amount Display (Compact & High Contrast)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(EmeraldPrimary.copy(alpha = 0.3f), Color(0xFF10B981)))),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (language == "bn") "হাতে মোট নগদ ক্যাশ:" else "Total Cash In Hand:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF166534),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "$currency${mainBalance.toIntOrNull() ?: String.format(Locale.US, "%.2f", mainBalance)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857)
                                )
                            }

                            // Quick Balance Edit Button
                            OutlinedButton(
                                onClick = { showSetBalanceDialog = true },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Balance", modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (language == "bn") "সংশোধন" else "Set", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Cash In / Out Buttons (Compact)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddCashDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfitGreen),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == "bn") "ক্যাশ জমা" else "Add Cash", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showWithdrawCashDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LossRed),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == "bn") "ক্যাশ উত্তোলন" else "Withdraw", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Day-End Settle Action Button (Compact)
                    Button(
                        onClick = { showDayEndSettleDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "দিনশেষের বিক্রি ক্যাশ মেন ব্যালেন্সে যুক্ত করুন" else "Add Day-End Sales to Main Cash",
                            style = MaterialTheme.typography.labelSmall,
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
            },
            onAddIncome = { amount, note, timestamp ->
                viewModel.addCashIncome(amount, note, timestamp)
            },
            onAddExpense = { amount, note, timestamp ->
                viewModel.addCashExpense(amount, note, "অন্যান্য", timestamp)
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

    val presetAmounts = if (isPositive) listOf("500", "1000", "2000", "5000") else listOf("100", "200", "500", "1000")
    val presetNotes = if (isPositive) listOf("ক্যাশ বিক্রি", "বাকি আদায়", "ব্যক্তিগত জমা", "মহাজন ফেরত") else listOf("দোকান ভাড়া", "বিদ্যুৎ বিল", "চা-নাস্তা", "ব্যক্তিগত খরচ")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) ProfitGreen else LossRed
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (language == "bn") "টাকার পরিমাণ ($currency) *" else "Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetAmounts.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val cur = amountStr.toDoubleOrNull() ?: 0.0
                                    val add = preset.toDoubleOrNull() ?: 0.0
                                    amountStr = (cur + add).toInt().toString()
                                }
                        ) {
                            Text(
                                text = "+$currency$preset",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isPositive) ProfitGreen else LossRed,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "কারণ / বিবরণ *" else "Note / Reason *") },
                    placeholder = { Text(if (isPositive) "যেমন: নগদ বিক্রি, ব্যক্তিগত জমা" else "যেমন: দোকান ভাড়া, চা-নাস্তা") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetNotes.take(3).forEach { noteSuggestion ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { note = noteSuggestion }
                        ) {
                            Text(
                                text = noteSuggestion,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                        shape = RoundedCornerShape(8.dp),
                        enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        Text(
                            text = if (isPositive) (if (language == "bn") "ক্যাশ জমা করুন" else "Deposit") else (if (language == "bn") "ক্যাশ উত্তোলন করুন" else "Withdraw"),
                            fontWeight = FontWeight.Bold
                        )
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
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
                                .size(34.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "দোকানের মূল ক্যাশ এডিট" else "Edit Main Cash Balance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${if (language == "bn") "বর্তমান:" else "Current:"} $currency${currentBalance.toIntOrNull() ?: currentBalance}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = mode == "SET",
                        onClick = {
                            mode = "SET"
                            amountStr = currentBalance.toIntOrNull()?.toString() ?: currentBalance.toString()
                        },
                        label = { Text(if (language == "bn") "সেট (=)" else "Set (=)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == "ADD",
                        onClick = {
                            mode = "ADD"
                            amountStr = ""
                        },
                        label = { Text(if (language == "bn") "বৃদ্ধি (+)" else "Add (+)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == "SUBTRACT",
                        onClick = {
                            mode = "SUBTRACT"
                            amountStr = ""
                        },
                        label = { Text(if (language == "bn") "হ্রাস (-)" else "Sub (-)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "সংশোধনের কারণ" else "Reason") },
                    placeholder = { Text(if (language == "bn") "যেমন: ক্যাশ ড্রয়ার সমন্বয়" else "e.g. Reconciliation") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Result Preview Card
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "আপডেটের পর নতুন ক্যাশ:" else "New Resulting Cash:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$currency${resultingBalance.toIntOrNull() ?: resultingBalance}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header (Title & Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "দিনশেষের বিক্রি ক্যাশ ক্লোজিং" else "Day-End Cash Settle",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = if (language == "bn") "নগদ বিক্রি ও আদায় মূল ক্যাশে যুক্তকরণ" else "Add register cash to main balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (language == "bn") "দিনশেষে ড্রয়ারে থাকা আজকের আসল নগদ ক্যাশ গুনে মূল ক্যাশে যুক্ত করুন।" else "Reconcile register cash and transfer to shop's main cash in hand.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Breakdown Card with clean, non-breaking aligned rows
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Row 1: আগের মূল ক্যাশ
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == "bn") "পূর্বে মূল ক্যাশ:" else "Previous Main Cash:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currency${currentMainBalance.toIntOrNull() ?: currentMainBalance}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Row 2: আজকের নগদ বিক্রি
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == "bn") "আজকের নগদ বিক্রি (+):" else "Today Cash Sales (+):",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ProfitGreen,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "+$currency${summary.todayCashSales.toIntOrNull() ?: summary.todayCashSales}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }

                            // Row 3: আজকের বাকি আদায় (যদি থাকে)
                            if (summary.todayCollectedDue > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == "bn") "আজকের বাকি আদায় (+):" else "Due Collected (+):",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "+$currency${summary.todayCollectedDue.toIntOrNull() ?: summary.todayCollectedDue}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }

                            // Row 4: বাকিতে বিক্রি (বকেয়া সম্পর্কিত ব্যাখ্যা)
                            if (summary.todayDueSales > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == "bn") "আজকের বাকি বিক্রি (বকেয়া):" else "Today Due Sales:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = DueOrange
                                    )
                                    Text(
                                        text = "$currency${summary.todayDueSales.toIntOrNull() ?: summary.todayDueSales}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DueOrange
                                    )
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            // Resulting Balance Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == "bn") "নতুন সম্ভাব্য মূল ক্যাশ:" else "New Main Cash:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    text = "$currency${newExpectedBalance.toIntOrNull() ?: newExpectedBalance}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }

                    if (summary.todayDueSales > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == "bn") "বাকিতে বিক্রি ৳${summary.todayDueSales.toIntOrNull() ?: summary.todayDueSales} মোট বাকিতে যুক্ত হয়েছে, ক্যাশে আসবে না।" else "Due sales are tracked under Customer Due, not in drawer.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF92400E),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customAmountStr,
                        onValueChange = { customAmountStr = it },
                        label = { Text(if (language == "bn") "মূল ক্যাশে যুক্ত করার পরিমাণ ($currency) *" else "Amount to Add ($currency) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(if (language == "bn") "নোট / মন্তব্য" else "Note") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
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
                        shape = RoundedCornerShape(8.dp),
                        enabled = settledAmount > 0
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "bn") "যুক্ত ও ক্লোজ করুন" else "Add & Close")
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
    onDeleteLog: (CashLog) -> Unit,
    onAddIncome: ((Double, String, Long) -> Unit)? = null,
    onAddExpense: ((Double, String, Long) -> Unit)? = null
) {
    // Filter states: "ALL", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"
    var selectedPeriod by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingLog by remember { mutableStateOf<CashLog?>(null) }
    var deletingLog by remember { mutableStateOf<CashLog?>(null) }

    val now = remember { System.currentTimeMillis() }
    val calendar = remember { Calendar.getInstance() }

    val startOfDay = remember(now) {
        calendar.apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val startOfWeek = remember(now) {
        calendar.apply {
            timeInMillis = now
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val startOfMonth = remember(now) {
        calendar.apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val startOfYear = remember(now) {
        calendar.apply {
            timeInMillis = now
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val filteredLogs = remember(cashLogs, selectedPeriod, searchQuery) {
        cashLogs.filter { log ->
            val matchesPeriod = when (selectedPeriod) {
                "DAILY" -> log.timestamp >= startOfDay
                "WEEKLY" -> log.timestamp >= startOfWeek
                "MONTHLY" -> log.timestamp >= startOfMonth
                "YEARLY" -> log.timestamp >= startOfYear
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                log.note.contains(searchQuery, ignoreCase = true) ||
                log.type.contains(searchQuery, ignoreCase = true) ||
                log.amount.toString().contains(searchQuery)
            }
            matchesPeriod && matchesSearch
        }
    }

    val totalIncome = remember(filteredLogs) {
        filteredLogs.filter { log ->
            log.type in listOf("DEPOSIT", "DAY_END_CLOSING", "INCOME") || (log.type == "MANUAL_ADJUST" && log.amount >= 0)
        }.sumOf { it.amount }
    }

    val totalExpense = remember(filteredLogs) {
        filteredLogs.filter { log ->
            log.type in listOf("WITHDRAWAL", "EXPENSE")
        }.sumOf { it.amount }
    }

    val netBalance = totalIncome - totalExpense

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Title, Search, Close)
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(if (language == "bn") "নোট খুঁজুন..." else "Search...", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F172A),
                                    unfocusedContainerColor = Color(0xFF0F172A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.2f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (language == "bn") "নগদ ক্যাশ বই" else "Cash Book",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (language == "bn") "ক্যাশ লেনদেনের সম্পূর্ণ হিসাব" else "Complete cash transaction log",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }
                }

                // Filter Tabs (সব, দৈনিক, সাপ্তাহিক, মাসিক, বার্ষিক)
                Surface(
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val filters = listOf(
                            "ALL" to (if (language == "bn") "সব" else "All"),
                            "DAILY" to (if (language == "bn") "দৈনিক" else "Daily"),
                            "WEEKLY" to (if (language == "bn") "সাপ্তাহিক" else "Weekly"),
                            "MONTHLY" to (if (language == "bn") "মাসিক" else "Monthly"),
                            "YEARLY" to (if (language == "bn") "বার্ষিক" else "Yearly")
                        )

                        filters.forEach { (key, label) ->
                            val isSelected = selectedPeriod == key
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF334155),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { selectedPeriod = key }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = Color.White,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Summary Dashboard (মোট আয় | মোট খরচ | নিট ব্যালেন্স)
                Surface(
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "bn") "মোট আয় (+)" else "Total In (+)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$currency${totalIncome.toIntOrNull() ?: totalIncome}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Color(0xFF334155))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "bn") "মোট খরচ (-)" else "Total Out (-)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$currency${totalExpense.toIntOrNull() ?: totalExpense}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(28.dp)
                                .background(Color(0xFF334155))
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "bn") "হাতে ক্যাশ" else "Net Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                            Text(
                                text = "$currency${netBalance.toIntOrNull() ?: netBalance}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netBalance >= 0) Color(0xFF60A5FA) else Color(0xFFF87171)
                            )
                        }
                    }
                }

                // Table Body Rows (Clean List Layout with no narrow column wrapping)
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == "bn") "কোনো ক্যাশ লেনদেন পাওয়া যায়নি" else "No cash log entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredLogs) { log ->
                            val isAddition = log.type in listOf("DEPOSIT", "DAY_END_CLOSING", "INCOME") || (log.type == "MANUAL_ADJUST" && log.amount >= 0)

                            val dateFormatted = remember(log.timestamp) {
                                val cal = Calendar.getInstance().apply { timeInMillis = log.timestamp }
                                val dayOfWeekBn = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                    Calendar.SATURDAY -> "শনি"
                                    Calendar.SUNDAY -> "রবি"
                                    Calendar.MONDAY -> "সোম"
                                    Calendar.TUESDAY -> "মঙ্গল"
                                    Calendar.WEDNESDAY -> "বুধ"
                                    Calendar.THURSDAY -> "বৃহস্পতি"
                                    Calendar.FRIDAY -> "শুক্র"
                                    else -> ""
                                }
                                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(log.timestamp))
                                val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                                Triple(dayOfWeekBn, dateStr, timeStr)
                            }

                            val typeLabel = when (log.type) {
                                "DEPOSIT" -> if (language == "bn") "ক্যাশ জমা" else "Deposit"
                                "WITHDRAWAL" -> if (language == "bn") "ক্যাশ উত্তোলন" else "Withdrawal"
                                "DAY_END_CLOSING" -> if (language == "bn") "বিক্রি জমা" else "Daily Sales"
                                "INCOME" -> if (language == "bn") "নগদ আয়" else "Cash In"
                                "EXPENSE" -> if (language == "bn") "নগদ খরচ" else "Cash Out"
                                "MANUAL_ADJUST" -> if (language == "bn") "ব্যালেন্স সংশোধন" else "Adjust"
                                else -> log.type
                            }

                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(0.6.dp, Color(0xFF334155)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingLog = log }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = if (isAddition) Color(0xFF16A34A).copy(alpha = 0.2f) else Color(0xFFDC2626).copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = typeLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAddition) Color(0xFF4ADE80) else Color(0xFFF87171),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = if (language == "bn") "${dateFormatted.first}, ${dateFormatted.second} • ${dateFormatted.third}" else "${dateFormatted.second} • ${dateFormatted.third}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 11.sp
                                            )
                                        }

                                        Text(
                                            text = "${if (isAddition) "+" else "-"}$currency${log.amount.toIntOrNull() ?: log.amount}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isAddition) Color(0xFF4ADE80) else Color(0xFFF87171)
                                        )
                                    }

                                    if (log.note.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = log.note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFE2E8F0),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Sticky Action Buttons (আয় / খরচ)
                if (onAddIncome != null && onAddExpense != null) {
                    Surface(
                        color = Color(0xFF1E293B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showIncomeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "+ আয় (Cash In)" else "+ Cash In",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = { showExpenseDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "- খরচ (Cash Out)" else "- Cash Out",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal for Adding Income from Cash Book
    if (showIncomeDialog && onAddIncome != null) {
        CashInputDialog(
            title = if (language == "bn") "নগদ আয় যোগ করুন (Cash In)" else "Add Cash Income",
            currency = currency,
            language = language,
            isPositive = true,
            onDismiss = { showIncomeDialog = false },
            onConfirm = { amount, note ->
                onAddIncome(amount, note, System.currentTimeMillis())
                showIncomeDialog = false
            }
        )
    }

    // Modal for Adding Expense from Cash Book
    if (showExpenseDialog && onAddExpense != null) {
        CashInputDialog(
            title = if (language == "bn") "নগদ খরচ কর্তন করুন (Cash Out)" else "Record Cash Expense",
            currency = currency,
            language = language,
            isPositive = false,
            onDismiss = { showExpenseDialog = false },
            onConfirm = { amount, note ->
                onAddExpense(amount, note, System.currentTimeMillis())
                showExpenseDialog = false
            }
        )
    }

    // Sub-dialog for editing a cash log
    if (editingLog != null) {
        val targetLog = editingLog!!
        var editAmountStr by remember { mutableStateOf(targetLog.amount.toIntOrNull()?.toString() ?: targetLog.amount.toString()) }
        var editNote by remember { mutableStateOf(targetLog.note) }

        Dialog(onDismissRequest = { editingLog = null }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "ক্যাশ লেনদেন সংশোধন" else "Edit Cash Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = {
                            deletingLog = targetLog
                            editingLog = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editAmountStr,
                        onValueChange = { editAmountStr = it },
                        label = { Text(if (language == "bn") "টাকার পরিমাণ ($currency)" else "Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text(if (language == "bn") "নোট / বিবরণ" else "Note") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
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
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (language == "bn") "সংরক্ষণ" else "Save")
                        }
                    }
                }
            }
        }
    }

    // Delete Log Confirmation Dialog
    if (deletingLog != null) {
        val target = deletingLog!!
        AlertDialog(
            onDismissRequest = { deletingLog = null },
            title = { Text(if (language == "bn") "লেনদেন ডিলিট নিশ্চিতকরণ" else "Confirm Delete") },
            text = { Text(if (language == "bn") "আপনি কি এই ক্যাশ এন্ট্রিটি মুছে ফেলতে চান?" else "Are you sure you want to delete this entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLog(target)
                        deletingLog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text(if (language == "bn") "ডিলিট করুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingLog = null }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
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
    val context = LocalContext.current
    val dateStr = remember(tx.timestamp) {
        SimpleDateFormat("d MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    }

    val isSale = tx.type == "SALE"
    val isStockIn = tx.type == "STOCK_IN" || tx.type == "PURCHASE"
    val isDamage = tx.type == "STOCK_OUT_DAMAGE"

    val isDueSale = isSale && tx.dueAmount > 0
    val isFullDue = isSale && tx.dueAmount > 0 && tx.paidAmount == 0.0
    val isPartialDue = isSale && tx.dueAmount > 0 && tx.paidAmount > 0.0

    // Cash profit vs due profit for this transaction
    val cashRatio = if (isSale && tx.totalAmount > 0) (tx.paidAmount / tx.totalAmount).coerceIn(0.0, 1.0) else 1.0
    val realizedProfit = if (isSale) tx.profitAmount * cashRatio else 0.0
    val dueProfit = if (isSale) tx.profitAmount * (1.0 - cashRatio) else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(
            1.dp,
            when {
                isFullDue -> Color(0xFFFECACA)
                isPartialDue -> Color(0xFFFED7AA)
                isSale -> Color(0xFFBBF7D0)
                isStockIn -> Color(0xFFBFDBFE)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: Date & Time + Type Badge + Invoice No
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isFullDue -> Color(0xFFFEF2F2)
                        isPartialDue -> Color(0xFFFFF7ED)
                        isSale -> Color(0xFFF0FDF4)
                        isStockIn -> Color(0xFFEFF6FF)
                        else -> Color(0xFFF8FAFC)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isFullDue -> Color(0xFFFECACA)
                            isPartialDue -> Color(0xFFFFEDD5)
                            isSale -> Color(0xFFDCFCE7)
                            isStockIn -> Color(0xFFDBEAFE)
                            else -> Color(0xFFE2E8F0)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isSale -> Icons.Default.ShoppingCart
                                isStockIn -> Icons.Default.AddBusiness
                                else -> Icons.Default.Inventory
                            },
                            contentDescription = null,
                            tint = when {
                                isFullDue -> LossRed
                                isPartialDue -> DueOrange
                                isSale -> ProfitGreen
                                isStockIn -> StockBlue
                                else -> Color(0xFF64748B)
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isFullDue -> if (language == "bn") "সম্পূর্ণ বাকি বিক্রি" else "Full Due Sale"
                                isPartialDue -> if (language == "bn") "আংশিক বাকি বিক্রি" else "Partial Due Sale"
                                isSale -> if (language == "bn") "নগদ বিক্রি" else "Cash Sale"
                                isStockIn -> if (language == "bn") "স্টক ইন / ক্রয়" else "Stock In / Purchase"
                                else -> if (language == "bn") "স্টক সমন্বয়" else "Adjustment"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFullDue -> LossRed
                                isPartialDue -> DueOrange
                                isSale -> ProfitGreen
                                isStockIn -> StockBlue
                                else -> Color(0xFF64748B)
                            }
                        )
                    }
                }

                // Date & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Middle Section: Product Details & Customer Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.productName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}  •  ${if (language == "bn") "দর: " else "Rate: "}$currency${tx.unitPrice.toIntOrNull() ?: tx.unitPrice}/${tx.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (tx.customerName.isNotBlank() && tx.customerName != "ক্যাশ কাস্টমার") {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "${if (language == "bn") "কাস্টমার: " else "Customer: "}${tx.customerName}${if (tx.customerPhone.isNotBlank()) " (${tx.customerPhone})" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (tx.invoiceNumber.isNotBlank()) {
                        Text(
                            text = "${if (language == "bn") "মেমো নং: #" else "Memo: #"}${tx.invoiceNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (tx.note.isNotBlank()) {
                        Text(
                            text = "${if (language == "bn") "নোট: " else "Note: "}${tx.note}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Total Bill Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isSale) (if (language == "bn") "মোট বিল" else "Total Bill") else (if (language == "bn") "মোট খরচ" else "Total"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSale) MaterialTheme.colorScheme.onSurface else Color(0xFF1E293B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Structured Mizan Ledger Breakdown (Cash vs Due vs Profit)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSale) {
                        // 1. নগদ জমা
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (language == "bn") "নগদ জমা" else "Cash Paid",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "+$currency${tx.paidAmount.toIntOrNull() ?: tx.paidAmount}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(22.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // 2. বাকি
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (language == "bn") "বাকি" else "Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (tx.dueAmount > 0) Color(0xFF9A3412) else Color(0xFF166534)
                            )
                            Text(
                                text = if (tx.dueAmount > 0) "$currency${tx.dueAmount.toIntOrNull() ?: tx.dueAmount}" else (if (language == "bn") "নেই" else "None"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (tx.dueAmount > 0) DueOrange else ProfitGreen
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(22.dp), color = MaterialTheme.colorScheme.outlineVariant)

                        // 3. লাভ (নগদ লাভ ও বাকির লাভ)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (language == "bn") "নগদ লাভ" else "Cash Profit",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "$currency${realizedProfit.toIntOrNull() ?: String.format(Locale.US, "%.1f", realizedProfit)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (realizedProfit >= 0) ProfitGreen else LossRed
                            )
                        }

                        if (dueProfit > 0) {
                            VerticalDivider(modifier = Modifier.height(22.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "বাকির লাভ" else "Due Profit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFB45309)
                                )
                                Text(
                                    text = "$currency${dueProfit.toIntOrNull() ?: String.format(Locale.US, "%.1f", dueProfit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DueOrange
                                )
                            }
                        }
                    } else {
                        // Stock In details
                        Column {
                            Text(
                                text = if (language == "bn") "ক্রয়কৃত পরিমাণ" else "Purchased Qty",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = StockBlue
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (language == "bn") "পেমেন্ট মাধ্যম" else "Payment Method",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = tx.paymentMethod,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Action Row: Quick buttons for edit, view invoice, SMS
            if (isSale) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tx.customerPhone.isNotBlank()) {
                        TextButton(
                            onClick = {
                                val msg = "শ্রদ্ধেয় ${tx.customerName}, মেমো #${tx.invoiceNumber} এ পণ্য: ${tx.productName} (${tx.quantity} ${tx.unit}), মোট বিল: $currency${tx.totalAmount}, নগদ জমা: $currency${tx.paidAmount}, বাকি: $currency${tx.dueAmount}।"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "মেসেজ / WhatsApp পাঠান"))
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (language == "bn") "শেয়ার" else "Share", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    OutlinedButton(
                        onClick = { onClick?.invoke() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (language == "bn") "এডিট / ফেরত" else "Edit / Return", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
