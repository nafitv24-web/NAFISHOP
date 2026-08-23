package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showJsonExportDialog by remember { mutableStateOf(false) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Shop Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = shopInfo.shopName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${shopInfo.ownerName} • ${shopInfo.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                if (shopInfo.address.isNotBlank()) {
                                    Text(
                                        text = shopInfo.address,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // 2. Language & Preferences
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "bn") "ভাষা ও পছন্দসমূহ" else "Language & Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleLanguage() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "অ্যাপের ভাষা (Language)" else "App Language",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "bn") "বর্তমানে: বাংলা" else "Current: English",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Switch(
                            checked = language == "bn",
                            onCheckedChange = { viewModel.toggleLanguage() }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "মুদ্রা প্রতীক (Currency)" else "Currency Symbol",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "bn") "ডিফল্ট: ৳ (টাকা)" else "Default: ৳ (BDT)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Text(
                            text = shopInfo.currency,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 3. Google Account & Cloud Backup (Google Drive & Gmail Integration)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "গুগল ড্রাইভ ও জিমেইল ব্যাকআপ" else "Google Drive & Gmail Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFDCFCE7),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Text(
                                text = if (language == "bn") "সার্ভার ছাড়া সুরক্ষিত" else "Serverless & Private",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfitGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shopInfo.userEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "${if (language == "bn") "সর্বশেষ ক্লাউড ব্যাকআপ: " else "Last Backup: "}${shopInfo.lastBackupDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfitGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = ProfitGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Google Drive Cloud Sync Button
                    Button(
                        onClick = { viewModel.backupToGoogleCloud() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (language == "bn") "ব্যাকআপ হচ্ছে..." else "Backing up...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (language == "bn") "গুগল ড্রাইভে ডাটা ব্যাকআপ নিন" else "Backup Data to Google Drive")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Export & Share SQLite .db Database directly to Gmail / Drive
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exportAndShareDatabase(context, viaEmail = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp), tint = LossRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "Gmail এ .db পাঠান" else "Send .db via Gmail",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportAndShareDatabase(context, viaEmail = false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "ডাটাবেস .db শেয়ার" else "Share .db File",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // 4. Local JSON Backup & Restore
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (language == "bn") "JSON ফাইল ব্যাকআপ ও রিস্টোর" else "JSON Backup & Restore",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exportAndShareJsonBackup(context, viaEmail = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp), tint = StockBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "Gmail এ JSON" else "Email JSON", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportedJsonText = viewModel.getExportJsonString()
                                    showJsonExportDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "JSON দেখুন" else "View JSON", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { showJsonImportDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "রিস্টোর" else "Restore", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 5. About App Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "দোকান খাতা ও ইনভেন্টরি ম্যানেজার",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "সংস্করণ ১.০ • অফলাইন-ফার্স্ট এবং ক্লাউড রেডি",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }

    // Edit Shop Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            shopInfo = shopInfo,
            language = language,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, owner, phone, address, curr ->
                viewModel.updateShopInfo(name, owner, phone, address, curr)
                showEditProfileDialog = false
            }
        )
    }

    // JSON Export Dialog
    if (showJsonExportDialog) {
        AlertDialog(
            onDismissRequest = { showJsonExportDialog = false },
            title = { Text(if (language == "bn") "ব্যাকআপ JSON ডাটা" else "Export JSON Data") },
            text = {
                Column {
                    Text(
                        text = if (language == "bn") "আপনার সব পণ্য, কাস্টমার এবং খরচের হিসাবের কপি নিচে তৈরি হয়েছে:" else "Copy this backup text to keep safe:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("ShopKhataBackup", exportedJsonText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, if (language == "bn") "ক্লিপবোর্ডে কপি হয়েছে!" else "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                    showJsonExportDialog = false
                }) {
                    Text(if (language == "bn") "কপি করুন" else "Copy")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonExportDialog = false }) {
                    Text(if (language == "bn") "বন্ধ করুন" else "Close")
                }
            }
        )
    }

    // JSON Import Dialog
    if (showJsonImportDialog) {
        var importInputText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showJsonImportDialog = false },
            title = { Text(if (language == "bn") "ব্যাকআপ থেকে রিস্টোর" else "Restore from JSON") },
            text = {
                Column {
                    Text(
                        text = if (language == "bn") "পূর্বে সেভ করা JSON টেক্সট এখানে পেস্ট করুন:" else "Paste the exported JSON text below:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        placeholder = { Text("{\"products\": [...]}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackupJson(importInputText) { success ->
                            if (success) {
                                Toast.makeText(context, if (language == "bn") "ডাটা সফলভাবে রিস্টোর হয়েছে!" else "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                showJsonImportDialog = false
                            } else {
                                Toast.makeText(context, if (language == "bn") "ভুল ফরম্যাট! অনুগ্রহ করে সঠিক JSON দিন।" else "Invalid JSON format!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = importInputText.isNotBlank()
                ) {
                    Text(if (language == "bn") "রিস্টোর করুন" else "Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJsonImportDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    shopInfo: com.example.data.model.ShopInfo,
    language: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var shopName by remember { mutableStateOf(shopInfo.shopName) }
    var ownerName by remember { mutableStateOf(shopInfo.ownerName) }
    var phone by remember { mutableStateOf(shopInfo.phone) }
    var address by remember { mutableStateOf(shopInfo.address) }
    var currency by remember { mutableStateOf(shopInfo.currency) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "দোকানের প্রোফাইল সম্পাদনা" else "Edit Shop Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text(if (language == "bn") "দোকানের নাম *" else "Shop Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(if (language == "bn") "মালিকের নাম" else "Owner Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (language == "bn") "মোবাইল নম্বর" else "Contact Phone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (language == "bn") "দোকানের ঠিকানা" else "Shop Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = currency,
                    onValueChange = { currency = it },
                    label = { Text(if (language == "bn") "কারেন্সি প্রতীক" else "Currency Symbol") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                            if (shopName.isNotBlank()) {
                                onSave(shopName, ownerName, phone, address, currency)
                            }
                        },
                        enabled = shopName.isNotBlank()
                    ) {
                        Text(if (language == "bn") "সংরক্ষণ করুন" else "Save")
                    }
                }
            }
        }
    }
}
