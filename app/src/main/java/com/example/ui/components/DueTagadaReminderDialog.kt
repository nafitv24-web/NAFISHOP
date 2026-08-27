package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Customer
import com.example.data.model.TransactionRecord
import com.example.ui.theme.*
import com.example.util.CalculationHelper.round2
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueTagadaReminderDialog(
    customers: List<Customer>,
    dueSales: List<TransactionRecord>,
    currency: String,
    language: String,
    shopName: String,
    onDismiss: () -> Unit,
    onCollectPayment: (Customer, Double, String) -> Unit,
    onNavigateToDueKhata: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Due Customers, 1: Due Memos / Invoices
    var searchQuery by remember { mutableStateOf("") }
    var collectingCustomer by remember { mutableStateOf<Customer?>(null) }

    // Filter customers with total due > 0
    val dueCustomers = remember(customers, searchQuery) {
        val list = customers.filter { it.totalDue > 0 }
        if (searchQuery.isBlank()) {
            list.sortedByDescending { it.totalDue }
        } else {
            list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true)
            }.sortedByDescending { it.totalDue }
        }
    }

    // Filter memo sales with dueAmount > 0
    val filteredDueSales = remember(dueSales, searchQuery) {
        if (searchQuery.isBlank()) {
            dueSales.sortedByDescending { it.timestamp }
        } else {
            dueSales.filter {
                it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.customerPhone.contains(searchQuery, ignoreCase = true) ||
                it.invoiceNumber.contains(searchQuery, ignoreCase = true)
            }.sortedByDescending { it.timestamp }
        }
    }

    val totalOverallCustomerDue = remember(customers) {
        customers.sumOf { it.totalDue.coerceAtLeast(0.0) }
    }

    val totalDueCustomersCount = remember(customers) {
        customers.count { it.totalDue > 0 }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. Header
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
                                .size(40.dp)
                                .background(Color(0xFFFFF7ED), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Sms,
                                contentDescription = null,
                                tint = DueOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "বাকি তাগাদা ও বকেয়া খাতা" else "Due Reminders & Ledger",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == "bn") "সকল বাকি কাস্টমার ও সর্বমোট বকেয়া" else "All due customers & total dues",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Summary Card showing Total Due across All Customers
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (language == "bn") "সর্বমোট বকেয়া বাকি" else "Total Outstanding Due",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currency${totalOverallCustomerDue.toIntOrNull() ?: String.format(Locale.US, "%.1f", totalOverallCustomerDue)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = LossRed
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.People, contentDescription = null, tint = DueOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == "bn") "$totalDueCustomersCount জন বাকি কাস্টমার" else "$totalDueCustomersCount Due Customers",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (language == "bn") "কাস্টমারের নাম বা ফোন দিয়ে খুঁজুন..." else "Search by customer name or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Tab Selector (বাকি কাস্টমার / বকেয়া মেমো)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = {}
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = if (language == "bn") "বাকি কাস্টমার (${dueCustomers.size})" else "Customers (${dueCustomers.size})",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = if (language == "bn") "বকেয়া মেমোসমূহ (${filteredDueSales.size})" else "Due Invoices (${filteredDueSales.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 5. Main Content Area
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // All Due Customers List
                        if (dueCustomers.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (language == "bn") "আলহামদুলিল্লাহ, কোনো কাস্টমারের কাছে বাকি নেই!" else "No pending customer dues!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (language == "bn") "সকল কাস্টমারের হিসাব পরিশোধিত।" else "All customer balances are clear.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(dueCustomers, key = { it.id }) { customer ->
                                    DueCustomerCard(
                                        customer = customer,
                                        currency = currency,
                                        language = language,
                                        shopName = shopName,
                                        onSendReminder = {
                                            val message = "শ্রদ্ধেয় ${customer.name}, $shopName এ আপনার সর্বমোট বকেয়া $currency${customer.totalDue.toIntOrNull() ?: customer.totalDue} টাকা বাকি রয়েছে। অনুগ্রহ করে দ্রুত পরিশোধের অনুরোধ রইল। ধন্যবাদ।"
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, message)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "তাগাদা পাঠান"))
                                        },
                                        onSendSmsDirect = {
                                            if (customer.phone.isNotBlank()) {
                                                val message = "শ্রদ্ধেয় ${customer.name}, $shopName এ আপনার সর্বমোট বকেয়া $currency${customer.totalDue.toIntOrNull() ?: customer.totalDue} টাকা বাকি রয়েছে। দ্রুত পরিশোধের বিনীত অনুরোধ রইল। ধন্যবাদ।"
                                                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("smsto:${customer.phone.trim()}")
                                                    putExtra("sms_body", message)
                                                }
                                                try {
                                                    context.startActivity(smsIntent)
                                                } catch (e: Exception) {
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, message)
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "তাগাদা পাঠান"))
                                                }
                                            } else {
                                                Toast.makeText(context, if (language == "bn") "কোনো ফোন নম্বর দেওয়া নেই" else "No phone number available", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onCallCustomer = {
                                            if (customer.phone.isNotBlank()) {
                                                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                                    data = Uri.parse("tel:${customer.phone.trim()}")
                                                }
                                                context.startActivity(callIntent)
                                            } else {
                                                Toast.makeText(context, if (language == "bn") "কোনো ফোন নম্বর দেওয়া নেই" else "No phone number available", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onCollectPaymentClick = {
                                            collectingCustomer = customer
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        // Due Memos List
                        if (filteredDueSales.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (language == "bn") "কোনো বকেয়া মেমো পাওয়া যায়নি!" else "No pending due memos found!",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredDueSales, key = { it.id }) { tx ->
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                        border = BorderStroke(1.dp, Color(0xFFFED7AA))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = tx.customerName.ifBlank { if (language == "bn") "নামহীন কাস্টমার" else "Unnamed Customer" },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (tx.customerPhone.isNotBlank()) {
                                                    Text(
                                                        text = tx.customerPhone,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "বকেয়া: $currency${tx.dueAmount.toIntOrNull() ?: tx.dueAmount} • মেমো #${tx.invoiceNumber.ifBlank { tx.id.toString() }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DueOrange
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    val message = "শ্রদ্ধেয় ${tx.customerName}, $shopName এ আপনার মেমো #${tx.invoiceNumber} বাবদ বকেয়া $currency${tx.dueAmount.toIntOrNull() ?: tx.dueAmount} টাকা বাকি আছে। দ্রুত পরিশোধের অনুরোধ রইল। ধন্যবাদ।"
                                                    val sendIntent = Intent().apply {
                                                        action = Intent.ACTION_SEND
                                                        putExtra(Intent.EXTRA_TEXT, message)
                                                        type = "text/plain"
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "তাগাদা পাঠান"))
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (language == "bn") "তাগাদা দিন" else "Remind", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

    // Quick Collect Payment Sub-Dialog
    collectingCustomer?.let { customer ->
        QuickCollectPaymentDialog(
            customer = customer,
            currency = currency,
            language = language,
            onDismiss = { collectingCustomer = null },
            onConfirmCollect = { amount, note ->
                onCollectPayment(customer, amount, note)
                collectingCustomer = null
                Toast.makeText(
                    context,
                    if (language == "bn") "${customer.name} এর কাছ থেকে $currency$amount জমা হয়েছে" else "Collected $currency$amount from ${customer.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
fun DueCustomerCard(
    customer: Customer,
    currency: String,
    language: String,
    shopName: String,
    onSendReminder: () -> Unit,
    onSendSmsDirect: () -> Unit,
    onCallCustomer: () -> Unit,
    onCollectPaymentClick: () -> Unit
) {
    val lastDateFormatted = remember(customer.lastTransactionDate) {
        if (customer.lastTransactionDate > 0) {
            SimpleDateFormat("d MMM yyyy", if (language == "bn") Locale("bn", "BD") else Locale.ENGLISH).format(Date(customer.lastTransactionDate))
        } else ""
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
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
                            .size(42.dp)
                            .background(Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.firstOrNull()?.uppercase() ?: "C",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleSmall,
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
                        if (lastDateFormatted.isNotBlank()) {
                            Text(
                                text = "${if (language == "bn") "সর্বশেষ লেনদেন:" else "Last Tx:"} $lastDateFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Due Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (language == "bn") "মোট বকেয়া" else "Total Due",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = LossRed,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$currency${customer.totalDue.toIntOrNull() ?: String.format(Locale.US, "%.1f", customer.totalDue)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = LossRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Direct Call
                if (customer.phone.isNotBlank()) {
                    OutlinedButton(
                        onClick = onCallCustomer,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = ProfitGreen, modifier = Modifier.size(15.dp))
                    }
                }

                // 2. Send SMS / Tagada Button
                Button(
                    onClick = onSendSmsDirect,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "bn") "তাগাদা পাঠান" else "Send SMS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // 3. Collect Due Button
                Button(
                    onClick = onCollectPaymentClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (language == "bn") "জমা নিন" else "Collect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun QuickCollectPaymentDialog(
    customer: Customer,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onConfirmCollect: (amount: Double, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf(customer.totalDue.toIntOrNull()?.toString() ?: customer.totalDue.toString()) }
    var note by remember { mutableStateOf(if (language == "bn") "বাকি আদায়" else "Due Payment Received") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (language == "bn") "বাকি টাকা জমা নিন" else "Collect Due Payment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${customer.name} (${if (language == "bn") "বর্তমান বাকি:" else "Due:"} $currency${customer.totalDue.toIntOrNull() ?: customer.totalDue})",
                            style = MaterialTheme.typography.bodySmall,
                            color = LossRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text(if (language == "bn") "জমার পরিমাণ ($currency) *" else "Amount ($currency) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick presets
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SuggestionChip(
                        onClick = {
                            amountStr = customer.totalDue.toIntOrNull()?.toString() ?: customer.totalDue.toString()
                        },
                        label = { Text(if (language == "bn") "সম্পূর্ণ বাকি ($currency${customer.totalDue.toIntOrNull() ?: customer.totalDue})" else "Full Due", fontSize = 11.sp) }
                    )
                    val halfDue = round2(customer.totalDue / 2.0)
                    if (halfDue > 0) {
                        SuggestionChip(
                            onClick = {
                                amountStr = halfDue.toIntOrNull()?.toString() ?: halfDue.toString()
                            },
                            label = { Text(if (language == "bn") "অর্ধেক ($currency$halfDue)" else "50%", fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "নোট / মন্তব্য" else "Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (amount > 0) {
                            onConfirmCollect(amount, note)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == "bn") "জমা নিশ্চিত করুন" else "Confirm Payment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
