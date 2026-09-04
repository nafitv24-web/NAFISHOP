package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.PendingOrder
import com.example.data.model.PendingOrderItem
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog displayed when user opens Stock-In:
 * 1. Allows direct single item Stock-In or Viewing Waiting/Pending Orders
 * 2. In Pending Orders list, user can select an order, review all items, edit received quantities & buy prices,
 *    mark missing items as "পাওয়া যায়নি" (NOT FOUND), and finalize stock-in.
 * 3. Upon receiving, prints/shares the final Stock-In Receipt PDF with "পাওয়া যায়নি" marked for missing items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingOrderReceiveDialog(
    order: PendingOrder,
    currency: String,
    language: String,
    shopName: String,
    onDismiss: () -> Unit,
    onConfirmReceive: (List<PendingOrderItem>) -> Unit,
    onDeleteOrder: (() -> Unit)? = null
) {
    val context = LocalContext.current

    // Local mutable state for the items being received
    var editableItems by remember(order) {
        mutableStateOf(
            order.items.map { item ->
                item.copy(
                    receivedQuantity = if (item.isNotFound) 0.0 else if (item.receivedQuantity > 0.0) item.receivedQuantity else item.orderedQuantity
                )
            }
        )
    }

    var showConfirmDialog by remember { mutableStateOf(false) }

    val receivedCount = editableItems.count { !it.isNotFound && it.receivedQuantity > 0 }
    val notFoundCount = editableItems.count { it.isNotFound || it.receivedQuantity <= 0 }
    val totalReceivedPcs = editableItems.filter { !it.isNotFound && it.receivedQuantity > 0 }.sumOf { it.receivedQuantity }
    val totalReceivedCost = editableItems.filter { !it.isNotFound && it.receivedQuantity > 0 }.sumOf { it.receivedQuantity * it.buyPrice }

    val orderDateFormatted = remember(order.timestamp) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top App Bar / Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "অর্ডার স্টক-ইন যাচাই" else "Receive Order Stock-In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "অর্ডার #${order.orderNumber} • $orderDateFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (onDeleteOrder != null && order.status != "RECEIVED") {
                        IconButton(onClick = onDeleteOrder) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Order", tint = LossRed)
                        }
                    }
                }

                // Supplier & Note Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        if (order.supplierName.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${if (language == "bn") "মহাজন/ডিলার: " else "Supplier: "}${order.supplierName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (order.orderNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${if (language == "bn") "অর্ডার নোট: " else "Note: "}${order.orderNote}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Instructions
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == "bn") "মাল হাতে পাওয়ার পর পরিমাণ ও দর মিলিয়ে নিন। যে পণ্য না আসবে 'পাওয়া যায়নি' বাটনে চাপ দিন।" else "Review received quantity and buy price. Mark missing items as 'Not Found'.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }
                }

                // Summary Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF86EFAC))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (language == "bn") "প্রাপ্ত পণ্য" else "Received", style = MaterialTheme.typography.labelSmall, color = Color(0xFF15803D))
                            Text("$receivedCount টি (${totalReceivedPcs.toIntOrNull() ?: totalReceivedPcs} পিছ)", fontWeight = FontWeight.Bold, color = Color(0xFF166534), fontSize = 13.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = if (notFoundCount > 0) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (notFoundCount > 0) Color(0xFFFECACA) else Color.Transparent)
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (language == "bn") "পাওয়া যায়নি" else "Not Found", style = MaterialTheme.typography.labelSmall, color = if (notFoundCount > 0) LossRed else MaterialTheme.colorScheme.outline)
                            Text("$notFoundCount টি", fontWeight = FontWeight.Bold, color = if (notFoundCount > 0) LossRed else MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (language == "bn") "মোট ক্রয় বিল" else "Total Bill", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1D4ED8))
                            val totalCostStr = if (totalReceivedCost % 1.0 == 0.0) totalReceivedCost.toInt().toString() else "%.1f".format(totalReceivedCost)
                            Text("$currency$totalCostStr", fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), fontSize = 13.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Item List with Editable Qty, Buy Price and "Not Found" toggle
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(editableItems, key = { it.productId }) { item ->
                        ReceiveItemRow(
                            item = item,
                            currency = currency,
                            language = language,
                            isAlreadyReceived = order.status == "RECEIVED",
                            onItemChange = { updated ->
                                editableItems = editableItems.map { if (it.productId == updated.productId) updated else it }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (language == "bn") "বন্ধ করুন" else "Close")
                    }

                    // PDF Button (Generate Received Memo PDF)
                    FilledTonalButton(
                        onClick = {
                            val tempOrder = order.copy(items = editableItems, receivedTimestamp = System.currentTimeMillis())
                            val pdfFile = PdfGenerator.generateReceivedOrderPdf(
                                context = context,
                                order = tempOrder,
                                shopName = shopName,
                                currency = currency
                            )
                            if (pdfFile != null) {
                                PdfGenerator.openOrSharePdf(
                                    context = context,
                                    file = pdfFile,
                                    chooserTitle = if (language == "bn") "প্রাপ্তি চালান PDF ডাউনলোড / শেয়ার" else "Receipt Memo PDF"
                                )
                            } else {
                                Toast.makeText(context, "PDF তৈরি করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "চালান PDF" else "Memo PDF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (order.status != "RECEIVED") {
                        Button(
                            onClick = {
                                if (receivedCount == 0) {
                                    Toast.makeText(
                                        context,
                                        if (language == "bn") "কোনো পণ্য প্রাপ্ত হয়নি! স্টক-ইন করার মতো কিছু নেই।" else "No received items to stock in!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                showConfirmDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == "bn") "স্টক-ইন চূড়ান্ত করুন" else "Confirm Stock-In",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog before final stock-in
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = if (language == "bn") "স্টক-ইন চূড়ান্ত নিশ্চিতকরণ" else "Confirm Stock-In",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (language == "bn") {
                            "প্রাপ্ত $receivedCount টি পণ্যের মোট ${totalReceivedPcs.toIntOrNull() ?: totalReceivedPcs} পিছ স্টক ইনভেন্টরিতে যোগ করা হবে।"
                        } else {
                            "Receive ${totalReceivedPcs.toIntOrNull() ?: totalReceivedPcs} pcs of $receivedCount items into inventory."
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    if (notFoundCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (language == "bn") {
                                "⚠️ $notFoundCount টি পণ্য 'পাওয়া যায়নি' হিসেবে চিহ্নিত থাকবে এবং এগুলো স্টক-ইন হবে না।"
                            } else {
                                "⚠️ $notFoundCount missing items will NOT be stocked in."
                            },
                            color = LossRed,
                            fontSize = 12.sp
                        )
                    }
                    val costStr = if (totalReceivedCost % 1.0 == 0.0) totalReceivedCost.toInt().toString() else "%.2f".format(totalReceivedCost)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${if (language == "bn") "মোট ক্রয় খরচ: " else "Total Buy Cost: "}$currency$costStr",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onConfirmReceive(editableItems)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == "bn") "হ্যাঁ, স্টক-ইন করুন" else "Yes, Stock In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(if (language == "bn") "ফিরে যান" else "Back")
                }
            }
        )
    }
}

@Composable
private fun ReceiveItemRow(
    item: PendingOrderItem,
    currency: String,
    language: String,
    isAlreadyReceived: Boolean,
    onItemChange: (PendingOrderItem) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    val isMissing = item.isNotFound || item.receivedQuantity <= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMissing) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isMissing) Color(0xFFFECACA) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header: Product Name + "পাওয়া যায়নি" (Not Found) Toggle Switch / Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (isMissing) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (isMissing) LossRed else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.productBarcode.isNotBlank()) {
                        Text(
                            text = "বারকোড: ${item.productBarcode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (!isAlreadyReceived) {
                    // "Not Found" / "পাওয়া যায়নি" Pill Button
                    FilterChip(
                        selected = isMissing,
                        onClick = {
                            if (isMissing) {
                                // Toggle back to found
                                onItemChange(
                                    item.copy(
                                        isNotFound = false,
                                        receivedQuantity = if (item.receivedQuantity > 0) item.receivedQuantity else item.orderedQuantity
                                    )
                                )
                            } else {
                                // Mark as not found
                                onItemChange(item.copy(isNotFound = true, receivedQuantity = 0.0))
                            }
                        },
                        label = {
                            Text(
                                text = if (isMissing) {
                                    if (language == "bn") "পাওয়া যায়নি ✕" else "Not Found ✕"
                                } else {
                                    if (language == "bn") "প্রাপ্ত হয়েছে ✓" else "Received ✓"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEE2E2),
                            selectedLabelColor = LossRed,
                            containerColor = Color(0xFFDCFCE7),
                            labelColor = Color(0xFF15803D)
                        ),
                        border = BorderStroke(1.dp, if (isMissing) Color(0xFFFCA5A5) else Color(0xFF86EFAC)),
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isMissing) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                    ) {
                        Text(
                            text = if (isMissing) (if (language == "bn") "পাওয়া যায়নি" else "Not Found") else (if (language == "bn") "স্টক ইন সম্পন্ন" else "Received"),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMissing) LossRed else Color(0xFF15803D),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Details & Edit Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isMissing) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ordered vs Received info
                Column {
                    Text(
                        text = "${if (language == "bn") "অর্ডার: " else "Ordered: "}${item.orderedQuantity.toIntOrNull() ?: item.orderedQuantity} ${item.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (isMissing) {
                        Text(
                            text = if (language == "bn") "ডেলিভারিতে পাওয়া যায়নি (০ ${item.unit})" else "Not received (0 ${item.unit})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = LossRed
                        )
                    } else {
                        Text(
                            text = "${if (language == "bn") "প্রাপ্ত: " else "Received: "}${item.receivedQuantity.toIntOrNull() ?: item.receivedQuantity} ${item.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                    }
                }

                // Buy Price & Total
                Column(horizontalAlignment = Alignment.End) {
                    val priceStr = if (item.buyPrice > 0) "$currency${item.buyPrice.toIntOrNull() ?: item.buyPrice}" else "00"
                    Text(
                        text = "${if (language == "bn") "ক্রয় দর: " else "Buy Rate: "}$priceStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (!isMissing) {
                        val itemTotal = item.receivedQuantity * item.buyPrice
                        val itemTotalStr = if (itemTotal % 1.0 == 0.0) itemTotal.toInt().toString() else "%.1f".format(itemTotal)
                        Text(
                            text = "$currency$itemTotalStr",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                    } else {
                        Text(
                            text = "$currency 0",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Edit Button (Allows adjusting received qty & buy price)
                if (!isAlreadyReceived) {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Item", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    // Modal to Edit Received Quantity and Buy Price for this item
    if (showEditDialog) {
        var editQtyStr by remember { mutableStateOf(if (item.receivedQuantity > 0) (item.receivedQuantity.toIntOrNull() ?: item.receivedQuantity).toString() else (item.orderedQuantity.toIntOrNull() ?: item.orderedQuantity).toString()) }
        var editBuyPriceStr by remember { mutableStateOf(if (item.buyPrice > 0) (item.buyPrice.toIntOrNull() ?: item.buyPrice).toString() else "00") }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "${item.productName} - ${if (language == "bn") "পরিমাণ ও দর এডিট" else "Edit Quantity & Price"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${if (language == "bn") "অর্ডারকৃত পরিমাণ: " else "Ordered: "}${item.orderedQuantity.toIntOrNull() ?: item.orderedQuantity} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editQtyStr,
                        onValueChange = { editQtyStr = it },
                        label = { Text(if (language == "bn") "হাতে প্রাপ্ত পরিমাণ (${item.unit})" else "Received Quantity (${item.unit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editBuyPriceStr,
                        onValueChange = { editBuyPriceStr = it },
                        label = { Text(if (language == "bn") "ক্রয় দর ($currency) [অজানা থাকলে 00 দিন]" else "Buy Price ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(if (language == "bn") "দাম জানা না থাকলে বা কম-বেশি হলে পরিবর্তন করুন" else "Adjust if changed or enter 00 if unknown")
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedQty = editQtyStr.toDoubleOrNull() ?: 0.0
                        val parsedBuy = if (editBuyPriceStr.trim() == "00" || editBuyPriceStr.trim() == "0") 0.0 else (editBuyPriceStr.toDoubleOrNull() ?: 0.0)
                        val isZero = parsedQty <= 0.0
                        onItemChange(
                            item.copy(
                                receivedQuantity = parsedQty,
                                buyPrice = parsedBuy,
                                isNotFound = isZero
                            )
                        )
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == "bn") "ঠিক আছে" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }
}

/**
 * Dialog displaying list of all Waiting/Pending Orders.
 * Shows status (WAITING vs RECEIVED), date, supplier, item count, and allows clicking to receive or view.
 */
@Composable
fun PendingOrdersListDialog(
    pendingOrders: List<PendingOrder>,
    currency: String,
    language: String,
    shopName: String,
    onDismiss: () -> Unit,
    onSelectOrder: (PendingOrder) -> Unit,
    onDeleteOrder: (String) -> Unit
) {
    var orderToDelete by remember { mutableStateOf<PendingOrder?>(null) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "WAITING", "RECEIVED"

    val filteredList = remember(pendingOrders, selectedFilter) {
        when (selectedFilter) {
            "WAITING" -> pendingOrders.filter { it.status == "WAITING" }
            "RECEIVED" -> pendingOrders.filter { it.status == "RECEIVED" }
            else -> pendingOrders
        }
    }

    val waitingCount = remember(pendingOrders) { pendingOrders.count { it.status == "WAITING" } }
    val receivedCount = remember(pendingOrders) { pendingOrders.count { it.status == "RECEIVED" } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "অর্ডারকৃত পণ্য তালিকা (ওয়েটিং)" else "Waiting Orders List",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == "bn") "$waitingCount টি অর্ডার আসার অপেক্ষায় আছে" else "$waitingCount orders in waiting",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Chips (All, Waiting, Received)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("${if (language == "bn") "সব অর্ডার" else "All"} (${pendingOrders.size})") },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = selectedFilter == "WAITING",
                        onClick = { selectedFilter = "WAITING" },
                        label = { Text("${if (language == "bn") "অপেক্ষারত" else "Waiting"} ($waitingCount)") },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF3C7),
                            selectedLabelColor = Color(0xFF92400E)
                        )
                    )
                    FilterChip(
                        selected = selectedFilter == "RECEIVED",
                        onClick = { selectedFilter = "RECEIVED" },
                        label = { Text("${if (language == "bn") "রিসিভড" else "Received"} ($receivedCount)") },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFDCFCE7),
                            selectedLabelColor = Color(0xFF166534)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Orders List
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inbox,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (language == "bn") "কোনো অর্ডার পাওয়া যায়নি" else "No orders found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (language == "bn") "কম স্টকের পণ্য অর্ডার করতে 'পণ্য অর্ডার' অপশনে যান" else "Create orders using 'Order Stock' in Inventory",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    val sdf = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredList, key = { it.id }) { order ->
                            val isWaiting = order.status == "WAITING"
                            val totalQty = order.items.sumOf { it.orderedQuantity }
                            val estCost = order.items.sumOf { it.orderedQuantity * it.buyPrice }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectOrder(order) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isWaiting) Color(0xFFFFFBEB) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, if (isWaiting) Color(0xFFFDE68A) else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isWaiting) Color(0xFFD97706) else Color(0xFF16A34A)
                                            ) {
                                                Text(
                                                    text = if (isWaiting) (if (language == "bn") "অপেক্ষারত" else "WAITING") else (if (language == "bn") "স্টক-ইন সম্পন্ন" else "RECEIVED"),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "অর্ডার #${order.orderNumber}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        IconButton(
                                            onClick = { orderToDelete = order },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    if (order.supplierName.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${if (language == "bn") "মহাজন / সাপ্লায়ার: " else "Supplier: "}${order.supplierName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${order.items.size} ${if (language == "bn") "টি পদ" else "items"} | ${totalQty.toIntOrNull() ?: totalQty} ${if (language == "bn") "পিছ" else "pcs"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Text(
                                            text = "${if (language == "bn") "আনুমানিক: " else "Est: "}$currency${estCost.toIntOrNull() ?: estCost}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sdf.format(Date(order.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            fontSize = 10.sp
                                        )

                                        // Action Button: "রিসিভ / স্টক-ইন করুন" or "ডিটেইলস / PDF দেখুন"
                                        Button(
                                            onClick = { onSelectOrder(order) },
                                            modifier = Modifier.height(30.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isWaiting) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = if (isWaiting) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                        ) {
                                            Icon(
                                                if (isWaiting) Icons.Default.Inventory2 else Icons.Default.Visibility,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isWaiting) (if (language == "bn") "রিসিভ / স্টক-ইন" else "Receive / Stock-In") else (if (language == "bn") "মেমো ও রিপোর্ট" else "View Report"),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (language == "bn") "বন্ধ করুন" else "Close")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    orderToDelete?.let { ord ->
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text(if (language == "bn") "অর্ডার মুছে ফেলতে চান?" else "Delete Order?") },
            text = { Text(if (language == "bn") "অর্ডার #${ord.orderNumber} তালিকা থেকে মুছে ফেলা হবে।" else "Order #${ord.orderNumber} will be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOrder(ord.id)
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) {
                    Text(if (language == "bn") "মুছে ফেলুন" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }
}
