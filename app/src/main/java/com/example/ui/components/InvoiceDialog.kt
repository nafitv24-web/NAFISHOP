package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.ui.viewmodel.InvoiceDetails
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.material.icons.filled.PictureAsPdf
import com.example.util.PdfGenerator

@Composable
fun InvoiceDialog(
    invoice: InvoiceDetails,
    currency: String = "৳",
    language: String = "bn",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(invoice.timestamp))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Dialog Header with Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "ডিজিটাল ক্যাশ মেমো" else "Digital Cash Memo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // Scrollable Invoice Receipt Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    // Store Header
                    Text(
                        text = invoice.shopName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF1E293B)
                    )
                    if (invoice.shopAddress.isNotBlank()) {
                        Text(
                            text = invoice.shopAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = "${if (language == "bn") "মোবাইল: " else "Phone: "}${invoice.shopPhone}",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Invoice & Customer Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${if (language == "bn") "মেমো নং: " else "Invoice: "} ${invoice.invoiceNumber}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "${if (language == "bn") "তারিখ: " else "Date: "} $dateStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${if (language == "bn") "ক্রেতা: " else "Customer: "} ${invoice.customerName}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF0F172A)
                            )
                            if (invoice.customerPhone.isNotBlank()) {
                                Text(
                                    text = invoice.customerPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Items Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == "bn") "পণ্যের বিবরণ" else "Item Description",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f),
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = if (language == "bn") "পরিমাণ" else "Qty",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = if (language == "bn") "দর" else "Rate",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = if (language == "bn") "মোট" else "Total",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1.2f),
                            color = Color(0xFF334155)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Items List
                    invoice.items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${item.product.name}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(2f),
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "${item.quantity.toIntOrNull() ?: item.quantity} ${item.product.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "$currency${item.customPrice.toIntOrNull() ?: item.customPrice}",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "$currency${item.total.toIntOrNull() ?: item.total}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f),
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFCBD5E1))
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Summary Calculations
                    InvoiceSummaryRow(
                        label = if (language == "bn") "মোট মূল্য (Subtotal):" else "Subtotal:",
                        value = "$currency${invoice.subTotal.toIntOrNull() ?: invoice.subTotal}"
                    )
                    if (invoice.discount > 0) {
                        InvoiceSummaryRow(
                            label = if (language == "bn") "ডিসকাউন্ট / ছাড়:" else "Discount:",
                            value = "- $currency${invoice.discount.toIntOrNull() ?: invoice.discount}",
                            color = ProfitGreen
                        )
                    }
                    InvoiceSummaryRow(
                        label = if (language == "bn") "সর্বমোট প্রদেয়:" else "Grand Total:",
                        value = "$currency${invoice.grandTotal.toIntOrNull() ?: invoice.grandTotal}",
                        isBold = true
                    )
                    InvoiceSummaryRow(
                        label = if (language == "bn") "জমা / পরিশোধিত:" else "Paid Amount:",
                        value = "$currency${invoice.paidAmount.toIntOrNull() ?: invoice.paidAmount}",
                        color = ProfitGreen
                    )
                    if (invoice.dueAmount > 0 || invoice.totalCurrentDue > 0) {
                        InvoiceSummaryRow(
                            label = if (language == "bn") "আজকের নতুন বাকি:" else "Today's New Due:",
                            value = "$currency${invoice.dueAmount.toIntOrNull() ?: invoice.dueAmount}",
                            color = DueOrange,
                            isBold = true
                        )
                        if (invoice.previousDue > 0) {
                            InvoiceSummaryRow(
                                label = if (language == "bn") "পূর্বের বাকি ছিল:" else "Previous Due:",
                                value = "$currency${invoice.previousDue.toIntOrNull() ?: invoice.previousDue}",
                                color = Color(0xFF64748B)
                            )
                        }
                        InvoiceSummaryRow(
                            label = if (language == "bn") "সর্বমোট বর্তমান বকেয়া:" else "Total Current Due:",
                            value = "$currency${invoice.totalCurrentDue.toIntOrNull() ?: invoice.totalCurrentDue}",
                            color = LossRed,
                            isBold = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (language == "bn") "পেমেন্ট মাধ্যম:" else "Payment Method:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = when(invoice.paymentMethod) {
                                "BKASH" -> "bKash (বিকাশ)"
                                "NAGAD" -> "Nagad (নগদ)"
                                "DUE" -> "বাকি (Credit)"
                                else -> "নগদ ক্যাশ (Cash)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    // Send SMS to Customer Card (If Due exists or customer has phone)
                    if (invoice.customerPhone.isNotBlank() || invoice.dueAmount > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF7ED),
                            border = BorderStroke(1.dp, Color(0xFFFED7AA)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (language == "bn") "📱 কাস্টমারকে এসএমএস / নোটিফিকেশন পাঠান" else "📱 Send SMS to Customer",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9A3412)
                                )
                                Text(
                                    text = if (language == "bn") "পূর্বের বকেয়া ও আজকের কেনা পণ্যসহ মোট বাকি পাঠানো হবে" else "Includes previous balance, purchased items & total due",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFC2410C)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val itemsSummary = invoice.items.joinToString("\n") {
                                        "• ${it.product.name} (${it.quantity.toIntOrNull() ?: it.quantity} ${it.product.unit}) = $currency${it.total.toIntOrNull() ?: it.total}"
                                    }
                                    val dueSms = com.example.util.CustomerSmsHelper.buildDueSaleMessage(
                                        shopName = invoice.shopName,
                                        shopPhone = invoice.shopPhone,
                                        customerName = invoice.customerName,
                                        invoiceNo = invoice.invoiceNumber,
                                        purchasedItemsSummary = itemsSummary,
                                        saleTotal = invoice.grandTotal,
                                        paidAmount = invoice.paidAmount,
                                        todayNewDue = invoice.dueAmount,
                                        previousDue = invoice.previousDue,
                                        totalCurrentDue = invoice.totalCurrentDue,
                                        currency = currency
                                    )

                                    Button(
                                        onClick = {
                                            com.example.util.CustomerSmsHelper.sendDirectSms(
                                                context = context,
                                                phone = invoice.customerPhone,
                                                message = dueSms
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (language == "bn") "এসএমএস পাঠান" else "Send SMS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            com.example.util.CustomerSmsHelper.sendWhatsAppMessage(
                                                context = context,
                                                phone = invoice.customerPhone,
                                                message = dueSms
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ProfitGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (language == "bn") "হোয়াটসঅ্যাপ" else "WhatsApp", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (language == "bn") "ধন্যবাদ! আবার আসবেন।" else "Thank you for shopping with us!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF059669)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: PDF Download / Share / Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val pdfFile = PdfGenerator.generateInvoicePdf(context, invoice, currency)
                            if (pdfFile != null) {
                                PdfGenerator.openOrSharePdf(
                                    context,
                                    pdfFile,
                                    if (language == "bn") "পিডিএফ ক্যাশ মেমো ডাউনলোড / প্রিন্ট" else "Download / Print PDF Memo"
                                )
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "bn") "পিডিএফ মেমো" else "PDF Memo", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareBody = buildString {
                                appendLine("🛒 ${invoice.shopName} - ক্যাশ মেমো")
                                appendLine("মেমো নং: ${invoice.invoiceNumber}")
                                appendLine("তারিখ: $dateStr")
                                appendLine("ক্রেতা: ${invoice.customerName}")
                                appendLine("-----------------------------")
                                invoice.items.forEach { item ->
                                    appendLine("${item.product.name} x ${item.quantity.toIntOrNull() ?: item.quantity} = $currency${item.total}")
                                }
                                appendLine("-----------------------------")
                                appendLine("সর্বমোট: $currency${invoice.grandTotal}")
                                appendLine("পরিশোধ: $currency${invoice.paidAmount}")
                                if (invoice.dueAmount > 0) {
                                    appendLine("বাকি: $currency${invoice.dueAmount}")
                                }
                                appendLine("পেমেন্ট মাধ্যম: ${invoice.paymentMethod}")
                                appendLine("ধন্যবাদ!")
                                appendLine("-----------------------------")
                                appendLine(com.example.util.CustomerSmsHelper.SPONSOR_FOOTER)
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Cash Memo"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "bn") "টেক্সট" else "Text")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (language == "bn") "সম্পন্ন" else "Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceSummaryRow(
    label: String,
    value: String,
    color: Color = Color(0xFF1E293B),
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF475569)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = color
        )
    }
}

fun Double.toIntOrNull(): Int? {
    return if (this % 1.0 == 0.0) this.toInt() else null
}
