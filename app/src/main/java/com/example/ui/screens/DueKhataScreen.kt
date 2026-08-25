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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Customer
import com.example.data.model.DueLog
import com.example.data.model.TransactionRecord
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueKhataScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val dueLogs by viewModel.dueLogs.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency = shopInfo.currency

    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    var selectedCustomerForPayment by remember { mutableStateOf<Customer?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    var selectedCustomerForDue by remember { mutableStateOf<Customer?>(null) }
    var showAddDueDialog by remember { mutableStateOf(false) }

    var selectedCustomerForHistory by remember { mutableStateOf<Customer?>(null) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    var selectedCustomerForEdit by remember { mutableStateOf<Customer?>(null) }
    var showEditCustomerDialog by remember { mutableStateOf(false) }

    var selectedCustomerForLedger by remember { mutableStateOf<Customer?>(null) }

    var editingDueLog by remember { mutableStateOf<Pair<DueLog, Customer>?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter { c ->
            searchQuery.isBlank() ||
                    c.name.contains(searchQuery, ignoreCase = true) ||
                    c.phone.contains(searchQuery, ignoreCase = true) ||
                    c.address.contains(searchQuery, ignoreCase = true)
        }
    }

    val totalDueSum = remember(customers) { customers.sumOf { it.totalDue } }
    val debtorsCount = remember(customers) { customers.count { it.totalDue > 0 } }

    if (selectedCustomerForLedger != null) {
        CustomerLedgerScreen(
            customer = selectedCustomerForLedger!!,
            viewModel = viewModel,
            onBack = { selectedCustomerForLedger = null }
        )
        return
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = DueOrange,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Customer")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (language == "bn") "নতুন কাস্টমার" else "Add Customer", fontWeight = FontWeight.Bold)
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
            // Total Due Overview Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == "bn") "দোকানের মোট পাওনা বাকি" else "Total Outstanding Due",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF9A3412),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "$currency${totalDueSum.toIntOrNull() ?: totalDueSum}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DueOrange
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFEDD5)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$debtorsCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = DueOrange
                            )
                            Text(
                                text = if (language == "bn") "জন বাকিদার" else "Debtors",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9A3412)
                            )
                        }
                    }
                }

                // PDF Download button for All Dues
                HorizontalDivider(color = Color(0xFFFED7AA), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "সম্পূর্ণ বাকি খাতার স্টেটমেন্ট" else "Full Due Statement",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9A3412),
                        fontWeight = FontWeight.Medium
                    )
                    FilledTonalButton(
                        onClick = {
                            val pdfFile = PdfGenerator.generateAllDuesPdf(
                                context = context,
                                shopName = shopInfo.shopName,
                                customers = customers,
                                totalDue = totalDueSum,
                                currency = currency
                            )
                            if (pdfFile != null) {
                                PdfGenerator.openOrSharePdf(
                                    context = context,
                                    file = pdfFile,
                                    chooserTitle = if (language == "bn") "বাকি খাতা PDF ডাউনলোড / শেয়ার করুন" else "Download / Share Dues PDF"
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    if (language == "bn") "PDF তৈরি করতে সমস্যা হয়েছে" else "Failed to generate PDF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DueOrange,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "বাকি PDF ডাউনলোড" else "Download Due PDF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Customer Search Bar
            PaddingValues(horizontal = 16.dp).let {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (language == "bn") "কাস্টমারের নাম, ফোন বা ঠিকানা খুঁজুন..." else "Search customer name, phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Customer List
            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(54.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (language == "bn") "কোনো কাস্টমার বা বাকি হিসাব পাওয়া যায়নি" else "No customer accounts found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        CustomerKhataCard(
                            customer = customer,
                            currency = currency,
                            language = language,
                            shopName = shopInfo.shopName,
                            onCollectPayment = {
                                selectedCustomerForPayment = customer
                                showPaymentDialog = true
                            },
                            onAddDue = {
                                selectedCustomerForDue = customer
                                showAddDueDialog = true
                            },
                            onViewHistory = {
                                selectedCustomerForLedger = customer
                            },
                            onEdit = {
                                selectedCustomerForEdit = customer
                                showEditCustomerDialog = true
                            },
                            onDelete = {
                                viewModel.deleteCustomer(customer)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Customer Dialog
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            currency = currency,
            language = language,
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, address, initialDue, imageUri ->
                viewModel.addCustomer(name, phone, address, initialDue, imageUri)
                showAddCustomerDialog = false
            }
        )
    }

    // Edit Customer Profile Dialog
    if (showEditCustomerDialog && selectedCustomerForEdit != null) {
        EditCustomerDialog(
            customer = selectedCustomerForEdit!!,
            currency = currency,
            language = language,
            onDismiss = { showEditCustomerDialog = false },
            onSave = { updatedCust ->
                viewModel.updateCustomer(updatedCust)
                showEditCustomerDialog = false
            }
        )
    }

    // Collect Due Dialog
    if (showPaymentDialog && selectedCustomerForPayment != null) {
        CollectDueDialog(
            customer = selectedCustomerForPayment!!,
            currency = currency,
            language = language,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { amount, note ->
                viewModel.collectCustomerDue(selectedCustomerForPayment!!, amount, note)
                showPaymentDialog = false
            }
        )
    }

    // Give Additional Due Dialog
    if (showAddDueDialog && selectedCustomerForDue != null) {
        GiveDueDialog(
            customer = selectedCustomerForDue!!,
            currency = currency,
            language = language,
            onDismiss = { showAddDueDialog = false },
            onConfirm = { amount, note ->
                viewModel.giveCustomerDue(selectedCustomerForDue!!, amount, note)
                showAddDueDialog = false
            }
        )
    }

    // Edit Due Log (Mistake Correction) Dialog
    if (editingDueLog != null) {
        val (logToEdit, cust) = editingDueLog!!
        EditDueLogDialog(
            dueLog = logToEdit,
            currency = currency,
            language = language,
            onDismiss = { editingDueLog = null },
            onSave = { newAmount, newType, newNote ->
                viewModel.editDueLogAndRecalculate(logToEdit, newAmount, newType, newNote, cust)
                editingDueLog = null
            },
            onDelete = {
                // Revert effect on customer before deleting
                var adjustedDue = cust.totalDue
                if (logToEdit.type == "DUE_GIVEN") {
                    adjustedDue = (adjustedDue - logToEdit.amount).coerceAtLeast(0.0)
                } else if (logToEdit.type == "DUE_COLLECTED") {
                    adjustedDue += logToEdit.amount
                }
                viewModel.updateCustomer(cust.copy(totalDue = adjustedDue, lastTransactionDate = System.currentTimeMillis()))
                viewModel.deleteDueLog(logToEdit)
                editingDueLog = null
            }
        )
    }

    // Customer Full History Dialog (Includes Due Logs + POS Transaction Invoices, PDF, SMS)
    if (showHistoryDialog && selectedCustomerForHistory != null) {
        val cust = selectedCustomerForHistory!!
        val customerDueLogs = remember(dueLogs, cust) {
            dueLogs.filter { it.customerId == cust.id }
        }
        val customerTransactions = remember(allTransactions, cust) {
            allTransactions.filter { tx ->
                tx.customerName.isNotBlank() && tx.customerName.equals(cust.name, ignoreCase = true) ||
                        (cust.phone.isNotBlank() && tx.customerPhone == cust.phone)
            }
        }

        CustomerFullHistoryDialog(
            customer = cust,
            dueLogs = customerDueLogs,
            transactions = customerTransactions,
            currency = currency,
            language = language,
            shopName = shopInfo.shopName,
            onDismiss = { showHistoryDialog = false },
            onEditLog = { log ->
                editingDueLog = Pair(log, cust)
            }
        )
    }
}

@Composable
fun CustomerKhataCard(
    customer: Customer,
    currency: String,
    language: String,
    shopName: String,
    onCollectPayment: () -> Unit,
    onAddDue: () -> Unit,
    onViewHistory: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewHistory),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Customer Photo / Avatar
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (customer.imageUri.isNotBlank()) {
                        AsyncImage(
                            model = customer.imageUri,
                            contentDescription = "Customer Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, DueOrange.copy(alpha = 0.5f), CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = if (customer.totalDue > 0) Color(0xFFFFEDD5) else Color(0xFFDCFCE7),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = customer.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = customer.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        if (customer.address.isNotBlank()) {
                            Text(
                                text = customer.address,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currency${customer.totalDue.toIntOrNull() ?: customer.totalDue}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (customer.totalDue > 0) DueOrange else ProfitGreen
                    )
                    Text(
                        text = if (customer.totalDue > 0) (if (language == "bn") "বাকি পাওনা" else "Due") else (if (language == "bn") "পরিশোধিত" else "Paid"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (customer.totalDue > 0) DueOrange else ProfitGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (language == "bn") "সকল লেনদেন হিস্ট্রি" else "Full History") },
                            leadingIcon = { Icon(Icons.Default.History, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                onViewHistory()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (language == "bn") "কাস্টমার তথ্য সম্পাদনা" else "Edit Info") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (language == "bn") "মুছে ফেলুন" else "Delete", color = LossRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = LossRed) },
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: জমা নিন, বাকি দিন, মেসেজ পাঠান, PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Collect Payment
                Button(
                    onClick = onCollectPayment,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "bn") "জমা নিন" else "Collect", style = MaterialTheme.typography.labelSmall)
                }

                // Add Due
                OutlinedButton(
                    onClick = onAddDue,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (language == "bn") "বাকি দিন" else "Add Due", style = MaterialTheme.typography.labelSmall)
                }

                // Send SMS Reminder
                if (customer.totalDue > 0 && customer.phone.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val msg = "শ্রদ্ধেয় ${customer.name}, $shopName এ আপনার মোট বকেয়া বাকি আছে $currency${customer.totalDue}। অনুগ্রহ করে সুবিধামতো পরিশোধ করার অনুরোধ রইল। ধন্যবাদ।"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, msg)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "মেসেজ বা WhatsApp এর মাধ্যমে রিমাইন্ডার পাঠান"))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send Message", tint = ProfitGreen, modifier = Modifier.size(18.dp))
                    }
                }

                // View History Button
                IconButton(
                    onClick = onViewHistory,
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "History", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var initialDueStr by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "নতুন কাস্টমার যোগ করুন" else "Add New Customer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DueOrange
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Customer Photo Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (imageUri.isNotBlank()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, DueOrange, CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (language == "bn") "কাস্টমারের ছবি" else "Customer Photo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DueOrange)
                            ) {
                                Text(
                                    text = if (imageUri.isNotBlank()) (if (language == "bn") "ছবি পরিবর্তন" else "Change") else (if (language == "bn") "ছবি নির্বাচন" else "Select Photo"),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (imageUri.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { imageUri = "" },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (language == "bn") "মুছুন" else "Remove", style = MaterialTheme.typography.labelSmall, color = LossRed)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (language == "bn") "কাস্টমারের নাম *" else "Customer Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == "bn") "মোবাইল নম্বর" else "Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (language == "bn") "ঠিকানা / গ্রাম" else "Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = initialDueStr,
                    onValueChange = { initialDueStr = it },
                    label = { Text(if (language == "bn") "পূর্বের বকেয়া বাকি ($currency)" else "Previous Due ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("0") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            if (name.isNotBlank()) {
                                onSave(name, phone, address, initialDueStr.toDoubleOrNull() ?: 0.0, imageUri)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                        enabled = name.isNotBlank()
                    ) {
                        Text(if (language == "bn") "যোগ করুন" else "Add")
                    }
                }
            }
        }
    }
}

@Composable
fun EditCustomerDialog(
    customer: Customer,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(customer.name) }
    var phone by remember { mutableStateOf(customer.phone) }
    var address by remember { mutableStateOf(customer.address) }
    var imageUri by remember { mutableStateOf(customer.imageUri) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "কাস্টমার তথ্য সম্পাদনা" else "Edit Customer Info",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Photo Picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (imageUri.isNotBlank()) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, DueOrange, CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Photo", tint = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (language == "bn") "কাস্টমারের ছবি" else "Customer Photo",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (imageUri.isNotBlank()) (if (language == "bn") "বদলান" else "Change") else (if (language == "bn") "ছবি দিন" else "Choose"),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            if (imageUri.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { imageUri = "" },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(if (language == "bn") "মুছুন" else "Remove", style = MaterialTheme.typography.labelSmall, color = LossRed)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (language == "bn") "নাম *" else "Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == "bn") "মোবাইল নম্বর" else "Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (language == "bn") "ঠিকানা" else "Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            if (name.isNotBlank()) {
                                onSave(customer.copy(name = name, phone = phone, address = address, imageUri = imageUri))
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text(if (language == "bn") "সংরক্ষণ করুন" else "Save Changes")
                    }
                }
            }
        }
    }
}

@Composable
fun CollectDueDialog(
    customer: Customer,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("বাকি আদায় / ক্যাশ জমা") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "বাকি আদায় / জমা গ্রহণ" else "Collect Due Payment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ProfitGreen
                )
                Text(
                    text = "${customer.name} (বর্তমান বাকি: $currency${customer.totalDue.toIntOrNull() ?: customer.totalDue})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (language == "bn") "প্রাপ্ত টাকার পরিমাণ ($currency) *" else "Collected Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "মন্তব্য / বিবরণ" else "Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                                onConfirm(amount, note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                    ) {
                        Text(if (language == "bn") "জমা নিশ্চিত করুন" else "Confirm Payment")
                    }
                }
            }
        }
    }
}

@Composable
fun GiveDueDialog(
    customer: Customer,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("নতুন বাকি প্রদান") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "নতুন বাকি যুক্ত করুন" else "Give Additional Due",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DueOrange
                )
                Text(
                    text = "${customer.name} (বর্তমান বাকি: $currency${customer.totalDue.toIntOrNull() ?: customer.totalDue})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (language == "bn") "বাকি টাকার পরিমাণ ($currency) *" else "Due Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "পণ্যের নাম বা বিবরণ" else "Note / Reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                                onConfirm(amount, note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DueOrange)
                    ) {
                        Text(if (language == "bn") "বাকি যোগ করুন" else "Add Due")
                    }
                }
            }
        }
    }
}

/**
 * Edit Due Log Dialog (Allows correcting mistakes in Due Given or Collected)
 */
@Composable
fun EditDueLogDialog(
    dueLog: DueLog,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit,
    onDelete: () -> Unit
) {
    var amountStr by remember { mutableStateOf(dueLog.amount.toString().replace(".0", "")) }
    var selectedType by remember { mutableStateOf(dueLog.type) }
    var note by remember { mutableStateOf(dueLog.note) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "লেনদেন সংশোধন / এডিট" else "Edit Ledger Entry",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (language == "bn") "ভুল হলে সঠিক তথ্য দিয়ে আপডেট করুন" else "Correct any erroneous entry details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Type selector: বাকি দেওয়া (+) vs জমা আদায় (-)
                Text(
                    text = if (language == "bn") "লেনদেনের ধরন:" else "Transaction Type:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "DUE_GIVEN",
                        onClick = { selectedType = "DUE_GIVEN" },
                        label = { Text(if (language == "bn") "বাকি দেওয়া (+)" else "Due Added (+)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFFEDD5),
                            selectedLabelColor = DueOrange
                        )
                    )
                    FilterChip(
                        selected = selectedType == "DUE_COLLECTED",
                        onClick = { selectedType = "DUE_COLLECTED" },
                        label = { Text(if (language == "bn") "জমা গ্রহণ (-)" else "Payment Received (-)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDCFCE7),
                            selectedLabelColor = ProfitGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (language == "bn") "টাকার পরিমাণ ($currency) *" else "Amount *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "বিবরণ / নোট" else "Note / Reason") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.background(Color(0xFFFEE2E2), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed)
                    }

                    Row {
                        TextButton(onClick = onDismiss) {
                            Text(if (language == "bn") "বাতিল" else "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amount = amountStr.toDoubleOrNull() ?: 0.0
                                if (amount > 0) {
                                    onSave(amount, selectedType, note)
                                }
                            }
                        ) {
                            Text(if (language == "bn") "আপডেট করুন" else "Update")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full Customer History Dialog: Shows Due Logs, POS Transactions, Direct Messaging and PDF sharing!
 */
@Composable
fun CustomerFullHistoryDialog(
    customer: Customer,
    dueLogs: List<DueLog>,
    transactions: List<TransactionRecord>,
    currency: String,
    language: String,
    shopName: String,
    onDismiss: () -> Unit,
    onEditLog: (DueLog) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Due Logs (খাতা), 1: POS Invoices (বিক্রয় রশিদ)

    val totalDuesGiven = remember(dueLogs) { dueLogs.filter { it.type == "DUE_GIVEN" }.sumOf { it.amount } }
    val totalDuesCollected = remember(dueLogs) { dueLogs.filter { it.type == "DUE_COLLECTED" }.sumOf { it.amount } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with Customer Photo & Due Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (customer.imageUri.isNotBlank()) {
                            AsyncImage(
                                model = customer.imageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, DueOrange, CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFFEDD5),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = customer.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DueOrange
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (customer.phone.isNotBlank()) {
                                Text(
                                    text = customer.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Ribbon: Total Due, Given, Collected
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF7ED),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (language == "bn") "মোট বকেয়া" else "Total Due", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9A3412))
                            Text("$currency${customer.totalDue.toIntOrNull() ?: customer.totalDue}", fontWeight = FontWeight.ExtraBold, color = DueOrange, style = MaterialTheme.typography.titleMedium)
                        }
                        VerticalDivider(modifier = Modifier.height(28.dp), color = Color(0xFFFED7AA))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (language == "bn") "মোট বাকি দেওয়া" else "Total Given", style = MaterialTheme.typography.labelSmall, color = LossRed)
                            Text("$currency${totalDuesGiven.toIntOrNull() ?: totalDuesGiven}", fontWeight = FontWeight.Bold, color = LossRed, style = MaterialTheme.typography.bodyMedium)
                        }
                        VerticalDivider(modifier = Modifier.height(28.dp), color = Color(0xFFFED7AA))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (language == "bn") "মোট আদায়/জমা" else "Collected", style = MaterialTheme.typography.labelSmall, color = ProfitGreen)
                            Text("$currency${totalDuesCollected.toIntOrNull() ?: totalDuesCollected}", fontWeight = FontWeight.Bold, color = ProfitGreen, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Bar: Messaging (SMS/WhatsApp) & PDF Statement
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Send Message Option
                    FilledTonalButton(
                        onClick = {
                            val msg = "শ্রদ্ধেয় ${customer.name}, $shopName এ আপনার মোট বাকি আছে $currency${customer.totalDue}। অনুগ্রহ করে দ্রুত পরিশোধ করার অনুরোধ রইল। ধন্যবাদ।"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, msg)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "মেসেজ / WhatsApp পাঠান"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFDCFCE7), contentColor = ProfitGreen),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Message, contentDescription = "SMS", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "bn") "মেসেজ পাঠান" else "Send SMS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    // Send / Download PDF Option
                    Button(
                        onClick = {
                            val pdf = PdfGenerator.generateCustomerDuePdf(
                                context = context,
                                shopName = shopName,
                                customer = customer,
                                history = dueLogs,
                                currency = currency
                            )
                            if (pdf != null) {
                                PdfGenerator.openOrSharePdf(
                                    context,
                                    pdf,
                                    if (language == "bn") "বাকি খাতার স্টেটমেন্ট PDF শেয়ার করুন" else "Share Due Statement PDF"
                                )
                            } else {
                                Toast.makeText(context, "PDF তৈরি করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "bn") "PDF স্টেটমেন্ট" else "Due PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: খাতার লেনদেন vs বিক্রয় চালান (POS)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (language == "bn") "বাকি খাতা রেকর্ড (${dueLogs.size})" else "Due Logs (${dueLogs.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (language == "bn") "বিক্রয় চালান (${transactions.size})" else "POS Invoices (${transactions.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content list
                if (selectedTab == 0) {
                    if (dueLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == "bn") "কোনো খাতা লেনদেন রেকর্ড পাওয়া যায়নি" else "No ledger records found",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(dueLogs, key = { it.id }) { log ->
                                val isCollected = log.type == "DUE_COLLECTED"
                                val dateStr = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(log.timestamp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isCollected) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, if (isCollected) Color(0xFFBBF7D0) else Color(0xFFFECACA)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (isCollected) (if (language == "bn") "বাকি আদায় / জমা" else "Payment Received") else (if (language == "bn") "বাকি প্রদান" else "Due Added"),
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCollected) ProfitGreen else LossRed,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = dateStr,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            if (log.note.isNotBlank()) {
                                                Text(
                                                    text = log.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${if (isCollected) "-" else "+"}$currency${log.amount.toIntOrNull() ?: log.amount}",
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isCollected) ProfitGreen else LossRed,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            // Edit Button to correct mistakes
                                            IconButton(
                                                onClick = { onEditLog(log) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Entry", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (transactions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == "bn") "এই কাস্টমারের কোনো বিক্রয় চালান পাওয়া যায়নি" else "No POS invoices found for customer",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(transactions, key = { it.id }) { tx ->
                                val dateStr = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = tx.productName.ifBlank { "বিক্রয় চালান #${tx.invoiceNumber}" },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${tx.quantity.toIntOrNull() ?: tx.quantity} ${tx.unit} • $dateStr",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                            if (tx.dueAmount > 0) {
                                                Text(
                                                    text = "বাকি: $currency${tx.dueAmount.toIntOrNull() ?: tx.dueAmount} | জমা: $currency${tx.paidAmount.toIntOrNull() ?: tx.paidAmount}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DueOrange,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$currency${tx.totalAmount.toIntOrNull() ?: tx.totalAmount}",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Text(
                                                text = tx.paymentMethod,
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
            }
        }
    }
}
