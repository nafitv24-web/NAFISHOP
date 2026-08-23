package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StockBlue

object PermissionHelper {
    fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        return permissions
    }

    fun areAllPermissionsGranted(context: Context): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun AppPermissionDialog(
    language: String,
    onDismiss: () -> Unit,
    onPermissionsResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val requiredPermissions = remember { PermissionHelper.getRequiredPermissions() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultsMap ->
        val allGranted = resultsMap.values.all { it }
        onPermissionsResult(allGranted)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        icon = {
            Surface(
                shape = CircleShape,
                color = EmeraldPrimary.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Permission Security",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = if (language == "bn") "অ্যাপের প্রয়োজনীয় অনুমতি (Permissions)" else "App Permissions Required",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (language == "bn")
                        "গুগল ড্রাইভে ডাটা ব্যাকআপ সংরক্ষণ, খাতা রিস্টোর এবং অ্যাপের হিসাব সুরক্ষিত রাখতে নিচের অনুমতিগুলো দেওয়া প্রয়োজন:"
                    else
                        "To export/import data to Google Drive, save local backups and keep your shop records secure, please grant these permissions:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 1. Google Drive & Storage Permission Item
                PermissionReasonItem(
                    icon = Icons.Default.CloudSync,
                    iconTint = EmeraldPrimary,
                    title = if (language == "bn") "গুগল ড্রাইভ ও স্টোরেজ এক্সেস" else "Google Drive & Storage Access",
                    description = if (language == "bn")
                        "দোকানের স্টক ও বাকি খাতার ডাটাবেস (.json / .db) নিরাপদে গুগল ড্রাইভে আপলোড ও ডাউনলোড করার জন্য।"
                    else
                        "To upload, download and restore shop backup files to Google Drive."
                )

                // 2. Internet & Network Item
                PermissionReasonItem(
                    icon = Icons.Default.Wifi,
                    iconTint = StockBlue,
                    title = if (language == "bn") "ইন্টারনেট ও ক্লাউড সংযোগ" else "Internet & Cloud Connection",
                    description = if (language == "bn")
                        "ক্লাউড সার্ভারের সাথে সরাসরি যোগাযোগ ও ডাটা আদান-প্রদানের জন্য।"
                    else
                        "To sync data with cloud servers and verify Google Account."
                )

                // 3. Notifications Item
                PermissionReasonItem(
                    icon = Icons.Default.NotificationsActive,
                    iconTint = Color(0xFFF59E0B),
                    title = if (language == "bn") "নোটিফিকেশন ও ব্যাকআপ সতর্কবার্তা" else "Notifications & Backup Alerts",
                    description = if (language == "bn")
                        "ব্যাকআপ সফল হওয়া এবং স্টক ফুরিয়ে যাওয়ার সতর্কবার্তা পাওয়ার জন্য।"
                    else
                        "To receive notifications about backup completion and low stock alerts."
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (language == "bn") "অনুমতি দিন (Allow All)" else "Allow Permissions",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (language == "bn") "পরে দিব" else "Later")
            }
        }
    )
}

@Composable
private fun PermissionReasonItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
