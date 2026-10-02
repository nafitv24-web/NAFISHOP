package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TailorOrder
import com.example.ui.components.AddEditTailorOrderDialog
import com.example.ui.components.TailorMeasurementSlipDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TailorKhataScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val tailorOrders by viewModel.tailorOrders.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val currency = shopInfo.currency

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PROGRESS, READY, DELIVERED, DUE

    var showAddDialog by remember { mutableStateOf(false) }
    var orderToEdit by remember { mutableStateOf<TailorOrder?>(null) }
    var orderForSlip by remember { mutableStateOf<TailorOrder?>(null) }
    var orderForPayment by remember { mutableStateOf<TailorOrder?>(null) }
    var orderToDelete by remember { mutableStateOf<TailorOrder?>(null) }

    // Summary calculations
    val totalOrdersCount = tailorOrders.size
    val inProgressCount = tailorOrders.count { it.status in listOf("RECEIVED", "CUTTING", "STITCHING") }
    val readyCount = tailorOrders.count { it.status == "READY" }
    val totalDueSum = tailorOrders.sumOf { it.dueAmount }

    // Filter & Search
    val filteredOrders = remember(tailorOrders, searchQuery, selectedFilter) {
        tailorOrders.filter { order ->
            val matchQuery = searchQuery.isBlank() ||
                    order.customerName.contains(searchQuery, ignoreCase = true) ||
                    order.customerPhone.contains(searchQuery, ignoreCase = true) ||
                    order.orderNumber.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "PROGRESS" -> order.status in listOf("RECEIVED", "CUTTING", "STITCHING")
                "READY" -> order.status == "READY"
                "DELIVERED" -> order.status == "DELIVERED"
                "DUE" -> order.dueAmount > 0
                else -> true
            }

            matchQuery && matchFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("নতুন জামার মাপ ও অর্ডার", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header stats banner
            TailorStatsBanner(
                totalOrders = totalOrdersCount,
                inProgress = inProgressCount,
                ready = readyCount,
                totalDue = totalDueSum,
                currency = currency
            )

            // Search Bar & Filter Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("কাস্টমারের নাম, ফোন বা রসিদ নং দিয়ে খুঁজুন...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterItems = listOf(
                        "ALL" to "সব ($totalOrdersCount)",
                        "PROGRESS" to "চলমান সেলাই ($inProgressCount)",
                        "READY" to "রেডি ($readyCount)",
                        "DUE" to "বাকি বিল",
                        "DELIVERED" to "ডেলিভার্ড"
                    )
                    items(filterItems) { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }

            // Order List
            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "কোনো অর্ডার পাওয়া যায়নি" else "এখনো কোনো দর্জি অর্ডার লেখা হয়নি",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "কামিজ, সেলোয়ার, বাচ্চাদের ফ্রক, পেটিকোট, ব্লাউজ ইত্যাদি জামার মাপ লিখে রাখতে নিচের বাটনে চাপ দিন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("প্রথম অর্ডার ও মাপ লিখুন")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        TailorOrderCard(
                            order = order,
                            currency = currency,
                            onViewSlip = { orderForSlip = order },
                            onEdit = { orderToEdit = order },
                            onCollectDue = { orderForPayment = order },
                            onChangeStatus = { newStatus ->
                                viewModel.updateTailorOrderStatus(order.id, newStatus)
                            },
                            onDelete = { orderToDelete = order }
                        )
                    }
                }
            }
        }
    }

    // Add Order Dialog
    if (showAddDialog) {
        AddEditTailorOrderDialog(
            initialOrder = null,
            currency = currency,
            onDismiss = { showAddDialog = false },
            onSave = { newOrder ->
                viewModel.saveTailorOrder(newOrder) {
                    showAddDialog = false
                    Toast.makeText(context, "দর্জি অর্ডার সফলভাবে সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Edit Order Dialog
    if (orderToEdit != null) {
        AddEditTailorOrderDialog(
            initialOrder = orderToEdit,
            currency = currency,
            onDismiss = { orderToEdit = null },
            onSave = { updatedOrder ->
                viewModel.saveTailorOrder(updatedOrder) {
                    orderToEdit = null
                    Toast.makeText(context, "অর্ডার আপডেট হয়েছে", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Slip / Measurement Dialog
    if (orderForSlip != null) {
        TailorMeasurementSlipDialog(
            order = orderForSlip!!,
            shopName = shopInfo.shopName,
            shopPhone = shopInfo.phone,
            currency = currency,
            onDismiss = { orderForSlip = null }
        )
    }

    // Due Payment Collection Dialog
    if (orderForPayment != null) {
        DuePaymentDialog(
            order = orderForPayment!!,
            currency = currency,
            onDismiss = { orderForPayment = null },
            onConfirm = { amount ->
                viewModel.collectTailorPayment(orderForPayment!!, amount)
                orderForPayment = null
                Toast.makeText(context, "বাকি টাকা আদায় সম্পন্ন হয়েছে", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete Confirmation Dialog
    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("অর্ডার ডিলিট করবেন?") },
            text = { Text("কাস্টমার ${orderToDelete!!.customerName}-এর অর্ডার (${orderToDelete!!.orderNumber}) মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTailorOrder(orderToDelete!!)
                        orderToDelete = null
                        Toast.makeText(context, "অর্ডার ডিলিট হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = LossRed)
                ) {
                    Text("হ্যাঁ, ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

/**
 * Top Statistics Header Banner
 */
@Composable
private fun TailorStatsBanner(
    totalOrders: Int,
    inProgress: Int,
    ready: Int,
    totalDue: Double,
    currency: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Checkroom,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "দর্জি ও জামার মাপ খাতা",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = EmeraldPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "মোট $totalOrders টি অর্ডার",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // In progress
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$inProgress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AmberTertiary
                    )
                    Text("চলমান কাজ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Ready
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = "$ready",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                    Text("রেডি / তৈরি", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Total Due
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = "$currency${totalDue.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (totalDue > 0) LossRed else EmeraldPrimary
                    )
                    Text("মোট বাকি বিল", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * Individual Order Item Card
 */
@Composable
private fun TailorOrderCard(
    order: TailorOrder,
    currency: String,
    onViewSlip: () -> Unit,
    onEdit: () -> Unit,
    onCollectDue: () -> Unit,
    onChangeStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val garments = order.parseGarments()

    val isOverdue = order.deliveryDate < System.currentTimeMillis() && order.status != "DELIVERED"
    val isToday = remember(order.deliveryDate) {
        val calNow = Calendar.getInstance()
        val calDel = Calendar.getInstance().apply { timeInMillis = order.deliveryDate }
        calNow.get(Calendar.YEAR) == calDel.get(Calendar.YEAR) && calNow.get(Calendar.DAY_OF_YEAR) == calDel.get(Calendar.DAY_OF_YEAR)
    }

    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (isOverdue) LossRed.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Customer Name & Order # & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = order.customerName.take(1),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = order.customerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = order.orderNumber.ifBlank { "T-${order.id}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (order.customerPhone.isNotBlank()) {
                                Text(
                                    text = " • ${order.customerPhone}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Status Button with Dropdown
                Box {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when (order.status.uppercase()) {
                            "READY" -> EmeraldPrimary.copy(alpha = 0.15f)
                            "DELIVERED" -> Color.Gray.copy(alpha = 0.15f)
                            "CUTTING" -> AmberTertiary.copy(alpha = 0.15f)
                            "STITCHING" -> TealDarkHeader.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.clickable { showStatusMenu = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = order.getStatusBengali(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (order.status.uppercase()) {
                                    "READY" -> EmeraldPrimary
                                    "DELIVERED" -> Color.Gray
                                    "CUTTING" -> AmberTertiary
                                    "STITCHING" -> TealDarkHeader
                                    else -> EmeraldPrimary
                                }
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        listOf(
                            "RECEIVED" to "অর্ডার নেওয়া হয়েছে",
                            "CUTTING" to "কাটিং হচ্ছে",
                            "STITCHING" to "সেলাই চলছে",
                            "READY" to "ডেলিভারির জন্য তৈরি (রেডি)",
                            "DELIVERED" to "ডেলিভারি সম্পন্ন"
                        ).forEach { (statusKey, statusLabel) ->
                            DropdownMenuItem(
                                text = { Text(statusLabel) },
                                onClick = {
                                    onChangeStatus(statusKey)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Garments List Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (garments.isNotEmpty()) {
                    garments.take(4).forEach { g ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = "${g.garmentName} (${g.quantity})",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (garments.size > 4) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "+${garments.size - 4}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Text(
                        text = "জামার মাপ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Delivery Date & Financials Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Delivery date badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isOverdue) LossRed else if (isToday) AmberTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isToday) "আজ ডেলিভারি!" else if (isOverdue) "ডেলিভারি বাকি!" else "ডেলিভারি: ${dateFormat.format(Date(order.deliveryDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday || isOverdue) FontWeight.Bold else FontWeight.Normal,
                        color = if (isOverdue) LossRed else if (isToday) AmberTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Billing breakdown: মোট ৳ | জমা ৳ | বাকি ৳
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "বিল: $currency${order.totalAmount.toInt()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (order.dueAmount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = LossRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "বাকি: $currency${order.dueAmount.toInt()}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = LossRed
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "পরিশোধ",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            // Action Buttons Row: View Slip, Edit, Collect Due, Call, WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // View Slip
                    FilledTonalButton(
                        onClick = onViewSlip,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                            contentColor = EmeraldPrimary
                        )
                    ) {
                        Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("মাপ ও রসিদ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Edit
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    }

                    // Collect Due (if due > 0)
                    if (order.dueAmount > 0) {
                        FilledTonalButton(
                            onClick = onCollectDue,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = LossRed.copy(alpha = 0.12f),
                                contentColor = LossRed
                            )
                        ) {
                            Text("বাকি জমা", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Call
                    if (order.customerPhone.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${order.customerPhone}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Delete
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

/**
 * Quick Collect Due Dialog
 */
@Composable
private fun DuePaymentDialog(
    order: TailorOrder,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(order.dueAmount.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("দর্জি বাকি আদায়") },
        text = {
            Column {
                Text("কাস্টমার: ${order.customerName}")
                Text("মোট বাকি: $currency${order.dueAmount.toInt()}", color = LossRed, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("জমা পরিমাণ ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("জমা নিন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
