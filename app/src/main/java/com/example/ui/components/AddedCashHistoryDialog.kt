package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CashAdditionItem
import com.example.data.model.CashLog
import com.example.data.model.parseCashAdditionItem
import com.example.ui.theme.ProfitGreen
import com.example.util.CalculationHelper
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddedCashHistoryDialog(
    cashLogs: List<CashLog>,
    shopName: String,
    shopPhone: String = "",
    currency: String = "৳",
    language: String = "bn",
    onDismiss: () -> Unit,
    onAddNewCash: () -> Unit,
    onDeleteLog: (CashLog) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("সব") }
    var itemToDelete by remember { mutableStateOf<CashAdditionItem?>(null) }

    // 1. Filter cashLogs to only include additions (DEPOSIT, positive MANUAL_ADJUST, or direct non-sale INCOME)
    val additionItems = remember(cashLogs) {
        cashLogs.filter { log ->
            val noteLower = log.note.trim()
            val isAutoExpense = noteLower.startsWith("খরচ:") || noteLower.startsWith("খরচ বাতিল")
            val isAutoDue = noteLower.startsWith("বাকি আদায়") || noteLower.startsWith("বাকি লগ")
            val isAutoPurchase = noteLower.startsWith("পণ্য ক্রয়") || noteLower.startsWith("স্টক ইন")
            val isAutoDayEnd = noteLower.startsWith("দিনশেষের বিক্রি")
            val isAutoSale = noteLower.startsWith("বিক্রি বাতিল") || noteLower.startsWith("ট্রানজেকশন")

            if (isAutoExpense || isAutoDue || isAutoPurchase || isAutoDayEnd || isAutoSale) {
                false
            } else {
                when (log.type) {
                    "DEPOSIT" -> true
                    "INCOME" -> !log.note.startsWith("বিক্রি")
                    "MANUAL_ADJUST" -> log.amount > 0 && !log.note.contains("ঘাটতি") && !log.note.contains("কমানো")
                    else -> log.amount > 0
                }
            }
        }.map { log ->
            parseCashAdditionItem(log)
        }.sortedByDescending { it.timestamp }
    }

    // 2. Apply Time Filters
    val filteredItems = remember(additionItems, selectedFilter) {
        val calNow = Calendar.getInstance()
        when (selectedFilter) {
            "আজ" -> {
                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                additionItems.filter { it.timestamp >= startOfDay }
            }
            "এই সপ্তাহ" -> {
                val startOfWeek = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                additionItems.filter { it.timestamp >= startOfWeek }
            }
            "এই মাস" -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                additionItems.filter { it.timestamp >= startOfMonth }
            }
            "এই বছর" -> {
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                additionItems.filter { it.timestamp >= startOfYear }
            }
            else -> additionItems
        }
    }

    // Calculations
    val totalAdded = filteredItems.sumOf { it.amount }
    val ownAdded = filteredItems.filter { it.sourceCategory == "OWN" }.sumOf { it.amount }
    val personAdded = filteredItems.filter { it.sourceCategory == "PERSON" }.sumOf { it.amount }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 10.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = Color(0xFF065F46),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFF047857), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "মূল ক্যাশে টাকা এড খতিয়ান" else "Added Cash to Drawer",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == "bn") "নিজের ক্যাশ ও ধার জমার বিস্তারিত তালিকা" else "Own funds & loans ledger",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA7F3D0),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF047857), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Period Filter Chips
                val filters = listOf("সব", "আজ", "এই সপ্তাহ", "এই মাস", "এই বছর")
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF059669) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Summary Metric Cards Ribbon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Total Added Cash
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "bn") "মোট এড করা ক্যাশ" else "Total Added",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color(0xFF047857)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+${CalculationHelper.formatCurrency(totalAdded, currency)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }

                    // From Own Cash
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE0F2FE),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "bn") "👤 নিজের ক্যাশ" else "👤 Own Funds",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color(0xFF0369A1)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+${CalculationHelper.formatCurrency(ownAdded, currency)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    // From Person / Loan
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (language == "bn") "🤝 ধার / আনা" else "🤝 Loans/Others",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+${CalculationHelper.formatCurrency(personAdded, currency)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }

                // List of Entries
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    if (filteredItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == "bn") "এই সময়কালে কোনো টাকা এড করার রেকর্ড নেই" else "No cash addition records found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(filteredItems, key = { it.id }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shadowElevation = 0.5.dp
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Source Badge
                                            when (item.sourceCategory) {
                                                "OWN" -> {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFDCFCE7),
                                                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = if (language == "bn") "নিজের ক্যাশ থেকে জমা" else "From Own Cash",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF166534),
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }
                                                "PERSON" -> {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFFEF3C7),
                                                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.Handshake, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = if (language == "bn") "ধার / আনা: ${item.personName}" else "From: ${item.personName}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF92400E),
                                                                fontSize = 11.sp
                                                            )
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFF1F5F9),
                                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                                    ) {
                                                        Text(
                                                            text = if (language == "bn") "অন্যান্য উৎস" else "Other Source",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF475569),
                                                            fontSize = 11.sp,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Amount Added
                                            Text(
                                                text = "+${CalculationHelper.formatCurrency(item.amount, currency)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = ProfitGreen
                                            )
                                        }

                                        if (item.note.isNotBlank() && item.note != "ক্যাশ জমা") {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = item.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))
                                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.8.dp)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Bottom info (Date & Balance)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                val dateFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                                                Text(
                                                    text = dateFormatted,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B),
                                                    fontSize = 10.5.sp
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (language == "bn") "ব্যালেন্স: ${CalculationHelper.formatCurrency(item.balanceAfter, currency)}" else "Bal: ${CalculationHelper.formatCurrency(item.balanceAfter, currency)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF334155),
                                                    fontSize = 11.sp
                                                )
                                                if (item.originalCashLog != null) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    IconButton(
                                                        onClick = { itemToDelete = item },
                                                        modifier = Modifier.size(22.dp)
                                                    ) {
                                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(15.dp))
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

                // Bottom Action Buttons (PDF Download & Add Cash)
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // PDF Download Button
                        OutlinedButton(
                            onClick = {
                                if (filteredItems.isEmpty()) {
                                    Toast.makeText(context, if (language == "bn") "কোনো রেকর্ড পাওয়া যায়নি" else "No records to export", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                val pdfFile = PdfGenerator.generateAddedCashHistoryPdf(
                                    context = context,
                                    shopName = shopName,
                                    shopPhone = shopPhone,
                                    periodTitle = selectedFilter,
                                    entries = filteredItems,
                                    currency = currency
                                )
                                if (pdfFile != null) {
                                    Toast.makeText(context, if (language == "bn") "পিডিএফ তৈরি হয়েছে" else "PDF Generated", Toast.LENGTH_SHORT).show()
                                    PdfGenerator.openOrSharePdf(context, pdfFile, "দোকানের মূল ক্যাশে টাকা যুক্ত করার খতিয়ান")
                                } else {
                                    Toast.makeText(context, if (language == "bn") "পিডিএফ তৈরি ব্যর্থ হয়েছে" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "মেমো পিডিএফ 📄" else "Download PDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Add Cash Button
                        Button(
                            onClick = {
                                onAddNewCash()
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "+ মূল ক্যাশ এড করুন" else "+ Add Cash",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(if (language == "bn") "এন্ট্রি ডিলিট নিশ্চিতকরণ" else "Confirm Delete") },
            text = {
                Text(if (language == "bn") "আপনি কি নিশ্চিতভাবে এই ${CalculationHelper.formatCurrency(target.amount, currency)} টাকা জমার রেকর্ডটি মুছে ফেলতে চান?" else "Are you sure you want to delete this cash deposit entry?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        target.originalCashLog?.let { onDeleteLog(it) }
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text(if (language == "bn") "ডিলিট করুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }
}
