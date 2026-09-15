package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionRecord
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CalculationHelper
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

enum class AccountsTab(val bnTitle: String, val enTitle: String) {
    CASH_BOOK("ক্যাশ খাতা", "Cash Book"),
    DAILY_CLOSING("দৈনিক হিসাব ও ক্লোজিং", "Daily Closing"),
    EXPENSES("খরচ হিসাব", "Expenses"),
    CASH_COUNTER("নোট গণক (ক্যাশ মিল)", "Cash Counter"),
    CALCULATOR("ক্যালকুলেটর", "Calculator"),
    DUE_SUMMARY("দেনা-পাওনা", "Dues Summary")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: ShopViewModel,
    initialTab: AccountsTab = AccountsTab.CASH_BOOK,
    onNavigateToDue: (() -> Unit)? = null,
    onNavigateToPos: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val currency = shopInfo.currency
    val summary by viewModel.dashboardSummary.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val dueLogs by viewModel.dueLogs.collectAsState()

    var selectedTab by remember { mutableStateOf(initialTab) }
    var showDayEndSettleDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Quick Header with Current Cash Balance and Tab Row
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Column {
                // Top Balance Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFECFDF5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Calculate,
                                contentDescription = "Accounts",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "দোকানের পূর্ণাঙ্গ হিসাব-নিকাশ" else "Shop Master Accounts",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == "bn") "নগদ, বাকি, লাভ-ক্ষতি, নোট গণক ও ক্যালকুলেটর" else "Cash, Dues, P&L, Denominations & Tools",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    // Live Cash Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (shopInfo.mainBalance >= 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (shopInfo.mainBalance >= 0) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == "bn") "হাতে নগদ: " else "Cash: ",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = if (shopInfo.mainBalance >= 0) Color(0xFF166534) else Color(0xFF991B1B)
                            )
                            Text(
                                text = CalculationHelper.formatCurrency(shopInfo.mainBalance, currency),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (shopInfo.mainBalance >= 0) EmeraldPrimary else LossRed
                            )
                        }
                    }
                }

                // Scrollable Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = EmeraldPrimary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                ) {
                    AccountsTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = if (language == "bn") tab.bnTitle else tab.enTitle,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            icon = {
                                val icon = when (tab) {
                                    AccountsTab.CASH_BOOK -> Icons.Default.ReceiptLong
                                    AccountsTab.DAILY_CLOSING -> Icons.Default.Assessment
                                    AccountsTab.EXPENSES -> Icons.Default.TrendingDown
                                    AccountsTab.CASH_COUNTER -> Icons.Default.Money
                                    AccountsTab.CALCULATOR -> Icons.Default.Calculate
                                    AccountsTab.DUE_SUMMARY -> Icons.Default.AccountBalanceWallet
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }
        }

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                AccountsTab.CASH_BOOK -> {
                    CashBookScreen(
                        viewModel = viewModel,
                        onBack = null
                    )
                }

                AccountsTab.DAILY_CLOSING -> {
                    DailyClosingTab(
                        summary = summary,
                        mainBalance = shopInfo.mainBalance,
                        allTransactions = allTransactions,
                        currency = currency,
                        language = language,
                        onOpenSettleDialog = { showDayEndSettleDialog = true },
                        onNavigateToPos = onNavigateToPos
                    )
                }

                AccountsTab.EXPENSES -> {
                    ExpenseScreen(
                        viewModel = viewModel,
                        initiallyShowAddDialog = false
                    )
                }

                AccountsTab.CASH_COUNTER -> {
                    CashCounterTab(
                        currentShopBalance = shopInfo.mainBalance,
                        currency = currency,
                        language = language
                    )
                }

                AccountsTab.CALCULATOR -> {
                    AccountsCalculatorTab(
                        language = language,
                        currency = currency
                    )
                }

                AccountsTab.DUE_SUMMARY -> {
                    DuesSummaryTab(
                        customers = customers,
                        dueLogs = dueLogs,
                        currency = currency,
                        language = language,
                        onNavigateToDue = onNavigateToDue
                    )
                }
            }
        }
    }

    if (showDayEndSettleDialog) {
        DayEndSettlementDialog(
            summary = summary,
            currentMainBalance = shopInfo.mainBalance,
            currency = currency,
            language = language,
            onDismiss = { showDayEndSettleDialog = false },
            onConfirm = { settledAmount, note ->
                viewModel.settleDayEndCashToMainBalance(settledAmount, note)
                showDayEndSettleDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 2: DAILY CLOSING & P&L TAB
// -------------------------------------------------------------
@Composable
fun DailyClosingTab(
    summary: com.example.data.model.DashboardSummary,
    mainBalance: Double,
    allTransactions: List<TransactionRecord>,
    currency: String,
    language: String,
    onOpenSettleDialog: () -> Unit,
    onNavigateToPos: (() -> Unit)?
) {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = calendar.timeInMillis
    val todayTransactions = remember(allTransactions, startOfToday) {
        allTransactions.filter { it.timestamp >= startOfToday && it.type == "SALE" }
    }

    val todayGrossSales = summary.todaySales
    val todayCost = summary.todayPurchases
    val todayGrossProfit = summary.todayProfit
    val todayExpenses = summary.todayExpenses
    val todayNetProfit = todayGrossProfit - todayExpenses

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Day Closing Action Banner
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == "bn") "আজকের ক্যাশ ড্রয়ার ও ক্লোজিং" else "Today's Cash Drawer & Closing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (language == "bn") "দোকানের বিক্রি ও ক্যাশ গুনে দিন শেষ করুন" else "Reconcile daily sales and settle to main cash",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = AmberTertiary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == "bn") "আজকের অমিলনকৃত ক্যাশ (ড্রয়ারে):" else "Unsettled Drawer Cash:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1)
                            )
                            Text(
                                text = CalculationHelper.formatCurrency(summary.todayUnclosedCash, currency),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF4ADE80)
                            )
                        }

                        Button(
                            onClick = onOpenSettleDialog,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "দিন ক্লোজ করুন" else "Settle Day",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Financial Breakdown (Gross Sales, Gross Profit, Expenses, Net Profit)
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (language == "bn") "আজকের হিসাব সারসংক্ষেপ (P&L)" else "Today's Profit & Loss Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // 4-Card Mini Grid
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Total Sales
                        MetricMiniCard(
                            title = if (language == "bn") "মোট বিক্রি" else "Total Sales",
                            value = CalculationHelper.formatCurrency(todayGrossSales, currency),
                            sub = if (language == "bn") "নগদ: ${CalculationHelper.formatCurrency(summary.todayCashSales, currency)}" else "Cash: ${CalculationHelper.formatCurrency(summary.todayCashSales, currency)}",
                            color = EmeraldPrimary,
                            bgColor = Color(0xFFECFDF5),
                            modifier = Modifier.weight(1f)
                        )
                        // Buying Cost
                        MetricMiniCard(
                            title = if (language == "bn") "মালের ক্রয়মূল্য" else "Product Cost",
                            value = CalculationHelper.formatCurrency(todayCost, currency),
                            sub = if (language == "bn") "কেনা দাম" else "Cost of goods",
                            color = Color(0xFF2563EB),
                            bgColor = Color(0xFFEFF6FF),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Expenses
                        MetricMiniCard(
                            title = if (language == "bn") "দোকান খরচ" else "Expenses",
                            value = CalculationHelper.formatCurrency(todayExpenses, currency),
                            sub = if (language == "bn") "আজকের খরচ" else "Today expense",
                            color = LossRed,
                            bgColor = Color(0xFFFEF2F2),
                            modifier = Modifier.weight(1f)
                        )
                        // Net Profit
                        MetricMiniCard(
                            title = if (language == "bn") "প্রকৃত নিট লাভ" else "Net Profit",
                            value = CalculationHelper.formatCurrency(todayNetProfit, currency),
                            sub = if (todayNetProfit >= 0) (if (language == "bn") "অর্জিত লাভ" else "Profit") else (if (language == "bn") "লোকসান" else "Loss"),
                            color = if (todayNetProfit >= 0) Color(0xFF059669) else LossRed,
                            bgColor = if (todayNetProfit >= 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Due Cash Flow Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (language == "bn") "আজকের বাকি আদায়" else "Today's Due Collected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = CalculationHelper.formatCurrency(summary.todayCollectedDue, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (language == "bn") "আজকের নতুন বাকি দেওয়া" else "Today's New Due Given",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = CalculationHelper.formatCurrency(summary.todayNewDueGiven, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = DueOrange
                            )
                        }
                    }
                }
            }
        }

        // 3. Today's Transactions List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == "bn") "আজকের বিক্রয় তালিকা (${todayTransactions.size}টি)" else "Today's Sales (${todayTransactions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (onNavigateToPos != null) {
                    TextButton(onClick = onNavigateToPos) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "bn") "নতুন বিক্রি" else "New Sale", fontSize = 12.sp)
                    }
                }
            }
        }

        if (todayTransactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == "bn") "আজকে এখনও কোনো বিক্রি রেকর্ড করা হয়নি।" else "No sales recorded yet today.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            items(todayTransactions) { tx ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                text = tx.customerName.ifBlank { if (language == "bn") "সাধারণ ক্রেতা" else "Walk-in Customer" },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                            Text(
                                text = "$timeStr • ${tx.paymentMethod}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CalculationHelper.formatCurrency(tx.totalAmount, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            if (tx.dueAmount > 0) {
                                Text(
                                    text = "${if (language == "bn") "বাকি" else "Due"}: ${CalculationHelper.formatCurrency(tx.dueAmount, currency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DueOrange,
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

@Composable
fun MetricMiniCard(
    title: String,
    value: String,
    sub: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, fontSize = 11.sp, color = Color(0xFF475569))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Spacer(modifier = Modifier.height(1.dp))
            Text(text = sub, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}

// -------------------------------------------------------------
// TAB 4: CASH COUNTER / DENOMINATION TAB (নোট গণক ও ক্যাশ মিল)
// -------------------------------------------------------------
@Composable
fun CashCounterTab(
    currentShopBalance: Double,
    currency: String,
    language: String
) {
    val denominations = listOf(1000, 500, 200, 100, 50, 20, 10, 5, 2, 1)
    val noteCounts = remember { mutableStateMapOf<Int, Int>() }
    val context = LocalContext.current

    val totalCounted = remember(noteCounts) {
        denominations.sumOf { denom -> (noteCounts[denom] ?: 0) * denom.toLong() }
    }

    val difference = totalCounted - currentShopBalance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Reconcile Summary Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == "bn") "মোট গোনাকৃত টাকা" else "Total Counted Cash",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = CalculationHelper.formatCurrency(totalCounted.toDouble(), currency),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    }

                    IconButton(onClick = {
                        noteCounts.clear()
                        Toast.makeText(context, if (language == "bn") "নোটের হিসাব রিসেট হয়েছে" else "Counts reset", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = LossRed)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Comparison with shop cash
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (language == "bn") "দোকানের মূল ক্যাশ" else "Shop Cash"}: ${CalculationHelper.formatCurrency(currentShopBalance, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Difference Badge
                    val isMatched = Math.abs(difference) < 0.01
                    val badgeColor = when {
                        isMatched -> Color(0xFF059669)
                        difference > 0 -> Color(0xFF2563EB)
                        else -> LossRed
                    }
                    val badgeBg = when {
                        isMatched -> Color(0xFFECFDF5)
                        difference > 0 -> Color(0xFFEFF6FF)
                        else -> Color(0xFFFEF2F2)
                    }
                    val statusText = when {
                        isMatched -> if (language == "bn") "✅ ক্যাশ মিলে গেছে" else "✅ Perfectly Matched"
                        difference > 0 -> "${if (language == "bn") "উদ্বৃত্ত: +" else "Surplus: +"}${CalculationHelper.formatCurrency(difference, currency)}"
                        else -> "${if (language == "bn") "ঘাটতি: " else "Shortage: "}${CalculationHelper.formatCurrency(difference, currency)}"
                    }

                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Copy breakdown button
                OutlinedButton(
                    onClick = {
                        val sb = StringBuilder()
                        sb.appendLine("--- ${if (language == "bn") "ক্যাশ নোটের হিসাব" else "Cash Denomination Count"} ---")
                        denominations.forEach { d ->
                            val c = noteCounts[d] ?: 0
                            if (c > 0) {
                                sb.appendLine("$d ৳ x $c টি = ${d * c} ৳")
                            }
                        }
                        sb.appendLine("--------------------------")
                        sb.appendLine("${if (language == "bn") "সর্বমোট" else "Total"}: $totalCounted ৳")
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Cash Count", sb.toString()))
                        Toast.makeText(context, if (language == "bn") "নোটের হিসাব কপি হয়েছে" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == "bn") "নোটের পূর্ণ বিবরণী কপি করুন" else "Copy Note Breakdown")
                }
            }
        }

        // Denominations List
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (language == "bn") "নোট বা কয়েন অনুযায়ী সংখ্যা লিখুন" else "Enter count of notes & coins",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.outline
                )

                denominations.forEach { denom ->
                    val count = noteCounts[denom] ?: 0
                    val subtotal = count * denom

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (count > 0) Color(0xFFF0FDF4) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Denom Label
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(90.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        when (denom) {
                                            1000 -> Color(0xFFFCE7F3)
                                            500 -> Color(0xFFCFFAFE)
                                            200 -> Color(0xFFFEF3C7)
                                            100 -> Color(0xFFDCFCE7)
                                            50 -> Color(0xFFFFEDD5)
                                            20 -> Color(0xFFF1F5F9)
                                            else -> Color(0xFFE2E8F0)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$denom",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "৳$denom",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Stepper (- and + buttons with text input)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (count > 0) noteCounts[denom] = count - 1
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Minus", tint = Color.Gray)
                            }

                            var textVal by remember(count) { mutableStateOf(if (count > 0) count.toString() else "") }
                            OutlinedTextField(
                                value = textVal,
                                onValueChange = { newVal ->
                                    val filtered = newVal.filter { it.isDigit() }
                                    textVal = filtered
                                    val parsed = filtered.toIntOrNull() ?: 0
                                    noteCounts[denom] = parsed
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold),
                                singleLine = true,
                                placeholder = { Text("0", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(44.dp)
                            )

                            IconButton(
                                onClick = { noteCounts[denom] = count + 1 },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = "Plus", tint = EmeraldPrimary)
                            }
                        }

                        // Subtotal
                        Text(
                            text = "= ৳$subtotal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (subtotal > 0) EmeraldPrimary else Color.Gray,
                            modifier = Modifier.width(90.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

data class CalcHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val expression: String,
    val result: String,
    val entries: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

private const val PREFS_CALC = "accounts_calc_prefs"
private const val KEY_CALC_HISTORY = "calc_history_json"

private fun loadCalcHistory(context: Context): List<CalcHistoryItem> {
    return try {
        val prefs = context.getSharedPreferences(PREFS_CALC, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_CALC_HISTORY, null) ?: return emptyList()
        val jsonArr = JSONArray(jsonStr)
        val list = mutableListOf<CalcHistoryItem>()
        for (i in 0 until jsonArr.length()) {
            val obj = jsonArr.getJSONObject(i)
            val entriesArr = obj.optJSONArray("entries") ?: JSONArray()
            val entriesList = mutableListOf<String>()
            for (j in 0 until entriesArr.length()) {
                entriesList.add(entriesArr.getString(j))
            }
            list.add(
                CalcHistoryItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    expression = obj.optString("expression", ""),
                    result = obj.optString("result", ""),
                    entries = entriesList,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveCalcHistory(context: Context, history: List<CalcHistoryItem>) {
    try {
        val prefs = context.getSharedPreferences(PREFS_CALC, Context.MODE_PRIVATE)
        val jsonArr = JSONArray()
        history.take(50).forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("expression", item.expression)
            obj.put("result", item.result)
            val entriesArr = JSONArray()
            item.entries.forEach { entriesArr.put(it) }
            obj.put("entries", entriesArr)
            obj.put("timestamp", item.timestamp)
            jsonArr.put(obj)
        }
        prefs.edit().putString(KEY_CALC_HISTORY, jsonArr.toString()).apply()
    } catch (e: Exception) {
        // Ignore
    }
}

// -------------------------------------------------------------
// TAB 5: BUILT-IN ACCOUNTS CALCULATOR WITH LIVE ENTRIES & HISTORY
// -------------------------------------------------------------
@Composable
fun AccountsCalculatorTab(
    language: String,
    currency: String
) {
    val context = LocalContext.current
    var display by remember { mutableStateOf("0") }
    var expression by remember { mutableStateOf("") }
    var lastOperator by remember { mutableStateOf<Char?>(null) }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var isNewNumber by remember { mutableStateOf(true) }

    var currentEntries by remember { mutableStateOf<List<String>>(emptyList()) }
    var historyList by remember { mutableStateOf<List<CalcHistoryItem>>(emptyList()) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        historyList = loadCalcHistory(context)
    }

    val liveEntries = remember(currentEntries, display, isNewNumber, lastOperator) {
        if (currentEntries.isEmpty()) {
            if (!isNewNumber && display != "0") listOf(display) else emptyList()
        } else {
            if (!isNewNumber && lastOperator != null) {
                currentEntries + "$lastOperator $display"
            } else {
                currentEntries
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Display Screen Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Top Header Row
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
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (language == "bn") "এন্ট্রি তালিকা (${liveEntries.size})" else "Entries (${liveEntries.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = expression.ifBlank { " " },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Middle Split Row: Left is entries tape, Right is main display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT COLUMN: Running Entries Tape
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        if (liveEntries.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (language == "bn") "এন্ট্রি তালিকা\n(যেমন: ২৫০+২৫০+২০০)" else "Entry breakdown\n(e.g. 250+250+200)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        } else {
                            val listState = rememberLazyListState()
                            LaunchedEffect(liveEntries.size) {
                                if (liveEntries.isNotEmpty()) {
                                    listState.animateScrollToItem(liveEntries.size - 1)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                itemsIndexed(liveEntries) { idx, entry ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = entry,
                                            fontSize = 12.sp,
                                            color = when {
                                                entry.startsWith("-") -> LossRed
                                                entry.startsWith("×") || entry.startsWith("÷") -> Color(0xFFF59E0B)
                                                else -> Color(0xFF34D399)
                                            },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // RIGHT COLUMN: Main Display & Subtotal
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = display,
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = if (display.length > 9) 24.sp else if (display.length > 6) 30.sp else 36.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (liveEntries.size > 1) {
                            Text(
                                text = if (language == "bn") "মোট: $currency$display" else "Total: $currency$display",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Bar: History Button & Copy/Clear Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showHistoryDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "হিসাব হিস্ট্রি (${historyList.size})" else "History (${historyList.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentEntries.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    currentEntries = emptyList()
                                    display = "0"
                                    expression = ""
                                    operand1 = null
                                    lastOperator = null
                                    isNewNumber = true
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = if (language == "bn") "এন্ট্রি মুছুন" else "Clear",
                                    color = LossRed,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        TextButton(
                            onClick = {
                                val copyText = if (expression.isNotBlank() && expression.contains("=")) {
                                    expression
                                } else if (liveEntries.size > 1) {
                                    "${liveEntries.joinToString(" ")} = $display"
                                } else {
                                    display
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Calc Result", copyText))
                                Toast.makeText(context, if (language == "bn") "কপি করা হয়েছে: $copyText" else "Copied: $copyText", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (language == "bn") "কপি" else "Copy", color = EmeraldPrimary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Calculator Buttons Grid
        val buttonRows = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", "00", ".", "=")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            buttonRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { btn ->
                        val isOp = btn in listOf("÷", "×", "-", "+", "=")
                        val isSpecial = btn in listOf("C", "⌫", "%")
                        val btnColor = when {
                            btn == "=" -> EmeraldPrimary
                            isOp -> Color(0xFF1E293B)
                            isSpecial -> Color(0xFFF1F5F9)
                            else -> MaterialTheme.colorScheme.surface
                        }
                        val textColor = when {
                            btn == "=" -> Color.White
                            isOp -> EmeraldPrimary
                            isSpecial -> LossRed
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Surface(
                            onClick = {
                                when (btn) {
                                    "C" -> {
                                        display = "0"
                                        expression = ""
                                        operand1 = null
                                        lastOperator = null
                                        isNewNumber = true
                                        currentEntries = emptyList()
                                    }
                                    "⌫" -> {
                                        if (display.length > 1) {
                                            display = display.dropLast(1)
                                        } else {
                                            display = "0"
                                            isNewNumber = true
                                        }
                                    }
                                    "%" -> {
                                        val v = display.toDoubleOrNull() ?: 0.0
                                        display = formatResult(v / 100.0)
                                        isNewNumber = true
                                    }
                                    "+", "-", "×", "÷" -> {
                                        val currentVal = display.toDoubleOrNull() ?: 0.0
                                        if (currentEntries.isEmpty()) {
                                            currentEntries = listOf(formatResult(currentVal))
                                        } else if (!isNewNumber) {
                                            if (lastOperator != null) {
                                                currentEntries = currentEntries + "$lastOperator ${formatResult(currentVal)}"
                                            } else {
                                                currentEntries = currentEntries + formatResult(currentVal)
                                            }
                                        }
                                        if (operand1 != null && lastOperator != null && !isNewNumber) {
                                            val res = calculate(operand1!!, currentVal, lastOperator!!)
                                            display = formatResult(res)
                                            operand1 = res
                                        } else {
                                            operand1 = currentVal
                                        }
                                        lastOperator = btn[0]
                                        expression = "${currentEntries.joinToString(" ")} $btn"
                                        isNewNumber = true
                                    }
                                    "=" -> {
                                        if (operand1 != null && lastOperator != null) {
                                            val currentVal = display.toDoubleOrNull() ?: 0.0
                                            val res = calculate(operand1!!, currentVal, lastOperator!!)
                                            val finalEntries = if (isNewNumber) {
                                                currentEntries
                                            } else {
                                                currentEntries + "$lastOperator ${formatResult(currentVal)}"
                                            }
                                            val resStr = formatResult(res)
                                            val fullExpr = "${finalEntries.joinToString(" ")} = $resStr"
                                            display = resStr
                                            expression = fullExpr
                                            currentEntries = finalEntries

                                            // Automatically save completed calculation to history
                                            val newHistoryItem = CalcHistoryItem(
                                                expression = fullExpr,
                                                result = resStr,
                                                entries = finalEntries,
                                                timestamp = System.currentTimeMillis()
                                            )
                                            val updatedHistory = listOf(newHistoryItem) + historyList
                                            historyList = updatedHistory
                                            saveCalcHistory(context, updatedHistory)

                                            operand1 = null
                                            lastOperator = null
                                            isNewNumber = true
                                        }
                                    }
                                    "." -> {
                                        if (isNewNumber) {
                                            display = "0."
                                            isNewNumber = false
                                        } else if (!display.contains(".")) {
                                            display += "."
                                        }
                                    }
                                    "00" -> {
                                        if (!isNewNumber && display != "0") {
                                            display += "00"
                                        }
                                    }
                                    else -> { // Digits 0-9
                                        if (isNewNumber || display == "0") {
                                            if (operand1 == null) {
                                                currentEntries = emptyList()
                                                expression = ""
                                            }
                                            display = btn
                                            isNewNumber = false
                                        } else {
                                            display += btn
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = btnColor,
                            shadowElevation = 1.dp,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = btn,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // CALCULATION HISTORY DIALOG
    if (showHistoryDialog) {
        Dialog(onDismissRequest = { showHistoryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF0F172A), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (language == "bn") "হিসাব হিস্ট্রি" else "Calculation History",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "bn") "${historyList.size}টি হিসাব সংরক্ষিত" else "${historyList.size} calculations saved",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        IconButton(
                            onClick = { showHistoryDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    if (historyList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = if (language == "bn") "কোনো পূর্ববর্তী হিসাব হিস্ট্রি নেই" else "No calculation history yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = if (language == "bn") "ক্যালকুলেটরে হিসাব সম্পন্ন করে '=' চাপলে স্বয়ংক্রিয়ভাবে এখানে সংরক্ষিত হবে।" else "Calculations completed with '=' will be saved here automatically.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (language == "bn") "সর্বশেষ হিসাবসমূহ" else "Recent Calculations",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.outline
                            )

                            TextButton(
                                onClick = { showClearConfirmDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "সব মুছুন" else "Clear All",
                                    color = LossRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(historyList, key = { it.id }) { item ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                                            Text(
                                                text = dateStr,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 10.sp
                                            )
                                            if (item.entries.size > 1) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = EmeraldPrimary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = if (language == "bn") "${item.entries.size}টি এন্ট্রি" else "${item.entries.size} entries",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EmeraldPrimary,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = item.expression,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (item.entries.size > 1) {
                                            Text(
                                                text = item.entries.joinToString("  |  "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF0284C7),
                                                fontSize = 11.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Action Buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = {
                                                    display = item.result
                                                    expression = item.expression
                                                    currentEntries = item.entries
                                                    operand1 = null
                                                    lastOperator = null
                                                    isNewNumber = true
                                                    showHistoryDialog = false
                                                    Toast.makeText(context, if (language == "bn") "হিসাব লোড করা হয়েছে" else "Loaded into calculator", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (language == "bn") "ব্যবহার করুন" else "Use", fontSize = 11.sp)
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            OutlinedButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Calculation", item.expression))
                                                    Toast.makeText(context, if (language == "bn") "কপি করা হয়েছে" else "Copied", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (language == "bn") "কপি" else "Copy", fontSize = 11.sp)
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            IconButton(
                                                onClick = {
                                                    val updated = historyList.filter { it.id != item.id }
                                                    historyList = updated
                                                    saveCalcHistory(context, updated)
                                                },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showHistoryDialog = false },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (language == "bn") "বন্ধ করুন" else "Close",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    // CONFIRM CLEAR HISTORY DIALOG
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(if (language == "bn") "সব হিস্ট্রি মুছবেন?" else "Clear All History?") },
            text = { Text(if (language == "bn") "সকল সংরক্ষিত হিসাব হিস্ট্রি মুছে ফেলা হবে।" else "All saved calculations will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        historyList = emptyList()
                        saveCalcHistory(context, emptyList())
                        showClearConfirmDialog = false
                        Toast.makeText(context, if (language == "bn") "সব হিস্ট্রি মুছে ফেলা হয়েছে" else "History cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text(if (language == "bn") "মুছুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }
}

private fun calculate(op1: Double, op2: Double, operator: Char): Double {
    return when (operator) {
        '+' -> op1 + op2
        '-' -> op1 - op2
        '×' -> op1 * op2
        '÷' -> if (op2 != 0.0) op1 / op2 else 0.0
        else -> op2
    }
}

private fun formatResult(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}

// -------------------------------------------------------------
// TAB 6: DUES SUMMARY TAB (দেনা-পাওনা সারসংক্ষেপ)
// -------------------------------------------------------------
@Composable
fun DuesSummaryTab(
    customers: List<com.example.data.model.Customer>,
    dueLogs: List<com.example.data.model.DueLog>,
    currency: String,
    language: String,
    onNavigateToDue: (() -> Unit)?
) {
    val totalCustomerDue = remember(customers) { customers.sumOf { it.totalDue } }
    val debtorsCount = remember(customers) { customers.count { it.totalDue > 0.0 } }
    val totalCollected = remember(dueLogs) { dueLogs.filter { it.type == "DUE_COLLECTED" }.sumOf { it.amount } }
    val totalGiven = remember(dueLogs) { dueLogs.filter { it.type == "DUE_GIVEN" }.sumOf { it.amount } }
    val activeDebtors = remember(customers) {
        customers.filter { it.totalDue > 0.0 }.sortedByDescending { it.totalDue }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (language == "bn") "দোকানের মোট বাকি ও পাওনা হিসাব" else "Total Receivables & Dues",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricMiniCard(
                            title = if (language == "bn") "মোট বকেয়া পাওনা" else "Total Due Outstanding",
                            value = CalculationHelper.formatCurrency(totalCustomerDue, currency),
                            sub = "$debtorsCount ${if (language == "bn") "জন বাকিদার" else "debtors"}",
                            color = DueOrange,
                            bgColor = Color(0xFFFFF7ED),
                            modifier = Modifier.weight(1f)
                        )

                        MetricMiniCard(
                            title = if (language == "bn") "মোট আদায়কৃত বাকি" else "Total Collected",
                            value = CalculationHelper.formatCurrency(totalCollected, currency),
                            sub = if (language == "bn") "খাতায় মোট জমা" else "Total deposited",
                            color = EmeraldPrimary,
                            bgColor = Color(0xFFECFDF5),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == "bn") "মোট বাকির পরিমাণ:" else "Outstanding Due:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = CalculationHelper.formatCurrency(totalCustomerDue, currency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DueOrange
                            )
                        }

                        if (onNavigateToDue != null) {
                            Button(
                                onClick = onNavigateToDue,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DueOrange)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == "bn") "বাকি খাতা খুলুন" else "Open Due Khata")
                            }
                        }
                    }
                }
            }
        }

        // Active Debtors List header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == "bn") "শীর্ষ বাকিদার কাস্টমার তালিকা" else "Top Customer Dues",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${activeDebtors.size} ${if (language == "bn") "জন" else "people"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        if (activeDebtors.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == "bn") "দোকানে কোনো বকেয়া বাকি নেই! হিসাব চমৎকার পরিষ্কার।" else "No outstanding customer dues!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }
        } else {
            items(activeDebtors.size) { index ->
                val debtor = activeDebtors[index]
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFFFF7ED), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = debtor.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = DueOrange
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = debtor.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (debtor.phone.isNotBlank()) {
                                    Text(
                                        text = debtor.phone,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CalculationHelper.formatCurrency(debtor.totalDue, currency),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DueOrange
                            )
                            Text(
                                text = if (language == "bn") "বকেয়া বাকি" else "Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
