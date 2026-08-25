package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
    onEditSale: (newQuantity: Double, newUnitPrice: Double, newCustomerName: String, newNote: String) -> Unit,
    onDeleteSale: () -> Unit
) {
    // Mode: "RETURN" (পণ্য ফেরত), "EDIT" (সংশোধন), "DELETE" (বাতিল)
    var selectedMode by remember { mutableStateOf("RETURN") }

    // Return State
    var returnQtyStr by remember { mutableStateOf(transaction.quantity.toIntOrNull()?.toString() ?: transaction.quantity.toString()) }
    var returnNote by remember { mutableStateOf("") }

    // Edit State
    var editQtyStr by remember { mutableStateOf(transaction.quantity.toIntOrNull()?.toString() ?: transaction.quantity.toString()) }
    var editPriceStr by remember { mutableStateOf(transaction.unitPrice.toIntOrNull()?.toString() ?: transaction.unitPrice.toString()) }
    var editCustomerName by remember { mutableStateOf(transaction.customerName) }
    var editNote by remember { mutableStateOf(transaction.note) }

    var showConfirmDelete by remember { mutableStateOf(false) }

    val dateFormatted = remember(transaction.timestamp) {
        SimpleDateFormat("hh:mm a, dd MMM yyyy", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFEF3C7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AssignmentReturn,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "বিক্রি লেনদেন ও ফেরত" else "Sale & Return Management",
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

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Summary Info Card of the Transaction
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = transaction.productName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$currency${transaction.totalAmount.toIntOrNull() ?: transaction.totalAmount}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${if (language == "bn") "বিক্রি পরিমাণ:" else "Qty:"} ${transaction.quantity.toIntOrNull() ?: transaction.quantity} ${transaction.unit} (@$currency${transaction.unitPrice.toIntOrNull() ?: transaction.unitPrice})",
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
                                text = "${if (language == "bn") "কাস্টমার:" else "Customer:"} ${transaction.customerName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = DueOrange
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selection Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedMode == "RETURN",
                        onClick = { selectedMode = "RETURN" },
                        label = {
                            Text(
                                text = if (language == "bn") "পণ্য ফেরত" else "Return Item",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.KeyboardReturn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF3C7),
                            selectedLabelColor = Color(0xFFB45309),
                            selectedLeadingIconColor = Color(0xFFB45309)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedMode == "EDIT",
                        onClick = { selectedMode = "EDIT" },
                        label = {
                            Text(
                                text = if (language == "bn") "এডিট" else "Edit",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(0.9f)
                    )

                    FilterChip(
                        selected = selectedMode == "DELETE",
                        onClick = { selectedMode = "DELETE" },
                        label = {
                            Text(
                                text = if (language == "bn") "বাতিল" else "Delete",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEE2E2),
                            selectedLabelColor = LossRed,
                            selectedLeadingIconColor = LossRed
                        ),
                        modifier = Modifier.weight(0.9f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on Mode
                when (selectedMode) {
                    "RETURN" -> {
                        Text(
                            text = if (language == "bn") "পণ্য ফেরত গ্রহণ করলে স্বয়ংক্রিয়ভাবে স্টক বৃদ্ধি পাবে এবং প্রয়োজনীয় আর্থিক সমন্বয় হবে।" else "Returning this product will automatically restore inventory stock and adjust balance.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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
                            placeholder = { Text(if (language == "bn") "যেমন: পণ্য পছন্দ হয়নি / ক্ষতিগ্রস্থ" else "e.g. Defective / returned by customer") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

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
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.KeyboardReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "পণ্য ফেরত নিশ্চিত ও স্টক সমন্বয় করুন" else "Confirm Return & Restock",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    "EDIT" -> {
                        OutlinedTextField(
                            value = editQtyStr,
                            onValueChange = { editQtyStr = it },
                            label = { Text(if (language == "bn") "বিক্রি পরিমাণ (${transaction.unit}) *" else "Quantity (${transaction.unit}) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editPriceStr,
                            onValueChange = { editPriceStr = it },
                            label = { Text(if (language == "bn") "একক বিক্রয় মূল্য ($currency) *" else "Unit Price ($currency) *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editCustomerName,
                            onValueChange = { editCustomerName = it },
                            label = { Text(if (language == "bn") "ক্রেতার নাম" else "Customer Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = editNote,
                            onValueChange = { editNote = it },
                            label = { Text(if (language == "bn") "নোট / মন্তব্য" else "Note") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val qty = editQtyStr.toDoubleOrNull() ?: 0.0
                                val price = editPriceStr.toDoubleOrNull() ?: 0.0
                                if (qty > 0 && price >= 0) {
                                    onEditSale(qty, price, editCustomerName, editNote)
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
                                text = if (language == "bn") "সংশোধন সংরক্ষণ করুন" else "Save Changes",
                                fontWeight = FontWeight.Bold
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
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (language == "bn") "সতর্কতা: এই লেনদেনটি ডিলিট করলে বিক্রিটি বাতিল হবে এবং পণ্যের স্টক আগের অবস্থানে ফেরত যাবে।" else "Warning: Deleting this transaction will cancel the sale and restore products to inventory.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LossRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onDeleteSale()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "বিক্রি বাতিল ও স্টক রিস্টোর করুন" else "Cancel Sale & Restock",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
