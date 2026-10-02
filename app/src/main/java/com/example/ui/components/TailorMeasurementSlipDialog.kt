package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TailorMeasurementTemplates
import com.example.data.model.TailorOrder
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TailorMeasurementSlipDialog(
    order: TailorOrder,
    shopName: String,
    shopPhone: String,
    currency: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val garments = order.parseGarments()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    val slipText = buildSlipPlainText(order, shopName, shopPhone, currency)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Header with Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ContentCut,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "দর্জি মাপ ও অর্ডার রসিদ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "অর্ডার নং: ${order.orderNumber.ifBlank { "T-${order.id}" }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Slip Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Shop Header in Slip
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = shopName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = "দর্জি ও সেলাই খাতা",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (shopPhone.isNotBlank()) {
                                Text(
                                    text = "মোবাইল: $shopPhone",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Customer & Delivery Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "কাস্টমারের নাম:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = order.customerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (order.customerPhone.isNotBlank()) {
                                        Text(
                                            text = "মোবাইল: ${order.customerPhone}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (order.customerAddress.isNotBlank()) {
                                        Text(
                                            text = "ঠিকানা: ${order.customerAddress}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (order.status.uppercase()) {
                                            "READY" -> EmeraldPrimary.copy(alpha = 0.15f)
                                            "DELIVERED" -> Color.Gray.copy(alpha = 0.15f)
                                            "CUTTING" -> AmberTertiary.copy(alpha = 0.15f)
                                            else -> TealDarkHeader.copy(alpha = 0.15f)
                                        }
                                    ) {
                                        Text(
                                            text = order.getStatusBengali(),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when (order.status.uppercase()) {
                                                "READY" -> EmeraldPrimary
                                                "DELIVERED" -> Color.Gray
                                                "CUTTING" -> AmberTertiary
                                                else -> TealDarkHeader
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "অর্ডার: ${dateFormat.format(Date(order.orderDate))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "ডেলিভারি: ${dateFormat.format(Date(order.deliveryDate))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = LossRed
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Garments & Measurements
                    Text(
                        text = "✂️ পোশাকের তালিকা ও মাপসমূহ:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (garments.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "কোনো নির্দিষ্ট মাপ যোগ করা হয়নি",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        garments.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Garment Title & Quantity & Rate
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = EmeraldPrimary,
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = item.garmentName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = " (${item.quantity} পিস)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (item.stitchingRate > 0) {
                                            Text(
                                                text = "$currency${item.totalRate.toInt()}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Measurements Grid
                                    if (item.measurements.isNotEmpty()) {
                                        val mEntries = item.measurements.entries.toList()
                                        // Display in 2 columns
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            val rows = (mEntries.size + 1) / 2
                                            for (r in 0 until rows) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 3.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    // Column 1
                                                    val e1 = mEntries[r * 2]
                                                    val label1 = getLabelForField(e1.key)
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = "$label1:",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Text(
                                                            text = e1.value,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    // Column 2 (if exists)
                                                    if (r * 2 + 1 < mEntries.size) {
                                                        val e2 = mEntries[r * 2 + 1]
                                                        val label2 = getLabelForField(e2.key)
                                                        Row(
                                                            modifier = Modifier.weight(1f),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text(
                                                                text = "$label2:",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            Text(
                                                                text = e2.value,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (item.notes.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "নোট: ${item.notes}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (order.designNotes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AmberTertiary.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "বিশেষ ডিজাইন / কাপড়ের বিবরণ:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberTertiary
                                )
                                Text(
                                    text = order.designNotes,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Billing Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("মোট মজুরি / বিল:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "$currency${order.totalAmount.toInt()}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("অগ্রিম জমা:", style = MaterialTheme.typography.bodyMedium, color = EmeraldPrimary)
                                Text(
                                    "$currency${order.advancePaid.toInt()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("বাকি টাকা:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "$currency${order.dueAmount.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (order.dueAmount > 0) LossRed else EmeraldPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Bottom Sharing & Calling Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy Slip
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(slipText))
                            Toast.makeText(context, "মাপের স্লিপ কপি হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("কপি")
                    }

                    // Send WhatsApp
                    Button(
                        onClick = {
                            sendWhatsApp(context, order.customerPhone, slipText)
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("শেয়ার / WhatsApp")
                    }

                    // Direct Call
                    if (order.customerPhone.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:${order.customerPhone}")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("কল")
                        }
                    }
                }
            }
        }
    }
}

private fun getLabelForField(key: String): String {
    val allFields = TailorMeasurementTemplates.kamizFields +
            TailorMeasurementTemplates.salwarFields +
            TailorMeasurementTemplates.frockFields +
            TailorMeasurementTemplates.petticoatFields +
            TailorMeasurementTemplates.blouseFields +
            TailorMeasurementTemplates.panjabiFields +
            TailorMeasurementTemplates.shirtFields +
            TailorMeasurementTemplates.pantFields +
            TailorMeasurementTemplates.maxiFields +
            TailorMeasurementTemplates.burqaFields +
            TailorMeasurementTemplates.otherFields

    return allFields.firstOrNull { it.key == key }?.bnLabel ?: key
}

private fun buildSlipPlainText(
    order: TailorOrder,
    shopName: String,
    shopPhone: String,
    currency: String
): String {
    val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sb = StringBuilder()
    sb.appendLine("✂️ $shopName - দর্জি মাপ ও রসিদ ✂️")
    if (shopPhone.isNotBlank()) sb.appendLine("📞 $shopPhone")
    sb.appendLine("-----------------------------")
    sb.appendLine("অর্ডার নং: ${order.orderNumber.ifBlank { "T-${order.id}" }}")
    sb.appendLine("কাস্টমার: ${order.customerName}")
    if (order.customerPhone.isNotBlank()) sb.appendLine("মোবাইল: ${order.customerPhone}")
    sb.appendLine("অর্ডার তারিখ: ${df.format(Date(order.orderDate))}")
    sb.appendLine("ডেলিভারি তারিখ: ${df.format(Date(order.deliveryDate))}")
    sb.appendLine("অবস্থা: ${order.getStatusBengali()}")
    sb.appendLine("-----------------------------")
    sb.appendLine("👗 পোশাক ও মাপসমূহ:")

    val garments = order.parseGarments()
    garments.forEachIndexed { i, g ->
        sb.appendLine("${i + 1}. ${g.garmentName} (${g.quantity} পিস) - দর: $currency${g.stitchingRate.toInt()}")
        g.measurements.forEach { (k, v) ->
            val label = getLabelForField(k)
            sb.appendLine("   • $label: $v")
        }
        if (g.notes.isNotBlank()) sb.appendLine("   • নোট: ${g.notes}")
    }

    if (order.designNotes.isNotBlank()) {
        sb.appendLine("-----------------------------")
        sb.appendLine("বিশেষ ডিজাইন: ${order.designNotes}")
    }

    sb.appendLine("-----------------------------")
    sb.appendLine("মোট বিল: $currency${order.totalAmount.toInt()}")
    sb.appendLine("অগ্রিম জমা: $currency${order.advancePaid.toInt()}")
    sb.appendLine("বাকি বিল: $currency${order.dueAmount.toInt()}")
    sb.appendLine("-----------------------------")
    sb.appendLine("ধন্যবাদ! আবার আসবেন।")
    return sb.toString()
}

private fun sendWhatsApp(context: Context, phone: String, message: String) {
    try {
        val cleanPhone = phone.replace("+", "").replace("-", "").replace(" ", "").trim()
        val formattedPhone = when {
            cleanPhone.startsWith("01") -> "88$cleanPhone"
            cleanPhone.startsWith("8801") -> cleanPhone
            else -> cleanPhone
        }
        val uri = if (formattedPhone.length >= 10) {
            Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
        } else {
            Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(shareIntent, "শেয়ার করুন"))
    }
}
