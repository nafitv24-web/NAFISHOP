package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.ui.theme.DueOrange
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LossRed
import com.example.util.StockNotificationHelper

/**
 * Dialog for setting global low-stock threshold, automated notification preference,
 * and bulk-applying user threshold to inventory items.
 */
@Composable
fun StockAlertSettingsDialog(
    currentThreshold: Double,
    isNotificationEnabled: Boolean,
    language: String,
    onDismiss: () -> Unit,
    onSave: (threshold: Double, enableNotification: Boolean, applyToAll: Boolean) -> Unit
) {
    val context = LocalContext.current
    var thresholdStr by remember {
        mutableStateOf(if (currentThreshold % 1.0 == 0.0) currentThreshold.toInt().toString() else currentThreshold.toString())
    }
    var notificationEnabled by remember { mutableStateOf(isNotificationEnabled) }
    var applyToAllProducts by remember { mutableStateOf(false) }

    val presetValues = listOf(2.0, 5.0, 10.0, 15.0, 20.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == "bn") "স্টক সতর্কীকরণ সীমা ও সেটিংস" else "Low Stock Alert Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (language == "bn") "ব্যবহারকারী নির্ধারিত স্টক অ্যালার্ট" else "User-Defined Minimum Threshold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Automated Notification Switch Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "bn") "স্বয়ংক্রিয় পুশ নোটিফিকেশন" else "Automated Push Alerts",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == "bn") "স্টক সীমার নিচে নামলে স্বয়ংক্রিয় নোটিফিকেশন ও সাউন্ড বাজবে।" else "Send push notifications when stock drops below threshold.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = notificationEnabled,
                            onCheckedChange = { notificationEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Minimum Stock Threshold Input
                Text(
                    text = if (language == "bn") "ডিফল্ট সতর্কীকরণ সীমা (পিস/একক):" else "Default Minimum Threshold (Qty):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = { thresholdStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = DueOrange)
                    },
                    trailingIcon = {
                        Text(
                            text = if (language == "bn") "পিস" else "Units",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Preset Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    presetValues.forEach { preset ->
                        val isSelected = thresholdStr.toDoubleOrNull() == preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = if (isSelected) BorderStroke(1.dp, Color(0xFFF59E0B)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    thresholdStr = if (preset % 1.0 == 0.0) preset.toInt().toString() else preset.toString()
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${preset.toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Apply to All Products Checkbox
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (applyToAllProducts) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (applyToAllProducts) Color(0xFF93C5FD) else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { applyToAllProducts = !applyToAllProducts }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = applyToAllProducts,
                            onCheckedChange = { applyToAllProducts = it },
                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = if (language == "bn") "দোকানের সকল বর্তমান পণ্যে এই সীমা সেট করুন" else "Apply this threshold to all inventory items",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (applyToAllProducts) Color(0xFF1E40AF) else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == "bn") "আগের সকল ভিন্ন লিমিট পরিবর্তন করে এই নতুন সীমা নির্ধারিত হবে।" else "Updates the alert limit on all existing products.",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Test Notification Action
                OutlinedButton(
                    onClick = {
                        val testProd = Product(
                            id = 999999,
                            name = "টেস্ট পণ্য (স্যাম্পল)",
                            stockQuantity = 2.0,
                            minStockAlert = thresholdStr.toDoubleOrNull() ?: 5.0,
                            unit = "পিস"
                        )
                        StockNotificationHelper.notifyProductLowStock(
                            context = context,
                            product = testProd,
                            threshold = testProd.minStockAlert,
                            force = true
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD97706)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == "bn") "🔔 টেস্ট নোটিফিকেশন চেক করুন" else "Test Push Notification",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (language == "bn") "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val th = thresholdStr.toDoubleOrNull() ?: 5.0
                            onSave(th, notificationEnabled, applyToAllProducts)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "সংরক্ষণ করুন" else "Save Settings",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog for quickly editing a single product's minimum stock threshold
 */
@Composable
fun SingleProductThresholdDialog(
    product: Product,
    language: String,
    onDismiss: () -> Unit,
    onSave: (newThreshold: Double) -> Unit
) {
    var thresholdStr by remember {
        mutableStateOf(if (product.minStockAlert % 1.0 == 0.0) product.minStockAlert.toInt().toString() else product.minStockAlert.toString())
    }
    val presets = listOf(2.0, 5.0, 10.0, 20.0)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = DueOrange, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == "bn") "স্টক সতর্কীকরণ সীমা পরিবর্তন" else "Change Alert Threshold",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product Stock Info
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "বর্তমান স্টক:" else "Current Stock:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${product.stockQuantity} ${product.unit}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (product.stockQuantity <= product.minStockAlert) LossRed else EmeraldPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (language == "bn") "ন্যূনতম সতর্কীকরণ সীমা (${product.unit}):" else "Minimum Alert Limit (${product.unit}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = { thresholdStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { p ->
                        val isSel = thresholdStr.toDoubleOrNull() == p
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSel) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = if (isSel) BorderStroke(1.dp, Color(0xFFF59E0B)) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    thresholdStr = if (p % 1.0 == 0.0) p.toInt().toString() else p.toString()
                                }
                        ) {
                            Box(modifier = Modifier.padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${p.toInt()} ${product.unit}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (language == "bn") "💡 স্টক এই সংখ্যার নিচে নামলে স্বয়ংক্রিয় নোটিফিকেশন ও হাইলাইট অ্যালার্ট সক্রিয় হবে।"
                    else "💡 Automated notification & highlight will trigger when stock drops below this number.",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (language == "bn") "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newTh = thresholdStr.toDoubleOrNull() ?: product.minStockAlert
                            onSave(newTh)
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text(if (language == "bn") "আপডেট করুন" else "Update", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
