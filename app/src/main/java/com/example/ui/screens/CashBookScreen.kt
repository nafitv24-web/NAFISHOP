package com.example.ui.screens

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

    // Filter states: "সব", "দৈনিক", "সাপ্তাহিক", "মাসিক", "বার্ষিক"
    var selectedPeriod by remember { mutableStateOf("সব") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showBalanceToggle by remember { mutableStateOf(true) }

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

    // Calculate chronological running balance from oldest to newest
    val allLogsWithBalance = remember(cashLogs) {
        val sortedAsc = cashLogs.sortedBy { it.timestamp }
        var currentBal = 0.0
        val withBal = sortedAsc.map { log ->
            val isAdd = log.type in listOf("DEPOSIT", "DAY_END_CLOSING", "INCOME") || (log.type == "MANUAL_ADJUST" && log.amount >= 0)
            if (isAdd) {
                currentBal += log.amount
            } else {
                currentBal -= log.amount
            }
            CashLogDisplayItem(log = log, isAddition = isAdd, runningBalance = currentBal)
        }
        withBal.reversed() // newest first for display
    }

    val filteredDisplayLogs = remember(allLogsWithBalance, selectedPeriod, searchQuery) {
        allLogsWithBalance.filter { item ->
            val log = item.log
            val matchesPeriod = when (selectedPeriod) {
                "দৈনিক", "DAILY", "Daily" -> log.timestamp >= startOfDay
                "সাপ্তাহিক", "WEEKLY", "Weekly" -> log.timestamp >= startOfWeek
                "মাসিক", "MONTHLY", "Monthly" -> log.timestamp >= startOfMonth
                "বার্ষিক", "YEARLY", "Yearly" -> log.timestamp >= startOfYear
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

    val totalIncome = remember(filteredDisplayLogs) {
        filteredDisplayLogs.filter { it.isAddition }.sumOf { it.log.amount }
    }

    val totalExpense = remember(filteredDisplayLogs) {
        filteredDisplayLogs.filter { !it.isAddition }.sumOf { it.log.amount }
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
                                    text = if (language == "bn") "নগদ ক্যাশ বই" else "Cash Book",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "bn") "ক্যাশ লেনদেন ও ব্যালেন্স হিসাব" else "Cash transaction history",
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
                .background(MaterialTheme.colorScheme.surface)
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

            // Subheader (Transaction Count + Balance Toggle + Period Filter Chips)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (language == "bn") "লেনদেন" else "Transactions"} ${filteredDisplayLogs.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

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

                    // Period Filter Chips (সব, দৈনিক, সাপ্তাহিক, মাসিক, বার্ষিক)
                    val filters = listOf("সব", "দৈনিক", "সাপ্তাহিক", "মাসিক", "বার্ষিক")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filters) { filterName ->
                            FilterChip(
                                selected = selectedPeriod == filterName,
                                onClick = { selectedPeriod = filterName },
                                label = {
                                    Text(
                                        text = filterName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selectedPeriod == filterName) FontWeight.Bold else FontWeight.Normal
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

            // Table Body Rows (Clean List Layout with running balance)
            if (filteredDisplayLogs.isEmpty()) {
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
                        .fillMaxWidth()
                ) {
                    items(filteredDisplayLogs, key = { it.log.id }) { item ->
                        val log = item.log
                        val isAddition = item.isAddition
                        val dateFormatted = remember(log.timestamp, language) {
                            formatLedgerDate(log.timestamp, language)
                        }

                        val typeLabel = when (log.type) {
                            "DEPOSIT" -> if (language == "bn") "ক্যাশ জমা" else "Deposit"
                            "WITHDRAWAL" -> if (language == "bn") "ক্যাশ উত্তোলন" else "Withdrawal"
                            "DAY_END_CLOSING" -> if (language == "bn") "বিক্রি ক্যাশ জমা" else "Daily Sales"
                            "INCOME" -> if (language == "bn") "নগদ আয়" else "Cash In"
                            "EXPENSE" -> if (language == "bn") "নগদ খরচ" else "Cash Out"
                            "MANUAL_ADJUST" -> if (language == "bn") "ব্যালেন্স সংশোধন" else "Adjust"
                            else -> log.type
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editingLog = log }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Column 1: Date & Note / Type
                                Column(modifier = Modifier.weight(1.5f)) {
                                    Text(
                                        text = dateFormatted,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (log.note.isNotBlank()) "${log.note} • $typeLabel" else typeLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 2
                                    )
                                }

                                // Column 2: জমা (Deposit / Sales / Cash In - Green)
                                Box(
                                    modifier = Modifier.weight(0.9f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (isAddition) {
                                        Text(
                                            text = "$currency${log.amount.toIntOrNull() ?: log.amount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                    }
                                }

                                // Column 3: প্রদত্ত / খরচ (Expense / Withdrawal - Red)
                                Box(
                                    modifier = Modifier.weight(0.9f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (!isAddition) {
                                        Text(
                                            text = "$currency${log.amount.toIntOrNull() ?: log.amount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = LossRed
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
                                            color = if (bal < 0) LossRed else DueOrange
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

            // Bottom Sticky Section (2 Buttons: Green '↓ জমা' & Red '↑ প্রদত্ত' + Dark Summary Ribbon)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 8.dp)
            ) {
                // Two Large Action Buttons matching screenshot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Green "জমা" Button
                    Button(
                        onClick = { showIncomeDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "জমা" else "Deposit",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Red "প্রদত্ত" Button (Expense / Cash Out)
                    Button(
                        onClick = { showExpenseDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "প্রদত্ত" else "Given",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // 3-Column Summary Ribbon matching screenshot
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(8.dp),
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
                        // Total Collected / জমা
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (language == "bn") "মোট জমা" else "Total Received",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currency${totalIncome.toIntOrNull() ?: totalIncome}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80)
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(26.dp), color = Color(0xFF475569))

                        // Total Given / প্রদত্ত
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (language == "bn") "মোট প্রদত্ত" else "Total Given",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currency${totalExpense.toIntOrNull() ?: totalExpense}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171)
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(26.dp), color = Color(0xFF475569))

                        // Balance
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = if (language == "bn") "ব্যালেন্স" else "Balance",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currency${netBalance.toIntOrNull() ?: netBalance} ক্যাশ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (netBalance >= 0) Color(0xFF4ADE80) else Color(0xFFF87171)
                            )
                        }
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
