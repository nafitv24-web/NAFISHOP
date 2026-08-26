package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CashLog
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashBookScreen(
    viewModel: ShopViewModel,
    onBack: (() -> Unit)? = null
) {
    val cashLogs by viewModel.cashLogs.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency = shopInfo.currency

    // Filter states: "ALL" (সব), "DAILY" (দৈনিক), "WEEKLY" (সাপ্তাহিক), "MONTHLY" (মাসিক), "YEARLY" (বার্ষিক)
    var selectedPeriod by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    // Dialog states
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

    // Filtered logs
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

    // Total Income & Expense calculations
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(if (language == "bn") "নোট বা বিবরণ খুঁজুন..." else "Search notes...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 8.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    } else {
                        Text(
                            text = if (language == "bn") "নগদ বই" else "Cash Book",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0F172A)) // Dark slate backdrop matching modern Cash Book theme
        ) {
            // Period Filter Bar (সব, দৈনিক, সাপ্তাহিক, মাসিক, বার্ষিক)
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF3B82F6) else Color(0xFF334155),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedPeriod = key }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Summary Dashboard Banner (মোট আয় | মোট খরচ | নিট ব্যালেন্স)
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

            // Table Body / LazyColumn of Cash Log Rows
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (language == "bn") "কোনো ক্যাশ এন্ট্রি পাওয়া যায়নি" else "No Cash Book Entries",
                            style = MaterialTheme.typography.bodyMedium,
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
            Surface(
                color = Color(0xFF1E293B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Green Income Button
                    Button(
                        onClick = { showIncomeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "আয়" else "Cash In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Red Expense Button
                    Button(
                        onClick = { showExpenseDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "খরচ" else "Cash Out",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Bottom Sticky Summary Footer (মোট আয় | মোট খরচ | ব্যালেন্স)
            Surface(
                color = Color(0xFF0F172A),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column 1: মোট আয়
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "মোট আয়" else "Total In",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$currency${totalIncome.toIntOrNull() ?: totalIncome}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4ADE80),
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0xFF334155))
                    )

                    // Column 2: মোট খরচ
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "মোট খরচ" else "Total Out",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$currency${totalExpense.toIntOrNull() ?: totalExpense}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF87171),
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(28.dp)
                            .background(Color(0xFF334155))
                    )

                    // Column 3: ব্যালেন্স
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "ব্যালেন্স" else "Balance",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "$currency${netBalance.toIntOrNull() ?: netBalance}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (netBalance >= 0) Color(0xFF60A5FA) else Color(0xFFF87171),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }

    // Cash In (Income) Entry Dialog
    if (showIncomeDialog) {
        CashEntryDialog(
            title = if (language == "bn") "নগদ আয় যোগ করুন (Cash In)" else "Add Cash Income",
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
        CashEntryDialog(
            title = if (language == "bn") "নগদ খরচ কর্তন করুন (Cash Out)" else "Record Cash Expense",
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

    // Edit Log Dialog
    if (editingLog != null) {
        val target = editingLog!!
        var editAmount by remember { mutableStateOf(target.amount.toIntOrNull()?.toString() ?: target.amount.toString()) }
        var editNote by remember { mutableStateOf(target.note) }

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
                            deletingLog = target
                            editingLog = null
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
                        TextButton(onClick = { editingLog = null }) {
                            Text(if (language == "bn") "বাতিল" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amt = editAmount.toDoubleOrNull() ?: target.amount
                                viewModel.editCashLog(target, amt, editNote)
                                editingLog = null
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
    if (deletingLog != null) {
        val target = deletingLog!!
        AlertDialog(
            onDismissRequest = { deletingLog = null },
            title = { Text(if (language == "bn") "লেনদেন ডিলিট নিশ্চিতকরণ" else "Confirm Delete") },
            text = { Text(if (language == "bn") "আপনি কি এই ক্যাশ এন্ট্রিটি মুছে ফেলতে চান?" else "Are you sure you want to delete this cash entry?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCashLog(target)
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
fun CashEntryDialog(
    title: String,
    currency: String,
    language: String,
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, timestamp: Long) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }

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
                    label = { Text(if (language == "bn") "বিবরণ / খাত *" else "Description / Purpose *") },
                    placeholder = { Text(if (isIncome) "যেমন: নগদ বিক্রি / মহাজন পেমেন্ট" else "যেমন: Halima Bill / দোকান ভাড়া") },
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
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            val finalNote = noteStr.ifBlank { if (isIncome) "ক্যাশ আয়" else "ক্যাশ খরচ" }
                            onConfirm(amount, finalNote, System.currentTimeMillis())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isIncome) Color(0xFF16A34A) else Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(if (isIncome) Icons.Default.Add else Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isIncome) (if (language == "bn") "ক্যাশে আয় যুক্ত করুন" else "Add Income") else (if (language == "bn") "ক্যাশ থেকে খরচ কর্তন করুন" else "Record Expense"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
