package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CashLog
import com.example.data.model.Product
import com.example.data.model.TransactionRecord
import com.example.ui.components.DueTagadaReminderDialog
import com.example.ui.components.EditOrReturnSaleDialog
import com.example.ui.components.NafiShopSmallLogo
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
    val isOnline by viewModel.isOnline.collectAsState()
    val autoBackupStatus by viewModel.autoBackupStatus.collectAsState()
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
    var showAllMemosDialog by remember { mutableStateOf(false) }
    var showDueSmsReminderDialog by remember { mutableStateOf(false) }
    var showCloudBackupInfoDialog by remember { mutableStateOf(false) }
    var showBusinessSummaryDetailDialog by remember { mutableStateOf(false) }
    var showAllServicesDialog by remember { mutableStateOf(false) }
    var isHeaderSearchVisible by remember { mutableStateOf(false) }
    var bannerPageIndex by remember { mutableIntStateOf(0) }
    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }

    // Date Filter State for All Transactions: "TODAY", "YESTERDAY", "WEEK", "MONTH", "CUSTOM", "ALL"
    var selectedDateFilter by remember { mutableStateOf("TODAY") }
    var customDateTimestamp by remember { mutableStateOf<Long?>(null) }
    var customDateLabel by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "SALE", "DUE", "STOCK_IN"
    var txSearchQuery by remember { mutableStateOf("") }

    val todayDateFormatted = remember {
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH)
        sdf.format(Date())
    }

    // Calculate time ranges for date filtering
    val (dateRangeStart, dateRangeEnd, dateFilterDisplayName) = remember(selectedDateFilter, customDateTimestamp, language) {
        val cal = Calendar.getInstance()
        val nowTime = System.currentTimeMillis()
        val dateDisplaySdf = SimpleDateFormat("d MMMM yyyy", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH)

        when (selectedDateFilter) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                val end = start + 86400000L - 1L
                Triple(start, end, if (language == "bn") "আজ (${dateDisplaySdf.format(Date(start))})" else "Today (${dateDisplaySdf.format(Date(start))})")
            }
            "YESTERDAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val start = cal.timeInMillis
                val end = start + 86400000L - 1L
                Triple(start, end, if (language == "bn") "গতকাল (${dateDisplaySdf.format(Date(start))})" else "Yesterday (${dateDisplaySdf.format(Date(start))})")
            }
            "WEEK" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.add(Calendar.DAY_OF_YEAR, -6)
                val start = cal.timeInMillis
                Triple(start, nowTime, if (language == "bn") "গত ৭ দিন" else "Last 7 Days")
            }
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                Triple(start, nowTime, if (language == "bn") "চলতি মাস" else "This Month")
            }
            "CUSTOM" -> {
                val target = customDateTimestamp ?: startOfToday
                val start = target
                val end = target + 86400000L - 1L
                Triple(start, end, if (customDateLabel.isNotBlank()) customDateLabel else dateDisplaySdf.format(Date(target)))
            }
            else -> {
                Triple(0L, Long.MAX_VALUE, if (language == "bn") "সব লেনদেন (সর্বকালীন)" else "All Transactions")
            }
        }
    }

    // Filter transactions based on date, type, and search query
    val filteredTransactions = remember(allTransactions, dateRangeStart, dateRangeEnd, selectedTypeFilter, txSearchQuery) {
        allTransactions.filter { tx ->
            val matchDate = tx.timestamp in dateRangeStart..dateRangeEnd
            val matchType = when (selectedTypeFilter) {
                "SALE" -> tx.type == "SALE"
                "DUE" -> tx.type == "SALE" && tx.dueAmount > 0
                "STOCK_IN" -> tx.type == "STOCK_IN" || tx.type == "PURCHASE"
                else -> true
            }
            val matchSearch = if (txSearchQuery.isBlank()) true else {
                val q = txSearchQuery.trim().lowercase()
                tx.productName.lowercase().contains(q) ||
                tx.customerName.lowercase().contains(q) ||
                tx.customerPhone.lowercase().contains(q) ||
                tx.invoiceNumber.lowercase().contains(q) ||
                tx.note.lowercase().contains(q)
            }
            matchDate && matchType && matchSearch
        }
    }

    // Summary calculations for the filtered transactions
    val filteredTotalSales = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "SALE" }.sumOf { it.totalAmount }
    }
    val filteredTotalProfit = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "SALE" }.sumOf { it.profitAmount }
    }
    val filteredTotalCashPaid = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "SALE" }.sumOf { it.paidAmount }
    }
    val filteredTotalDue = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "SALE" }.sumOf { it.dueAmount }
    }

    // Group transactions by formatted date string
    val groupedTransactions = remember(filteredTransactions, language) {
        val groupSdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val headerSdf = SimpleDateFormat("EEEE, d MMMM yyyy", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH)
        val todayStr = groupSdf.format(Date(startOfToday))
        val yesterdayStr = groupSdf.format(Date(startOfToday - 86400000L))

        filteredTransactions.groupBy { tx ->
            val key = groupSdf.format(Date(tx.timestamp))
            val dateLabel = when (key) {
                todayStr -> if (language == "bn") "আজ • ${headerSdf.format(Date(tx.timestamp))}" else "Today • ${headerSdf.format(Date(tx.timestamp))}"
                yesterdayStr -> if (language == "bn") "গতকাল • ${headerSdf.format(Date(tx.timestamp))}" else "Yesterday • ${headerSdf.format(Date(tx.timestamp))}"
                else -> headerSdf.format(Date(tx.timestamp))
            }
            dateLabel
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Curved Deep Teal Hero Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(TealDarkHeader, TealGradientEnd)
                        )
                    )
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == "bn") "ড্যাশবোর্ড" else "Dashboard",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${shopInfo.shopName} • $todayDateFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD1FAE5)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Search Toggle Button
                            IconButton(
                                onClick = { isHeaderSearchVisible = !isHeaderSearchVisible },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Notification Bell with Badge
                            Box {
                                IconButton(
                                    onClick = { showAllMemosDialog = true },
                                    modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                ) {
                                    Icon(
                                        Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(10.dp)
                                ) {}
                            }
                        }
                    }

                    // In-Header Animated Search Box
                    AnimatedVisibility(visible = isHeaderSearchVisible) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = txSearchQuery,
                                onValueChange = { txSearchQuery = it },
                                placeholder = {
                                    Text(
                                        if (language == "bn") "মেমো, কাস্টমার, বা পণ্য খুঁজুন..." else "Search invoices, customers, products...",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                },
                                singleLine = true,
                                trailingIcon = {
                                    if (txSearchQuery.isNotBlank()) {
                                        IconButton(onClick = { txSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.White,
                                    unfocusedBorderColor = Color(0x66FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0x26000000),
                                    unfocusedContainerColor = Color(0x26000000)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Hero Promotional & Stats Carousel Banner
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFF0FDF4),
                                        Color(0xFFE6F4F1),
                                        Color(0xFFECFDF5)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Brand & Details
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rounded Brand Logo
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    shadowElevation = 3.dp,
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        NafiShopSmallLogo(size = 42.dp)
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (language == "bn") "আজকের মোট বিক্রি" else "Today's Total Sales",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF0F5147),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFDCFCE7),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = if (summary.todayProfit >= 0) "+$currency${summary.todayProfit.toIntOrNull() ?: summary.todayProfit}" else "$currency${summary.todayProfit.toIntOrNull() ?: summary.todayProfit}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (summary.todayProfit >= 0) ProfitGreen else LossRed,
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$currency${summary.todayTotalSales.toIntOrNull() ?: summary.todayTotalSales}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TealDarkHeader
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            onClick = { showCashHistoryDialog = true },
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0F5147).copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${if (language == "bn") "হাতে নগদ:" else "Cash:"} $currency${mainBalance.toIntOrNull() ?: mainBalance}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F5147),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Right Visual Action Pill
                            Surface(
                                onClick = onNavigateToPos,
                                shape = RoundedCornerShape(14.dp),
                                color = TealDarkHeader,
                                shadowElevation = 2.dp,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == "bn") "মেমো তৈরি" else "New Sale",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Carousel Dots Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = index == bannerPageIndex
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) TealDarkHeader else Color(0xFFCBD5E1))
                                .size(width = if (isSelected) 18.dp else 6.dp, height = 6.dp)
                                .clickable { bannerPageIndex = index }
                        )
                    }
                }
            }
        }

        // 3. The Iconic 3x2 Core Action Grid (6 Rounded Elevated Cards)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Row 1: নগদ বিক্রি, বাকি খাতা, ক্যাশ খাতা
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. নগদ বিক্রি
                    CoreGridActionCard(
                        title = if (language == "bn") "নগদ বিক্রি" else "Cash Sale",
                        icon = Icons.Default.GridView,
                        iconBg = Color(0xFFECFDF5),
                        iconTint = EmeraldPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToPos
                    )

                    // 2. বাকি খাতা
                    CoreGridActionCard(
                        title = if (language == "bn") "বাকি খাতা" else "Due Khata",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconBg = Color(0xFFFFF7ED),
                        iconTint = DueOrange,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToDue
                    )

                    // 3. ক্যাশ খাতা
                    CoreGridActionCard(
                        title = if (language == "bn") "ক্যাশ খাতা" else "Cash Book",
                        icon = Icons.Default.ReceiptLong,
                        iconBg = Color(0xFFF0FDF4),
                        iconTint = ProfitGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { showCashHistoryDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 2: স্টক ও পণ্য, দোকান খরচ, লাভ ও রিপোর্ট
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 4. স্টক ও পণ্য
                    CoreGridActionCard(
                        title = if (language == "bn") "স্টক ও পণ্য" else "Stock",
                        icon = Icons.Default.Inventory2,
                        iconBg = Color(0xFFEFF6FF),
                        iconTint = StockBlue,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToStock
                    )

                    // 5. দোকান খরচ
                    CoreGridActionCard(
                        title = if (language == "bn") "দোকান খরচ" else "Expenses",
                        icon = Icons.Default.TrendingDown,
                        iconBg = Color(0xFFFEF2F2),
                        iconTint = LossRed,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToExpenses
                    )

                    // 6. লাভ ও রিপোর্ট
                    CoreGridActionCard(
                        title = if (language == "bn") "লাভ ও রিপোর্ট" else "Reports",
                        icon = Icons.Default.Assessment,
                        iconBg = Color(0xFFFAF5FF),
                        iconTint = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f),
                        onClick = { showBusinessSummaryDetailDialog = true }
                    )
                }
            }
        }

        // 4. "সার্ভিসেস ও অপশন" (Quick Services Row with 'See All')
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "সার্ভিসেস ও অপশন" else "Services & Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { showAllServicesDialog = true }) {
                        Text(
                            text = if (language == "bn") "সব দেখুন" else "See All",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Service 1: স্টক ইন
                    ServiceCircleItem(
                        title = if (language == "bn") "স্টক ইন" else "Stock In",
                        icon = Icons.Default.AddBox,
                        bgColor = Color(0xFFEFF6FF),
                        iconTint = StockBlue,
                        onClick = onOpenStockInDialog
                    )

                    // Service 2: ডিজিটাল মেমো
                    ServiceCircleItem(
                        title = if (language == "bn") "ডিজিটাল মেমো" else "Invoices",
                        icon = Icons.Default.Receipt,
                        bgColor = Color(0xFFF0FDF4),
                        iconTint = ProfitGreen,
                        onClick = { showAllMemosDialog = true }
                    )

                    // Service 3: বাকি তাগাদা
                    ServiceCircleItem(
                        title = if (language == "bn") "বাকি তাগাদা" else "Due SMS",
                        icon = Icons.Default.Sms,
                        bgColor = Color(0xFFFFFBEB),
                        iconTint = DueOrange,
                        onClick = { showDueSmsReminderDialog = true }
                    )

                    // Service 4: ব্যাকআপ
                    ServiceCircleItem(
                        title = if (language == "bn") "ডাটা ব্যাকআপ" else "Backup",
                        icon = Icons.Default.CloudSync,
                        bgColor = Color(0xFFF5F3FF),
                        iconTint = Color(0xFF7C3AED),
                        onClick = { showCloudBackupInfoDialog = true }
                    )

                    // Service 5: ক্যাশ জমা
                    ServiceCircleItem(
                        title = if (language == "bn") "ক্যাশ জমা" else "Cash In",
                        icon = Icons.Default.AccountBalanceWallet,
                        bgColor = Color(0xFFECFDF5),
                        iconTint = EmeraldPrimary,
                        onClick = { showAddCashDialog = true }
                    )
                }
            }
        }

        // 5. "ব্যবসায়িক সারসংক্ষেপ" (Business Summary & Financial Metrics with 'See All')
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "ব্যবসায়িক সারসংক্ষেপ" else "Business Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(onClick = { showBusinessSummaryDetailDialog = true }) {
                        Text(
                            text = if (language == "bn") "সব দেখুন" else "See All",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Main Cash in Hand Card (High Priority Core Balance)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.5.dp),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.AccountBalanceWallet,
                                            contentDescription = "Main Cash",
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (language == "bn") "দোকানের মূল ক্যাশ (হাতে নগদ)" else "Main Cash in Hand",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (language == "bn") "চলতি নগদ তহবিল ব্যালেন্স" else "Current Cash Balance",
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Current Balance Display Pill
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
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

                                OutlinedButton(
                                    onClick = { showSetBalanceDialog = true },
                                    shape = RoundedCornerShape(8.dp),
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

                        // Quick Cash In / Out Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                        // Day-End Settle Action Button
                        Button(
                            onClick = { showDayEndSettleDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (summary.todayUnclosedCash > 0) EmeraldPrimary else MaterialTheme.colorScheme.secondary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val btnText = if (summary.todayUnclosedCash > 0) {
                                if (language == "bn") "দিনশেষের বিক্রি ক্যাশ ক্লোজিং (+$currency${summary.todayUnclosedCash.toIntOrNull() ?: summary.todayUnclosedCash})"
                                else "Day-End Cash Settle (+$currency${summary.todayUnclosedCash.toIntOrNull() ?: summary.todayUnclosedCash})"
                            } else {
                                if (language == "bn") "দিনশেষের বিক্রি ক্যাশ ক্লোজিং (ক্লোজড ✓ ৳০)"
                                else "Day-End Cash Settle (Settled ✓ ৳0)"
                            }
                            Text(
                                text = btnText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Financial Overview 2x2 Grid (আজকের বিক্রি, আজকের লাভ, মোট বকেয়া বাকি, মোট স্টক মূল্য)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (language == "bn") "আজকের মোট বিক্রি" else "Today's Sales",
                        value = "$currency${summary.todayTotalSales.toIntOrNull() ?: summary.todayTotalSales}",
                        icon = Icons.Default.PointOfSale,
                        iconColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (language == "bn") "আজকের নিট লাভ" else "Today's Profit",
                        value = "$currency${summary.todayProfit.toIntOrNull() ?: summary.todayProfit}",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconColor = if (summary.todayProfit >= 0) ProfitGreen else LossRed,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (language == "bn") "মোট বকেয়া বাকি" else "Total Due",
                        value = "$currency${summary.totalOutstandingDue.toIntOrNull() ?: summary.totalOutstandingDue}",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconColor = DueOrange,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (language == "bn") "দোকানের মোট স্টক" else "Total Stock Value",
                        value = "$currency${summary.totalStockValue.toIntOrNull() ?: summary.totalStockValue}",
                        icon = Icons.Default.Inventory2,
                        iconColor = StockBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
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

        // 6. Interactive Date-Wise Transactions Explorer & Ledger
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header with Title and PDF Download
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "তারিখ অনুযায়ী সকল লেনদেন" else "Transactions by Date",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = dateFilterDisplayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (filteredTransactions.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = {
                                    val pdfFile = PdfGenerator.generateTransactionsListPdf(
                                        context = context,
                                        shopName = shopInfo.shopName,
                                        title = if (language == "bn") "লেনদেন রিপোর্ট - $dateFilterDisplayName" else "Transactions Record - $dateFilterDisplayName",
                                        transactions = filteredTransactions,
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
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "PDF" else "PDF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date Filter Chips Row
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Today
                        item {
                            FilterChip(
                                selected = selectedDateFilter == "TODAY",
                                onClick = {
                                    selectedDateFilter = "TODAY"
                                    customDateTimestamp = null
                                },
                                label = { Text(if (language == "bn") "আজকে" else "Today", fontSize = 12.sp) },
                                leadingIcon = if (selectedDateFilter == "TODAY") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // 2. Yesterday
                        item {
                            FilterChip(
                                selected = selectedDateFilter == "YESTERDAY",
                                onClick = {
                                    selectedDateFilter = "YESTERDAY"
                                    customDateTimestamp = null
                                },
                                label = { Text(if (language == "bn") "গতকাল" else "Yesterday", fontSize = 12.sp) },
                                leadingIcon = if (selectedDateFilter == "YESTERDAY") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // 3. Last 7 Days
                        item {
                            FilterChip(
                                selected = selectedDateFilter == "WEEK",
                                onClick = {
                                    selectedDateFilter = "WEEK"
                                    customDateTimestamp = null
                                },
                                label = { Text(if (language == "bn") "গত ৭ দিন" else "7 Days", fontSize = 12.sp) },
                                leadingIcon = if (selectedDateFilter == "WEEK") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // 4. This Month
                        item {
                            FilterChip(
                                selected = selectedDateFilter == "MONTH",
                                onClick = {
                                    selectedDateFilter = "MONTH"
                                    customDateTimestamp = null
                                },
                                label = { Text(if (language == "bn") "চলতি মাস" else "Month", fontSize = 12.sp) },
                                leadingIcon = if (selectedDateFilter == "MONTH") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // 5. Custom Date Picker Button (Interactive Calendar Dialog)
                        item {
                            Button(
                                onClick = {
                                    val pickerCal = Calendar.getInstance()
                                    if (customDateTimestamp != null) {
                                        pickerCal.timeInMillis = customDateTimestamp!!
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val selectedCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, y)
                                                set(Calendar.MONTH, m)
                                                set(Calendar.DAY_OF_MONTH, d)
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            customDateTimestamp = selectedCal.timeInMillis
                                            val customSdf = SimpleDateFormat("d MMMM yyyy", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH)
                                            customDateLabel = customSdf.format(selectedCal.time)
                                            selectedDateFilter = "CUSTOM"
                                        },
                                        pickerCal.get(Calendar.YEAR),
                                        pickerCal.get(Calendar.MONTH),
                                        pickerCal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                colors = if (selectedDateFilter == "CUSTOM") {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (selectedDateFilter == "CUSTOM" && customDateLabel.isNotBlank()) customDateLabel else (if (language == "bn") "তারিখ বাছুন 🗓️" else "Pick Date 🗓️"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // 6. All Time
                        item {
                            FilterChip(
                                selected = selectedDateFilter == "ALL",
                                onClick = {
                                    selectedDateFilter = "ALL"
                                    customDateTimestamp = null
                                },
                                label = { Text(if (language == "bn") "সব সময়" else "All Time", fontSize = 12.sp) },
                                leadingIcon = if (selectedDateFilter == "ALL") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Type Filter Chips & Search Bar Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Type chips
                        FilterChip(
                            selected = selectedTypeFilter == "ALL",
                            onClick = { selectedTypeFilter = "ALL" },
                            label = { Text(if (language == "bn") "সব" else "All", fontSize = 11.sp) },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "SALE",
                            onClick = { selectedTypeFilter = "SALE" },
                            label = { Text(if (language == "bn") "🛍️ বিক্রি" else "Sales", fontSize = 11.sp) },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "DUE",
                            onClick = { selectedTypeFilter = "DUE" },
                            label = { Text(if (language == "bn") "🟠 বাকি" else "Due", fontSize = 11.sp) },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        )
                        FilterChip(
                            selected = selectedTypeFilter == "STOCK_IN",
                            onClick = { selectedTypeFilter = "STOCK_IN" },
                            label = { Text(if (language == "bn") "📦 স্টক ইন" else "Stock", fontSize = 11.sp) },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search input
                    OutlinedTextField(
                        value = txSearchQuery,
                        onValueChange = { txSearchQuery = it },
                        placeholder = {
                            Text(
                                if (language == "bn") "মেমো নং, কাস্টমার বা পণ্যের নাম দিয়ে খুঁজুন..." else "Search memo, customer, product...",
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        trailingIcon = {
                            if (txSearchQuery.isNotBlank()) {
                                IconButton(onClick = { txSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Realtime Summary Mini Card for Selected Filter
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Total Sales
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "মোট বিক্রি" else "Total Sales",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "$currency${filteredTotalSales.toIntOrNull() ?: filteredTotalSales}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            // 2. Realized Profit
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "মোট লাভ" else "Profit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "$currency${filteredTotalProfit.toIntOrNull() ?: String.format(Locale.US, "%.1f", filteredTotalProfit)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filteredTotalProfit >= 0) ProfitGreen else LossRed
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            // 3. Cash Paid
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "নগদ আদায়" else "Cash In",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "$currency${filteredTotalCashPaid.toIntOrNull() ?: filteredTotalCashPaid}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(24.dp), color = MaterialTheme.colorScheme.outlineVariant)

                            // 4. Total Due
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (language == "bn") "বাকি বিক্রি" else "Due Sales",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "$currency${filteredTotalDue.toIntOrNull() ?: filteredTotalDue}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (filteredTotalDue > 0) DueOrange else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }

        // 7. Grouped Chronological Transactions Feed
        if (filteredTransactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == "bn") "এই তারিখ / ফিল্টারে কোনো লেনদেন পাওয়া যায়নি" else "No transactions found for this date/filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else {
            groupedTransactions.forEach { (dateHeader, txsInGroup) ->
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${txsInGroup.size} ${if (language == "bn") "টি লেনদেন" else "txs"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                items(txsInGroup) { tx ->
                    TransactionFeedItem(
                        tx = tx,
                        currency = currency,
                        language = language,
                        onClick = {
                            editingTransaction = tx
                        }
                    )
                }
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
            onEditSale = { newQty, newPrice, newPaid, newCustomerName, newCustomerPhone, newNote ->
                viewModel.editSaleTransaction(txToEdit, newQty, newPrice, newPaid, newCustomerName, newCustomerPhone, newNote)
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
        Dialog(
            onDismissRequest = { showCashHistoryDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            CashBookScreen(
                viewModel = viewModel,
                onBack = { showCashHistoryDialog = false }
            )
        }
    }

    if (showAllMemosDialog) {
        AllMemosDialog(
            transactions = allTransactions.filter { it.type == "SALE" },
            currency = currency,
            language = language,
            shopName = shopInfo.shopName,
            onDismiss = { showAllMemosDialog = false },
            onSelectTx = { tx ->
                editingTransaction = tx
                showAllMemosDialog = false
            }
        )
    }

    if (showDueSmsReminderDialog) {
        DueTagadaReminderDialog(
            customers = customers,
            dueSales = allTransactions.filter { it.type == "SALE" && it.dueAmount > 0 },
            currency = currency,
            language = language,
            shopName = shopInfo.shopName,
            onDismiss = { showDueSmsReminderDialog = false },
            onCollectPayment = { customer, amount, note ->
                viewModel.collectCustomerDue(customer, amount, note)
            },
            onNavigateToDueKhata = onNavigateToDue
        )
    }

    if (showCloudBackupInfoDialog) {
        CloudBackupInfoDialog(
            language = language,
            shopName = shopInfo.shopName,
            userEmail = shopInfo.userEmail,
            isOnline = isOnline,
            autoBackupStatus = autoBackupStatus,
            onDismiss = { showCloudBackupInfoDialog = false },
            onBackupNow = {
                viewModel.backupToLocalFile(context) { success, path ->
                    Toast.makeText(context, if (success) "ব্যাকআপ ফাইল তৈরি হয়েছে: $path" else "ব্যর্থ: $path", Toast.LENGTH_LONG).show()
                }
            },
            onRestoreFromCloud = { customEmail, onDone ->
                if (customEmail.isNotBlank() && customEmail != shopInfo.userEmail) {
                    viewModel.updateUserEmail(customEmail)
                }
                viewModel.importFromGoogleDriveCloud(context, customEmail = customEmail) { res ->
                    onDone(res)
                }
            }
        )
    }

    if (showBusinessSummaryDetailDialog) {
        BusinessSummaryDetailDialog(
            summary = summary,
            mainBalance = mainBalance,
            currency = currency,
            language = language,
            onDismiss = { showBusinessSummaryDetailDialog = false },
            onOpenCashBook = {
                showBusinessSummaryDetailDialog = false
                showCashHistoryDialog = true
            }
        )
    }

    if (showAllServicesDialog) {
        AllServicesDialog(
            language = language,
            onDismiss = { showAllServicesDialog = false },
            onNavigatePos = {
                showAllServicesDialog = false
                onNavigateToPos()
            },
            onNavigateDue = {
                showAllServicesDialog = false
                onNavigateToDue()
            },
            onNavigateStock = {
                showAllServicesDialog = false
                onNavigateToStock()
            },
            onNavigateExpenses = {
                showAllServicesDialog = false
                onNavigateToExpenses()
            },
            onOpenCashBook = {
                showAllServicesDialog = false
                showCashHistoryDialog = true
            },
            onOpenStockIn = {
                showAllServicesDialog = false
                onOpenStockInDialog()
            },
            onOpenMemos = {
                showAllServicesDialog = false
                showAllMemosDialog = true
            },
            onOpenDueSms = {
                showAllServicesDialog = false
                showDueSmsReminderDialog = true
            },
            onOpenBackup = {
                showAllServicesDialog = false
                showCloudBackupInfoDialog = true
            },
            onOpenCashIn = {
                showAllServicesDialog = false
                showAddCashDialog = true
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
    // The actual unclosed cash received today from sales & due collections:
    val defaultClosingAmount = summary.todayUnclosedCash
    var customAmountStr by remember(summary.todayUnclosedCash) { 
        mutableStateOf(if (defaultClosingAmount > 0) (defaultClosingAmount.toIntOrNull()?.toString() ?: defaultClosingAmount.toString()) else "0") 
    }
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

                    if (summary.todayUnclosedCash <= 0.0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = ProfitGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "bn")
                                        "আজকের সকল নগদ বিক্রি ইতিমধ্যে মূল ক্যাশে যুক্ত ও ক্লোজ করা হয়েছে (অবশিষ্ট ৳০)। পুনরায় নতুন বিক্রি করলে সেটি আবার এখানে দেখাবে।"
                                    else
                                        "All today's cash sales have already been settled to main balance (Remaining 0). New sales will appear here automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF14532D),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

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

                            // Row 2: আজকের মোট নগদ বিক্রি
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == "bn") "আজকের মোট নগদ বিক্রি (+):" else "Today Cash Sales (+):",
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

                            // Row 3.5: ইতিমধ্যে ক্লোজ করা হয়েছে (যদি থাকে)
                            if (summary.todayClosedCash > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == "bn") "ইতিমধ্যে ক্লোজ করা হয়েছে (-):" else "Already Settled (-):",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "-$currency${summary.todayClosedCash.toIntOrNull() ?: summary.todayClosedCash}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Row 3.6: অবশিষ্ট ক্যাশ ক্লোজিংয়ের জন্য
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == "bn") "অবশিষ্ট ক্লোজিং ক্যাশ:" else "Remaining to Settle:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$currency${summary.todayUnclosedCash.toIntOrNull() ?: summary.todayUnclosedCash}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
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

// -------------------------------------------------------------
// HELPER COMPOSABLES FOR REDESIGNED DASHBOARD
// -------------------------------------------------------------

@Composable
fun CoreGridActionCard(
    title: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ServiceCircleItem(
    title: String,
    icon: ImageVector,
    bgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = bgColor,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, iconTint.copy(alpha = 0.2f)),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// -------------------------------------------------------------
// QUICK SERVICE DIALOGS
// -------------------------------------------------------------

@Composable
fun AllMemosDialog(
    transactions: List<TransactionRecord>,
    currency: String,
    language: String,
    shopName: String,
    onDismiss: () -> Unit,
    onSelectTx: (TransactionRecord) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(transactions, searchQuery) {
        if (searchQuery.isBlank()) transactions else {
            val q = searchQuery.trim().lowercase()
            transactions.filter {
                it.invoiceNumber.lowercase().contains(q) ||
                it.customerName.lowercase().contains(q) ||
                it.productName.lowercase().contains(q)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
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
                                .size(36.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "সকল ডিজিটাল ক্যাশ মেমো" else "All Digital Invoices",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (language == "bn") "মেমো নং বা ক্রেতার নাম..." else "Search invoice #, customer...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (language == "bn") "কোনো মেমো পাওয়া যায়নি" else "No invoices found", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filtered) { tx ->
                            Card(
                                onClick = { onSelectTx(tx) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "মেমো #${tx.invoiceNumber}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${tx.customerName.ifBlank { if (language == "bn") "সাধারণ কাস্টমার" else "General Customer" }} • ${tx.productName} (${tx.quantity} ${tx.unit})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date(tx.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$currency${tx.totalAmount}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ProfitGreen
                                        )
                                        if (tx.dueAmount > 0) {
                                            Text(
                                                text = "${if (language == "bn") "বাকি:" else "Due:"} $currency${tx.dueAmount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DueOrange
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
    }
}

@Composable
fun CloudBackupInfoDialog(
    language: String,
    shopName: String,
    userEmail: String,
    isOnline: Boolean,
    autoBackupStatus: String?,
    onDismiss: () -> Unit,
    onBackupNow: () -> Unit,
    onRestoreFromCloud: (String, (com.example.data.model.RestoreResult) -> Unit) -> Unit
) {
    var emailInput by remember { mutableStateOf(userEmail) }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreFeedback by remember { mutableStateOf<String?>(null) }
    var isSuccessFeedback by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFFF5F3FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(30.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (language == "bn") "ক্লাউড ডাটা ও রিস্টোর" else "Cloud Data & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (language == "bn") "আপনার জিমেইল অ্যাকাউন্টের মাধ্যমে পণ্য, কাস্টমার, বাকি ও বিক্রির ডাটা ক্লাউডে সুরক্ষিত থাকে।" else "Your products, due records and sales are safely synced to your Gmail cloud.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOnline) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, if (isOnline) Color(0xFFBBF7D0) else Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isOnline) Icons.Default.CheckCircle else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isOnline) ProfitGreen else DueOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = autoBackupStatus ?: (if (isOnline) (if (language == "bn") "অনলাইন - ক্লাউড সিঙ্ক সক্রিয়" else "Online - Cloud Sync Active") else (if (language == "bn") "অফলাইন মোড" else "Offline Mode")),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) Color(0xFF166534) else Color(0xFF92400E)
                            )
                            Text(
                                text = if (isOnline) shopName else (if (language == "bn") "ইন্টারনেট পেলে সকল অফলাইন হিসাব স্বয়ংক্রিয় ব্যাকআপ হবে" else "All offline records will auto-backup once connected"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOnline) Color(0xFF15803D) else Color(0xFFB45309)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Gmail address field
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text(if (language == "bn") "আপনার জিমেইল / অ্যাকাউন্ট" else "Your Gmail / Account") },
                    placeholder = { Text("example@gmail.com") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (restoreFeedback != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSuccessFeedback) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, if (isSuccessFeedback) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = restoreFeedback ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSuccessFeedback) Color(0xFF166534) else Color(0xFF991B1B),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cloud Restore Button
                Button(
                    onClick = {
                        isRestoring = true
                        restoreFeedback = null
                        onRestoreFromCloud(emailInput) { res ->
                            isRestoring = false
                            isSuccessFeedback = res.success
                            if (res.success) {
                                restoreFeedback = if (language == "bn")
                                    "ডাটা সফলভাবে রিস্টোর হয়েছে!\n• পণ্য: ${res.productCount} টি\n• কাস্টমার: ${res.customerCount} জন\n• লেনদেন: ${res.transactionCount} টি\n• বাকি খাতা: ${res.dueLogCount} টি"
                                else
                                    "Restored successfully!\n• Products: ${res.productCount}\n• Customers: ${res.customerCount}\n• Transactions: ${res.transactionCount}"
                            } else {
                                restoreFeedback = res.message
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = StockBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (language == "bn") "ক্লাউড থেকে লোড হচ্ছে..." else "Loading from Cloud...")
                    } else {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == "bn") "ক্লাউড থেকে আগের ডাটা লোড করুন" else "Load Previous Data from Cloud", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Local Backup Save Button
                OutlinedButton(
                    onClick = onBackupNow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == "bn") "ফাইলে ব্যাকআপ সেভ করুন" else "Save Backup to File", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(if (language == "bn") "বন্ধ করুন" else "Close")
                }
            }
        }
    }
}

@Composable
fun BusinessSummaryDetailDialog(
    summary: com.example.data.model.DashboardSummary,
    mainBalance: Double,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onOpenCashBook: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
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
                                .size(36.dp)
                                .background(Color(0xFFFAF5FF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "ব্যবসায়িক পূর্ণাঙ্গ সারসংক্ষেপ" else "Comprehensive Business Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Hands-on Cash Balance
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(if (language == "bn") "দোকানের মূল ক্যাশ (হাতে নগদ)" else "Cash in Hand", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                Text(text = "$currency${mainBalance.toIntOrNull() ?: mainBalance}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                            }
                            Button(onClick = onOpenCashBook, shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                                Text(if (language == "bn") "ক্যাশ খাতা" else "Cash Book", fontSize = 11.sp)
                            }
                        }
                    }

                    // 2. Today's Sales breakdown
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (language == "bn") "আজকের বিক্রির বিস্তারিত" else "Today's Sales Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "মোট বিক্রি:" else "Total Sales:", style = MaterialTheme.typography.bodyMedium)
                                Text("$currency${summary.todayTotalSales}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "নগদ আদায়কৃত বিক্রি:" else "Cash Sales:", style = MaterialTheme.typography.bodyMedium, color = ProfitGreen)
                                Text("$currency${summary.todayCashSales}", fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "আজকের বাকি বিক্রি:" else "Due Sales:", style = MaterialTheme.typography.bodyMedium, color = DueOrange)
                                Text("$currency${summary.todayDueSales}", fontWeight = FontWeight.Bold, color = DueOrange)
                            }
                        }
                    }

                    // 3. Profit breakdown
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (language == "bn") "আজকের লাভের বিস্তারিত" else "Today's Profit Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "মোট অর্জিত লাভ:" else "Total Profit:", style = MaterialTheme.typography.bodyMedium)
                                Text("$currency${summary.todayProfit}", fontWeight = FontWeight.Bold, color = ProfitGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "নগদ বিক্রির লাভ:" else "Cash Realized Profit:", style = MaterialTheme.typography.bodyMedium)
                                Text("$currency${summary.todayRealizedProfit}", fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "বাকি বিক্রির লাভ:" else "Due Unrealized Profit:", style = MaterialTheme.typography.bodyMedium)
                                Text("$currency${summary.todayDueProfit}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 4. Expenses and Inventory
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(if (language == "bn") "খরচ ও ইনভেন্টরি" else "Expenses & Inventory", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "আজকের দোকান খরচ:" else "Today's Expenses:", style = MaterialTheme.typography.bodyMedium)
                                Text("$currency${summary.todayExpenses}", fontWeight = FontWeight.Bold, color = LossRed)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "দোকানের মোট স্টক মূল্য:" else "Total Stock Value:", style = MaterialTheme.typography.bodyMedium)
                                Text("$currency${summary.totalStockValue}", fontWeight = FontWeight.Bold, color = StockBlue)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (language == "bn") "মোট বকেয়া কাস্টমার বাকি:" else "Total Customer Due:", style = MaterialTheme.typography.bodyMedium)
                                Text("$currency${summary.totalOutstandingDue}", fontWeight = FontWeight.Bold, color = DueOrange)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllServicesDialog(
    language: String,
    onDismiss: () -> Unit,
    onNavigatePos: () -> Unit,
    onNavigateDue: () -> Unit,
    onNavigateStock: () -> Unit,
    onNavigateExpenses: () -> Unit,
    onOpenCashBook: () -> Unit,
    onOpenStockIn: () -> Unit,
    onOpenMemos: () -> Unit,
    onOpenDueSms: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenCashIn: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "অ্যাপের সকল সার্ভিস ও অপশন" else "All Services & Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Row 1
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CoreGridActionCard(
                            title = if (language == "bn") "নগদ বিক্রি (POS)" else "POS Sale",
                            icon = Icons.Default.GridView,
                            iconBg = Color(0xFFECFDF5),
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigatePos
                        )
                        CoreGridActionCard(
                            title = if (language == "bn") "বাকি খাতা" else "Due Khata",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconBg = Color(0xFFFFF7ED),
                            iconTint = DueOrange,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateDue
                        )
                        CoreGridActionCard(
                            title = if (language == "bn") "ক্যাশ খাতা" else "Cash Book",
                            icon = Icons.Default.ReceiptLong,
                            iconBg = Color(0xFFF0FDF4),
                            iconTint = ProfitGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenCashBook
                        )
                    }

                    // Row 2
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CoreGridActionCard(
                            title = if (language == "bn") "স্টক ও পণ্য" else "Stock Inventory",
                            icon = Icons.Default.Inventory2,
                            iconBg = Color(0xFFEFF6FF),
                            iconTint = StockBlue,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateStock
                        )
                        CoreGridActionCard(
                            title = if (language == "bn") "দোকান খরচ" else "Expenses",
                            icon = Icons.Default.TrendingDown,
                            iconBg = Color(0xFFFEF2F2),
                            iconTint = LossRed,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateExpenses
                        )
                        CoreGridActionCard(
                            title = if (language == "bn") "স্টক ইন" else "Stock In",
                            icon = Icons.Default.AddBox,
                            iconBg = Color(0xFFEFF6FF),
                            iconTint = StockBlue,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenStockIn
                        )
                    }

                    // Row 3
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CoreGridActionCard(
                            title = if (language == "bn") "ডিজিটাল মেমো" else "Invoices",
                            icon = Icons.Default.Receipt,
                            iconBg = Color(0xFFF0FDF4),
                            iconTint = ProfitGreen,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenMemos
                        )
                        CoreGridActionCard(
                            title = if (language == "bn") "বাকি তাগাদা" else "Due SMS",
                            icon = Icons.Default.Sms,
                            iconBg = Color(0xFFFFFBEB),
                            iconTint = DueOrange,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenDueSms
                        )
                        CoreGridActionCard(
                            title = if (language == "bn") "ক্লাউড ব্যাকআপ" else "Cloud Backup",
                            icon = Icons.Default.CloudSync,
                            iconBg = Color(0xFFF5F3FF),
                            iconTint = Color(0xFF7C3AED),
                            modifier = Modifier.weight(1f),
                            onClick = onOpenBackup
                        )
                    }

                    // Row 4
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CoreGridActionCard(
                            title = if (language == "bn") "ক্যাশ জমা" else "Add Cash",
                            icon = Icons.Default.AddCircleOutline,
                            iconBg = Color(0xFFECFDF5),
                            iconTint = EmeraldPrimary,
                            modifier = Modifier.weight(1f),
                            onClick = onOpenCashIn
                        )
                    }
                }
            }
        }
    }
}
