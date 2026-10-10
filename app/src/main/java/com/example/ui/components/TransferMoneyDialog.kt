package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Customer
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.CalculationHelper
import com.example.util.CustomerSmsHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * টাকা হস্তান্তর (Money Transfer & Loan Giving) Screen Dialog
 * Matches the user's requested layout:
 * - Header: ← হস্তান্তর
 * - পরিমাণ (Amount) with Calculator shortcut
 * - থেকে: (From Account) Dropdown
 * - প্রতি: (To Account / Customer Loan) Dropdown
 * - Date & Time selectors
 * - Note input with Mic / Camera
 * - Big Blue "হস্তান্তর" button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferMoneyDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit,
    preselectedCustomer: Customer? = null
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val currency = shopInfo.currency
    val customers by viewModel.customers.collectAsState()

    var amountStr by remember { mutableStateOf("") }
    var selectedFromAccount by remember { mutableStateOf(if (language == "bn") "দোকানের মেইন ব্যালেন্স" else "Shop Main Balance") }
    var selectedToAccount by remember { mutableStateOf(if (language == "bn") "কাস্টমার / ব্যক্তিকে কর্জ প্রদান (ধার)" else "Customer Loan (Credit)") }

    var expandedFromDropdown by remember { mutableStateOf(false) }
    var expandedToDropdown by remember { mutableStateOf(false) }

    // Target person / customer info when loan is selected
    var borrowerName by remember { mutableStateOf(preselectedCustomer?.name ?: "") }
    var borrowerPhone by remember { mutableStateOf(preselectedCustomer?.phone ?: "") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(preselectedCustomer) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var customerSearchQuery by remember { mutableStateOf("") }

    var selectedTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf("") }
    var showQuickCalc by remember { mutableStateOf(false) }

    val fromOptions = listOf(
        if (language == "bn") "দোকানের মেইন ব্যালেন্স" else "Shop Main Balance",
        if (language == "bn") "ব্যাংক অ্যাকাউন্ট" else "Bank Account",
        if (language == "bn") "বিকাশ / নগদ (MFS)" else "bKash / Nagad",
        if (language == "bn") "মালিকের ব্যক্তিগত তহবিল" else "Personal Fund"
    )

    val toOptions = listOf(
        if (language == "bn") "কাস্টমার / ব্যক্তিকে কর্জ প্রদান (ধার)" else "Customer Loan (Credit)",
        if (language == "bn") "ব্যাংক অ্যাকাউন্টে জমা" else "Deposit to Bank",
        if (language == "bn") "বিকাশ / নগদ অ্যাকাউন্টে" else "Transfer to MFS",
        if (language == "bn") "দোকানের ক্যাশ ড্রয়ার" else "Cash Drawer",
        if (language == "bn") "মালিকের ব্যক্তিগত উত্তোলন" else "Owner Drawing"
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val compressed = com.example.util.ImageStorageHelper.saveCompressedImage(context, uri, "transfer")
            attachedImageUri = compressed ?: uri.toString()
            Toast.makeText(context, if (language == "bn") "রশিদের ছবি যুক্ত হয়েছে" else "Bill photo attached", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF121824) // Sleek dark surface matching screenshot
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top App Bar: ← হস্তান্তর
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "bn") "হস্তান্তর" else "Transfer",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Field 1: পরিমাণ (Amount) with Calculator Shortcut
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text(if (language == "bn") "পরিমাণ" else "Amount", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("0.00", color = Color(0xFF64748B)) },
                        leadingIcon = {
                            Text(currency, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(start = 12.dp))
                        },
                        trailingIcon = {
                            IconButton(onClick = { showQuickCalc = !showQuickCalc }) {
                                Icon(
                                    Icons.Default.Calculate,
                                    contentDescription = "Calculator",
                                    tint = if (showQuickCalc) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick Calculator popup
                    if (showQuickCalc) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (language == "bn") "দ্রুত টাকার পরিমাণ নির্বাচন করুন:" else "Quick select amount:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(100, 500, 1000, 2000, 5000).forEach { quickAmt ->
                                        FilledTonalButton(
                                            onClick = {
                                                val current = amountStr.toDoubleOrNull() ?: 0.0
                                                amountStr = (current + quickAmt).toInt().toString()
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF334155), contentColor = Color.White)
                                        ) {
                                            Text("+$quickAmt", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Field 2: থেকে: (From Account)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedFromDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (language == "bn") "থেকে:" else "From:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedFromAccount,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    if (selectedFromAccount.contains("মেইন ব্যালেন্স") || selectedFromAccount.contains("Main Balance")) {
                                        Text(
                                            text = "${if (language == "bn") "বর্তমান ক্যাশ:" else "Balance:"} $currency${CalculationHelper.formatAmount(shopInfo.mainBalance)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4ADE80),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expandedFromDropdown,
                            onDismissRequest = { expandedFromDropdown = false }
                        ) {
                            fromOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        selectedFromAccount = opt
                                        expandedFromDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Field 3: প্রতি: (To Account / Loan)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedToDropdown = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (language == "bn") "প্রতি:" else "To:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedToAccount,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expandedToDropdown,
                            onDismissRequest = { expandedToDropdown = false }
                        ) {
                            toOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        selectedToAccount = opt
                                        expandedToDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Target Borrower / Customer Section (when "কর্জ প্রদান" is active)
                    val isLoanTarget = selectedToAccount.contains("কর্জ") || selectedToAccount.contains("Loan")
                    if (isLoanTarget) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF60A5FA),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (language == "bn") "কর্জ গ্রহীতা (বাকি কাস্টমার খাতা)" else "Borrower Info (Due Khata)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF93C5FD)
                                        )
                                    }

                                    TextButton(
                                        onClick = { showCustomerPicker = !showCustomerPicker },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (language == "bn") "কাস্টমার নির্বাচন" else "Pick Customer",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                if (showCustomerPicker) {
                                    OutlinedTextField(
                                        value = customerSearchQuery,
                                        onValueChange = { customerSearchQuery = it },
                                        placeholder = { Text(if (language == "bn") "কাস্টমার খুঁজুন..." else "Search customer...", fontSize = 12.sp) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(6.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF0F172A),
                                            unfocusedContainerColor = Color(0xFF0F172A)
                                        )
                                    )

                                    val filteredPickList = customers.filter {
                                        customerSearchQuery.isBlank() ||
                                        it.name.contains(customerSearchQuery, ignoreCase = true) ||
                                        it.phone.contains(customerSearchQuery, ignoreCase = true)
                                    }.take(5)

                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        filteredPickList.forEach { c ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        selectedCustomer = c
                                                        borrowerName = c.name
                                                        borrowerPhone = c.phone
                                                        showCustomerPicker = false
                                                    }
                                                    .padding(vertical = 2.dp),
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF334155).copy(alpha = 0.5f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(c.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(
                                                        "${if (c.totalDue > 0) "বাকি:" else "ব্যালেন্স:"} $currency${CalculationHelper.formatAmount(c.totalDue)}",
                                                        color = if (c.totalDue > 0) DueOrange else ProfitGreen,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = borrowerName,
                                    onValueChange = {
                                        borrowerName = it
                                        if (selectedCustomer != null && selectedCustomer!!.name != it) {
                                            selectedCustomer = null
                                        }
                                    },
                                    label = { Text(if (language == "bn") "কর্জ গ্রহীতার নাম *" else "Borrower Name *", color = Color(0xFF94A3B8)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = borrowerPhone,
                                    onValueChange = { borrowerPhone = it },
                                    label = { Text(if (language == "bn") "মোবাইল নম্বর [ঐচ্ছিক]" else "Phone Number [Optional]", color = Color(0xFF94A3B8)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Field 4: Date & Time Selectors Row
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                    val dateStr = formatBengaliOrEnglishDate(selectedTimestamp, language)
                    val timeStr = formatBengaliOrEnglishTime(selectedTimestamp, language)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Box
                        Surface(
                            modifier = Modifier
                                .weight(1.3f)
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            cal.set(Calendar.YEAR, y)
                                            cal.set(Calendar.MONTH, m)
                                            cal.set(Calendar.DAY_OF_MONTH, d)
                                            selectedTimestamp = cal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                cal.add(Calendar.DAY_OF_YEAR, -1)
                                                selectedTimestamp = cal.timeInMillis
                                            }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                                selectedTimestamp = cal.timeInMillis
                                            }
                                    )
                                }
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }
                        }

                        // Time Box
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            cal.set(Calendar.HOUR_OF_DAY, hour)
                                            cal.set(Calendar.MINUTE, minute)
                                            selectedTimestamp = cal.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Field 5: Note Input with Mic & Camera
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        placeholder = {
                            Text(
                                text = if (language == "bn") "এখানে নোট লিখুন [ঐচ্ছিক]" else "Write note here [Optional]",
                                color = Color(0xFF64748B)
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Mic, contentDescription = "Voice note", tint = Color(0xFF94A3B8))
                        },
                        trailingIcon = {
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = "Attach bill",
                                    tint = if (attachedImageUri.isNotBlank()) Color(0xFF4ADE80) else Color(0xFF94A3B8)
                                )
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Field 6: Big Blue "হস্তান্তর" Button
                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull() ?: 0.0
                            if (amount <= 0) {
                                Toast.makeText(context, if (language == "bn") "অনুগ্রহ করে সঠিক পরিমাণ লিখুন" else "Enter valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            if (isLoanTarget && borrowerName.isBlank()) {
                                Toast.makeText(context, if (language == "bn") "কর্জ গ্রহীতার নাম লিখুন" else "Enter borrower name", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Perform transfer
                            val cleanName = borrowerName.trim()
                            val cleanPhone = borrowerPhone.trim()
                            val transferNote = note.trim()

                            val isFromShopCash = selectedFromAccount.contains("মেইন ব্যালেন্স") || selectedFromAccount.contains("Main Balance")

                            // 1. Withdraw from shop cash if source is shop main balance
                            if (isFromShopCash) {
                                val reason = if (isLoanTarget) {
                                    "কর্জ হস্তান্তর ($cleanName): ${transferNote.ifBlank { "নগদ কর্জ প্রদান" }}"
                                } else {
                                    "হস্তান্তর ($selectedToAccount): $transferNote"
                                }
                                viewModel.withdrawCashFromMainBalance(amount, reason, selectedTimestamp)
                            }

                            // 2. If destination is loan to person / customer
                            if (isLoanTarget) {
                                val fullDueNote = if (transferNote.isNotBlank()) "নগদ কর্জ / ধার প্রদান • $transferNote" else "নগদ কর্জ / ধার প্রদান"

                                val existingCust = selectedCustomer ?: customers.find {
                                    (cleanPhone.isNotBlank() && it.phone.trim() == cleanPhone) ||
                                    (cleanName.isNotBlank() && it.name.trim().equals(cleanName, ignoreCase = true))
                                }

                                if (existingCust != null) {
                                    viewModel.giveCustomerDue(
                                        customer = existingCust,
                                        amountDue = amount,
                                        note = fullDueNote,
                                        selectedProducts = emptyList(),
                                        customTimestamp = selectedTimestamp
                                    )
                                } else {
                                    viewModel.addCustomer(
                                        name = cleanName,
                                        phone = cleanPhone,
                                        address = "কর্জ গ্রহীতা",
                                        initialDue = amount
                                    )
                                }

                                // Optionally send SMS
                                if (cleanPhone.isNotBlank()) {
                                    val smsMsg = "শ্রদ্ধেয় $cleanName, ${shopInfo.shopName} থেকে আপনি $currency${amount.toIntOrNull() ?: amount} টাকা নগদ কর্জ/ধার গ্রহণ করেছেন। তারিখ: $dateStr।\n-------------------------\n${CustomerSmsHelper.SPONSOR_FOOTER}"
                                    CustomerSmsHelper.sendDirectSms(context, cleanPhone, smsMsg)
                                }
                            } else if (selectedToAccount.contains("মেইন ব্যালেন্স") || selectedToAccount.contains("Main Balance")) {
                                // Transfer into shop cash
                                viewModel.addCashToMainBalance(amount, "হস্তান্তর প্রাপ্তি ($selectedFromAccount): $transferNote", selectedTimestamp)
                            }

                            Toast.makeText(
                                context,
                                if (language == "bn") "৳${amount.toIntOrNull() ?: amount} টাকা সফলভাবে হস্তান্তর ও কর্জ হিসেবে লিপিবদ্ধ হয়েছে!" else "Transfer of $currency$amount completed successfully!",
                                Toast.LENGTH_LONG
                            ).show()

                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)) // Exact bright blue from screenshot
                    ) {
                        Text(
                            text = if (language == "bn") "হস্তান্তর" else "Transfer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun formatBengaliOrEnglishDate(timestamp: Long, language: String): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val month = cal.get(Calendar.MONTH)
    val year = cal.get(Calendar.YEAR)

    return if (language == "bn") {
        val monthsBn = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন", "জুলাই", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
        val banglaDigits = mapOf('0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪', '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯')
        fun toBn(s: String) = s.map { banglaDigits[it] ?: it }.joinToString("")
        val dayStr = if (day < 10) "০${toBn(day.toString())}" else toBn(day.toString())
        "$dayStr-${monthsBn.getOrElse(month) { "" }}-${toBn(year.toString())}"
    } else {
        val sdf = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
        sdf.format(Date(timestamp))
    }
}

private fun formatBengaliOrEnglishTime(timestamp: Long, language: String): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val hour = cal.get(Calendar.HOUR)
    val displayHour = if (hour == 0) 12 else hour
    val minute = cal.get(Calendar.MINUTE)
    val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"

    return if (language == "bn") {
        val banglaDigits = mapOf('0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪', '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯')
        fun toBn(s: String) = s.map { banglaDigits[it] ?: it }.joinToString("")
        val hStr = if (displayHour < 10) "০${toBn(displayHour.toString())}" else toBn(displayHour.toString())
        val mStr = if (minute < 10) "০${toBn(minute.toString())}" else toBn(minute.toString())
        "$hStr:$mStr $amPm"
    } else {
        val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
        sdf.format(Date(timestamp))
    }
}
