package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.RestoreResult
import com.example.ui.components.AdminPanelDialog
import com.example.ui.components.AppNoticeDialog
import com.example.ui.components.AppPermissionDialog
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.NafiShopFullBrandCard
import com.example.ui.components.NafiShopLogoBadge
import com.example.ui.components.NafiShopSmallLogo
import com.example.ui.components.PermissionHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(
    viewModel: ShopViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    val appUpdateInfo by viewModel.appUpdateInfo.collectAsState()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsState()
    val activeNotice by viewModel.activeNotice.collectAsState()

    var showAdminPanelDialog by remember { mutableStateOf(false) }
    var showAppUpdateDialog by remember { mutableStateOf(false) }
    var showNoticeDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showJsonExportDialog by remember { mutableStateOf(false) }
    var showJsonImportDialog by remember { mutableStateOf(false) }
    var showRestoreResultDialog by remember { mutableStateOf(false) }
    var showDriveExportConfirmDialog by remember { mutableStateOf(false) }
    var showDriveImportConfirmDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var restoreResultData by remember { mutableStateOf<RestoreResult?>(null) }
    var exportedJsonText by remember { mutableStateOf("") }

    // File Pickers
    val saveJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveJsonBackupToUri(context, it) { success ->
                if (success) {
                    Toast.makeText(context, if (language == "bn") "JSON ব্যাকআপ সফলভাবে সেভ হয়েছে!" else "JSON backup saved successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, if (language == "bn") "ব্যাকআপ সেভ ব্যর্থ হয়েছে!" else "Failed to save backup!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val saveDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.saveDatabaseBackupToUri(context, it) { success ->
                if (success) {
                    Toast.makeText(context, if (language == "bn") "SQLite ডাটাবেস ব্যাকআপ সফলভাবে সেভ হয়েছে!" else "SQLite database backup saved successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, if (language == "bn") "ডাটাবেস ব্যাকআপ সেভ ব্যর্থ হয়েছে!" else "Failed to save database backup!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val pickRestoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.restoreFromUri(context, it) { res ->
                restoreResultData = res
                showRestoreResultDialog = true
            }
        }
    }

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
                            NafiShopLogoBadge(size = 50.dp)
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

                    // Dark Mode / Light Mode Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDarkMode() }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (themeMode == "DARK") Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (themeMode == "DARK") AmberTertiary else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "ডার্ক মোড / লাইট মোড" else "Dark Mode / Light Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (themeMode) {
                                        "DARK" -> if (language == "bn") "বর্তমানে: ডার্ক মোড চালু" else "Current: Dark Mode"
                                        "LIGHT" -> if (language == "bn") "বর্তমানে: লাইট মোড চালু" else "Current: Light Mode"
                                        else -> if (language == "bn") "বর্তমানে: সিস্টেম ডিফল্ট" else "Current: System Default"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Switch(
                            checked = themeMode == "DARK",
                            onCheckedChange = { isDark ->
                                viewModel.setThemeMode(if (isDark) "DARK" else "LIGHT")
                            }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "গুগল ক্লাউড ও অটো ব্যাকআপ" else "Google Cloud & Auto Backup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFDCFCE7),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Text(
                                text = if (language == "bn") "সুরক্ষিত" else "Protected",
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfitGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // User Email Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .clickable { showEditProfileDialog = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(if (shopInfo.userEmail.isNotBlank()) Color(0xFFDCFCE7) else Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (shopInfo.userEmail.isNotBlank()) Icons.Default.Mail else Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = if (shopInfo.userEmail.isNotBlank()) ProfitGreen else Color(0xFF475569),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (shopInfo.userEmail.isNotBlank()) shopInfo.userEmail else (if (language == "bn") "আপনার জিমেইল অ্যাকাউন্ট যুক্ত করুন" else "Add your Gmail Account"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (shopInfo.userEmail.isNotBlank()) Color(0xFF1E293B) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (shopInfo.lastBackupDate.isNotEmpty()) {
                                    "${if (language == "bn") "সর্বশেষ ব্যাকআপ: " else "Last Backup: "}${shopInfo.lastBackupDate}"
                                } else {
                                    if (language == "bn") "ট্যাপ করে নিজস্ব জিমেইল সেট করুন" else "Tap to set your Gmail address"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (shopInfo.lastBackupDate.isNotEmpty()) ProfitGreen else Color(0xFF64748B),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Email",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // HERO: Google Drive Cloud Backup & Restore Card (Primary Cloud Solution)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (language == "bn") "গুগল ড্রাইভ ক্লাউড ব্যাকআপ" else "Google Drive Cloud Backup",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (language == "bn") "মোবাইল পরিবর্তন করলেও সব ডাটা ফেরত পান" else "Recover all data anytime when changing phone",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF38BDF8).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Google Drive",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF38BDF8),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // 2 Main Google Drive Actions:
                            // 1. Export to Drive (ড্রাইভে ব্যাকআপ রাখুন)
                            // 2. Import from Drive (ড্রাইভ থেকে রিস্টোর)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!PermissionHelper.areAllPermissionsGranted(context)) {
                                            showPermissionDialog = true
                                        } else {
                                            showDriveExportConfirmDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isSyncing,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "bn") "ড্রাইভে ব্যাকআপ রাখুন" else "Export to Drive",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        if (!PermissionHelper.areAllPermissionsGranted(context)) {
                                            showPermissionDialog = true
                                        } else {
                                            showDriveImportConfirmDialog = true
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isSyncing,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF334155),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (language == "bn") "ড্রাইভ থেকে রিস্টোর" else "Import from Drive",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == "bn")
                                            "আপনার গুগল একাউন্টের ড্রাইভে ফাইল নিরাপদে সংরক্ষিত হয়। অন্য যেকোনো নতুন মোবাইলে এই একাউন্ট দিয়ে এক ক্লিকে সম্পূর্ণ দোকান রিস্টোর করতে পারবেন।"
                                        else
                                            "Data is safely backed up to your Google Drive account. When switching devices, simply log in and restore with one tap.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isSyncing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = EmeraldPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = syncMessage ?: (if (language == "bn") "গুগল ড্রাইভ সিঙ্ক হচ্ছে..." else "Syncing with Google Drive..."),
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }
            }
        }

        // 4. Admin Panel & App Updates Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "এডমিন প্যানেল" else "Admin Panel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (language == "bn") "অ্যাপ আপডেট ও ইউজার নোটিফিকেশন" else "App Updates & User Broadcasts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isUpdateAvailable) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "v${viewModel.currentAppVersion}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isUpdateAvailable) DueOrange else ProfitGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (activeNotice != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNoticeDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${if (language == "bn") "সর্বশেষ নোটিশ: " else "Notice: "}${activeNotice?.title}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = if (language == "bn") "দেখুন" else "View",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Admin Panel Button
                        Button(
                            onClick = { showAdminPanelDialog = true },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == "bn") "এডমিন প্যানেল" else "Admin Panel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Check / Download Update Button
                        OutlinedButton(
                            onClick = {
                                viewModel.checkForUpdates(manualCheck = true) { hasUpdate, msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (hasUpdate) {
                                        showAppUpdateDialog = true
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = null, modifier = Modifier.size(16.dp), tint = StockBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (language == "bn") "আপডেট চেক" else "Check Update",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }

        // 4. Save & Export Manual Backup Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = StockBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "ম্যানুয়াল ব্যাকআপ ফাইল সংরক্ষণ ও শেয়ার" else "Manual Backup & Export",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == "bn")
                            "আপনার ফোনের মেমোরি, গুগল ড্রাইভ বা জিমেইলে ব্যাকআপ ফাইল সংরক্ষণ করে রাখুন।"
                        else
                            "Save backup files directly to phone storage, Google Drive or Email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: Save JSON & Save DB to Drive/Device via SAF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                saveJsonLauncher.launch("DokanKhata_Backup_$timestamp.json")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(18.dp), tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "JSON ফাইল সেভ" else "Save JSON File", style = MaterialTheme.typography.labelMedium)
                        }

                        FilledTonalButton(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                saveDbLauncher.launch("DokanKhata_DB_$timestamp.db")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp), tint = StockBlue)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "ডাটাবেস .db সেভ" else "Save .db File", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row 2: Send via Gmail & Share
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.exportAndShareJsonBackup(context, viaEmail = true) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Mail, contentDescription = null, modifier = Modifier.size(18.dp), tint = LossRed)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "Gmail এ পাঠান" else "Send via Gmail", style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { viewModel.exportAndShareJsonBackup(context, viaEmail = false) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "ফাইল শেয়ার" else "Share File", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // View JSON text button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                exportedJsonText = viewModel.getExportJsonString()
                                showJsonExportDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (language == "bn") "ব্যাকআপ JSON কোড দেখুন ও কপি করুন" else "View & Copy Backup JSON Code", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 5. RESTORE DATA (রিস্টোর করুন - নতুন ফোন বা রি-ইন্সটল এর পর)
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "ডাটা ফিরিয়ে আনুন (Restore Data)" else "Restore Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEFF6FF),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = StockBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn")
                                    "অ্যাপ আনইন্সটল করার পর পুনরায় ইন্সটল করলে অথবা নতুন ফোনে আগের ডাটা ফেরত আনতে পূর্বের সংরক্ষিত .json বা .db ব্যাকআপ ফাইলটি নির্বাচন করুন।"
                                else
                                    "To restore all products, customers, transactions and settings after re-installing the app or on a new phone, pick your backup file (.json or .db).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Restore Button - Pick File from Storage / Drive
                    Button(
                        onClick = { pickRestoreFileLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "📁 ব্যাকআপ ফাইল নির্বাচন করুন (.json / .db)" else "📁 Select Backup File (.json / .db)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Secondary Restore Button - Paste JSON Text
                    OutlinedButton(
                        onClick = { showJsonImportDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == "bn") "📋 JSON টেক্সট পেস্ট করে রিস্টোর" else "📋 Paste JSON Backup Text",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 6. Google Account & Logout Card
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
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "অনলাইন ক্লাউড অ্যাকাউন্ট" else "Online Cloud Account",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = EmeraldPrimary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (language == "bn") "সংযুক্ত" else "Connected",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (shopInfo.userEmail.isNotBlank())
                            (if (language == "bn") "সংযুক্ত জিমেইল: ${shopInfo.userEmail}" else "Linked Gmail: ${shopInfo.userEmail}")
                        else
                            (if (language == "bn") "কোনো জিমেইল আইডি লগিন করা নেই" else "No Gmail account logged in"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditProfileDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "অ্যাকাউন্ট পরিবর্তন" else "Change Account")
                        }

                        Button(
                            onClick = { viewModel.logoutUser() },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed.copy(alpha = 0.9f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (language == "bn") "লগআউট" else "Logout")
                        }
                    }
                }
            }
        }

        // 7. App Permissions & Security Card
        item {
            val allGranted = PermissionHelper.areAllPermissionsGranted(context)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = if (allGranted) EmeraldPrimary else Color(0xFFF59E0B),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "অ্যাপ পারমিশন ও নিরাপত্তা" else "App Permissions & Access",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (allGranted) EmeraldPrimary.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (allGranted)
                                    (if (language == "bn") "অনুমোদিত" else "Granted")
                                else
                                    (if (language == "bn") "অনুমতি বাকি" else "Action Needed"),
                                color = if (allGranted) EmeraldPrimary else Color(0xFFD97706),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (allGranted)
                            (if (language == "bn") "✅ গুগল ড্রাইভ ক্লাউড ব্যাকআপ ও স্টোরেজ এক্সেসের সকল প্রয়োজনীয় পারমিশন সক্রিয় আছে।" else "✅ All storage and Google Drive permissions are granted.")
                        else
                            (if (language == "bn") "⚠️ গুগল ড্রাইভে ফাইল সেভ ও রিস্টোর করার জন্য স্টোরেজ ও নেটওয়ার্ক পারমিশন সক্রিয় করুন।" else "⚠️ Please grant permissions to enable smooth Google Drive backup & restore."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!allGranted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showPermissionDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "পারমিশন অনুমোদন করুন" else "Grant App Permissions",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 8. Official Brand & About App Card
        item {
            NafiShopFullBrandCard(
                tagline = if (language == "bn") "দোকানদারের হিসাবের বিশ্বস্ত সঙ্গী • ভার্সন ${viewModel.currentAppVersion}" else "Your Trusted Business Ledger Partner • v${viewModel.currentAppVersion}",
                showTagline = true
            )
        }
    }

    // Edit Shop Profile Dialog
    if (showEditProfileDialog) {
        EditProfileDialog(
            shopInfo = shopInfo,
            language = language,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, owner, phone, address, curr, email ->
                viewModel.updateShopInfo(name, owner, phone, address, curr, email)
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
                        text = if (language == "bn") "আপনার সব পণ্য, কাস্টমার, হিসাব ও বিক্রয়ের ব্যাকআপ কোড তৈরি হয়েছে:" else "Copy this backup text to keep safe:",
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
            title = { Text(if (language == "bn") "JSON টেক্সট থেকে রিস্টোর" else "Restore from JSON Text") },
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
                        placeholder = { Text("{\"appName\": \"ShopKhata\", \"products\": [...]}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackupJson(importInputText) { res ->
                            showJsonImportDialog = false
                            restoreResultData = res
                            showRestoreResultDialog = true
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

    // Admin Panel Dialog
    if (showAdminPanelDialog) {
        AdminPanelDialog(
            viewModel = viewModel,
            onDismiss = { showAdminPanelDialog = false }
        )
    }

    // App Update Available Dialog
    if (showAppUpdateDialog) {
        AppUpdateDialog(
            updateInfo = appUpdateInfo,
            currentVersion = viewModel.currentAppVersion,
            language = language,
            onDismiss = { showAppUpdateDialog = false }
        )
    }

    // User Notice Dialog
    if (showNoticeDialog && activeNotice != null) {
        AppNoticeDialog(
            notice = activeNotice!!,
            language = language,
            onDismiss = { showNoticeDialog = false }
        )
    }

    // Drive Export Confirmation Dialog
    if (showDriveExportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDriveExportConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (language == "bn") "গুগল ড্রাইভে রপ্তানি নিশ্চিত করুন" else "Confirm Export to Google Drive",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == "bn")
                            "আপনার দোকানের সকল স্টক, বিক্রয় ভাউচার, বাকি খাতা ও খরচের হিসাব গুগল ড্রাইভ ক্লাউড স্টোরেজে আপলোড ও সিঙ্ক হবে।"
                        else
                            "All inventory, customer dues, sales vouchers and expenses will be uploaded to Google Drive AppFolder.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${if (language == "bn") "সংযুক্ত জিমেইল: " else "Google Account: "}${shopInfo.userEmail.ifBlank { "Google Account" }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (shopInfo.lastBackupDate.isNotEmpty()) {
                                Text(
                                    text = "${if (language == "bn") "পূর্বের ব্যাকআপ: " else "Previous Backup: "}${shopInfo.lastBackupDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDriveExportConfirmDialog = false
                        viewModel.exportToGoogleDriveCloud(context) { success, timeStr ->
                            if (success) {
                                Toast.makeText(
                                    context,
                                    if (language == "bn") "✅ গুগল ড্রাইভে সফলভাবে রপ্তানি হয়েছে! ($timeStr)" else "✅ Successfully exported to Google Drive! ($timeStr)",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == "bn") "রপ্তানি শুরু করুন" else "Start Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveExportConfirmDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Drive Import Confirmation Dialog
    if (showDriveImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDriveImportConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = StockBlue,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (language == "bn") "গুগল ড্রাইভ থেকে আমদানি" else "Import from Google Drive",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == "bn")
                            "গুগল ড্রাইভ ক্লাউড থেকে সর্বশেষ ব্যাকআপ ফাইলটি নামিয়ে বর্তমান দোকানের ডাটা রিস্টোর করা হবে।"
                        else
                            "The latest backup from Google Drive will be downloaded and restored.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${if (language == "bn") "অ্যাকাউন্ট: " else "Account: "}${shopInfo.userEmail.ifBlank { "Google Account" }}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDriveImportConfirmDialog = false
                        viewModel.importFromGoogleDriveCloud(context) { res ->
                            restoreResultData = res
                            showRestoreResultDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StockBlue)
                ) {
                    Text(if (language == "bn") "আমদানি ও রিস্টোর" else "Import & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDriveImportConfirmDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Restore Result Dialog
    if (showRestoreResultDialog && restoreResultData != null) {
        val result = restoreResultData!!
        AlertDialog(
            onDismissRequest = { showRestoreResultDialog = false },
            icon = {
                Icon(
                    imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (result.success) ProfitGreen else LossRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (result.success)
                        (if (language == "bn") "ডাটা রিস্টোর সম্পন্ন!" else "Restore Successful!")
                    else
                        (if (language == "bn") "রিস্টোর ব্যর্থ হয়েছে" else "Restore Failed")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (result.success) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(
                            text = if (language == "bn") "উদ্ধারকৃত তথ্যের বিবরণ:" else "Restored Items Summary:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        result.restoredShopInfo?.let { s ->
                            val balText = if (s.mainBalance % 1.0 == 0.0) s.mainBalance.toLong().toString() else String.format(Locale.US, "%.2f", s.mainBalance)
                            Text(
                                text = "• ${if (language == "bn") "দোকানের মূল ক্যাশ (হাতে নগদ): " else "Cash Balance: "}${s.currency}$balText",
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        }
                        if (result.cashLogCount > 0) {
                            Text(text = "• ${if (language == "bn") "ক্যাশ লেনদেন ও খতিয়ান: " else "Cash Ledger Logs: "}${result.cashLogCount} টি")
                        }
                        if (result.productCount > 0) {
                            Text(text = "• ${if (language == "bn") "পণ্য তালিকা: " else "Products: "}${result.productCount} টি")
                        }
                        if (result.customerCount > 0) {
                            Text(text = "• ${if (language == "bn") "গ্রাহক/বাকি খাতা: " else "Customers: "}${result.customerCount} জন")
                        }
                        if (result.transactionCount > 0) {
                            Text(text = "• ${if (language == "bn") "বিক্রয় ও লেনদেন: " else "Transactions: "}${result.transactionCount} টি")
                        }
                        if (result.expenseCount > 0) {
                            Text(text = "• ${if (language == "bn") "খরচের হিসাব: " else "Expenses: "}${result.expenseCount} টি")
                        }
                        if (result.dueLogCount > 0) {
                            Text(text = "• ${if (language == "bn") "বাকির খতিয়ান: " else "Due Records: "}${result.dueLogCount} টি")
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!result.success) {
                        OutlinedButton(
                            onClick = {
                                showRestoreResultDialog = false
                                pickRestoreFileLauncher.launch("*/*")
                            }
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (language == "bn") "ফাইল নির্বাচন" else "Pick File")
                        }
                    }
                    Button(onClick = { showRestoreResultDialog = false }) {
                        Text(if (language == "bn") "ঠিক আছে" else "OK")
                    }
                }
            }
        )
    }

    // App Permission Dialog
    if (showPermissionDialog) {
        AppPermissionDialog(
            language = language,
            onDismiss = { showPermissionDialog = false },
            onPermissionsResult = { granted ->
                showPermissionDialog = false
            }
        )
    }
}

@Composable
fun EditProfileDialog(
    shopInfo: com.example.data.model.ShopInfo,
    language: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String) -> Unit
) {
    var shopName by remember { mutableStateOf(shopInfo.shopName) }
    var ownerName by remember { mutableStateOf(shopInfo.ownerName) }
    var userEmail by remember { mutableStateOf(shopInfo.userEmail) }
    var phone by remember { mutableStateOf(shopInfo.phone) }
    var address by remember { mutableStateOf(shopInfo.address) }
    var currency by remember { mutableStateOf(shopInfo.currency) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (language == "bn") "দোকান ও প্রোফাইল তথ্য" else "Shop & Profile Details",
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
                    label = { Text(if (language == "bn") "আপনার নাম / মালিকের নাম" else "Your / Owner Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = userEmail,
                    onValueChange = { userEmail = it },
                    label = { Text(if (language == "bn") "আপনার নিজস্ব জিমেইল (Gmail)" else "Your Gmail (for Backup)") },
                    placeholder = { Text("example@gmail.com") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                var emailError by remember { mutableStateOf<String?>(null) }

                if (emailError != null) {
                    Text(
                        text = emailError!!,
                        color = LossRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

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
                            val cleanMail = userEmail.trim().lowercase()
                            val gmailRegex = "^[a-zA-Z0-9._%+-]{4,30}@gmail\\.com$".toRegex()
                            if (cleanMail.isNotBlank() && (!cleanMail.matches(gmailRegex) || cleanMail.substringBefore("@").length < 5)) {
                                emailError = if (language == "bn") "সঠিক জিমেইল আইডি দিন (যেমন: yourname@gmail.com)!" else "Please enter a valid Gmail address!"
                                return@Button
                            }
                            if (shopName.isNotBlank()) {
                                onSave(shopName, ownerName, phone, address, currency, cleanMail)
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
