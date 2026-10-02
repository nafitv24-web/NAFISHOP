package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CashLog
import com.example.data.model.TransactionRecord
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CalculationHelper
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Authentic "টং খাতা" (Tong Khata) Cashbox & Cash Accounts Screen
 * Faithfully matches Screenshot_20260925-183548 and Screenshot_20260925-183720
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashboxTongScreen(
    viewModel: ShopViewModel,
    onNavigateToDue: (() -> Unit)? = null,
    onNavigateToPos: (() -> Unit)? = null,
    onNavigateToReports: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val isBn = language == "bn"
    val shopInfo by viewModel.shopInfo.collectAsState()
    val currency = shopInfo.currency

    val allTransactions by viewModel.allTransactions.collectAsState()
    val cashLogs by viewModel.cashLogs.collectAsState()
    val dueLogs by viewModel.dueLogs.collectAsState()
    val allExpenses by viewModel.expenses.collectAsState()
    val summary by viewModel.dashboardSummary.collectAsState()

    // Mode: Main Cashbox vs Detailed "নগদ হিসাব"
    var isDetailedView by remember { mutableStateOf(false) }

    // Sub-tab selection:
    // Main View: 0 -> রিপোর্ট, 1 -> ক্যাশবাক্স, 2 -> মালিক হিসাব
    var mainSubTab by remember { mutableIntStateOf(1) }
    // Detailed View: 0 -> দৈনিক, 1 -> মাসিক
    var detailedSubTab by remember { mutableIntStateOf(0) }

    // Date state
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val now = remember { System.currentTimeMillis() }

    // Start and end of selected day
    val selectedDayStart = remember(selectedCalendar) {
        val cal = selectedCalendar.clone() as Calendar
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val selectedDayEnd = selectedDayStart + 86400000L - 1L

    val isSelectedToday = remember(selectedDayStart) {
        val calNow = Calendar.getInstance()
        calNow.set(Calendar.HOUR_OF_DAY, 0)
        calNow.set(Calendar.MINUTE, 0)
        calNow.set(Calendar.SECOND, 0)
        calNow.set(Calendar.MILLISECOND, 0)
        selectedDayStart == calNow.timeInMillis
    }

    // Interactive Dialog States
    var showAddCashDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showOwnerCashDialog by remember { mutableStateOf(false) }
    var isOwnerDeposit by remember { mutableStateOf(true) }
    var showSetOpeningCashDialog by remember { mutableStateOf(false) }
    var showReportSummaryDialog by remember { mutableStateOf(false) }
    var showOwnerLedgerDialog by remember { mutableStateOf(false) }

    // ----------------------------------------------------
    // COMPUTE REAL DATA FOR SELECTED DAY / MONTH
    // ----------------------------------------------------
    val daySalesTxs = remember(allTransactions, selectedDayStart, selectedDayEnd) {
        allTransactions.filter { it.timestamp in selectedDayStart..selectedDayEnd && it.type == "SALE" }
    }
    val dayExpenses = remember(allExpenses, selectedDayStart, selectedDayEnd) {
        allExpenses.filter { it.timestamp in selectedDayStart..selectedDayEnd }
    }
    val dayDueCollected = remember(dueLogs, selectedDayStart, selectedDayEnd) {
        dueLogs.filter { it.timestamp in selectedDayStart..selectedDayEnd && it.type == "DUE_COLLECTED" }
    }
    val dayCashLogs = remember(cashLogs, selectedDayStart, selectedDayEnd) {
        cashLogs.filter { it.timestamp in selectedDayStart..selectedDayEnd }
    }

    // Owner cash additions / withdrawals
    val dayOwnerIn = remember(dayCashLogs) {
        dayCashLogs.filter { it.type == "INCOME" || it.type == "OWNER_IN" || it.note.contains("মালিক", ignoreCase = true) }
            .sumOf { it.amount }
    }
    val dayOwnerOut = remember(dayCashLogs) {
        dayCashLogs.filter { it.type == "OWNER_OUT" || it.type == "WITHDRAW" || it.note.contains("মালিক উত্তোলন", ignoreCase = true) }
            .sumOf { it.amount }
    }
    val allOwnerIn = remember(cashLogs) {
        cashLogs.filter { it.type == "INCOME" || it.type == "OWNER_IN" || it.note.contains("মালিক", ignoreCase = true) }
            .sumOf { it.amount }
    }
    val allOwnerOut = remember(cashLogs) {
        cashLogs.filter { it.type == "OWNER_OUT" || it.type == "WITHDRAW" || it.note.contains("মালিক উত্তোলন", ignoreCase = true) }
            .sumOf { it.amount }
    }
    val ownerBalance = allOwnerIn - allOwnerOut

    // Sales breakdown
    val totalSales = daySalesTxs.sumOf { it.totalAmount }
    val cashSales = daySalesTxs.filter { it.paymentMethod.equals("CASH", ignoreCase = true) || it.paymentMethod.isBlank() }.sumOf { it.paidAmount }
    val instantDigitalSales = daySalesTxs.filter { !it.paymentMethod.equals("CASH", ignoreCase = true) && it.paymentMethod.isNotBlank() }.sumOf { it.paidAmount }
    val dueSales = daySalesTxs.sumOf { it.dueAmount }

    // Percentages
    val cashSalePercent = if (totalSales > 0) ((cashSales / totalSales) * 100).toInt() else 0
    val instantSalePercent = if (totalSales > 0) ((instantDigitalSales / totalSales) * 100).toInt() else 0
    val dueSalePercent = if (totalSales > 0) ((dueSales / totalSales) * 100).toInt() else 0

    // Cash Status (ক্যাশের অবস্থা)
    val openingCash = shopInfo.mainBalance
    val totalReceived = cashSales + instantDigitalSales + dayDueCollected.sumOf { it.amount } + dayOwnerIn
    val totalPaid = dayExpenses.sumOf { it.amount } + dayOwnerOut
    val currentCashBalance = openingCash + totalReceived - totalPaid

    val moneyInPercent = if (totalReceived + totalPaid > 0) ((totalReceived / (totalReceived + totalPaid)) * 100).toInt() else if (totalReceived > 0) 100 else 0
    val moneyOutPercent = if (totalReceived + totalPaid > 0) ((totalPaid / (totalReceived + totalPaid)) * 100).toInt() else 0

    // Date formatting in Bengali
    val bnDateLabel = remember(selectedCalendar, isSelectedToday) {
        if (isSelectedToday) {
            "আজ"
        } else {
            val df = SimpleDateFormat("dd MMMM", Locale("bn", "BD"))
            df.format(selectedCalendar.time)
        }
    }
    val bnDateSubtitle = remember(selectedCalendar) {
        val df = SimpleDateFormat("d MMMM, yyyy", Locale("bn", "BD"))
        df.format(selectedCalendar.time)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isDetailedView) "নগদ হিসাব" else "ক্যাশবাক্স",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = if (isDetailedView) TextAlign.Start else TextAlign.Center
                    )
                },
                navigationIcon = {
                    if (isDetailedView) {
                        IconButton(onClick = { isDetailedView = false }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                },
                actions = {
                    // PDF Export Icon
                    IconButton(onClick = {
                        val pdfFile = PdfGenerator.generateTransactionsListPdf(
                            context = context,
                            shopName = shopInfo.shopName,
                            title = "ক্যাশবাক্স লেনদেন রিপোর্ট - $bnDateSubtitle",
                            transactions = daySalesTxs,
                            currency = currency
                        )
                        if (pdfFile != null) {
                            PdfGenerator.openOrSharePdf(context, pdfFile, "ক্যাশবাক্স PDF ডাউনলোড / শেয়ার")
                        } else {
                            Toast.makeText(context, "আজকের কোনো লেনদেন পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "PDF",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (!isDetailedView) {
                        // Analytics / Report Chart Icon
                        IconButton(onClick = {
                            if (onNavigateToReports != null) {
                                onNavigateToReports()
                            } else {
                                showReportSummaryDialog = true
                            }
                        }) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "Reports",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TongRedPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = TongAppBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(TongAppBackground),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ----------------------------------------------------
            // 1. SUB-TABS ROW (Pills)
            // ----------------------------------------------------
            item {
                Surface(
                    color = Color.White,
                    shadowElevation = 1.dp
                ) {
                    if (!isDetailedView) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Tab 0: রিপোর্ট
                            TongSubTabPill(
                                title = "রিপোর্ট",
                                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                isSelected = mainSubTab == 0,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    mainSubTab = 0
                                    showReportSummaryDialog = true
                                }
                            )

                            // Tab 1: ক্যাশবাক্স
                            TongSubTabPill(
                                title = "ক্যাশবাক্স",
                                icon = Icons.Default.PointOfSale,
                                isSelected = mainSubTab == 1,
                                modifier = Modifier.weight(1f),
                                onClick = { mainSubTab = 1 }
                            )

                            // Tab 2: মালিক হিসাব
                            TongSubTabPill(
                                title = "মালিক হিসাব",
                                icon = Icons.Default.AccountBalance,
                                isSelected = mainSubTab == 2,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    mainSubTab = 2
                                    showOwnerLedgerDialog = true
                                }
                            )
                        }
                    } else {
                        // Detailed Subtabs: দৈনিক vs মাসিক
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TongSubTabPill(
                                title = "দৈনিক",
                                icon = Icons.Default.CalendarToday,
                                isSelected = detailedSubTab == 0,
                                modifier = Modifier.weight(1f),
                                onClick = { detailedSubTab = 0 }
                            )
                            TongSubTabPill(
                                title = "মাসিক",
                                icon = Icons.Default.CalendarMonth,
                                isSelected = detailedSubTab == 1,
                                modifier = Modifier.weight(1f),
                                onClick = { detailedSubTab = 1 }
                            )
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 2. DATE SELECTOR BAR & DETAILS TOGGLE
            // ----------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Card: Date Picker with Arrows
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    val cal = selectedCalendar.clone() as Calendar
                                    cal.add(Calendar.DAY_OF_YEAR, -1)
                                    selectedCalendar = cal
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color(0xFF475569))
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        val cal = selectedCalendar
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val newCal = Calendar.getInstance().apply {
                                                    set(y, m, d)
                                                }
                                                selectedCalendar = newCal
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                            ) {
                                Text(
                                    text = bnDateLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = bnDateSubtitle,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            IconButton(
                                onClick = {
                                    val cal = selectedCalendar.clone() as Calendar
                                    cal.add(Calendar.DAY_OF_YEAR, 1)
                                    selectedCalendar = cal
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color(0xFF475569))
                            }
                        }
                    }

                    // Right Card: বিস্তারিত Switch
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .height(54.dp)
                            .clickable { isDetailedView = !isDetailedView }
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "বিস্তারিত",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Switch(
                                checked = isDetailedView,
                                onCheckedChange = { isDetailedView = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = TongRedPrimary,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.height(26.dp)
                            )
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 3. SALES CIRCLE & BREAKDOWN SECTION
            // ----------------------------------------------------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left: Iconic Double-Ring Sales Circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(0.95f)
                                .aspectRatio(1f)
                        ) {
                            // Outer Glow Ring
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(6.dp, Color(0xFFFCE7F3)),
                                shadowElevation = 3.dp,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = TongRedPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "আজকের মোট বিক্রি",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$currency ${CalculationHelper.formatAmount(totalSales)}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TongRedPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (totalSales <= 0.0) "কোনো বিক্রি নেই" else "${daySalesTxs.size} টি বিক্রি সম্পন্ন",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Right: 3 Stacked Rounded Cards
                        Column(
                            modifier = Modifier.weight(1.05f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. নগদ বিক্রি
                            SalesBreakdownPillCard(
                                title = "নগদ বিক্রি",
                                amount = "$currency ${CalculationHelper.formatAmount(cashSales)}",
                                percent = "$cashSalePercent%",
                                progress = if (totalSales > 0) (cashSales / totalSales).toFloat() else 0f,
                                icon = Icons.Default.Payments,
                                iconTint = Color(0xFF10B981),
                                badgeBg = Color(0xFFD1FAE5),
                                progressColor = Color(0xFF10B981)
                            )

                            // 2. ইনস্ট্যান্ট নগদ বিক্রি (e.g. Mobile Banking / Digital)
                            SalesBreakdownPillCard(
                                title = "ইনস্ট্যান্ট নগদ বিক্রি",
                                amount = "$currency ${CalculationHelper.formatAmount(instantDigitalSales)}",
                                percent = "$instantSalePercent%",
                                progress = if (totalSales > 0) (instantDigitalSales / totalSales).toFloat() else 0f,
                                icon = Icons.Default.Smartphone,
                                iconTint = Color(0xFF3B82F6),
                                badgeBg = Color(0xFFDBEAFE),
                                progressColor = Color(0xFF3B82F6),
                                isFocused = instantSalePercent > 0
                            )

                            // 3. বাকি বিক্রি
                            SalesBreakdownPillCard(
                                title = "বাকি বিক্রি",
                                amount = "$currency ${CalculationHelper.formatAmount(dueSales)}",
                                percent = "$dueSalePercent%",
                                progress = if (totalSales > 0) (dueSales / totalSales).toFloat() else 0f,
                                icon = Icons.Default.Schedule,
                                iconTint = Color(0xFFF59E0B),
                                badgeBg = Color(0xFFFEF3C7),
                                progressColor = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ----------------------------------------------------
            // 4. SECTION: ক্যাশের অবস্থা
            // ----------------------------------------------------
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Section Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = TongRedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ক্যাশের অবস্থা",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                text = "বিস্তারিত দেখুন >",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.clickable { isDetailedView = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3 Horizontal Status Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. দিনের শুরুতে ক্যাশ
                            CashStatusMiniCard(
                                title = "দিনের শুরুতে ক্যাশ",
                                amount = "$currency ${CalculationHelper.formatAmount(openingCash)}",
                                icon = Icons.Default.Savings,
                                iconTint = Color(0xFF64748B),
                                bgTint = Color(0xFFFDF2F4),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showSetOpeningCashDialog = true }
                            )

                            // 2. মোট পেলাম
                            CashStatusMiniCard(
                                title = "মোট পেলাম",
                                amount = "$currency ${CalculationHelper.formatAmount(totalReceived)}",
                                icon = Icons.Default.ArrowCircleUp,
                                iconTint = Color(0xFF10B981),
                                bgTint = Color(0xFFECFDF5),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showAddCashDialog = true }
                            )

                            // 3. মোট দিলাম
                            CashStatusMiniCard(
                                title = "মোট দিলাম",
                                amount = "$currency ${CalculationHelper.formatAmount(totalPaid)}",
                                icon = Icons.Default.ArrowCircleDown,
                                iconTint = Color(0xFFEF4444),
                                bgTint = Color(0xFFFFF1F2),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showAddExpenseDialog = true }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ----------------------------------------------------
            // 5. SECTION: বর্তমান ক্যাশ ব্যালেন্স
            // ----------------------------------------------------
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "বর্তমান ক্যাশ ব্যালেন্স",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "$currency ${CalculationHelper.formatAmount(currentCashBalance)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = if (currentCashBalance >= 0) Color(0xFF047857) else Color(0xFFDC2626)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Split Two-Tone Summary Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Left: টাকা এসেছে
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFECFDF5))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "টাকা এসেছে",
                                                fontSize = 11.sp,
                                                color = Color(0xFF065F46),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "$currency ${CalculationHelper.formatAmount(totalReceived)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF065F46)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$moneyInPercent%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF047857)
                                    )
                                }

                                // Center Divider
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(54.dp)
                                        .background(Color(0xFFCBD5E1))
                                )

                                // Right: টাকা গেছে
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFFFF1F2))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "টাকা গেছে",
                                                fontSize = 11.sp,
                                                color = Color(0xFF991B1B),
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "$currency ${CalculationHelper.formatAmount(totalPaid)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF991B1B)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "$moneyOutPercent%",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
            }

            // ----------------------------------------------------
            // 6. SECTION: মালিকের হিসাব
            // ----------------------------------------------------
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shadowElevation = 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = TongRedPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "মালিকের হিসাব",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                            Text(
                                text = "বিস্তারিত দেখুন >",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.clickable { showOwnerLedgerDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // মালিক দিলেন
                            CashStatusMiniCard(
                                title = "মালিক দিলেন",
                                amount = "$currency ${CalculationHelper.formatAmount(dayOwnerIn)}",
                                icon = Icons.Default.ArrowCircleUp,
                                iconTint = Color(0xFF10B981),
                                bgTint = Color(0xFFECFDF5),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        isOwnerDeposit = true
                                        showOwnerCashDialog = true
                                    }
                            )

                            // মালিক নিলেন
                            CashStatusMiniCard(
                                title = "মালিক নিলেন",
                                amount = "$currency ${CalculationHelper.formatAmount(dayOwnerOut)}",
                                icon = Icons.Default.ArrowCircleDown,
                                iconTint = Color(0xFFEF4444),
                                bgTint = Color(0xFFFFF1F2),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        isOwnerDeposit = false
                                        showOwnerCashDialog = true
                                    }
                            )

                            // মালিকের ব্যালেন্স
                            val balLabel = if (ownerBalance >= 0) "জমা" else "দেনা"
                            CashStatusMiniCard(
                                title = "মালিকের ব্যালেন্স",
                                amount = "$currency ${CalculationHelper.formatAmount(Math.abs(ownerBalance))} ($balLabel)",
                                icon = Icons.Default.AccountBalanceWallet,
                                iconTint = Color(0xFF2563EB),
                                bgTint = Color(0xFFEFF6FF),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showOwnerLedgerDialog = true }
                            )
                        }
                    }
                }
            }

            // ----------------------------------------------------
            // 7. IF DETAILED VIEW: TODAY'S REAL LEDGER TRANSACTION LIST
            // ----------------------------------------------------
            if (isDetailedView) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "দিনের মোট লেনদেন হিসাব (${daySalesTxs.size + dayExpenses.size + dayDueCollected.size} টি)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { showAddCashDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("+ টাকা এলো", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { showAddExpenseDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("- টাকা গেল", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            if (daySalesTxs.isEmpty() && dayExpenses.isEmpty() && dayDueCollected.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "আজকের দিনে কোনো লেনদেন রেকর্ড পাওয়া যায়নি",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                // Sales records
                                daySalesTxs.forEach { sale ->
                                    TongTransactionRow(
                                        title = if (sale.customerName.isNotBlank()) "বিক্রয়: ${sale.customerName}" else "নগদ বিক্রয়",
                                        subtitle = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date(sale.timestamp)) + " • মেমো #${sale.invoiceNumber}",
                                        amount = "+$currency ${CalculationHelper.formatAmount(sale.paidAmount)}",
                                        isIncome = true,
                                        extraTag = if (sale.paymentMethod.isNotBlank()) sale.paymentMethod else "CASH"
                                    )
                                }

                                // Due collections
                                dayDueCollected.forEach { due ->
                                    TongTransactionRow(
                                        title = "বাকি আদায়: ${due.customerName}",
                                        subtitle = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date(due.timestamp)),
                                        amount = "+$currency ${CalculationHelper.formatAmount(due.amount)}",
                                        isIncome = true,
                                        extraTag = "DUE"
                                    )
                                }

                                // Expenses
                                dayExpenses.forEach { exp ->
                                    TongTransactionRow(
                                        title = "খরচ: ${exp.title}",
                                        subtitle = SimpleDateFormat("hh:mm a", Locale("bn", "BD")).format(Date(exp.timestamp)) + " • ${exp.category}",
                                        amount = "-$currency ${CalculationHelper.formatAmount(exp.amount)}",
                                        isIncome = false,
                                        extraTag = "EXPENSE"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // DIALOGS: ADD CASH IN, EXPENSE, OWNER, OPENING BALANCE
    // ----------------------------------------------------
    if (showAddCashDialog) {
        TongQuickCashDialog(
            title = "টাকা এসেছে (নগদ গ্রহণ)",
            confirmBtnText = "জমা করুন",
            currency = currency,
            onDismiss = { showAddCashDialog = false },
            onConfirm = { amt, note ->
                viewModel.addCashIncome(
                    amount = amt,
                    reason = note.ifBlank { "সরাসরি ক্যাশ গ্রহণ" }
                )
                showAddCashDialog = false
                Toast.makeText(context, "$currency $amt টাকা ক্যাশে যুক্ত হয়েছে", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddExpenseDialog) {
        TongQuickCashDialog(
            title = "টাকা গেছে (খরচ / পরিশোধ)",
            confirmBtnText = "খরচ কাটুন",
            currency = currency,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { amt, note ->
                viewModel.addExpense(
                    title = note.ifBlank { "সাধারণ খরচ" },
                    category = "দৈনিক খরচ",
                    amount = amt,
                    note = "ক্যাশবাক্স থেকে প্রদান"
                )
                showAddExpenseDialog = false
                Toast.makeText(context, "$currency $amt টাকা ক্যাশ থেকে পরিশোধিত", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showOwnerCashDialog) {
        TongQuickCashDialog(
            title = if (isOwnerDeposit) "মালিক ক্যাশে টাকা দিলেন" else "মালিক ক্যাশ থেকে উত্তোলন করলেন",
            confirmBtnText = if (isOwnerDeposit) "মালিকের টাকা জমা করুন" else "মালিকের উত্তোলন লিপিবদ্ধ করুন",
            currency = currency,
            onDismiss = { showOwnerCashDialog = false },
            onConfirm = { amt, note ->
                val finalNote = if (isOwnerDeposit) "মালিক জমা: ${note.ifBlank { "ব্যক্তিগত মূলধন" }}"
                               else "মালিক উত্তোলন: ${note.ifBlank { "ব্যক্তিগত খরচ" }}"
                if (isOwnerDeposit) {
                    viewModel.addCashIncome(amt, finalNote)
                } else {
                    viewModel.addCashExpense(amt, finalNote, category = "মালিক উত্তোলন")
                }
                showOwnerCashDialog = false
                Toast.makeText(context, "মালিকের হিসাব সংরক্ষিত", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showSetOpeningCashDialog) {
        TongQuickCashDialog(
            title = "দিনের শুরুতে ক্যাশ ব্যালেন্স সেট করুন",
            confirmBtnText = "সংরক্ষণ করুন",
            currency = currency,
            defaultAmount = openingCash.toString(),
            onDismiss = { showSetOpeningCashDialog = false },
            onConfirm = { amt, note ->
                viewModel.updateShopInfo(
                    name = shopInfo.shopName,
                    owner = shopInfo.ownerName,
                    phone = shopInfo.phone,
                    address = shopInfo.address,
                    currency = shopInfo.currency,
                    mainBalance = amt
                )
                showSetOpeningCashDialog = false
                Toast.makeText(context, "শুরুর ক্যাশ $currency $amt নির্ধারিত হয়েছে", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showReportSummaryDialog) {
        TongReportSummaryDialog(
            currency = currency,
            dateStr = bnDateSubtitle,
            totalSales = totalSales,
            cashSales = cashSales,
            digitalSales = instantDigitalSales,
            dueSales = dueSales,
            totalReceived = totalReceived,
            totalPaid = totalPaid,
            currentBalance = currentCashBalance,
            onDismiss = { showReportSummaryDialog = false }
        )
    }

    if (showOwnerLedgerDialog) {
        TongOwnerLedgerDialog(
            currency = currency,
            ownerName = shopInfo.ownerName,
            totalDeposited = allOwnerIn,
            totalWithdrawn = allOwnerOut,
            netBalance = ownerBalance,
            onAddDeposit = {
                showOwnerLedgerDialog = false
                isOwnerDeposit = true
                showOwnerCashDialog = true
            },
            onAddWithdrawal = {
                showOwnerLedgerDialog = false
                isOwnerDeposit = false
                showOwnerCashDialog = true
            },
            onDismiss = { showOwnerLedgerDialog = false }
        )
    }
}

// --------------------------------------------------------
// SUB-TAB PILL BUTTON
// --------------------------------------------------------
@Composable
fun TongSubTabPill(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) TongRedPrimary else Color.White,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier.height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color(0xFF475569),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF334155),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --------------------------------------------------------
// SALES BREAKDOWN PILL CARD
// --------------------------------------------------------
@Composable
fun SalesBreakdownPillCard(
    title: String,
    amount: String,
    percent: String,
    progress: Float,
    icon: ImageVector,
    iconTint: Color,
    badgeBg: Color,
    progressColor: Color,
    isFocused: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isFocused) progressColor.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
        shadowElevation = if (isFocused) 2.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = badgeBg,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = amount,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
                Text(
                    text = percent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Rounded Progress Bar
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.15f)
            )
        }
    }
}

// --------------------------------------------------------
// CASH STATUS MINI CARD
// --------------------------------------------------------
@Composable
fun CashStatusMiniCard(
    title: String,
    amount: String,
    icon: ImageVector,
    iconTint: Color,
    bgTint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgTint,
        border = BorderStroke(1.dp, iconTint.copy(alpha = 0.2f)),
        modifier = modifier.height(82.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amount,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --------------------------------------------------------
// TRANSACTION RECORD ROW
// --------------------------------------------------------
@Composable
fun TongTransactionRow(
    title: String,
    subtitle: String,
    amount: String,
    isIncome: Boolean,
    extraTag: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isIncome) Color(0xFFECFDF5) else Color(0xFFFFF1F2),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (isIncome) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
            Text(
                text = amount,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isIncome) Color(0xFF059669) else Color(0xFFDC2626)
            )
        }
    }
}

// --------------------------------------------------------
// QUICK CASH ENTRY DIALOG
// --------------------------------------------------------
@Composable
fun TongQuickCashDialog(
    title: String,
    confirmBtnText: String,
    currency: String,
    defaultAmount: String = "",
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(defaultAmount) }
    var noteStr by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0F172A)
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("টাকার পরিমাণ ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = noteStr,
                    onValueChange = { noteStr = it },
                    label = { Text("বিবরণ / নোট (ঐচ্ছিক)") },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onConfirm(amt, noteStr)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TongRedPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(confirmBtnText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --------------------------------------------------------
// REPORT SUMMARY DIALOG
// --------------------------------------------------------
@Composable
fun TongReportSummaryDialog(
    currency: String,
    dateStr: String,
    totalSales: Double,
    cashSales: Double,
    digitalSales: Double,
    dueSales: Double,
    totalReceived: Double,
    totalPaid: Double,
    currentBalance: Double,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ক্যাশবাক্স বিস্তারিত বিবরণ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }
                Text(
                    text = "তারিখ: $dateStr",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                ReportRow("মোট বিক্রি", "$currency ${CalculationHelper.formatAmount(totalSales)}")
                ReportRow("নগদ বিক্রি", "$currency ${CalculationHelper.formatAmount(cashSales)}")
                ReportRow("ইনস্ট্যান্ট নগদ বিক্রি", "$currency ${CalculationHelper.formatAmount(digitalSales)}")
                ReportRow("বাকি বিক্রি", "$currency ${CalculationHelper.formatAmount(dueSales)}")

                HorizontalDivider(color = Color(0xFFF1F5F9))

                ReportRow("মোট টাকা এসেছে", "$currency ${CalculationHelper.formatAmount(totalReceived)}", Color(0xFF059669))
                ReportRow("মোট টাকা গেছে", "$currency ${CalculationHelper.formatAmount(totalPaid)}", Color(0xFFDC2626))
                ReportRow("বর্তমান ক্যাশ ব্যালেন্স", "$currency ${CalculationHelper.formatAmount(currentBalance)}", TongRedPrimary, isBold = true)

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TongRedPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("ঠিক আছে", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: String, valueColor: Color = Color(0xFF0F172A), isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF334155))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

// --------------------------------------------------------
// OWNER LEDGER DIALOG
// --------------------------------------------------------
@Composable
fun TongOwnerLedgerDialog(
    currency: String,
    ownerName: String,
    totalDeposited: Double,
    totalWithdrawn: Double,
    netBalance: Double,
    onAddDeposit: () -> Unit,
    onAddWithdrawal: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মালিকের হিসাব খাতা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                    }
                }

                Text(
                    text = "দোকানের মালিক: $ownerName",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                ReportRow("মালিক মোট জমা দিয়েছেন", "$currency ${CalculationHelper.formatAmount(totalDeposited)}", Color(0xFF059669))
                ReportRow("মালিক মোট উত্তোলন করেছেন", "$currency ${CalculationHelper.formatAmount(totalWithdrawn)}", Color(0xFFDC2626))
                val balLabel = if (netBalance >= 0) "দোকানে জমা রয়েছে" else "দোকান পাবে"
                ReportRow("মালিকের অবশিষ্ট ব্যালেন্স", "$currency ${CalculationHelper.formatAmount(Math.abs(netBalance))} ($balLabel)", Color(0xFF2563EB), isBold = true)

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onAddDeposit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ মালিক জমা", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onAddWithdrawal,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("- মালিক উত্তোলন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
