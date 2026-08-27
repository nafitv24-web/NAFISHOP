package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.TransactionRecord
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrReturnSaleDialog(
    transaction: TransactionRecord,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onReturnItem: (returnQty: Double, note: String) -> Unit,
    onEditSale: (newQuantity: Double, newUnitPrice: Double, newPaidAmount: Double, newCustomerName: String, newCustomerPhone: String, newNote: String) -> Unit,
    onDeleteSale: () -> Unit
) {
    val isSale = transaction.type.equals("SALE", ignoreCase = true)
    // Mode: "EDIT" (সংশোধন), "RETURN" (পণ্য ফেরত), "DELETE" (বাতিল)
    var selectedMode by remember { mutableStateOf("EDIT") }

    // Return State
    var returnQtyStr by remember { mutableStateOf(transaction.quantity.toIntOrNull()?.toString() ?: transaction.quantity.toString()) }
    var returnNote by remember { mutableStateOf("") }

    // Edit State
    var editQtyStr by remember { mutableStateOf(transaction.quantity.toIntOrNull()?.toString() ?: transaction.quantity.toString()) }
    var editPriceStr by remember { mutableStateOf(transaction.unitPrice.toIntOrNull()?.toString() ?: transaction.unitPrice.toString()) }
    var editPaidStr by remember { mutableStateOf(transaction.paidAmount.toIntOrNull()?.toString() ?: transaction.paidAmount.toString()) }
    var editCustomerName by remember { mutableStateOf(transaction.customerName) }
    var editCustomerPhone by remember { mutableStateOf(transaction.customerPhone) }
    var editNote by remember { mutableStateOf(transaction.note) }

    val dateFormatted = remember(transaction.timestamp) {
        SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    // Dynamic Live Math
    val currentQty = editQtyStr.toDoubleOrNull() ?: 0.0
    val currentPrice = editPriceStr.toDoubleOrNull() ?: 0.0
    val liveTotal = (currentQty * currentPrice).coerceAtLeast(0.0)
    val currentPaid = editPaidStr.toDoubleOrNull() ?: 0.0
    val liveDue = (liveTotal - currentPaid).coerceAtLeast(0.0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(
                                    if (isSale) MaterialTheme.colorScheme.primaryContainer else Color(0xFFEFF6FF),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isSale) Icons.Default.EditNote else Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = if (isSale) MaterialTheme.colorScheme.primary else StockBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    isSale -> if (language == "bn") "বিক্রি লেনদেন সংশোধন" else "Edit Sale Transaction"
                                    transaction.type.contains("STOCK", ignoreCase = true) -> if (language == "bn") "স্টক লেনদেন সংশোধন" else "Edit Stock Transaction"
                                    else -> if (language == "bn") "লেনদেন সংশোধন" else "Edit Transaction"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = dateFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.outline)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Summary Info Card of the Transaction
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = transaction.productName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSale) Color(0xFFDCFCE7) else Color(0xFFDBEAFE)
                            ) {
                                Text(
                                    text = "$currency${transaction.totalAmount.toIntOrNull() ?: transaction.totalAmount}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSale) Color(0xFF15803D) else StockBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${if (language == "bn") "পরিমাণ:" else "Qty:"} ${transaction.quantity.toIntOrNull() ?: transaction.quantity} ${transaction.unit} • ${if (language == "bn") "দর:" else "Rate:"} $currency${transaction.unitPrice.toIntOrNull() ?: transaction.unitPrice}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (transaction.invoiceNumber.isNotBlank()) {
                                Text(
                                    text = "#${transaction.invoiceNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        if (transaction.customerName.isNotBlank() && transaction.customerName != "ক্যাশ কাস্টমার") {
                            Text(
                                text = "${if (language == "bn") "ক্রেতা:" else "Customer:"} ${transaction.customerName}${if (transaction.customerPhone.isNotBlank()) " (${transaction.customerPhone})" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = DueOrange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Segmented Mode Selector
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // 1. এডিট Tab
                        val isEdit = selectedMode == "EDIT"
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isEdit) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            border = if (isEdit) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { selectedMode = "EDIT" }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = if (isEdit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "এডিট / সংশোধন" else "Edit",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isEdit) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isEdit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // 2. পণ্য ফেরত Tab (For Sales)
                        if (isSale) {
                            val isReturn = selectedMode == "RETURN"
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = if (isReturn) Color(0xFFFEF3C7) else Color.Transparent,
                                border = if (isReturn) BorderStroke(1.dp, Color(0xFFFDE68A)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { selectedMode = "RETURN" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardReturn,
                                        contentDescription = null,
                                        tint = if (isReturn) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == "bn") "পণ্য ফেরত" else "Return",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isReturn) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isReturn) Color(0xFFB45309) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // 3. বাতিল Tab
                        val isDelete = selectedMode == "DELETE"
                        Surface(
                            shape = RoundedCornerShape(9.dp),
                            color = if (isDelete) Color(0xFFFEE2E2) else Color.Transparent,
                            border = if (isDelete) BorderStroke(1.dp, Color(0xFFFECACA)) else null,
                            modifier = Modifier
                                .weight(0.9f)
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { selectedMode = "DELETE" }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = if (isDelete) LossRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "বাতিল" else "Delete",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isDelete) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isDelete) LossRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Contents
                when (selectedMode) {
                    "EDIT" -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editQtyStr,
                                onValueChange = { editQtyStr = it },
                                label = { Text(if (language == "bn") "পরিমাণ (${transaction.unit}) *" else "Qty (${transaction.unit}) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = editPriceStr,
                                onValueChange = { editPriceStr = it },
                                label = { Text(if (language == "bn") "একক দর ($currency) *" else "Price ($currency) *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Math Preview Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
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
                                        text = if (language == "bn") "নতুন মোট বিল" else "New Total",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$currency${liveTotal.toIntOrNull() ?: String.format(Locale.US, "%.1f", liveTotal)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (isSale) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = if (language == "bn") "অবশিষ্ট বাকি" else "Remaining Due",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (liveDue > 0) DueOrange else ProfitGreen
                                        )
                                        Text(
                                            text = if (liveDue > 0) "$currency${liveDue.toIntOrNull() ?: String.format(Locale.US, "%.1f", liveDue)}" else (if (language == "bn") "পরিশোধিত" else "Paid"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (liveDue > 0) DueOrange else ProfitGreen
                                        )
                                    }
                                }
                            }
                        }

                        if (isSale) {
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = editPaidStr,
                                onValueChange = { editPaidStr = it },
                                label = { Text(if (language == "bn") "নগদ জমা ($currency)" else "Cash Paid ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Quick Preset Chips for Paid Amount
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                SuggestionChip(
                                    onClick = {
                                        editPaidStr = (liveTotal.toIntOrNull()?.toString() ?: liveTotal.toString())
                                    },
                                    label = { Text(if (language == "bn") "সম্পূর্ণ নগদ ($currency${liveTotal.toIntOrNull() ?: liveTotal})" else "Full Paid", fontSize = 11.sp) }
                                )
                                SuggestionChip(
                                    onClick = { editPaidStr = "0" },
                                    label = { Text(if (language == "bn") "সম্পূর্ণ বাকি" else "Full Due", fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editCustomerName,
                                onValueChange = { editCustomerName = it },
                                label = { Text(if (language == "bn") "ক্রেতা/সাপ্লায়ার নাম" else "Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1.1f)
                            )

                            OutlinedTextField(
                                value = editCustomerPhone,
                                onValueChange = { editCustomerPhone = it },
                                label = { Text(if (language == "bn") "ফোন নম্বর" else "Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(0.9f)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            label = { Text(if (language == "bn") "নোট / মন্তব্য" else "Note / Remarks") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                val qty = editQtyStr.toDoubleOrNull() ?: 0.0
                                val price = editPriceStr.toDoubleOrNull() ?: 0.0
                                val paid = if (isSale) (editPaidStr.toDoubleOrNull() ?: 0.0) else (qty * price)
                                if (qty > 0 && price >= 0) {
                                    onEditSale(qty, price, paid, editCustomerName, editCustomerPhone, editNote)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "পরিবর্তন সংরক্ষণ করুন" else "Save Changes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    "RETURN" -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "bn") "পণ্য ফেরত নিলে স্বয়ংক্রিয়ভাবে স্টক বৃদ্ধি পাবে এবং প্রয়োজনীয় আর্থিক সমন্বয় হবে।" else "Returning item will automatically restore stock & adjust ledger balance.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF92400E),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = returnQtyStr,
                            onValueChange = { returnQtyStr = it },
                            label = { Text(if (language == "bn") "ফেরত পরিমাণ (${transaction.unit}) *" else "Return Qty (${transaction.unit}) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = returnNote,
                            onValueChange = { returnNote = it },
                            label = { Text(if (language == "bn") "ফেরতের কারণ / মন্তব্য" else "Reason / Note") },
                            placeholder = { Text(if (language == "bn") "যেমন: ত্রুটিপূর্ণ / কাস্টমার ফেরত দিল" else "e.g. Returned by customer") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                val qty = returnQtyStr.toDoubleOrNull() ?: 0.0
                                if (qty > 0) {
                                    onReturnItem(qty, returnNote)
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.KeyboardReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "পণ্য ফেরত নিশ্চিত করুন" else "Confirm Return",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    "DELETE" -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFECACA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = if (language == "bn") "সতর্কতা: এই লেনদেনটি ডিলিট করলে বিক্রি বা ক্রয় বাতিল হবে এবং পণ্যের স্টক স্বয়ংক্রিয়ভাবে আগের অবস্থানে সমন্বয় হবে।" else "Warning: Deleting this transaction will cancel the record and adjust inventory stock.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LossRed,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                onDeleteSale()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "লেনদেন বাতিল ও স্টক সমন্বয়" else "Cancel Transaction & Adjust Stock",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
