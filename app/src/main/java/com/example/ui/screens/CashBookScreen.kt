package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.data.model.CashLog
import com.example.data.model.MasterCashEntry
import com.example.data.model.parseCashAdditionItem
import com.example.ui.components.AddedCashHistoryDialog
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CalculationHelper
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBookScreen(
    viewModel: ShopViewModel,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? android.app.Activity
    val density = LocalDensity.current

    var navBarBottomPx by remember { mutableIntStateOf(0) }
    var statusBarTopPx by remember { mutableIntStateOf(0) }

    DisposableEffect(view) {
        val check = {
            val root = ViewCompat.getRootWindowInsets(view)
                ?: activity?.window?.decorView?.let { ViewCompat.getRootWindowInsets(it) }
            if (root != null) {
                val nav = root.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val sys = root.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                navBarBottomPx = maxOf(nav, sys)
                statusBarTopPx = root.getInsets(WindowInsetsCompat.Type.statusBars()).top
            }
        }
        check()
        val listener = androidx.core.view.OnApplyWindowInsetsListener { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            navBarBottomPx = maxOf(nav, sys)
            statusBarTopPx = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(view, listener)
        onDispose {
            ViewCompat.setOnApplyWindowInsetsListener(view, null)
        }
    }

    val systemNavHeightPx = remember(context) {
        val resId = context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }
    val systemNavHeightDp = with(density) { systemNavHeightPx.toDp() }

    val navBarBottomDp = with(density) { navBarBottomPx.toDp() }
    val effectiveNavBottomPadding = maxOf(
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding(),
        navBarBottomDp,
        systemNavHeightDp
    )

    val statusBarTopDp = with(density) { statusBarTopPx.toDp() }
    val effectiveTopPadding = maxOf(
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        statusBarTopDp
    )

    val cashLogs by viewModel.cashLogs.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val dueLogs by viewModel.dueLogs.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency = shopInfo.currency

    // Filter states: "সব", "দৈনিক", "গতকাল", "সাপ্তাহিক", "মাসিক", "বার্ষিক", "তারিখ বাছুন"
    var selectedPeriod by remember { mutableStateOf("সব") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "INCOME", "EXPENSE"
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showBalanceToggle by remember { mutableStateOf(true) }

    // Custom Date Range / Specific Date Filter
    var customSelectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var customDateLabel by remember { mutableStateOf("") }

    // Dialog states
    var showIncomeDialog by remember { mutableStateOf(false) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showAddedCashHistoryDialog by remember { mutableStateOf(false) }
    var showDayEndSettleDialog by remember { mutableStateOf(false) }
    var editingCashLog by remember { mutableStateOf<CashLog?>(null) }
    var deletingCashLog by remember { mutableStateOf<CashLog?>(null) }
    var viewingEntryDetails by remember { mutableStateOf<MasterCashEntry?>(null) }
    var showAdjustBalanceDialog by remember { mutableStateOf(false) }

    val now = remember { System.currentTimeMillis() }
    val calendar = remember { Calendar.getInstance() }

    val startOfToday = remember(now) {
        calendar.apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val startOfYesterday = remember(now) {
        calendar.apply {
            timeInMillis = now - 86400000L
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val endOfYesterday = startOfToday - 1

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

    // 1. Build Comprehensive Unified Master Cash Ledger
    val masterEntries = remember(cashLogs, allTransactions, dueLogs, expenses, shopInfo.mainBalance, language) {
        val list = mutableListOf<MasterCashEntry>()

        // NOTE: Individual sale transactions stay in the daily register and are NOT added directly to
        // the master cash ledger until settled via Cash Closing. Cash from sales enters the master ledger
        // solely through confirmed DAY_END_CLOSING entries.

        // B. Due Collected Inflow
        val collectedDues = dueLogs.filter { it.type == "DUE_COLLECTED" && it.amount > 0 }
        for (due in collectedDues) {
            list.add(
                MasterCashEntry(
                    id = "DUE_${due.id}",
                    source = "DUE_COLLECTED",
                    timestamp = due.timestamp,
                    title = if (language == "bn") "বাকি আদায় (${due.customerName})" else "Due Collected (${due.customerName})",
                    note = if (due.note.isNotBlank()) due.note else (if (language == "bn") "বাকি থেকে নগদ গ্রহণ" else "Due collection"),
                    categoryOrCustomer = due.customerName,
                    amount = CalculationHelper.round2(due.amount),
                    isAddition = true,
                    paymentMethod = "CASH"
                )
            )
        }

        // C. Shop Expenses Outflow
        for (exp in expenses) {
            if (exp.amount > 0) {
                list.add(
                    MasterCashEntry(
                        id = "EXP_${exp.id}",
                        source = "EXPENSE",
                        timestamp = exp.timestamp,
                        title = if (language == "bn") "দোকানের খরচ: ${exp.title}" else "Expense: ${exp.title}",
                        note = if (exp.note.isNotBlank()) "${exp.category} • ${exp.note}" else exp.category,
                        categoryOrCustomer = exp.category,
                        amount = CalculationHelper.round2(exp.amount),
                        isAddition = false,
                        paymentMethod = "CASH"
                    )
                )
            }
        }

        // D. Stock Purchases / Supplier Outflow
        val purchases = allTransactions.filter { (it.type == "STOCK_IN" || it.type == "PURCHASE") && it.totalAmount > 0 }
        for (pur in purchases) {
            val invLabel = if (pur.invoiceNumber.isNotBlank()) " (মেমো #${pur.invoiceNumber})" else ""
            list.add(
                MasterCashEntry(
                    id = "PUR_${pur.id}",
                    source = "PURCHASE",
                    timestamp = pur.timestamp,
                    title = if (language == "bn") "পণ্য ক্রয়: ${pur.productName}$invLabel" else "Purchase: ${pur.productName}$invLabel",
                    note = "পরিমাণ: ${formatQuantity(pur.quantity)} ${pur.unit} • দর: $currency${pur.unitPrice.toIntOrNull() ?: pur.unitPrice}",
                    categoryOrCustomer = "স্টক ক্রয়",
                    amount = CalculationHelper.round2(pur.totalAmount),
                    isAddition = false,
                    paymentMethod = pur.paymentMethod,
                    invoiceNumber = pur.invoiceNumber
                )
            )
        }

        // E. Manual & System Cash Logs (Direct Incomes, Deposits, Withdrawals, Cash Closings, Adjustments)
        // Skip auto-generated duplicate notes to avoid double-counting!
        for (log in cashLogs) {
            val noteLower = log.note.trim()
            val isAutoExpenseDuplicate = noteLower.startsWith("খরচ:") || noteLower.startsWith("খরচ বাতিল")
            val isAutoDueDuplicate = noteLower.startsWith("বাকি আদায়") || noteLower.startsWith("বাকি লগ")
            val isAutoPurchaseDuplicate = noteLower.startsWith("পণ্য ক্রয়") || noteLower.startsWith("স্টক ইন")
            val isAutoSaleDuplicate = noteLower.startsWith("বিক্রি বাতিল") || noteLower.startsWith("ট্রানজেকশন")

            if (!isAutoExpenseDuplicate && !isAutoDueDuplicate && !isAutoPurchaseDuplicate && !isAutoSaleDuplicate) {
                val isAdd = when (log.type) {
                    "DEPOSIT", "INCOME", "DAY_END_CLOSING" -> true
                    "WITHDRAWAL", "EXPENSE" -> false
                    "MANUAL_ADJUST" -> {
                        if (log.note.contains("ঘাটতি") || log.note.contains("কমানো") || log.amount < 0) {
                            false
                        } else {
                            true
                        }
                    }
                    else -> log.amount >= 0
                }
                val typeLabel = when (log.type) {
                    "DEPOSIT" -> if (language == "bn") "ক্যাশ জমা" else "Cash Deposit"
                    "WITHDRAWAL" -> if (language == "bn") "ক্যাশ উত্তোলন" else "Cash Withdrawal"
                    "INCOME" -> if (language == "bn") "নগদ আয়" else "Cash Income"
                    "EXPENSE" -> if (language == "bn") "নগদ খরচ" else "Cash Expense"
                    "DAY_END_CLOSING" -> if (language == "bn") "ক্যাশ ক্লোজিং" else "Day-End Closing"
                    "MANUAL_ADJUST" -> if (language == "bn") "ব্যালেন্স সমন্বয়" else "Balance Adjustment"
                    else -> log.type
                }
                val itemTitle = if (log.type == "DAY_END_CLOSING") {
                    if (log.note.isNotBlank()) "ক্যাশ ক্লোজিং: ${log.note}" else "ক্যাশ ক্লোজিং: দিনের নগদ বিক্রি ও আদায়"
                } else if (log.note.isNotBlank()) {
                    "$typeLabel: ${log.note}"
                } else {
                    typeLabel
                }
                list.add(
                    MasterCashEntry(
                        id = "LOG_${log.id}",
                        source = log.type,
                        timestamp = log.timestamp,
                        title = itemTitle,
                        note = if (log.note.isNotBlank()) log.note else typeLabel,
                        categoryOrCustomer = if (log.type == "DAY_END_CLOSING") (if (language == "bn") "ক্যাশ ক্লোজিং" else "Day-End Closing") else typeLabel,
                        amount = CalculationHelper.round2(Math.abs(log.amount)),
                        isAddition = isAdd,
                        paymentMethod = "CASH",
                        originalCashLog = log
                    )
                )
            }
        }

        // F. Opening Capital / Cash In Hand Baseline Reconciliation
        // Calculate the net cash movement from all recorded transactions so far
        val transactionsNetSum = list.sumOf { if (it.isAddition) it.amount else -it.amount }
        // The baseline difference between the shop's true cash in hand and the recorded transactions:
        val baselineDiff = CalculationHelper.round2(shopInfo.mainBalance - transactionsNetSum)

        // If there is an opening cash balance, initial capital, or historical baseline difference,
        // include it as the foundational opening cash entry at the start of the ledger:
        if (Math.abs(baselineDiff) > 0.001) {
            val earliestTimestamp = list.minOfOrNull { it.timestamp } ?: (System.currentTimeMillis() - 86400000L)
            list.add(
                MasterCashEntry(
                    id = "OPENING_BALANCE",
                    source = "INITIAL_BALANCE",
                    timestamp = earliestTimestamp - 60000L, // 1 minute before the first transaction
                    title = if (language == "bn") "প্রারম্ভিক ক্যাশ তহবিল (হাতে নগদ)" else "Opening Cash Balance",
                    note = if (language == "bn") "দোকানের মূল ক্যাশ তহবিল / ব্যালেন্স সমন্বয়" else "Opening Shop Cash Balance",
                    categoryOrCustomer = if (language == "bn") "প্রারম্ভিক তহবিল" else "Opening Funds",
                    amount = Math.abs(baselineDiff),
                    isAddition = baselineDiff > 0,
                    paymentMethod = "CASH"
                )
            )
        }

        // Sort strictly oldest to newest for chronological running balance calculation
        val sortedAsc = list.sortedBy { it.timestamp }
        var currentRunningBalance = 0.0
        val withRunningBalance = sortedAsc.map { entry ->
            if (entry.isAddition) {
                currentRunningBalance += entry.amount
            } else {
                currentRunningBalance -= entry.amount
            }
            entry.copy(runningBalance = CalculationHelper.round2(currentRunningBalance))
        }

        // Return newest first for display in UI
        withRunningBalance.reversed()
    }

    // 2. Filter by Date Period, Type (Inflow/Outflow), and Search Query
    val filteredEntries = remember(masterEntries, selectedPeriod, selectedTypeFilter, searchQuery, customSelectedDateMillis) {
        masterEntries.filter { item ->
            // Period Filter
            val matchesPeriod = when (selectedPeriod) {
                "দৈনিক", "DAILY", "Daily" -> item.timestamp >= startOfToday
                "গতকাল", "YESTERDAY", "Yesterday" -> item.timestamp in startOfYesterday..endOfYesterday
                "সাপ্তাহিক", "WEEKLY", "Weekly" -> item.timestamp >= startOfWeek
                "মাসিক", "MONTHLY", "Monthly" -> item.timestamp >= startOfMonth
                "বার্ষিক", "YEARLY", "Yearly" -> item.timestamp >= startOfYear
                "তারিখ বাছুন", "CUSTOM", "Custom Date" -> {
                    if (customSelectedDateMillis != null) {
                        val cal = Calendar.getInstance().apply { timeInMillis = customSelectedDateMillis!! }
                        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                        val start = cal.timeInMillis
                        val end = start + 86400000L - 1
                        item.timestamp in start..end
                    } else true
                }
                else -> true
            }

            // Type Filter
            val matchesType = when (selectedTypeFilter) {
                "INCOME" -> item.isAddition
                "EXPENSE" -> !item.isAddition
                else -> true
            }

            // Search Filter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.note.contains(searchQuery, ignoreCase = true) ||
                item.categoryOrCustomer.contains(searchQuery, ignoreCase = true) ||
                item.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                item.amount.toString().contains(searchQuery)
            }

            matchesPeriod && matchesType && matchesSearch
        }
    }

    // Exact summary metrics
    val totalIncome = remember(filteredEntries) {
        CalculationHelper.round2(filteredEntries.filter { it.isAddition }.sumOf { it.amount })
    }

    val totalExpense = remember(filteredEntries) {
        CalculationHelper.round2(filteredEntries.filter { !it.isAddition }.sumOf { it.amount })
    }

    val netBalance = CalculationHelper.round2(totalIncome - totalExpense)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (onBack != null) Modifier.padding(top = effectiveTopPadding) else Modifier,
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(if (language == "bn") "মেমো, কাস্টমার, নোট খুঁজুন..." else "Search memo, customer, note...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFFFEDD5), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (language == "bn") "ক" else "C",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DueOrange
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "দোকানের মূল ক্যাশ খাতা" else "Shop Master Cash Book",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "bn") "সকল নগদ ক্রয়-বিক্রয় ও ব্যালেন্স হিসাব" else "Complete Cash Ledger & Balances",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    // PDF Statement Download / Share Button
                    IconButton(onClick = {
                        val periodName = if (selectedPeriod == "তারিখ বাছুন" && customDateLabel.isNotBlank()) {
                            customDateLabel
                        } else selectedPeriod
                        val pdfFile = PdfGenerator.generateMasterCashBookPdf(
                            context = context,
                            shopName = shopInfo.shopName,
                            periodTitle = periodName,
                            entries = filteredEntries,
                            currency = currency
                        )
                        if (pdfFile != null) {
                            PdfGenerator.openOrSharePdf(context, pdfFile, "দোকানের মূল ক্যাশ খাতা স্টেটমেন্ট")
                        }
                    }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Cash Addition Ledger Icon
                    IconButton(onClick = { showAddedCashHistoryDialog = true }) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = if (language == "bn") "ক্যাশ এড খতিয়ান" else "Added Cash History",
                            tint = ProfitGreen
                        )
                    }

                    // Cash Balance Adjustment Button
                    IconButton(onClick = { showAdjustBalanceDialog = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = if (language == "bn") "ক্যাশ সংশোধন" else "Adjust Cash",
                            tint = DueOrange
                        )
                    }

                    // Search Button
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            val bottomPadding = if (onBack != null) {
                maxOf(effectiveNavBottomPadding, 48.dp) + 8.dp
            } else {
                6.dp
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = bottomPadding)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 1. Two Large Action Buttons (জমা & প্রদত্ত) placed on top matching screenshot
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Green "↓ জমা" Button (Deposit / Cash In)
                        Button(
                            onClick = { showIncomeDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "↓ জমা" else "↓ Deposit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Red "↑ প্রদত্ত" Button (Expense / Cash Out)
                        Button(
                            onClick = { showExpenseDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "↑ প্রদত্ত" else "↑ Given",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // 2. 3-Column Summary Ribbon placed directly under buttons matching screenshot
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Total Collected / মোট জমা (Clickable to add deposit)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showIncomeDialog = true }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (language == "bn") "মোট জমা" else "Total Received",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CalculationHelper.formatCurrency(totalIncome, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4ADE80)
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(26.dp), color = Color(0xFF475569))

                            // Total Given / মোট প্রদত্ত (Clickable to add expense)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showExpenseDialog = true }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (language == "bn") "মোট প্রদত্ত" else "Total Given",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CalculationHelper.formatCurrency(totalExpense, currency),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171)
                                )
                            }

                            VerticalDivider(modifier = Modifier.height(26.dp), color = Color(0xFF475569))

                            // Balance (Clickable to adjust cash)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clickable { showAdjustBalanceDialog = true }
                                    .padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    text = if (language == "bn") "ব্যালেন্স" else "Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val displayBalance = if (selectedPeriod == "সব") netBalance else shopInfo.mainBalance
                                Text(
                                    text = "${CalculationHelper.formatCurrency(displayBalance, currency)} ${if (language == "bn") "ক্যাশ" else "Cash"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (displayBalance >= 0) Color(0xFF4ADE80) else Color(0xFFFB923C),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Added Cash to Drawer Banner Card
            val addedCashItems = remember(cashLogs) {
                cashLogs.filter { log ->
                    val nl = log.note.trim()
                    val isAuto = nl.startsWith("খরচ:") || nl.startsWith("খরচ বাতিল") || nl.startsWith("বাকি আদায়") || nl.startsWith("বাকি লগ") || nl.startsWith("পণ্য ক্রয়") || nl.startsWith("স্টক ইন") || nl.startsWith("দিনশেষের বিক্রি") || nl.startsWith("বিক্রি বাতিল")
                    !isAuto && (log.type == "DEPOSIT" || (log.type == "INCOME" && !log.note.startsWith("বিক্রি")) || (log.type == "MANUAL_ADJUST" && log.amount > 0 && !log.note.contains("ঘাটতি")))
                }
            }
            val totalAddedCashAmount = remember(addedCashItems) {
                addedCashItems.sumOf { Math.abs(it.amount) }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showAddedCashHistoryDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFD1FAE5), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "মূল ক্যাশে টাকা এড খতিয়ান" else "Added Cash to Drawer",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "মোট এড: ${CalculationHelper.formatCurrency(totalAddedCashAmount, currency)} (${addedCashItems.size} বার)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick PDF Download
                        IconButton(
                            onClick = {
                                val parsedList = addedCashItems.map { parseCashAdditionItem(it) }.sortedByDescending { it.timestamp }
                                if (parsedList.isEmpty()) {
                                    Toast.makeText(context, if (language == "bn") "কোনো রেকর্ড পাওয়া যায়নি" else "No records found", Toast.LENGTH_SHORT).show()
                                } else {
                                    val pdfFile = PdfGenerator.generateAddedCashHistoryPdf(
                                        context = context,
                                        shopName = shopInfo.shopName,
                                        shopPhone = shopInfo.phone,
                                        periodTitle = "সকল সময়",
                                        entries = parsedList,
                                        currency = currency
                                    )
                                    if (pdfFile != null) {
                                        Toast.makeText(context, if (language == "bn") "পিডিএফ তৈরি হয়েছে" else "PDF Generated", Toast.LENGTH_SHORT).show()
                                        PdfGenerator.openOrSharePdf(context, pdfFile, "দোকানের মূল ক্যাশে টাকা যুক্ত করার খতিয়ান")
                                    } else {
                                        Toast.makeText(context, if (language == "bn") "পিডিএফ ব্যর্থ হয়েছে" else "PDF failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                        }

                        // Open Dialog Button
                        Button(
                            onClick = { showAddedCashHistoryDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (language == "bn") "খতিয়ান" else "View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Cash Closing Status & Daily Register Cash Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (summary.todayUnclosedCash > 0) Color(0xFFFFFBEB) else Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, if (summary.todayUnclosedCash > 0) Color(0xFFFDE68A) else Color(0xFFBBF7D0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(
                                    if (summary.todayUnclosedCash > 0) Color(0xFFFEF3C7) else Color(0xFFDCFCE7),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (summary.todayUnclosedCash > 0) Icons.Default.Savings else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (summary.todayUnclosedCash > 0) Color(0xFFD97706) else Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (summary.todayUnclosedCash > 0) {
                                    if (language == "bn") "চলতি নগদ বিক্রি: $currency${summary.todayUnclosedCash.toIntOrNull() ?: summary.todayUnclosedCash}" else "Unclosed Cash: $currency${summary.todayUnclosedCash}"
                                } else {
                                    if (language == "bn") "আজকের ক্যাশ ক্লোজিং সম্পন্ন" else "Today's Cash Settled"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.todayUnclosedCash > 0) Color(0xFF92400E) else Color(0xFF166534)
                            )
                            Text(
                                text = if (summary.todayUnclosedCash > 0) {
                                    if (language == "bn") "ড্রয়ারে জমা আছে, ক্লোজ করলে মূল খাতায় যুক্ত হবে" else "In drawer, will add to ledger upon closing"
                                } else {
                                    if (language == "bn") "মোট ক্লোজিং জমা: $currency${summary.todayClosedCash.toIntOrNull() ?: summary.todayClosedCash}" else "Total settled: $currency${summary.todayClosedCash}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (summary.todayUnclosedCash > 0) Color(0xFFB45309) else Color(0xFF15803D),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showDayEndSettleDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (summary.todayUnclosedCash > 0) Color(0xFFD97706) else Color(0xFF16A34A)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (language == "bn") "ক্যাশ ক্লোজিং" else "Close Cash",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Subheader: Transaction Count & Period Filters (Matching screenshot)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${if (language == "bn") "লেনদেন" else "Transactions"} ${filteredEntries.size}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedPeriod == "তারিখ বাছুন" && customDateLabel.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = customDateLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (language == "bn") "ব্যালেন্স" else "Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Switch(
                                checked = showBalanceToggle,
                                onCheckedChange = { showBalanceToggle = it },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Period Filter Chips (সব, দৈনিক, সাপ্তাহিক, মাসিক, বার্ষিক, গতকাল, 🗓️ তারিখ বাছুন)
                    val filters = listOf("সব", "দৈনিক", "সাপ্তাহিক", "মাসিক", "বার্ষিক", "গতকাল", "তারিখ বাছুন 🗓️")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filters) { filterName ->
                            val isCustomPicker = filterName.startsWith("তারিখ বাছুন")
                            val isSelected = if (isCustomPicker) selectedPeriod == "তারিখ বাছুন" else selectedPeriod == filterName

                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isCustomPicker) {
                                        selectedPeriod = "তারিখ বাছুন"
                                        val cal = Calendar.getInstance()
                                        val datePickerDialog = DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val selCal = Calendar.getInstance().apply {
                                                    set(y, m, d, 0, 0, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                customSelectedDateMillis = selCal.timeInMillis
                                                customDateLabel = String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y)
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        )
                                        datePickerDialog.show()
                                    } else {
                                        selectedPeriod = filterName
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (isCustomPicker && customDateLabel.isNotBlank()) "🗓️ $customDateLabel" else filterName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Table Column Header (Dark slate strip matching screenshot)
            Surface(
                color = Color(0xFF334155),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "তারিখ ও বিবরণ" else "Date & Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = if (language == "bn") "জমা" else "Received",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4ADE80), // Green
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                    Text(
                        text = if (language == "bn") "প্রদত্ত" else "Given",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF87171), // Red
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(0.9f)
                    )
                    if (showBalanceToggle) {
                        Text(
                            text = if (language == "bn") "ব্যালেন্স" else "Balance",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Table Body Rows (Clean List Layout with exact running balance)
            if (filteredEntries.isEmpty()) {
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
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == "bn") "কোনো ক্যাশ লেনদেন পাওয়া যায়নি" else "No cash transactions found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { item ->
                        val isAddition = item.isAddition
                        val dateFormatted = remember(item.timestamp, language) {
                            formatLedgerDate(item.timestamp, language)
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.originalCashLog != null) {
                                        editingCashLog = item.originalCashLog
                                    } else {
                                        viewingEntryDetails = item
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Column 1: Date & Title / Note
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val detailText = if (item.note.isNotBlank() && item.note != item.title) {
                                        "${item.title} • ${item.note}"
                                    } else {
                                        item.title
                                    }
                                    Text(
                                        text = detailText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Column 2: জমা (Deposit / Sales / Cash In - Green)
                                Box(
                                    modifier = Modifier.weight(0.9f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (isAddition) {
                                        Text(
                                            text = "$currency${item.amount.toIntOrNull() ?: item.amount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4ADE80)
                                        )
                                    }
                                }

                                // Column 3: প্রদত্ত / খরচ (Expense / Outflow - Red)
                                Box(
                                    modifier = Modifier.weight(0.9f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (!isAddition) {
                                        Text(
                                            text = "$currency${item.amount.toIntOrNull() ?: item.amount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF87171)
                                        )
                                    }
                                }

                                // Column 4: Running Balance
                                if (showBalanceToggle) {
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        val bal = item.runningBalance
                                        Text(
                                            text = "${if (bal < 0) "-" else ""}$currency${Math.abs(bal).toIntOrNull() ?: Math.abs(bal)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (bal < 0) Color(0xFFFB923C) else Color(0xFF4ADE80)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    // Cash In (Income) Entry Dialog
    if (showIncomeDialog) {
        MasterCashEntryDialog(
            title = if (language == "bn") "নগদ জমা / আয় যোগ করুন" else "Add Cash Income",
            currency = currency,
            language = language,
            isIncome = true,
            onDismiss = { showIncomeDialog = false },
            onConfirm = { amount, note, timestamp ->
                viewModel.addCashIncome(amount, note, timestamp)
                showIncomeDialog = false
            }
        )
    }

    // Cash Out (Expense) Entry Dialog
    if (showExpenseDialog) {
        MasterCashEntryDialog(
            title = if (language == "bn") "নগদ খরচ / উত্তোলন কর্তন করুন" else "Record Cash Expense",
            currency = currency,
            language = language,
            isIncome = false,
            onDismiss = { showExpenseDialog = false },
            onConfirm = { amount, note, timestamp ->
                viewModel.addCashExpense(amount, note, "অন্যান্য", timestamp)
                showExpenseDialog = false
            }
        )
    }

    // Non-editable entry details dialog (Sales, Due collection, Purchases)
    if (viewingEntryDetails != null) {
        val entry = viewingEntryDetails!!
        Dialog(onDismissRequest = { viewingEntryDetails = null }) {
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
                            text = if (language == "bn") "লেনদেনের বিস্তারিত তথ্য" else "Transaction Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { viewingEntryDetails = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = if (entry.isAddition) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (entry.isAddition) (if (language == "bn") "নগদ জমা (ইনফ্লো)" else "Cash Inflow") else (if (language == "bn") "নগদ ব্যয় (আউটফ্লো)" else "Cash Outflow"),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (entry.isAddition) Color(0xFF15803D) else Color(0xFFB91C1C),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currency${entry.amount.toIntOrNull() ?: entry.amount}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (entry.isAddition) ProfitGreen else LossRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    DetailItemRow(label = if (language == "bn") "শিরোনাম" else "Title", value = entry.title)
                    DetailItemRow(label = if (language == "bn") "খাত / বিবরণ" else "Details", value = entry.note.ifBlank { "N/A" })
                    DetailItemRow(label = if (language == "bn") "তারিখ ও সময়" else "Date & Time", value = formatMasterLedgerDate(entry.timestamp, language))
                    if (entry.invoiceNumber.isNotBlank()) {
                        DetailItemRow(label = if (language == "bn") "মেমো নম্বর" else "Invoice #", value = entry.invoiceNumber)
                    }
                    if (entry.categoryOrCustomer.isNotBlank()) {
                        DetailItemRow(label = if (language == "bn") "কাস্টমার / ক্যাটাগরি" else "Customer / Category", value = entry.categoryOrCustomer)
                    }
                    DetailItemRow(label = if (language == "bn") "পেমেন্ট মাধ্যম" else "Payment Method", value = entry.paymentMethod)
                    DetailItemRow(label = if (language == "bn") "তৎকালীন ব্যালেন্স" else "Balance After", value = "$currency${entry.runningBalance.toIntOrNull() ?: entry.runningBalance}")

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewingEntryDetails = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (language == "bn") "ঠিক আছে" else "OK")
                    }
                }
            }
        }
    }

    // Edit CashLog Dialog
    if (editingCashLog != null) {
        val target = editingCashLog!!
        var editAmount by remember { mutableStateOf(target.amount.toIntOrNull()?.toString() ?: target.amount.toString()) }
        var editNote by remember { mutableStateOf(target.note) }

        Dialog(onDismissRequest = { editingCashLog = null }) {
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
                            deletingCashLog = target
                            editingCashLog = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
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
                        label = { Text(if (language == "bn") "নোট / বিবরণ" else "Note / Description") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingCashLog = null }) {
                            Text(if (language == "bn") "বাতিল" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = editAmount.toDoubleOrNull() ?: target.amount
                                viewModel.editCashLog(target, amt, editNote)
                                editingCashLog = null
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (language == "bn") "সংরক্ষণ করুন" else "Save")
                        }
                    }
                }
            }
        }
    }

    // Delete Log Confirmation Dialog
    if (deletingCashLog != null) {
        val target = deletingCashLog!!
        AlertDialog(
            onDismissRequest = { deletingCashLog = null },
            title = { Text(if (language == "bn") "লেনদেন ডিলিট নিশ্চিতকরণ" else "Confirm Delete") },
            text = { Text(if (language == "bn") "আপনি কি এই ক্যাশ এন্ট্রিটি মুছে ফেলতে চান?" else "Are you sure you want to delete this cash entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCashLog(target)
                        deletingCashLog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text(if (language == "bn") "ডিলিট করুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCashLog = null }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }

    if (showAdjustBalanceDialog) {
        EditMainCashBalanceDialog(
            currentBalance = shopInfo.mainBalance,
            currency = currency,
            language = language,
            onDismiss = { showAdjustBalanceDialog = false },
            onConfirm = { newBalance, note ->
                viewModel.updateMainBalance(newBalance, note)
                showAdjustBalanceDialog = false
            }
        )
    }

    if (showAddedCashHistoryDialog) {
        AddedCashHistoryDialog(
            cashLogs = cashLogs,
            shopName = shopInfo.shopName,
            shopPhone = shopInfo.phone,
            currency = currency,
            language = language,
            onDismiss = { showAddedCashHistoryDialog = false },
            onAddNewCash = {
                showAddedCashHistoryDialog = false
                showIncomeDialog = true
            },
            onDeleteLog = { logToDelete ->
                viewModel.deleteCashLog(logToDelete)
            }
        )
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

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MasterCashEntryDialog(
    title: String,
    currency: String,
    language: String,
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, timestamp: Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("OWN") } // "OWN", "PERSON", "OTHER"
    var personName by remember { mutableStateOf("") }

    val presetNotes = if (isIncome) {
        listOf("ক্যাশ বিক্রি", "বাকি আদায়", "ব্যক্তিগত জমা", "মহাজন বাকি ফেরত", "অন্যান্য আয়")
    } else {
        listOf("দোকান ভাড়া", "বিদ্যুৎ বিল", "কর্মচারীর বেতন", "মাল ক্রয় / চালান", "ব্যক্তিগত খরচ", "চা-নাস্তা", "যাতায়াত")
    }

    val presetAmounts = if (isIncome) {
        listOf("500", "1000", "2000", "5000")
    } else {
        listOf("100", "200", "500", "1000")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (isIncome) Color(0xFFDCFCE7) else Color(0xFFFEE2E2), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isIncome) Icons.Default.Add else Icons.Default.Remove,
                                contentDescription = null,
                                tint = if (isIncome) Color(0xFF16A34A) else Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isIncome) Color(0xFF15803D) else Color(0xFFB91C1C),
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // If adding cash to drawer, show Source Selector
                if (isIncome) {
                    Text(
                        text = if (language == "bn") "টাকা কোথা থেকে এড করছেন? *" else "Cash Source *",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sources = listOf(
                            Triple("OWN", "👤 নিজের ক্যাশ", Color(0xFF059669)),
                            Triple("PERSON", "🤝 কারো থেকে আনা", Color(0xFFD97706)),
                            Triple("OTHER", "🏷️ অন্যান্য", Color(0xFF475569))
                        )
                        sources.forEach { (key, label, activeColor) ->
                            val isSelected = selectedSource == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) activeColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(1.2.dp, if (isSelected) activeColor else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedSource = key }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    fontSize = 10.5.sp,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    if (selectedSource == "PERSON") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = personName,
                            onValueChange = { personName = it },
                            label = { Text(if (language == "bn") "কার কাছ থেকে এনেছেন? (নাম) *" else "Person Name *") },
                            placeholder = { Text("যেমন: রহিম ভাই / মহাজন / চাচা") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (language == "bn") "টাকার পরিমাণ ($currency) *" else "Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Preset Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetAmounts.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val current = amountStr.toDoubleOrNull() ?: 0.0
                                    val add = preset.toDoubleOrNull() ?: 0.0
                                    amountStr = (current + add).toInt().toString()
                                }
                        ) {
                            Text(
                                text = "+$currency$preset",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Note Field
                OutlinedTextField(
                    value = noteStr,
                    onValueChange = { noteStr = it },
                    label = { Text(if (language == "bn") "বিবরণ / নোট (ঐচ্ছিক)" else "Description / Note (Optional)") },
                    placeholder = { Text(if (isIncome) "যেমন: ব্যবসার প্রয়োজনে এনেছি" else "যেমন: দোকান ভাড়া / ব্যক্তিগত খরচ") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Note Category Suggestions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetNotes.take(3).forEach { noteSuggestion ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { noteStr = noteSuggestion }
                        ) {
                            Text(
                                text = noteSuggestion,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                val isButtonEnabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0 &&
                        (!isIncome || selectedSource != "PERSON" || personName.isNotBlank())

                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val finalNote = if (isIncome) {
                                when (selectedSource) {
                                    "OWN" -> {
                                        val base = "নিজের ক্যাশ থেকে জমা"
                                        if (noteStr.isNotBlank()) "$base • ${noteStr.trim()}" else base
                                    }
                                    "PERSON" -> {
                                        val base = "কার কাছ থেকে আনা: ${personName.trim()}"
                                        if (noteStr.isNotBlank()) "$base • ${noteStr.trim()}" else base
                                    }
                                    else -> {
                                        noteStr.ifBlank { "ক্যাশ জমা" }
                                    }
                                }
                            } else {
                                noteStr.ifBlank { "ক্যাশ খরচ" }
                            }
                            onConfirm(amount, finalNote, System.currentTimeMillis())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncome) Color(0xFF16A34A) else Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = isButtonEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(if (isIncome) Icons.Default.Add else Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isIncome) (if (language == "bn") "ক্যাশে জমা যুক্ত করুন" else "Add Cash") else (if (language == "bn") "ক্যাশ থেকে খরচ কর্তন করুন" else "Record Expense"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

fun formatMasterLedgerDate(timestamp: Long, language: String): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()

    val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

    val isYesterday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) - 1

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

    return if (language == "bn") {
        when {
            isToday -> "আজ, $timeFormat"
            isYesterday -> "গতকাল, $timeFormat"
            else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))
        }
    } else {
        when {
            isToday -> "Today, $timeFormat"
            isYesterday -> "Yesterday, $timeFormat"
            else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.US).format(Date(timestamp))
        }
    }
}

private fun formatQuantity(qty: Double): String {
    return if (qty % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", qty)
    } else {
        String.format(Locale.US, "%.2f", qty)
    }
}
