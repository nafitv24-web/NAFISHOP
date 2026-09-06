package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.LossRed
import com.example.util.CalculationHelper
import com.example.util.CustomerSmsHelper

@Composable
fun PaymentCollectedSmsDialog(
    customer: Customer,
    collectedAmount: Double,
    previousDue: Double,
    shopName: String,
    shopPhone: String,
    currency: String = "৳",
    language: String = "bn",
    transactionType: String = "COLLECTED", // "COLLECTED" (জমা) or "GIVEN" (বাকি)
    note: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isGiven = transactionType == "GIVEN"
    val remainingDue = if (isGiven) {
        (previousDue + collectedAmount).coerceAtLeast(0.0)
    } else {
        (previousDue - collectedAmount).coerceAtLeast(0.0)
    }

    val smsMessage = remember(customer, collectedAmount, previousDue, shopName, shopPhone, note, transactionType) {
        CustomerSmsHelper.buildLedgerTransactionMessage(
            shopName = shopName.ifBlank { "NAFI KHATA" },
            shopPhone = shopPhone,
            customerName = customer.name,
            type = transactionType,
            amount = collectedAmount,
            note = note.ifBlank { if (isGiven) "নতুন বাকি প্রদান" else "বাকি টাকা জমা আদায়" },
            previousDue = previousDue,
            totalCurrentDue = remainingDue,
            currency = currency
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isGiven) LossRed.copy(alpha = 0.12f) else ProfitGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGiven) Icons.Default.Receipt else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isGiven) LossRed else ProfitGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isGiven) {
                        if (language == "bn") "বাকি এন্ট্রি সফল হয়েছে!" else "Due Entry Saved!"
                    } else {
                        if (language == "bn") "টাকা জমা সফল হয়েছে!" else "Payment Collected Successfully!"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isGiven) LossRed else ProfitGreen,
                    fontSize = 18.sp
                )

                Text(
                    text = if (language == "bn") "গ্রাহককে তাৎক্ষণিক রসিদ মেসেজ পাঠান:" else "Send instant receipt to customer:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = customer.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (customer.phone.isNotBlank()) {
                                Text(
                                    text = customer.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            thickness = 0.8.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isGiven) "প্রদত্ত বাকি:" else "জমা আদায়:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currency${CalculationHelper.formatAmount(collectedAmount)}",
                                fontWeight = FontWeight.Bold,
                                color = if (isGiven) LossRed else ProfitGreen,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "পূর্বের বাকি ছিল:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$currency${CalculationHelper.formatAmount(previousDue)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "বর্তমান অবশিষ্ট বাকি:",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "$currency${CalculationHelper.formatAmount(remainingDue)}",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (remainingDue > 0) LossRed else ProfitGreen,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        if (note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "মন্তব্য: $note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Send Options Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // WhatsApp Button
                    Button(
                        onClick = {
                            CustomerSmsHelper.sendWhatsAppMessage(context, customer.phone, smsMessage)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "WhatsApp এ রসিদ পাঠান" else "Send via WhatsApp",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Direct SMS Button
                    OutlinedButton(
                        onClick = {
                            CustomerSmsHelper.sendDirectSms(context, customer.phone, smsMessage)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            Icons.Default.Message,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "সরাসরি SMS পাঠান" else "Send Direct SMS",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Generic Share Button (IMO / Messenger / Bluetooth)
                    OutlinedButton(
                        onClick = {
                            CustomerSmsHelper.shareFallback(context, smsMessage, "কাস্টমারকে রসিদ মেসেজ পাঠান")
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "অন্যান্য মাধ্যমে শেয়ার করুন" else "Share via Other Apps",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    // Dismiss Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (language == "bn") "মেসেজ ছাড়া বন্ধ করুন" else "Dismiss",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
