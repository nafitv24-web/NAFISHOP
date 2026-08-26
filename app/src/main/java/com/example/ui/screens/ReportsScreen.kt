package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.TransactionRecord
import com.example.ui.components.EditOrReturnSaleDialog
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
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

    var selectedPeriod by remember { mutableStateOf("TODAY") } // TODAY, WEEK, MONTH, ALL
    var editingTransaction by remember { mutableStateOf<TransactionRecord?>(null) }

    val periodRange = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        val endTime = System.currentTimeMillis()
        val startTime = when (selectedPeriod) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "WEEK" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            "MONTH" -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.timeInMillis
            }
            else -> 0L
        }
        startTime to endTime
    }

    val periodTransactions = remember(transactions, periodRange) {
        transactions.filter { it.timestamp >= periodRange.first && it.timestamp <= periodRange.second }
    }

    val periodExpenses = remember(expenses, periodRange) {
        expenses.filter { it.timestamp >= periodRange.first && it.timestamp <= periodRange.second }
    }

    // Calculations
    val totalSales = remember(periodTransactions) {
        periodTransactions.filter { it.type == "SALE" }.sumOf { it.totalAmount }
    }
    val totalSalesCost = remember(periodTransactions) {
        periodTransactions.filter { it.type == "SALE" }.sumOf { it.costPrice * it.quantity }
    }
    val grossProfit = remember(periodTransactions) {
        periodTransactions.filter { it.type == "SALE" }.sumOf { it.profitAmount }
    }
    val totalExpensesSum = remember(periodExpenses) {
        periodExpenses.sumOf { it.amount }
    }
    val netProfit = remember(grossProfit, totalExpensesSum) {
        grossProfit - totalExpensesSum
    }
    val totalPurchases = remember(periodTransactions) {
        periodTransactions.filter { it.type == "STOCK_IN" || it.type == "PURCHASE" }.sumOf { it.totalAmount }
    }

    val periodTitle = when (selectedPeriod) {
        "TODAY" -> if (language == "bn") "আজকের হিসাব রিপোর্ট" else "Today's Report"
        "WEEK" -> if (language == "bn") "গত ৭ দিনের রিপোর্ট" else "Last 7 Days Report"
        "MONTH" -> if (language == "bn") "গত ৩০ দিনের রিপোর্ট" else "Last 30 Days Report"
        else -> if (language == "bn") "সর্বকালের মোট রিপোর্ট" else "All Time Report"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "TODAY" to if (language == "bn") "আজ" else "Today",
                    "WEEK" to if (language == "bn") "সপ্তাহ" else "Week",
                    "MONTH" to if (language == "bn") "মাস" else "Month",
                    "ALL" to if (language == "bn") "সব সময়" else "All"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedPeriod == key,
                        onClick = { selectedPeriod = key },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

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
                                    appendLine("দোকানের খরচ: $currency${totalExpensesSum.toIntOrNull() ?: totalExpensesSum}")
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
            Text(
                text = "${if (language == "bn") "এই সময়ের লেনদেন সমূহ" else "Transactions in Period"} (${periodTransactions.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (periodTransactions.isEmpty()) {
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
            items(periodTransactions.take(15)) { tx ->
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
