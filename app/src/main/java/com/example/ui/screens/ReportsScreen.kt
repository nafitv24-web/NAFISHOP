package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.data.model.TransactionRecord
import com.example.ui.components.EditOrReturnSaleDialog
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CalculationHelper
import java.text.SimpleDateFormat
import java.util.*

import com.example.util.PdfGenerator

@Composable
fun ReportsScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val transactions by viewModel.allTransactions.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency = shopInfo.currency

    var selectedPeriod by remember { mutableStateOf("ALL") } // ALL, TODAY, YESTERDAY, WEEK, MONTH, CUSTOM
    var customTimestamp by remember { mutableStateOf<Long?>(null) }
    var customLabel by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }
    var showExpenseDetailsList by remember { mutableStateOf(false) }
    var showSalesDetailsList by remember { mutableStateOf(false) }
    var transactionTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "SALE", "PURCHASE"
    var showAllTransactions by remember { mutableStateOf(false) }

    val dateDisplaySdf = remember(language) {
        SimpleDateFormat("d MMMM yyyy", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH)
    }

    val periodRange = remember(selectedPeriod, customTimestamp) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "ALL" -> 0L to Long.MAX_VALUE
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                start to (start + 86400000L - 1L)
            }
            "YESTERDAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val start = cal.timeInMillis
                start to (start + 86400000L - 1L)
            }
            "WEEK" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.add(Calendar.DAY_OF_YEAR, -6)
                cal.timeInMillis to Long.MAX_VALUE
            }
            "MONTH" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis to Long.MAX_VALUE
            }
            "CUSTOM" -> {
                val start = customTimestamp ?: System.currentTimeMillis()
                start to (start + 86400000L - 1L)
            }
            else -> 0L to Long.MAX_VALUE
        }
    }

    val periodTransactions = remember(transactions, periodRange, selectedPeriod) {
        if (selectedPeriod == "ALL") {
            transactions
        } else {
            transactions.filter { it.timestamp in periodRange.first..periodRange.second }
        }
    }

    val periodExpenses = remember(expenses, periodRange, selectedPeriod) {
        if (selectedPeriod == "ALL") {
            expenses
        } else {
            expenses.filter { it.timestamp in periodRange.first..periodRange.second }
        }
    }

    // Calculations
    val totalSales = remember(periodTransactions) {
        CalculationHelper.round2(periodTransactions.filter { it.type == "SALE" }.sumOf { it.totalAmount })
    }
    val totalSalesCost = remember(periodTransactions) {
        CalculationHelper.round2(periodTransactions.filter { it.type == "SALE" }.sumOf { tx ->
            if (tx.costPrice > 0) {
                tx.costPrice * tx.quantity
            } else if (tx.totalAmount > tx.profitAmount && tx.profitAmount != 0.0) {
                tx.totalAmount - tx.profitAmount
            } else {
                0.0
            }
        })
    }
    val grossProfit = remember(periodTransactions, totalSales, totalSalesCost) {
        val profitFromField = CalculationHelper.round2(periodTransactions.filter { it.type == "SALE" }.sumOf { it.profitAmount })
        if (profitFromField != 0.0) profitFromField else CalculationHelper.round2(totalSales - totalSalesCost)
    }
    val totalExpensesSum = remember(periodExpenses) {
        CalculationHelper.round2(periodExpenses.sumOf { it.amount })
    }
    val expensesByCategory = remember(periodExpenses) {
        periodExpenses
            .groupBy { it.category.ifBlank { "অন্যান্য" } }
            .map { (cat, list) ->
                val sum = CalculationHelper.round2(list.sumOf { it.amount })
                val count = list.size
                Triple(cat, sum, count)
            }
            .sortedByDescending { it.second }
    }
    val netProfit = remember(grossProfit, totalExpensesSum) {
        CalculationHelper.round2(grossProfit - totalExpensesSum)
    }
    val totalPurchases = remember(periodTransactions) {
        CalculationHelper.round2(periodTransactions.filter { it.type == "STOCK_IN" || it.type == "PURCHASE" }.sumOf { it.totalAmount })
    }

    val initialTab by viewModel.reportsScreenInitialTab.collectAsState()
    var selectedReportTab by remember { mutableStateOf(initialTab) } // 0: লাভ-ক্ষতি রিপোর্ট, 1: স্টক ইন-আউট হিসাব
    var stockInOutFilter by remember { mutableStateOf("ALL") } // "ALL", "STOCK_IN", "STOCK_OUT"

    LaunchedEffect(initialTab) {
        selectedReportTab = initialTab
    }

    // Stock In & Out Specific Calculations
    val stockInTransactions = remember(periodTransactions) {
        periodTransactions.filter { it.type == "STOCK_IN" || it.type == "PURCHASE" }
    }
    val stockOutTransactions = remember(periodTransactions) {
        periodTransactions.filter { it.type == "SALE" || it.type == "STOCK_OUT_DAMAGE" || it.type == "DAMAGE" }
    }

    val totalStockInAmount = remember(stockInTransactions) {
        stockInTransactions.sumOf { it.totalAmount }
    }
    val totalStockInQty = remember(stockInTransactions) {
        stockInTransactions.sumOf { it.quantity }
    }

    val totalStockOutSales = remember(stockOutTransactions) {
        stockOutTransactions.sumOf { it.totalAmount }
    }
    val totalStockOutCost = remember(stockOutTransactions) {
        stockOutTransactions.sumOf {
            val rate = if (it.costPrice > 0) it.costPrice else it.unitPrice
            rate * it.quantity
        }
    }
    val totalStockOutQty = remember(stockOutTransactions) {
        stockOutTransactions.sumOf { it.quantity }
    }

    val displayedStockTransactions = remember(periodTransactions, stockInOutFilter) {
        when (stockInOutFilter) {
            "STOCK_IN" -> stockInTransactions
            "STOCK_OUT" -> stockOutTransactions
            else -> periodTransactions.filter {
                it.type in listOf("STOCK_IN", "PURCHASE", "SALE", "STOCK_OUT_DAMAGE", "DAMAGE")
            }
        }.sortedByDescending { it.timestamp }
    }

    val periodTitle = when (selectedPeriod) {
        "ALL" -> if (language == "bn") "সব সময়ের মোট রিপোর্ট" else "All Time Report"
        "TODAY" -> if (language == "bn") "আজকের হিসাব রিপোর্ট (${dateDisplaySdf.format(Date(periodRange.first))})" else "Today's Report (${dateDisplaySdf.format(Date(periodRange.first))})"
        "YESTERDAY" -> if (language == "bn") "গতকালের হিসাব রিপোর্ট (${dateDisplaySdf.format(Date(periodRange.first))})" else "Yesterday's Report (${dateDisplaySdf.format(Date(periodRange.first))})"
        "WEEK" -> if (language == "bn") "গত ৭ দিনের রিপোর্ট" else "Last 7 Days Report"
        "MONTH" -> if (language == "bn") "চলতি মাসের রিপোর্ট" else "This Month's Report"
        "CUSTOM" -> if (customLabel.isNotBlank()) customLabel else dateDisplaySdf.format(Date(periodRange.first))
        else -> if (language == "bn") "সব সময়ের মোট রিপোর্ট" else "All Time Report"
    }

    val periodSalesTxs = remember(periodTransactions) {
        periodTransactions.filter { it.type == "SALE" }
    }
    val cashSalesAmount = remember(periodSalesTxs) {
        CalculationHelper.round2(periodSalesTxs.sumOf { it.paidAmount })
    }
    val dueSalesAmount = remember(periodSalesTxs) {
        CalculationHelper.round2(periodSalesTxs.sumOf { it.dueAmount })
    }

    val filteredPeriodTxs = remember(periodTransactions, transactionTypeFilter) {
        val list = when (transactionTypeFilter) {
            "SALE" -> periodTransactions.filter { it.type == "SALE" }
            "PURCHASE" -> periodTransactions.filter { it.type == "STOCK_IN" || it.type == "PURCHASE" }
            else -> periodTransactions
        }
        list.sortedByDescending { it.timestamp }
    }

    val itemSdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Primary Screen Switcher: [ লাভ-ক্ষতি ও পূর্ণাঙ্গ রিপোর্ট | পণ্য স্টক ইন-আউট হিসাব ]
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isProfitTab = selectedReportTab == 0
                    Surface(
                        onClick = { selectedReportTab = 0 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isProfitTab) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = if (isProfitTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "লাভ-ক্ষতি রিপোর্ট" else "Profit & Loss",
                                fontWeight = if (isProfitTab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isProfitTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isStockTab = selectedReportTab == 1
                    Surface(
                        onClick = { selectedReportTab = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isStockTab) EmeraldPrimary else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapVert,
                                contentDescription = null,
                                tint = if (isStockTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "পণ্য স্টক ইন-আউট" else "Stock In-Out",
                                fontWeight = if (isStockTab) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isStockTab) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Period Tabs
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = selectedPeriod == "ALL",
                        onClick = {
                            selectedPeriod = "ALL"
                            customTimestamp = null
                        },
                        label = { Text(if (language == "bn") "সব সময়" else "All Time", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == "TODAY",
                        onClick = {
                            selectedPeriod = "TODAY"
                            customTimestamp = null
                        },
                        label = { Text(if (language == "bn") "আজ" else "Today", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == "YESTERDAY",
                        onClick = {
                            selectedPeriod = "YESTERDAY"
                            customTimestamp = null
                        },
                        label = { Text(if (language == "bn") "গতকাল" else "Yesterday", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == "WEEK",
                        onClick = {
                            selectedPeriod = "WEEK"
                            customTimestamp = null
                        },
                        label = { Text(if (language == "bn") "গত ৭ দিন" else "7 Days", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == "MONTH",
                        onClick = {
                            selectedPeriod = "MONTH"
                            customTimestamp = null
                        },
                        label = { Text(if (language == "bn") "চলতি মাস" else "Month", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
                item {
                    Button(
                        onClick = {
                            val pickerCal = Calendar.getInstance()
                            if (customTimestamp != null) {
                                pickerCal.timeInMillis = customTimestamp!!
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
                                    customTimestamp = selectedCal.timeInMillis
                                    customLabel = dateDisplaySdf.format(selectedCal.time)
                                    selectedPeriod = "CUSTOM"
                                },
                                pickerCal.get(Calendar.YEAR),
                                pickerCal.get(Calendar.MONTH),
                                pickerCal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        colors = if (selectedPeriod == "CUSTOM") {
                            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (selectedPeriod == "CUSTOM" && customLabel.isNotBlank()) customLabel else (if (language == "bn") "তারিখ বাছুন 🗓️" else "Pick Date 🗓️"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    FilterChip(
                        selected = selectedPeriod == "ALL",
                        onClick = {
                            selectedPeriod = "ALL"
                            customTimestamp = null
                        },
                        label = { Text(if (language == "bn") "সব সময়" else "All", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        if (selectedReportTab == 0) {
            // Highlight Net Profit Card
            item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                if (netProfit >= 0) listOf(Color(0xFF065F46), Color(0xFF059669))
                                else listOf(Color(0xFF991B1B), Color(0xFFDC2626))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = periodTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = {
                                    val shareTxt = buildString {
                                        appendLine("📊 ${shopInfo.shopName} - $periodTitle")
                                        appendLine("-----------------------------")
                                        appendLine("মোট বিক্রি: $currency$totalSales")
                                        appendLine("বিক্রিত পণ্যের কেনা দাম: $currency$totalSalesCost")
                                        appendLine("গ্রস লাভ: $currency$grossProfit")
                                        appendLine("দোকানের মোট খরচ: $currency$totalExpensesSum")
                                        if (expensesByCategory.isNotEmpty()) {
                                            appendLine("--- দোকান খরচ খাতওয়ারী হিসাব ---")
                                            expensesByCategory.forEach { (cat, amt, count) ->
                                                val pct = if (totalExpensesSum > 0) ((amt / totalExpensesSum) * 100).toInt() else 0
                                                appendLine(" • $cat: $currency${amt.toIntOrNull() ?: amt} ($pct% - $count টি)")
                                            }
                                        }
                                        appendLine("-----------------------------")
                                        appendLine("✨ নিট লাভ (Net Profit): $currency$netProfit")
                                        appendLine("-----------------------------")
                                        appendLine(com.example.util.CustomerSmsHelper.SPONSOR_FOOTER)
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareTxt)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Report"))
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(Color(0x33FFFFFF), CircleShape)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (language == "bn") "নিট লাভ / মুনাফা (Net Profit)" else "Net Profit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD1FAE5)
                        )
                        Text(
                            text = "$currency${netProfit.toIntOrNull() ?: netProfit}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
            }
        }

        // Detailed Financial Breakdown
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "bn") "লাভ ও ক্ষতির পূর্ণাঙ্গ বিবরণ" else "Profit & Loss Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ReportLineRow(
                        label = if (language == "bn") "১. মোট পণ্য বিক্রি (Gross Sales)" else "Gross Sales",
                        value = "$currency${totalSales.toIntOrNull() ?: totalSales}",
                        valueColor = EmeraldPrimary
                    )
                    ReportLineRow(
                        label = if (language == "bn") "২. বিক্রিত পণ্যের ক্রয়মূল্য (COGS)" else "Cost of Goods Sold",
                        value = "- $currency${totalSalesCost.toIntOrNull() ?: totalSalesCost}",
                        valueColor = Color(0xFF64748B)
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    ReportLineRow(
                        label = if (language == "bn") "৩. মোট বিক্রয় লাভ (Gross Profit)" else "Gross Profit",
                        value = "$currency${grossProfit.toIntOrNull() ?: grossProfit}",
                        valueColor = ProfitGreen,
                        isBold = true
                    )
                    ReportLineRow(
                        label = if (language == "bn") "৪. দোকানের মোট খরচ (Expenses)" else "Store Expenses",
                        value = "- $currency${totalExpensesSum.toIntOrNull() ?: totalExpensesSum}",
                        valueColor = LossRed
                    )
                    if (expensesByCategory.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = if (language == "bn") "দোকান খরচের খাতওয়ারী সংক্ষেপ:" else "Expense Breakdown by Sector:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                expensesByCategory.take(4).forEach { (cat, amt, count) ->
                                    val pct = if (totalExpensesSum > 0) ((amt / totalExpensesSum) * 100).toInt() else 0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "• $cat ($pct% • $count টি)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "-$currency${amt.toIntOrNull() ?: amt}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = LossRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    ReportLineRow(
                        label = if (language == "bn") "৫. নিট লাভ (Net Profit)" else "Net Profit",
                        value = "$currency${netProfit.toIntOrNull() ?: netProfit}",
                        valueColor = if (netProfit >= 0) ProfitGreen else LossRed,
                        isBold = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val allDue = customers.sumOf { it.totalDue }
                                val pdf = PdfGenerator.generateReportPdf(
                                    context = context,
                                    shopName = shopInfo.shopName,
                                    periodTitle = periodTitle,
                                    totalSales = totalSales,
                                    salesCost = totalSalesCost,
                                    grossProfit = grossProfit,
                                    expenses = totalExpensesSum,
                                    netProfit = netProfit,
                                    purchases = totalPurchases,
                                    dueAmount = allDue,
                                    transactions = periodTransactions,
                                    expensesList = periodExpenses,
                                    currency = currency
                                )
                                if (pdf != null) {
                                    PdfGenerator.openOrSharePdf(
                                        context,
                                        pdf,
                                        if (language == "bn") "হিসাব রিপোর্ট পিডিএফ" else "Business Report PDF"
                                    )
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "পিডিএফ রিপোর্ট ডাউনলোড" else "Download PDF Report", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val allDue = customers.sumOf { it.totalDue }
                                val textSummary = buildString {
                                    appendLine("📊 ${shopInfo.shopName} - $periodTitle")
                                    appendLine("-----------------------------")
                                    appendLine("মোট বিক্রি: $currency${totalSales.toIntOrNull() ?: totalSales}")
                                    appendLine("ক্রয়মূল্য খরচ: $currency${totalSalesCost.toIntOrNull() ?: totalSalesCost}")
                                    appendLine("বিক্রয় লাভ: $currency${grossProfit.toIntOrNull() ?: grossProfit}")
                                    appendLine("দোকানের মোট খরচ: $currency${totalExpensesSum.toIntOrNull() ?: totalExpensesSum}")
                                    if (expensesByCategory.isNotEmpty()) {
                                        appendLine("--- দোকান খরচ খাতওয়ারী হিসাব ---")
                                        expensesByCategory.forEach { (cat, amt, count) ->
                                            val pct = if (totalExpensesSum > 0) ((amt / totalExpensesSum) * 100).toInt() else 0
                                            appendLine(" • $cat: $currency${amt.toIntOrNull() ?: amt} ($pct%)")
                                        }
                                    }
                                    appendLine("-----------------------------")
                                    appendLine("নিট লাভ (Net Profit): $currency${netProfit.toIntOrNull() ?: netProfit}")
                                    appendLine("মোট বাকি পাওনা: $currency${allDue.toIntOrNull() ?: allDue}")
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, textSummary)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Report"))
                            },
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == "bn") "শেয়ার" else "Share")
                        }
                    }
                }
            }
        }

        // Dedicated Sales Breakdown & Invoice History Section (পণ্য বিক্রয় ও মেমোর বিবরণী)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    .size(36.dp)
                                    .background(ProfitGreen.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "পণ্য বিক্রয় ও লেনদেন বিবরণী" else "Sales & Transactions Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "bn") "মোট বিক্রয় মেমো: ${periodSalesTxs.size} টি" else "Total Invoices: ${periodSalesTxs.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ProfitGreen.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$currency$totalSales",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 3-stat summary row: Gross Sales, COGS, Profit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stat 1: মোট বিক্রি
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (language == "bn") "মোট বিক্রি" else "Gross Sales",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currency$totalSales",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Stat 2: ক্রয়মূল্য (COGS)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (language == "bn") "ক্রয়মূল্য (COGS)" else "COGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currency$totalSalesCost",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Stat 3: বিক্রি লাভ
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = ProfitGreen.copy(alpha = 0.08f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (language == "bn") "বিক্রি লাভ" else "Gross Profit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ProfitGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$currency$grossProfit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cash received vs Due row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "নগদ আদায়: $currency$cashSalesAmount | বাকিতে বিক্রি: $currency$dueSalesAmount" else "Cash: $currency$cashSalesAmount | Due: $currency$dueSalesAmount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showSalesDetailsList = !showSalesDetailsList },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (showSalesDetailsList) ProfitGreen.copy(alpha = 0.08f) else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = if (showSalesDetailsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showSalesDetailsList) {
                                if (language == "bn") "বিক্রি তালিকা সংক্ষেপ করুন" else "Hide Sales Details"
                            } else {
                                if (language == "bn") "বিক্রির প্রতিটি এন্ট্রি দেখুন (${periodSalesTxs.size} টি)" else "View All Sales Entries (${periodSalesTxs.size})"
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = ProfitGreen
                        )
                    }

                    AnimatedVisibility(
                        visible = showSalesDetailsList,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (periodSalesTxs.isEmpty()) {
                                Text(
                                    text = if (language == "bn") "এই সময়কালের কোনো বিক্রয় এন্ট্রি পাওয়া যায়নি।" else "No sales entries found in this period.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                periodSalesTxs.forEach { saleTx ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
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
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = saleTx.productName.ifBlank { if (language == "bn") "পণ্য বিক্রি" else "Sale" },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (saleTx.invoiceNumber.isNotBlank()) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                                        ) {
                                                            Text(
                                                                text = "#${saleTx.invoiceNumber}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                val detailsLine = buildString {
                                                    if (saleTx.quantity > 0) {
                                                        append("${saleTx.quantity} ${saleTx.unit.ifBlank { "pcs" }} @ $currency${saleTx.unitPrice}")
                                                    }
                                                    if (saleTx.customerName.isNotBlank()) {
                                                        append(" • ${saleTx.customerName}")
                                                    }
                                                }
                                                if (detailsLine.isNotBlank()) {
                                                    Text(
                                                        text = detailsLine,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = itemSdf.format(Date(saleTx.timestamp)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "$currency${saleTx.totalAmount}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ProfitGreen
                                                )
                                                if (saleTx.profitAmount != 0.0) {
                                                    Text(
                                                        text = "${if (language == "bn") "লাভ" else "Profit"}: $currency${saleTx.profitAmount}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ProfitGreen
                                                    )
                                                }
                                                if (saleTx.dueAmount > 0) {
                                                    Text(
                                                        text = "${if (language == "bn") "বাকি" else "Due"}: $currency${saleTx.dueAmount}",
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
                }
            }
        }

        // Dedicated Store Expenses by Sector Section (দোকান খরচ কোন খাতে কত টাকা খরচ হলো)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                    .size(38.dp)
                                    .background(Color(0xFFFEF2F2), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = LossRed,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "দোকান খরচ খাতওয়ারী বিবরণ" else "Store Expenses by Sector",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (language == "bn") "কোন খাতে কত টাকা খরচ হয়েছে (${periodExpenses.size} টি এন্ট্রি)"
                                    else "Breakdown by sector (${periodExpenses.size} entries)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                        ) {
                            Text(
                                text = "-$currency${totalExpensesSum.toIntOrNull() ?: totalExpensesSum}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = LossRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (periodExpenses.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "bn") "নির্বাচিত সময়ে কোনো দোকান খরচ রেকর্ড করা হয়নি" else "No store expenses recorded for this period",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Category Breakdown Cards
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            expensesByCategory.forEach { (cat, catAmt, count) ->
                                val pct = if (totalExpensesSum > 0) (catAmt / totalExpensesSum) else 0.0
                                val pctFormatted = String.format(Locale.US, "%.1f", pct * 100)

                                val (catIcon, catColor) = when {
                                    cat.contains("ভাড়া") -> Icons.Default.Storefront to Color(0xFF2563EB)
                                    cat.contains("বিদ্যুৎ") || cat.contains("বিল") -> Icons.Default.ElectricBolt to Color(0xFFD97706)
                                    cat.contains("বেতন") || cat.contains("কর্মচারী") -> Icons.Default.Badge to Color(0xFF0D9488)
                                    cat.contains("নাস্তা") || cat.contains("চা") || cat.contains("আপ্যায়ন") -> Icons.Default.LocalCafe to Color(0xFFEA580C)
                                    cat.contains("পরিবহন") || cat.contains("যাতায়াত") || cat.contains("গাড়ি") -> Icons.Default.LocalShipping to Color(0xFF6366F1)
                                    cat.contains("প্যাকিং") || cat.contains("বক্স") || cat.contains("প্যাকেট") -> Icons.Default.Inventory2 to Color(0xFF9333EA)
                                    else -> Icons.Default.Category to Color(0xFF64748B)
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .background(catColor.copy(alpha = 0.15f), CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        catIcon,
                                                        contentDescription = null,
                                                        tint = catColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = cat,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "$count টি খরচ এন্ট্রি • মোট খরচের $pctFormatted%",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }

                                            Text(
                                                text = "-$currency${catAmt.toIntOrNull() ?: catAmt}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = LossRed
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        LinearProgressIndicator(
                                            progress = { pct.toFloat().coerceIn(0f, 1f) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = catColor,
                                            trackColor = catColor.copy(alpha = 0.15f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Toggle button for individual expense entries
                        OutlinedButton(
                            onClick = { showExpenseDetailsList = !showExpenseDetailsList },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                if (showExpenseDetailsList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showExpenseDetailsList)
                                    (if (language == "bn") "খরচের বিস্তারিত তালিকা লুকান" else "Hide Detailed Entries")
                                else
                                    (if (language == "bn") "খরচের প্রতিটি এন্ট্রি দেখুন (${periodExpenses.size} টি)" else "View All Expense Entries (${periodExpenses.size})"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (showExpenseDetailsList) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val expSdf = SimpleDateFormat("dd MMM, hh:mm a", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                periodExpenses.sortedByDescending { it.timestamp }.forEach { exp ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = exp.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Text(
                                                        text = exp.category.ifBlank { "অন্যান্য" },
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            if (exp.note.isNotBlank()) {
                                                Text(
                                                    text = exp.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    fontSize = 11.sp
                                                )
                                            }
                                            Text(
                                                text = expSdf.format(Date(exp.timestamp)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 10.sp
                                            )
                                        }
                                        Text(
                                            text = "-$currency${exp.amount.toIntOrNull() ?: exp.amount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LossRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Additional Stats (Stock In Purchases & Current Due)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (language == "bn") "নতুন স্টক ক্রয়" else "Purchases",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "$currency${totalPurchases.toIntOrNull() ?: totalPurchases}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StockBlue
                        )
                    }
                }

                val allDue = customers.sumOf { it.totalDue }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (language == "bn") "বর্তমান বাকি পাওনা" else "Outstanding Due",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "$currency${allDue.toIntOrNull() ?: allDue}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DueOrange
                        )
                    }
                }
            }
        }

        // Transaction History for Period
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (language == "bn") "এই সময়ের লেনদেন সমূহ" else "Transactions in Period"} (${periodTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (periodTransactions.size > 15) {
                        TextButton(onClick = { showAllTransactions = !showAllTransactions }) {
                            Text(
                                text = if (showAllTransactions) {
                                    if (language == "bn") "সংক্ষেপ করুন (১৫টি)" else "Show Less"
                                } else {
                                    if (language == "bn") "সবগুলো দেখুন (${periodTransactions.size}টি)" else "View All (${periodTransactions.size})"
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Filter tabs for transactions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = transactionTypeFilter == "ALL",
                        onClick = { transactionTypeFilter = "ALL" },
                        label = { Text(if (language == "bn") "সব (${periodTransactions.size})" else "All (${periodTransactions.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = transactionTypeFilter == "SALE",
                        onClick = { transactionTypeFilter = "SALE" },
                        label = { Text(if (language == "bn") "বিক্রি (${periodTransactions.count { it.type == "SALE" }})" else "Sales", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = transactionTypeFilter == "PURCHASE",
                        onClick = { transactionTypeFilter = "PURCHASE" },
                        label = { Text(if (language == "bn") "স্টক ইন (${periodTransactions.count { it.type == "STOCK_IN" || it.type == "PURCHASE" }})" else "Stock In", fontSize = 11.sp) }
                    )
                }
            }
        }

        if (filteredPeriodTxs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(if (language == "bn") "কোনো লেনদেন নেই" else "No transactions in this period", color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        } else {
            val displayList = if (showAllTransactions) filteredPeriodTxs else filteredPeriodTxs.take(15)
            items(displayList) { tx ->
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
        } else {
            // STOCK IN & STOCK OUT DETAILED REPORT
            // 1. Stock In-Out Summary Cards
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (language == "bn") "স্টক ইন ও আউট বিবরণী" else "Stock In & Out Statement",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = periodTitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2 Primary Stat Cards: Stock In & Stock Out
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Total Stock In Box
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = Color(0xFF16A34A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (language == "bn") "মোট স্টক ইন" else "Total Stock In",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$currency${totalStockInAmount.toIntOrNull() ?: totalStockInAmount}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF14532D)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${totalStockInQty.toIntOrNull() ?: totalStockInQty} টি পণ্য • ${stockInTransactions.size} চালান",
                                        fontSize = 10.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }

                            // Total Stock Out Box
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
                                border = BorderStroke(1.dp, Color(0xFFFECDD3))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = Color(0xFFE11D48),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (language == "bn") "মোট স্টক আউট" else "Total Stock Out",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFBE123C)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "$currency${totalStockOutSales.toIntOrNull() ?: totalStockOutSales}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF881337)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "কেনা খরচ: $currency${totalStockOutCost.toIntOrNull() ?: totalStockOutCost} • ${totalStockOutQty.toIntOrNull() ?: totalStockOutQty} টি",
                                        fontSize = 10.sp,
                                        color = Color(0xFF9F1239)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Net Movement Strip
                        val netFlow = totalStockInAmount - totalStockOutCost
                        val isNetPositive = netFlow >= 0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isNetPositive) Color(0xFFEFF6FF) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (isNetPositive) Color(0xFFBFDBFE) else Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isNetPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isNetPositive) Color(0xFF2563EB) else Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "bn") "স্টক মূলধনের নিট পরিবর্তন:" else "Net Stock Capital Flow:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isNetPositive) Color(0xFF1E40AF) else Color(0xFF92400E)
                                    )
                                }
                                Text(
                                    text = "${if (isNetPositive) "+" else ""}$currency${netFlow.toIntOrNull() ?: netFlow}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNetPositive) Color(0xFF1E40AF) else Color(0xFF92400E)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Download PDF & Share Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val pdf = PdfGenerator.generateStockInOutPdf(
                                        context = context,
                                        shopName = shopInfo.shopName,
                                        periodTitle = periodTitle,
                                        totalStockInAmount = totalStockInAmount,
                                        totalStockInQty = totalStockInQty,
                                        totalStockOutSales = totalStockOutSales,
                                        totalStockOutCost = totalStockOutCost,
                                        totalStockOutQty = totalStockOutQty,
                                        transactions = displayedStockTransactions,
                                        currency = currency
                                    )
                                    if (pdf != null) {
                                        PdfGenerator.openOrSharePdf(
                                            context,
                                            pdf,
                                            if (language == "bn") "স্টক ইন-আউট রিপোর্ট পিডিএফ" else "Stock In-Out Report PDF"
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == "bn") "স্টক ইন-আউট পিডিএফ" else "Download Stock PDF",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val shareText = buildString {
                                        append("📦 ${shopInfo.shopName} - স্টক ইন-আউট হিসাব\n")
                                        append("📅 সময়কাল: $periodTitle\n")
                                        append("━━━━━━━━━━━━━━━━━━━\n")
                                        append("📥 মোট স্টক ইন: $currency${totalStockInAmount.toIntOrNull() ?: totalStockInAmount} (${totalStockInQty.toIntOrNull() ?: totalStockInQty} টি পণ্য)\n")
                                        append("📤 মোট স্টক আউট (বিক্রি): $currency${totalStockOutSales.toIntOrNull() ?: totalStockOutSales} (${totalStockOutQty.toIntOrNull() ?: totalStockOutQty} টি পণ্য)\n")
                                        append("💰 স্টক আউট ক্রয়মূল্য: $currency${totalStockOutCost.toIntOrNull() ?: totalStockOutCost}\n")
                                        append("📊 নিট স্টক মূলধন পরিবর্তন: ${if (isNetPositive) "+" else ""}$currency${netFlow.toIntOrNull() ?: netFlow}\n")
                                        append("━━━━━━━━━━━━━━━━━━━\n")
                                        append("মোট লেনদেন: ${displayedStockTransactions.size} টি এন্ট্রি\n")
                                        append("হিসাব রাখা হয়েছে NAFI KHATA অ্যাপে।")
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, if (language == "bn") "স্টক রিপোর্ট শেয়ার করুন" else "Share Stock Report"))
                                },
                                modifier = Modifier.weight(0.7f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (language == "bn") "শেয়ার" else "Share", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 2. Section Title and Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "পণ্যভিত্তিক বিবরণ (${displayedStockTransactions.size})" else "Movement Records (${displayedStockTransactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = stockInOutFilter == "ALL",
                            onClick = { stockInOutFilter = "ALL" },
                            label = { Text(if (language == "bn") "সব" else "All", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = stockInOutFilter == "STOCK_IN",
                            onClick = { stockInOutFilter = "STOCK_IN" },
                            label = { Text("📥 ইন (${stockInTransactions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        FilterChip(
                            selected = stockInOutFilter == "STOCK_OUT",
                            onClick = { stockInOutFilter = "STOCK_OUT" },
                            label = { Text("📤 আউট (${stockOutTransactions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // 3. Transactions List
            if (displayedStockTransactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == "bn") "এই সময়ে কোনো পণ্য স্টক ইন বা আউট করা হয়নি" else "No stock in or out transactions in this period",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(displayedStockTransactions) { tx ->
                    StockMovementCard(
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
    }

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
}

@Composable
private fun ReportLineRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Color(0xFF0F172A) else Color(0xFF475569)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun StockMovementCard(
    tx: TransactionRecord,
    currency: String,
    language: String,
    onClick: () -> Unit
) {
    val isStockIn = tx.type == "STOCK_IN" || tx.type == "PURCHASE"
    val isDamage = tx.type == "STOCK_OUT_DAMAGE" || tx.type == "DAMAGE"
    val timeFormatted = SimpleDateFormat("d MMMM yyyy, hh:mm a", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH).format(Date(tx.timestamp))

    val badgeBg = when {
        isStockIn -> Color(0xFFECFDF5)
        isDamage -> Color(0xFFFEF2F2)
        else -> Color(0xFFEFF6FF)
    }
    val badgeTextColor = when {
        isStockIn -> Color(0xFF059669)
        isDamage -> Color(0xFFDC2626)
        else -> Color(0xFF2563EB)
    }
    val badgeIcon = when {
        isStockIn -> Icons.Default.ArrowDownward
        isDamage -> Icons.Default.Warning
        else -> Icons.Default.ArrowUpward
    }
    val badgeText = when {
        isStockIn -> if (language == "bn") "স্টক ইন (ক্রয়)" else "STOCK IN"
        isDamage -> if (language == "bn") "স্টক আউট (ড্যামেজ)" else "STOCK OUT (DMG)"
        else -> if (language == "bn") "স্টক আউট (বিক্রি)" else "STOCK OUT (SALE)"
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // In / Out Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    border = BorderStroke(0.5.dp, badgeTextColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(badgeIcon, contentDescription = null, tint = badgeTextColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeTextColor)
                    }
                }

                // Date & Time
                Text(
                    text = timeFormatted,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Name & Memo/Customer
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isStockIn) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (isStockIn) Color(0xFF16A34A) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = tx.productName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val extraInfo = listOfNotNull(
                            tx.invoiceNumber.takeIf { it.isNotBlank() }?.let { "মেমো: $it" },
                            tx.customerName.takeIf { it.isNotBlank() }?.let { "গ্রাহক: $it" }
                        ).joinToString(" • ")
                        if (extraInfo.isNotBlank()) {
                            Text(
                                text = extraInfo,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Total Price
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isStockIn) Color(0xFF15803D) else Color(0xFF0F172A)
                    )
                    Text(
                        text = if (isStockIn) "মোট ক্রয়মূল্য" else "মোট বিক্রয়মূল্য",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity & Unit
                Text(
                    text = "পরিমাণ: ${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Rate / Unit Price
                val unitPrice = if (isStockIn) {
                    tx.costPrice.takeIf { it > 0 } ?: tx.unitPrice
                } else {
                    tx.unitPrice
                }
                Text(
                    text = "দর: $currency${unitPrice.toIntOrNull() ?: unitPrice}/${tx.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Profit or Note
                if (!isStockIn && tx.profitAmount != 0.0) {
                    Text(
                        text = "লাভ: $currency${tx.profitAmount.toIntOrNull() ?: tx.profitAmount}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                } else if (tx.note.isNotBlank()) {
                    Text(
                        text = "নোট: ${tx.note}",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
