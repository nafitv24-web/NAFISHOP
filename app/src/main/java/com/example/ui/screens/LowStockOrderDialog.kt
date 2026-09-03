package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Product
import com.example.data.model.ReorderItem
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.DueOrange
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.example.util.CalculationHelper
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LowStockOrderDialog(
    allProducts: List<Product>,
    lowStockProducts: List<Product>,
    currency: String,
    language: String,
    shopName: String,
    shopPhone: String,
    onDismiss: () -> Unit,
    onQuickStockIn: (List<ReorderItem>) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Initialize list of items from lowStockProducts (or default to empty if none)
    var orderItems by remember {
        mutableStateOf(
            lowStockProducts.map { prod ->
                // Smart recommended order quantity: at least 10 pcs or deficiency from threshold
                val recommended = if (prod.minStockAlert > 0) {
                    ((prod.minStockAlert * 2) - prod.stockQuantity).coerceAtLeast(10.0)
                } else {
                    10.0
                }
                ReorderItem(
                    product = prod,
                    orderQuantity = recommended,
                    unitPrice = prod.buyPrice
                )
            }
        )
    }

    var selectedProductIds by remember {
        mutableStateOf(orderItems.map { it.product.id }.toSet())
    }

    var supplierName by remember { mutableStateOf("") }
    var orderNote by remember { mutableStateOf("") }

    var showAddProductSheet by remember { mutableStateOf(false) }
    var addProductSearchQuery by remember { mutableStateOf("") }
    var showStockInConfirmDialog by remember { mutableStateOf(false) }

    val activeSelectedItems = remember(orderItems, selectedProductIds) {
        orderItems.filter { selectedProductIds.contains(it.product.id) && it.orderQuantity > 0 }
    }

    val totalSelectedCount = activeSelectedItems.size
    val totalOrderPcs = activeSelectedItems.sumOf { it.orderQuantity }
    val estimatedTotalCost = activeSelectedItems.sumOf { it.orderQuantity * it.unitPrice }

    fun buildOrderShareText(): String {
        val dateStr = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.append("🛒 *পণ্য অর্ডার তালিকা (Purchase Order)*\n")
        sb.append("দোকান: $shopName\n")
        if (shopPhone.isNotBlank()) sb.append("মোবাইল: $shopPhone\n")
        sb.append("তারিখ: $dateStr\n")
        if (supplierName.isNotBlank()) sb.append("ডিলার/মহাজন: $supplierName\n")
        sb.append("----------------------------------------\n")

        activeSelectedItems.forEachIndexed { idx, item ->
            val p = item.product
            val itemTotal = item.orderQuantity * item.unitPrice
            val itemTotalStr = if (itemTotal % 1.0 == 0.0) itemTotal.toInt().toString() else "%.1f".format(itemTotal)
            val qtyStr = if (item.orderQuantity % 1.0 == 0.0) item.orderQuantity.toInt().toString() else item.orderQuantity.toString()
            val stockStr = if (p.stockQuantity % 1.0 == 0.0) p.stockQuantity.toInt().toString() else p.stockQuantity.toString()
            val buyStr = if (item.unitPrice % 1.0 == 0.0) item.unitPrice.toInt().toString() else item.unitPrice.toString()

            sb.append("${idx + 1}. *${p.name}*\n")
            if (p.barcode.isNotBlank()) sb.append("   কোড: ${p.barcode}\n")
            sb.append("   বর্তমান স্টক: $stockStr ${p.unit}\n")
            sb.append("   অর্ডার পরিমাণ: *$qtyStr ${p.unit}* (দর: $currency$buyStr, মোট: $currency$itemTotalStr)\n")
        }

        sb.append("----------------------------------------\n")
        val totalPcsStr = if (totalOrderPcs % 1.0 == 0.0) totalOrderPcs.toInt().toString() else "%.1f".format(totalOrderPcs)
        val costStr = if (estimatedTotalCost % 1.0 == 0.0) estimatedTotalCost.toInt().toString() else "%.2f".format(estimatedTotalCost)
        sb.append("মোট পণ্য: $totalSelectedCount টি | মোট অর্ডার: $totalPcsStr পিছ\n")
        sb.append("আনুমানিক মোট বিল: $currency$costStr\n")
        if (orderNote.isNotBlank()) {
            sb.append("বিশেষ দ্রষ্টব্য: $orderNote\n")
        }
        sb.append("----------------------------------------\n")
        sb.append("সৌজন্যে: নাফি এন্ড নাজমুল টেলিকম (NAFI SHOP)")
        return sb.toString()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = "Order Low Stock",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "কম স্টক পণ্য অর্ডার মেমো" else "Low Stock Purchase Order",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == "bn") "মহাজন বা ডিলারের কাছে পণ্য অর্ডার তৈরি করুন" else "Prepare reorder list for suppliers & dealers",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Supplier & Note inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text(if (language == "bn") "ডিলার / মহাজনের নাম (ঐচ্ছিক)" else "Supplier / Dealer (Optional)", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = orderNote,
                        onValueChange = { orderNote = it },
                        label = { Text(if (language == "bn") "নোট (যেমন: জরুরি)" else "Note (Optional)", fontSize = 11.sp) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Bar: Select All / Deselect All & Add Product
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                selectedProductIds = if (selectedProductIds.size == orderItems.size) {
                                    emptySet()
                                } else {
                                    orderItems.map { it.product.id }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                if (selectedProductIds.size == orderItems.size && orderItems.isNotEmpty()) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = EmeraldPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedProductIds.size == orderItems.size && orderItems.isNotEmpty()) {
                                    if (language == "bn") "সব বাতিল" else "Deselect"
                                } else {
                                    if (language == "bn") "সব নির্বাচন (${orderItems.size})" else "Select All"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    // Add other product button
                    FilledTonalButton(
                        onClick = { showAddProductSheet = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "+ অন্য পণ্য যোগ" else "+ Add Item",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Products Reorder List
                if (orderItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = ProfitGreen,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (language == "bn") "বর্তমানে কোনো কম স্টক পণ্য নেই!" else "No low stock products right now!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == "bn") "অন্যান্য পণ্য অর্ডারের জন্য উপরের '+ অন্য পণ্য যোগ' বাটনে চাপুন।" else "Tap '+ Add Item' to order any other product.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(orderItems, key = { it.product.id }) { item ->
                            val p = item.product
                            val isSelected = selectedProductIds.contains(p.id)

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isSelected) DueOrange.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    // Row 1: Checkbox, Name, Current Stock & Delete
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedProductIds = if (checked) {
                                                    selectedProductIds + p.id
                                                } else {
                                                    selectedProductIds - p.id
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = DueOrange
                                            ),
                                            modifier = Modifier.size(24.dp)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = p.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (p.category.isNotBlank()) {
                                                    Text(
                                                        text = p.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                                if (p.barcode.isNotBlank()) {
                                                    Text(
                                                        text = "• ${p.barcode}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }

                                        // Current stock warning badge
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (p.stockQuantity <= 0) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                                        ) {
                                            Text(
                                                text = "${if (language == "bn") "স্টক: " else "Stock: "}${p.stockQuantity.toIntOrNull() ?: p.stockQuantity} ${p.unit}",
                                                color = if (p.stockQuantity <= 0) LossRed else Color(0xFFB45309),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                orderItems = orderItems.filter { it.product.id != p.id }
                                                selectedProductIds = selectedProductIds - p.id
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove item",
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Row 2: Order Quantity Controls & Subtotal
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surface,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Unit Buy Price
                                        Column {
                                            Text(
                                                text = if (language == "bn") "ক্রয় দর" else "Buy Price",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "$currency${item.unitPrice.toIntOrNull() ?: item.unitPrice}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // Quantity Counter (Minus, Value Input, Plus)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (language == "bn") "অর্ডার:" else "Order:",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706),
                                                modifier = Modifier.padding(end = 6.dp),
                                                fontSize = 11.sp
                                            )

                                            // Minus button
                                            FilledIconButton(
                                                onClick = {
                                                    if (item.orderQuantity > 1) {
                                                        val updatedQty = item.orderQuantity - 1
                                                        orderItems = orderItems.map {
                                                            if (it.product.id == p.id) it.copy(orderQuantity = updatedQty) else it
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                                ),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                                            }

                                            // Quantity input text
                                            var qtyInputText by remember(item.orderQuantity) {
                                                mutableStateOf(
                                                    if (item.orderQuantity % 1.0 == 0.0) item.orderQuantity.toInt().toString() else item.orderQuantity.toString()
                                                )
                                            }

                                            OutlinedTextField(
                                                value = qtyInputText,
                                                onValueChange = { newVal: String ->
                                                    qtyInputText = newVal
                                                    val parsed = newVal.toDoubleOrNull()
                                                    if (parsed != null && parsed >= 0) {
                                                        orderItems = orderItems.map {
                                                            if (it.product.id == p.id) it.copy(orderQuantity = parsed) else it
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .width(68.dp)
                                                    .padding(horizontal = 4.dp),
                                                textStyle = MaterialTheme.typography.titleSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            // Plus button
                                            FilledIconButton(
                                                onClick = {
                                                    val updatedQty = item.orderQuantity + 1
                                                    orderItems = orderItems.map {
                                                        if (it.product.id == p.id) it.copy(orderQuantity = updatedQty) else it
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = Color(0xFFFEF3C7)
                                                ),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                            }
                                        }

                                        // Item subtotal
                                        val lineTotal = item.orderQuantity * item.unitPrice
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (language == "bn") "মোট মূল্য" else "Subtotal",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "$currency${lineTotal.toIntOrNull() ?: "%.1f".format(lineTotal)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                    }

                                    // Quick bump chips: +5, +10, +20, +50
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (language == "bn") "দ্রুত যোগ:" else "Quick:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        listOf(5, 10, 20, 50).forEach { bump ->
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier
                                                    .padding(horizontal = 2.dp)
                                                    .clickable {
                                                        val updatedQty = item.orderQuantity + bump
                                                        orderItems = orderItems.map {
                                                            if (it.product.id == p.id) it.copy(orderQuantity = updatedQty) else it
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = "+$bump",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)), // Amber tint
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFDE68A)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${if (language == "bn") "নির্বাচিত আইটেম: " else "Selected: "}$totalSelectedCount ${if (language == "bn") "টি" else "items"}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            val pcsStr = if (totalOrderPcs % 1.0 == 0.0) totalOrderPcs.toInt().toString() else "%.1f".format(totalOrderPcs)
                            Text(
                                text = "${if (language == "bn") "মোট অর্ডার: " else "Total Pcs: "}$pcsStr ${if (language == "bn") "পিছ" else "pcs"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFB45309)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (language == "bn") "আনুমানিক মোট ক্রয় বাজেট" else "Est. Purchase Budget",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E),
                                fontSize = 10.sp
                            )
                            Text(
                                text = CalculationHelper.formatCurrency(estimatedTotalCost, currency),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons: PDF, WhatsApp/SMS, Copy, Stock-in
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // PDF Button
                    Button(
                        onClick = {
                            if (activeSelectedItems.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    if (language == "bn") "অনুগ্রহ করে অন্তত একটি পণ্য নির্বাচন করুন" else "Please select at least one item",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val pdfFile = PdfGenerator.generateLowStockOrderPdf(
                                context = context,
                                shopName = shopName,
                                shopPhone = shopPhone,
                                supplierName = supplierName,
                                note = orderNote,
                                items = activeSelectedItems,
                                currency = currency
                            )

                            if (pdfFile != null) {
                                PdfGenerator.openOrSharePdf(
                                    context = context,
                                    file = pdfFile,
                                    chooserTitle = if (language == "bn") "অর্ডার মেমো PDF শেয়ার / ডাউনলোড করুন" else "Share / Download Order PDF"
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    if (language == "bn") "PDF তৈরিতে সমস্যা হয়েছে" else "Failed to generate PDF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD97706) // Amber
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "অর্ডার PDF" else "Order PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Share WhatsApp / Message
                    FilledTonalButton(
                        onClick = {
                            if (activeSelectedItems.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    if (language == "bn") "অনুগ্রহ করে অন্তত একটি পণ্য নির্বাচন করুন" else "Please select at least one item",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@FilledTonalButton
                            }

                            val textToShare = buildOrderShareText()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "পণ্য অর্ডার - $shopName")
                                putExtra(Intent.EXTRA_TEXT, textToShare)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, if (language == "bn") "অর্ডার পাঠান" else "Share Order"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFECFDF5),
                            contentColor = EmeraldPrimary
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "মেসেজ / শেয়ার" else "Share Msg",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }

                    // Copy Text Button
                    FilledTonalButton(
                        onClick = {
                            if (activeSelectedItems.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    if (language == "bn") "অনুগ্রহ করে অন্তত একটি পণ্য নির্বাচন করুন" else "Please select at least one item",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@FilledTonalButton
                            }

                            val textToShare = buildOrderShareText()
                            clipboardManager.setText(AnnotatedString(textToShare))
                            Toast.makeText(
                                context,
                                if (language == "bn") "অর্ডার তালিকা কপি করা হয়েছে!" else "Order list copied to clipboard!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(0.7f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (language == "bn") "কপি" else "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Quick Stock In Button
                    FilledTonalButton(
                        onClick = {
                            if (activeSelectedItems.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    if (language == "bn") "অনুগ্রহ করে অন্তত একটি পণ্য নির্বাচন করুন" else "Please select at least one item",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@FilledTonalButton
                            }
                            showStockInConfirmDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEFF6FF),
                            contentColor = Color(0xFF1D4ED8)
                        ),
                        modifier = Modifier.weight(0.9f),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF1D4ED8))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (language == "bn") "স্টক-ইন" else "Receive",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet to Add Other Products
    if (showAddProductSheet) {
        val nonSelectedProducts = remember(allProducts, orderItems, addProductSearchQuery) {
            val existingIds = orderItems.map { it.product.id }.toSet()
            allProducts.filter { !existingIds.contains(it.id) && (addProductSearchQuery.isBlank() || it.name.contains(addProductSearchQuery, ignoreCase = true) || it.barcode.contains(addProductSearchQuery, ignoreCase = true)) }
        }

        AlertDialog(
            onDismissRequest = { showAddProductSheet = false },
            title = {
                Text(
                    text = if (language == "bn") "অর্ডারে পণ্য যুক্ত করুন" else "Add Product to Order",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = addProductSearchQuery,
                        onValueChange = { addProductSearchQuery = it },
                        label = { Text(if (language == "bn") "পণ্য খুঁজুন..." else "Search product...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (nonSelectedProducts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == "bn") "কোনো পণ্য পাওয়া যায়নি" else "No products found",
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(nonSelectedProducts) { prod ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            val newItem = ReorderItem(
                                                product = prod,
                                                orderQuantity = 10.0,
                                                unitPrice = prod.buyPrice
                                            )
                                            orderItems = orderItems + newItem
                                            selectedProductIds = selectedProductIds + prod.id
                                            showAddProductSheet = false
                                            Toast.makeText(
                                                context,
                                                if (language == "bn") "${prod.name} অর্ডারে যুক্ত হয়েছে" else "${prod.name} added to order",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = "${if (language == "bn") "বর্তমান স্টক: " else "Stock: "}${prod.stockQuantity.toIntOrNull() ?: prod.stockQuantity} ${prod.unit} • $currency${prod.buyPrice.toIntOrNull() ?: prod.buyPrice}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = EmeraldPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddProductSheet = false }) {
                    Text(if (language == "bn") "বন্ধ করুন" else "Close")
                }
            }
        )
    }

    // Confirmation dialog before quick stock-in
    if (showStockInConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showStockInConfirmDialog = false },
            icon = { Icon(Icons.Default.Inventory2, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp)) },
            title = {
                Text(
                    text = if (language == "bn") "স্টক-ইন নিশ্চিতকরণ" else "Confirm Stock-In",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == "bn") {
                        "নির্বাচিত $totalSelectedCount টি পণ্যের মোট $totalOrderPcs পিছ স্টক আপনার ইনভেন্টরিতে সরাসরি যোগ করতে চান?\n\nআনুমানিক ক্রয় খরচ: $currency$estimatedTotalCost"
                    } else {
                        "Do you want to directly receive $totalOrderPcs pcs of $totalSelectedCount items into your stock?\n\nTotal cost: $currency$estimatedTotalCost"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showStockInConfirmDialog = false
                        onQuickStockIn(activeSelectedItems)
                        Toast.makeText(
                            context,
                            if (language == "bn") "পণ্যসমূহ সফলভাবে স্টক-ইন করা হয়েছে!" else "Stock successfully updated!",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == "bn") "হ্যাঁ, স্টক-ইন করুন" else "Yes, Stock In")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStockInConfirmDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }
}
