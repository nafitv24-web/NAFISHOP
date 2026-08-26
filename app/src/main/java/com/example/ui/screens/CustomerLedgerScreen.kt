package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.Customer
import com.example.data.model.DueLog
import com.example.data.model.Product
import com.example.data.model.TransactionRecord
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

data class LedgerEntry(
    val id: Long,
    val sourceId: Long,
    val isDueLog: Boolean,
    val type: String, // "GIVEN" (বাকি দেওয়া/প্রদত্ত) or "COLLECTED" (জমা/আদায়)
    val title: String,
    val note: String,
    val amount: Double,
    val timestamp: Long,
    val runningBalance: Double = 0.0,
    val originalDueLog: DueLog? = null,
    val originalTransaction: TransactionRecord? = null
)

/**
 * Full Customer Credit/Debit Ledger History Screen (Matches the user's Khata Book style)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerLedgerScreen(
    customer: Customer,
    viewModel: ShopViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val currency = shopInfo.currency

    val allCustomers by viewModel.customers.collectAsState()
    // Keep customer state up-to-date from viewModel
    val currentCustomer = remember(allCustomers, customer.id) {
        allCustomers.find { it.id == customer.id } ?: customer
    }

    val dueLogs by viewModel.dueLogs.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val products by viewModel.products.collectAsState()

    var selectedFilterPeriod by remember { mutableStateOf("সব") } // সব, দৈনিক, সাপ্তাহিক, মাসিক, বার্ষিক
    var showBalanceToggle by remember { mutableStateOf(true) }

    // Dialogs
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var addTransactionType by remember { mutableStateOf("GIVEN") } // "GIVEN" (প্রদত্ত) or "COLLECTED" (জমা)

    var editingLog by remember { mutableStateOf<DueLog?>(null) }
    var showEditLogDialog by remember { mutableStateOf(false) }

    var showEditCustomerDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Filter due logs and transactions for this customer
    val customerDueLogs = remember(dueLogs, currentCustomer.id) {
        dueLogs.filter { it.customerId == currentCustomer.id }
    }

    val customerTransactions = remember(allTransactions, currentCustomer.name, currentCustomer.phone) {
        allTransactions.filter { tx ->
            (tx.customerName.isNotBlank() && tx.customerName.equals(currentCustomer.name, ignoreCase = true)) ||
                    (tx.customerPhone.isNotBlank() && tx.customerPhone == currentCustomer.phone)
        }
    }

    // Build unified chronological ledger entries with running balance
    val allLedgerEntries = remember(customerDueLogs, customerTransactions) {
        val rawList = mutableListOf<LedgerEntry>()

        // 1. Add all Due Logs
        customerDueLogs.forEach { log ->
            val isGiven = log.type == "DUE_GIVEN"
            rawList.add(
                LedgerEntry(
                    id = log.id * 10 + 1,
                    sourceId = log.id,
                    isDueLog = true,
                    type = if (isGiven) "GIVEN" else "COLLECTED",
                    title = if (isGiven) "বাকি প্রদান" else "জমা গ্রহণ",
                    note = log.note.ifBlank { if (isGiven) "বাকি হিসাব" else "নগদ জমা" },
                    amount = log.amount,
                    timestamp = log.timestamp,
                    originalDueLog = log
                )
            )
        }

        // 2. Add POS Transactions that have due and are not already in dueLogs
        val loggedInvoices = customerDueLogs.mapNotNull { log ->
            val match = "INV-\\d+".toRegex().find(log.note)
            match?.value
        }.toSet()

        customerTransactions.forEach { tx ->
            if (tx.dueAmount > 0 && !loggedInvoices.contains(tx.invoiceNumber)) {
                rawList.add(
                    LedgerEntry(
                        id = tx.id * 10 + 2,
                        sourceId = tx.id,
                        isDueLog = false,
                        type = "GIVEN",
                        title = "বিক্রয় চালান #${tx.invoiceNumber}",
                        note = "${tx.productName} (${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit}) • বাকি: $currency${tx.dueAmount.toIntOrNull() ?: tx.dueAmount}",
                        amount = tx.dueAmount,
                        timestamp = tx.timestamp,
                        originalTransaction = tx
                    )
                )
            }
        }

        // Sort ascending by time to calculate chronological running balance
        val sortedAsc = rawList.sortedBy { it.timestamp }
        var currentBal = 0.0
        val withRunningBalance = sortedAsc.map { entry ->
            if (entry.type == "GIVEN") {
                currentBal -= entry.amount // Customer balance becomes more negative (owes money)
            } else {
                currentBal += entry.amount // Customer balance increases towards 0 or positive
            }
            entry.copy(runningBalance = currentBal)
        }

        // Sort descending (newest first) for viewing
        withRunningBalance.sortedByDescending { it.timestamp }
    }

    // Filter by period
    val filteredEntries = remember(allLedgerEntries, selectedFilterPeriod) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        when (selectedFilterPeriod) {
            "দৈনিক", "Daily" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                allLedgerEntries.filter { it.timestamp >= calendar.timeInMillis }
            }
            "সাপ্তাহিক", "Weekly" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                allLedgerEntries.filter { it.timestamp >= calendar.timeInMillis }
            }
            "মাসিক", "Monthly" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                allLedgerEntries.filter { it.timestamp >= calendar.timeInMillis }
            }
            "বার্ষিক", "Yearly" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -365)
                allLedgerEntries.filter { it.timestamp >= calendar.timeInMillis }
            }
            else -> allLedgerEntries
        }
    }

    val totalGiven = remember(filteredEntries) {
        filteredEntries.filter { it.type == "GIVEN" }.sumOf { it.amount }
    }
    val totalCollected = remember(filteredEntries) {
        filteredEntries.filter { it.type == "COLLECTED" }.sumOf { it.amount }
    }
    val netBalance = totalCollected - totalGiven

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showEditCustomerDialog = true }
                    ) {
                        if (currentCustomer.imageUri.isNotBlank()) {
                            AsyncImage(
                                model = currentCustomer.imageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, DueOrange, CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (currentCustomer.totalDue > 0) Color(0xFFFFEDD5) else Color(0xFFDCFCE7),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = currentCustomer.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (currentCustomer.totalDue > 0) DueOrange else ProfitGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = currentCustomer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            if (currentCustomer.phone.isNotBlank()) {
                                Text(
                                    text = currentCustomer.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // WhatsApp / SMS reminder
                    if (currentCustomer.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val msg = com.example.util.CustomerSmsHelper.buildLedgerTransactionMessage(
                                    shopName = shopInfo.shopName,
                                    shopPhone = shopInfo.phone,
                                    customerName = currentCustomer.name,
                                    type = "STATEMENT",
                                    amount = 0.0,
                                    note = "বকেয়া বাকি তাগাদা",
                                    previousDue = currentCustomer.totalDue,
                                    totalCurrentDue = currentCustomer.totalDue,
                                    currency = currency
                                )
                                com.example.util.CustomerSmsHelper.sendDirectSms(context, currentCustomer.phone, msg)
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share Statement")
                        }
                    }

                    // PDF Statement
                    IconButton(
                        onClick = {
                            val pdf = PdfGenerator.generateCustomerDuePdf(
                                context = context,
                                shopName = shopInfo.shopName,
                                customer = currentCustomer,
                                history = customerDueLogs,
                                currency = currency
                            )
                            if (pdf != null) {
                                PdfGenerator.openOrSharePdf(
                                    context = context,
                                    file = pdf,
                                    chooserTitle = if (language == "bn") "বাকি খাতার PDF শেয়ার করুন" else "Share Ledger PDF"
                                )
                            } else {
                                Toast.makeText(context, "PDF তৈরি করা যায়নি", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Statement", tint = DueOrange)
                    }

                    // Call Customer
                    if (currentCustomer.phone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${currentCustomer.phone}")
                                }
                                context.startActivity(callIntent)
                            }
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call Customer", tint = ProfitGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            // Sticky Bottom Action Section (Big Green জমা & Red প্রদত্ত Buttons + Summary Ribbon)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(bottom = 6.dp)
            ) {
                // Two Action Buttons: জমা (Green) & প্রদত্ত (Red)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Green "জমা" Button (Receive Money)
                    Button(
                        onClick = {
                            addTransactionType = "COLLECTED"
                            showAddTransactionDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
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

                    // Red "প্রদত্ত" Button (Give Credit/Due)
                    Button(
                        onClick = {
                            addTransactionType = "GIVEN"
                            showAddTransactionDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
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

                // Bottom 3-Column Summary Ribbon (মোট জমা | মোট প্রদত্ত | ব্যালেন্স)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E293B), // Dark slate bar matching screenshot
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
                                text = "$currency${totalCollected.toIntOrNull() ?: totalCollected}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4ADE80) // Bright green
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
                                text = "$currency${totalGiven.toIntOrNull() ?: totalGiven}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF87171) // Bright red
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(26.dp), color = Color(0xFF475569))

                        // Current Balance / ব্যালেন্স
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
                            val isDue = currentCustomer.totalDue > 0
                            Text(
                                text = if (isDue) "-$currency${currentCustomer.totalDue.toIntOrNull() ?: currentCustomer.totalDue} বাকি"
                                else (if (language == "bn") "পরিশোধিত" else "Settled"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDue) Color(0xFFFB923C) else Color(0xFF4ADE80)
                            )
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
            // Sub-Header: Transaction Count & Period Filters
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (language == "bn") "লেনদেন" else "Transactions"} ${filteredEntries.size}",
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
                                selected = selectedFilterPeriod == filterName,
                                onClick = { selectedFilterPeriod = filterName },
                                label = {
                                    Text(
                                        text = filterName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (selectedFilterPeriod == filterName) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            // Table Column Headers (তারিখ | জমা | প্রদত্ত | ব্যালেন্স)
            Surface(
                color = Color(0xFF334155), // Dark header row
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

            // Ledger Transaction Rows List
            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (language == "bn") "এই সময়ে কোনো লেনদেন পাওয়া যায়নি" else "No ledger records in this period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (language == "bn") "নিচের 'জমা' বা 'প্রদত্ত' চেপে লেনদেন যোগ করুন" else "Tap Deposit or Given below to add transaction",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        LedgerRowItem(
                            entry = entry,
                            currency = currency,
                            language = language,
                            showBalance = showBalanceToggle,
                            onClick = {
                                if (entry.isDueLog && entry.originalDueLog != null) {
                                    editingLog = entry.originalDueLog
                                    showEditLogDialog = true
                                }
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }

    // Add Transaction Dialog (Matches Screenshot 2: "লেনদেন যোগ")
    if (showAddTransactionDialog) {
        AddLedgerTransactionDialog(
            customer = currentCustomer,
            initialType = addTransactionType,
            currency = currency,
            language = language,
            products = products,
            shopName = shopInfo.shopName,
            shopPhone = shopInfo.phone,
            onDismiss = { showAddTransactionDialog = false },
            onSave = { type, amount, note, timestamp ->
                if (type == "GIVEN") {
                    viewModel.giveCustomerDue(currentCustomer, amount, note)
                } else {
                    viewModel.collectCustomerDue(currentCustomer, amount, note)
                }
                showAddTransactionDialog = false
            }
        )
    }

    // Edit Log Dialog (to correct mistake in ledger)
    if (showEditLogDialog && editingLog != null) {
        EditDueLogDialog(
            dueLog = editingLog!!,
            currency = currency,
            language = language,
            onDismiss = { showEditLogDialog = false },
            onSave = { newAmount, newType, newNote ->
                viewModel.editDueLogAndRecalculate(
                    oldLog = editingLog!!,
                    newAmount = newAmount,
                    newType = newType,
                    newNote = newNote,
                    customer = currentCustomer
                )
                showEditLogDialog = false
            },
            onDelete = {
                viewModel.deleteDueLog(editingLog!!)
                showEditLogDialog = false
            }
        )
    }

    // Edit Customer Info Dialog
    if (showEditCustomerDialog) {
        EditCustomerDialog(
            customer = currentCustomer,
            currency = currency,
            language = language,
            onDismiss = { showEditCustomerDialog = false },
            onSave = { updated ->
                viewModel.updateCustomer(updated)
                showEditCustomerDialog = false
            }
        )
    }
}

/**
 * Individual Ledger Row item with Date, Note, Given/Received, Running Balance
 */
@Composable
fun LedgerRowItem(
    entry: LedgerEntry,
    currency: String,
    language: String,
    showBalance: Boolean,
    onClick: () -> Unit
) {
    val isCollected = entry.type == "COLLECTED"
    val dateFormatted = remember(entry.timestamp, language) {
        formatLedgerDate(entry.timestamp, language)
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column 1: Date & Note / Item Description
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = dateFormatted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.note.ifBlank { entry.title },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2
                )
            }

            // Column 2: জমা (Received - Green)
            Box(
                modifier = Modifier.weight(0.9f),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isCollected) {
                    Text(
                        text = "$currency${entry.amount.toIntOrNull() ?: entry.amount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
            }

            // Column 3: প্রদত্ত (Given - Red)
            Box(
                modifier = Modifier.weight(0.9f),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (!isCollected) {
                    Text(
                        text = "$currency${entry.amount.toIntOrNull() ?: entry.amount}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = LossRed
                    )
                }
            }

            // Column 4: Running Balance (e.g. -385)
            if (showBalance) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    val bal = entry.runningBalance
                    Text(
                        text = "${if (bal < 0) "-" else ""}$currency${Math.abs(bal).toIntOrNull() ?: Math.abs(bal)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (bal < 0) DueOrange else ProfitGreen
                    )
                }
            }
        }
    }
}

/**
 * "লেনদেন যোগ" (Add Transaction Screen Dialog) - Exactly matches Screenshot 2
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLedgerTransactionDialog(
    customer: Customer,
    initialType: String, // "GIVEN" or "COLLECTED"
    currency: String,
    language: String,
    products: List<Product>,
    shopName: String = "",
    shopPhone: String = "",
    onDismiss: () -> Unit,
    onSave: (type: String, amount: Double, note: String, timestamp: Long) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) } // "GIVEN" (প্রদত্ত) or "COLLECTED" (জমা)
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var showProductPicker by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(context, if (language == "bn") "রশিদের ছবি যুক্ত হয়েছে" else "Bill photo attached", Toast.LENGTH_SHORT).show()
            if (note.isBlank()) {
                note = "বিল/রশিদ যুক্ত করা হয়েছে"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Back Arrow and Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == "bn") "লেনদেন যোগ" else "Add Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customer Header Card (Photo, Name, Balance)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (customer.imageUri.isNotBlank()) {
                            AsyncImage(
                                model = customer.imageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, DueOrange, CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = if (customer.totalDue > 0) Color(0xFFFFEDD5) else Color(0xFFDCFCE7),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = customer.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (customer.totalDue > 0) DueOrange else ProfitGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val isDue = customer.totalDue > 0
                            Text(
                                text = "${if (language == "bn") "ব্যালেন্স" else "Balance"} ${if (isDue) "-$currency${customer.totalDue.toIntOrNull() ?: customer.totalDue} বাকি" else (if (language == "bn") "০ পরিশোধিত" else "0 Settled")}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isDue) DueOrange else ProfitGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Type Toggle: প্রদত্ত (Given) vs জমা (Deposit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "GIVEN",
                        onClick = { selectedType = "GIVEN" },
                        label = {
                            Text(
                                text = if (language == "bn") "প্রদত্ত (বাকি দেওয়া)" else "Given (Due)",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEE2E2),
                            selectedLabelColor = LossRed
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedType == "COLLECTED",
                        onClick = { selectedType = "COLLECTED" },
                        label = {
                            Text(
                                text = if (language == "bn") "জমা (আদায় গ্রহণ)" else "Deposit (Received)",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDCFCE7),
                            selectedLabelColor = ProfitGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Amount Input Field with Calculator Icon
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = {
                        Text(
                            text = if (selectedType == "GIVEN") (if (language == "bn") "প্রদত্ত টাকার পরিমাণ ($currency) *" else "Given Amount *")
                            else (if (language == "bn") "জমা টাকার পরিমাণ ($currency) *" else "Deposit Amount *"),
                            color = if (selectedType == "GIVEN") LossRed else ProfitGreen
                        )
                    },
                    trailingIcon = {
                        Icon(Icons.Default.Calculate, contentDescription = "Calculator", tint = MaterialTheme.colorScheme.primary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Note Input Field with Mic / Edit Icon
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = {
                        Text(if (language == "bn") "এখানে নোট লিখুন [ঐচ্ছিক]" else "Write note here [Optional]")
                    },
                    leadingIcon = {
                        Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row: "বিল যোগ করুন" & "বস্তু যোগ করুন"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // বিল যোগ করুন (Attach Receipt Photo)
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "বিল যোগ করুন" else "Add Bill",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // বস্তু যোগ করুন (Pick from Inventory Products)
                    OutlinedButton(
                        onClick = { showProductPicker = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = StockBlue)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "বস্তু যোগ করুন" else "Add Items",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Date & Time Selector Row
                val dateStr = remember(selectedTimestamp, language) {
                    formatLedgerDate(selectedTimestamp, language)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateStr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = { selectedTimestamp = System.currentTimeMillis() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (language == "bn") "বর্তমান সময়" else "Now", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Action Buttons: "বার্তা পাঠান" & "সংরক্ষণ করুন"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // বার্তা পাঠান Button
                    OutlinedButton(
                        onClick = {
                            val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
                            val prevDue = customer.totalDue
                            val totalCurrentDue = if (selectedType == "GIVEN") prevDue + enteredAmount else (prevDue - enteredAmount).coerceAtLeast(0.0)
                            val msg = com.example.util.CustomerSmsHelper.buildLedgerTransactionMessage(
                                shopName = shopName,
                                shopPhone = shopPhone,
                                customerName = customer.name,
                                type = selectedType,
                                amount = enteredAmount,
                                note = note,
                                previousDue = prevDue,
                                totalCurrentDue = totalCurrentDue,
                                currency = currency
                            )
                            com.example.util.CustomerSmsHelper.sendDirectSms(context, customer.phone, msg)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "বার্তা পাঠান" else "Send SMS",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // সংরক্ষণ করুন Button
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount > 0) {
                                onSave(selectedType, amount, note, selectedTimestamp)
                            } else {
                                Toast.makeText(context, if (language == "bn") "সঠিক টাকার পরিমাণ লিখুন" else "Enter valid amount", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedType == "GIVEN") LossRed else ProfitGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "সংরক্ষণ করুন" else "Save Entry",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Quick Item / Product Picker Modal
    if (showProductPicker) {
        QuickProductSelectionDialog(
            products = products,
            currency = currency,
            language = language,
            onDismiss = { showProductPicker = false },
            onItemsSelected = { selectedList ->
                val totalCost = selectedList.sumOf { it.first.sellPrice * it.second }
                val itemsSummary = selectedList.joinToString(", ") { "${it.first.name} (${it.second.toIntOrNull() ?: it.second} ${it.first.unit})" }

                amountStr = (totalCost.toIntOrNull() ?: totalCost).toString()
                note = itemsSummary
                showProductPicker = false
            }
        )
    }
}

/**
 * Quick Product Selection Dialog for "বস্তু যোগ করুন"
 */
@Composable
fun QuickProductSelectionDialog(
    products: List<Product>,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onItemsSelected: (List<Pair<Product, Double>>) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val selectedItems = remember { mutableStateMapOf<Long, Double>() }

    val filtered = remember(products, search) {
        products.filter {
            search.isBlank() || it.name.contains(search, ignoreCase = true) || it.barcode.contains(search, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (language == "bn") "পণ্য তালিকা থেকে যোগ করুন" else "Select Products",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(if (language == "bn") "পণ্য সার্চ করুন..." else "Search product...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filtered) { prod ->
                        val qty = selectedItems[prod.id] ?: 0.0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (qty > 0) EmeraldPrimary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (qty > 0) EmeraldPrimary else Color.Transparent),
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
                                    Text(prod.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "$currency${prod.sellPrice.toIntOrNull() ?: prod.sellPrice} / ${prod.unit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (qty > 0) {
                                        IconButton(
                                            onClick = {
                                                if (qty > 1.0) selectedItems[prod.id] = qty - 1.0
                                                else selectedItems.remove(prod.id)
                                            },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                                        }
                                        Text(
                                            text = "${qty.toIntOrNull() ?: qty}",
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedItems[prod.id] = (selectedItems[prod.id] ?: 0.0) + 1.0 },
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = EmeraldPrimary)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

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
                            val result = selectedItems.mapNotNull { (prodId, quantity) ->
                                val p = products.find { it.id == prodId }
                                if (p != null) Pair(p, quantity) else null
                            }
                            onItemsSelected(result)
                        },
                        enabled = selectedItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(if (language == "bn") "যুক্ত করুন (${selectedItems.size})" else "Done (${selectedItems.size})")
                    }
                }
            }
        }
    }
}

/**
 * Bengali / English date formatter
 */
fun formatLedgerDate(timestamp: Long, language: String): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
    val formattedHour = if (hour == 0) 12 else hour
    val minuteStr = String.format(Locale.getDefault(), "%02d", minute)

    return if (language == "bn") {
        val daysBn = listOf("", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
        val monthsBn = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন", "জুলাই", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
        val dayName = daysBn.getOrElse(dayOfWeek) { "" }
        val monthName = monthsBn.getOrElse(month) { "" }
        val banglaDigits = mapOf('0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪', '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯')
        fun toBn(s: String) = s.map { banglaDigits[it] ?: it }.joinToString("")

        "$dayName, ${toBn(day.toString())} $monthName ${toBn(year.toString())} ${toBn(formattedHour.toString())}:${toBn(minuteStr)} $amPm"
    } else {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy hh:mm a", Locale.ENGLISH)
        sdf.format(Date(timestamp))
    }
}
