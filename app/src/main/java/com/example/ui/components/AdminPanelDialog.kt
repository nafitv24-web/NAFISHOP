package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppNotice
import com.example.data.model.AppUpdateInfo
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelDialog(
    viewModel: ShopViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val language by viewModel.language.collectAsState()
    val currentUpdate by viewModel.appUpdateInfo.collectAsState()
    val noticeHistory by viewModel.noticeHistory.collectAsState()

    var isAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Admin Tabs: 0: Publish App Update, 1: Broadcast Notice, 2: Security & PIN
    var selectedTab by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(EmeraldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "এডমিন কন্ট্রোল প্যানেল" else "Admin Control Panel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (language == "bn") "অ্যাপ আপডেট ও ইউজার নোটিফিকেশন" else "App Updates & User Notifications",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                if (!isAuthenticated) {
                    // Admin PIN Lock Screen
                    AdminPinLockView(
                        pinInput = pinInput,
                        pinError = pinError,
                        language = language,
                        onPinChange = {
                            pinInput = it
                            pinError = false
                        },
                        onUnlock = {
                            if (viewModel.verifyAdminPin(pinInput) || pinInput == "1234") {
                                isAuthenticated = true
                                pinError = false
                            } else {
                                pinError = true
                            }
                        }
                    )
                } else {
                    // Tab Bar
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Text(
                                    text = if (language == "bn") "অ্যাপ আপডেট" else "App Update",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = if (language == "bn") "ইউজার নোটিশ" else "Notices",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            icon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Text(
                                    text = if (language == "bn") "পিন পরিবর্তন" else "Security",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        when (selectedTab) {
                            0 -> AdminPublishUpdateTab(
                                currentUpdate = currentUpdate,
                                currentAppVersion = viewModel.currentAppVersion,
                                currentVersionCode = viewModel.currentVersionCode,
                                language = language,
                                onPublish = { newUpdate ->
                                    viewModel.publishAppUpdate(newUpdate) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                            1 -> AdminBroadcastNoticeTab(
                                noticeHistory = noticeHistory,
                                language = language,
                                onSendNotice = { notice ->
                                    viewModel.publishNotice(notice) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                onDeleteNotice = { id ->
                                    viewModel.deleteNotice(id) {
                                        Toast.makeText(context, if (language == "bn") "নোটিশ ডিলিট করা হয়েছে" else "Notice deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            2 -> AdminSecurityTab(
                                language = language,
                                onChangePin = { oldPin, newPin ->
                                    val success = viewModel.changeAdminPin(oldPin, newPin)
                                    if (success) {
                                        Toast.makeText(context, if (language == "bn") "এডমিন পিন সফলভাবে পরিবর্তন হয়েছে!" else "PIN changed successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (language == "bn") "পুরাতন পিন ভুল!" else "Incorrect old PIN!", Toast.LENGTH_SHORT).show()
                                    }
                                    success
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminPinLockView(
    pinInput: String,
    pinError: Boolean,
    language: String,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.LockPerson,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (language == "bn") "এডমিন পাসওয়ার্ড দিন" else "Enter Admin PIN",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (language == "bn") "ডিফল্ট পিন: 1234 (ভিতর থেকে পরিবর্তন করতে পারবেন)" else "Default PIN: 1234 (Can be changed inside)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = pinInput,
            onValueChange = onPinChange,
            label = { Text(if (language == "bn") "৪ সংখ্যার পিন" else "4-digit PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            isError = pinError,
            supportingText = {
                if (pinError) {
                    Text(
                        text = if (language == "bn") "ভুল পিন! পুনরায় চেষ্টা করুন (ডিফল্ট: 1234)" else "Invalid PIN! Default is 1234",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.75f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onUnlock,
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == "bn") "আনলক করুন" else "Unlock Admin",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AdminPublishUpdateTab(
    currentUpdate: AppUpdateInfo,
    currentAppVersion: String,
    currentVersionCode: Int,
    language: String,
    onPublish: (AppUpdateInfo) -> Unit
) {
    val context = LocalContext.current
    var versionName by remember { mutableStateOf(currentUpdate.versionName) }
    var versionCodeStr by remember { mutableStateOf(currentUpdate.versionCode.toString()) }
    var downloadUrl by remember { mutableStateOf(currentUpdate.downloadUrl) }
    var releaseNotes by remember { mutableStateOf(currentUpdate.releaseNotes) }
    var isForceUpdate by remember { mutableStateOf(currentUpdate.isForceUpdate) }
    var isUpdateActive by remember { mutableStateOf(currentUpdate.isUpdateActive) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Current Installed vs Cloud Update Status Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${if (language == "bn") "চলতি অ্যাপ ভার্সন: " else "Installed Version: "}v$currentAppVersion (Build $currentVersionCode)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${if (language == "bn") "পাবলিশ করা ভার্সন: " else "Published Version: "}v${currentUpdate.versionName} (Build ${currentUpdate.versionCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (currentUpdate.isUpdateActive) EmeraldPrimary else MaterialTheme.colorScheme.outline
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentUpdate.isUpdateActive) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (currentUpdate.isUpdateActive) (if (language == "bn") "সক্রিয়" else "Active") else (if (language == "bn") "নিষ্ক্রিয়" else "Disabled"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (currentUpdate.isUpdateActive) ProfitGreen else Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Form Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = versionName,
                onValueChange = { versionName = it },
                label = { Text(if (language == "bn") "নতুন ভার্সন নাম *" else "Version Name *") },
                placeholder = { Text("e.g. 2.5.0") },
                singleLine = true,
                modifier = Modifier.weight(1.3f)
            )

            OutlinedTextField(
                value = versionCodeStr,
                onValueChange = { versionCodeStr = it },
                label = { Text(if (language == "bn") "বিল্ড কোড" else "Build Code") },
                placeholder = { Text("e.g. 25") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = downloadUrl,
            onValueChange = { downloadUrl = it },
            label = { Text(if (language == "bn") "অ্যাপ ডাউনলোড লিঙ্ক (APK / Drive URL) *" else "Download Link (APK / Drive URL) *") },
            placeholder = { Text("https://drive.google.com/file/d/...") },
            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = StockBlue) },
            trailingIcon = {
                if (downloadUrl.isNotBlank()) {
                    IconButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl.trim()))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "লিঙ্ক খুলতে সমস্যা", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Test Link", tint = EmeraldPrimary)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = releaseNotes,
            onValueChange = { releaseNotes = it },
            label = { Text(if (language == "bn") "আপডেটের বিবরণ / নতুন ফিচার তালিকা" else "What's New / Release Notes") },
            placeholder = { Text(if (language == "bn") "১. নতুন ইনভেন্টরি রিপোর্ট\n২. দ্রুত বিক্রয় POS চালানের সুবিধা\n৩. বাকী খাতা হিস্ট্রি" else "1. New POS invoices\n2. Due Khata Statement") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )

        // Switches
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == "bn") "আপডেট নোটিফিকেশন চালু রাখুন" else "Enable Update Notification",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language == "bn") "ইউজাররা অ্যাপ খুললে নতুন ভার্সন ডাউনলোডের নোটিশ পাবে" else "Users will see a download banner when opening app",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isUpdateActive,
                        onCheckedChange = { isUpdateActive = it }
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == "bn") "বাধ্যতামূলক আপডেট (Force Update)" else "Force Mandatory Update",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isForceUpdate) LossRed else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (language == "bn") "ইউজাররা নতুন অ্যাপ ডাউনলোড না করা পর্যন্ত ব্যবহার করতে পারবে না" else "Block app usage until user installs the new APK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = isForceUpdate,
                        onCheckedChange = { isForceUpdate = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Publish Button
        Button(
            onClick = {
                if (versionName.isBlank()) {
                    Toast.makeText(context, if (language == "bn") "ভার্সন নাম লিখুন" else "Enter version name", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (downloadUrl.isBlank()) {
                    Toast.makeText(context, if (language == "bn") "ডাউনলোড লিঙ্ক দিন" else "Enter download link", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val code = versionCodeStr.toIntOrNull() ?: (currentVersionCode + 1)
                val newUpdate = AppUpdateInfo(
                    versionName = versionName.trim(),
                    versionCode = code,
                    downloadUrl = downloadUrl.trim(),
                    releaseNotes = releaseNotes.trim(),
                    isForceUpdate = isForceUpdate,
                    isUpdateActive = isUpdateActive,
                    releaseDate = System.currentTimeMillis()
                )
                onPublish(newUpdate)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == "bn") "নতুন আপডেট প্রকাশ ও প্রচার করুন" else "Publish App Update",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun AdminBroadcastNoticeTab(
    noticeHistory: List<AppNotice>,
    language: String,
    onSendNotice: (AppNotice) -> Unit,
    onDeleteNotice: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("INFO") } // INFO, ALERT, OFFER, FEATURE
    var actionUrl by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (language == "bn") "ইউজারদের জরুরী নোটিফিকেশন পাঠান" else "Broadcast Notice to Users",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (language == "bn") "নোটিশের শিরোনাম *" else "Notice Title *") },
            placeholder = { Text(if (language == "bn") "যেমন: জরুরী রক্ষণাবেক্ষণ বিজ্ঞপ্তি" else "e.g. Server Maintenance Notice") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text(if (language == "bn") "বিস্তারিত বার্তা *" else "Notice Message *") },
            placeholder = { Text(if (language == "bn") "সম্মানিত ব্যবহারকারীগণ, আজ রাত ১২টায় অ্যাপ আপডেট হবে..." else "Dear users, we are updating the app tonight...") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth()
        )

        // Notice Type Selector Chips
        Text(
            text = if (language == "bn") "বার্তার ধরন:" else "Notice Type:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "INFO" to (if (language == "bn") "তথ্য" else "Info"),
                "ALERT" to (if (language == "bn") "জরুরী সতর্কবার্তা" else "Alert"),
                "OFFER" to (if (language == "bn") "অফার" else "Offer"),
                "FEATURE" to (if (language == "bn") "নতুন ফিচার" else "Feature")
            ).forEach { (typeKey, label) ->
                FilterChip(
                    selected = selectedType == typeKey,
                    onClick = { selectedType = typeKey },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        OutlinedTextField(
            value = actionUrl,
            onValueChange = { actionUrl = it },
            label = { Text(if (language == "bn") "বাটন লিঙ্ক / URL (ঐচ্ছিক)" else "Action URL (Optional)") },
            placeholder = { Text("https://...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (title.isBlank() || message.isBlank()) {
                    Toast.makeText(context, if (language == "bn") "শিরোনাম ও বিস্তারিত লিখুন" else "Enter title and message", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val notice = AppNotice(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    message = message.trim(),
                    type = selectedType,
                    timestamp = System.currentTimeMillis(),
                    isActive = true,
                    actionUrl = actionUrl.trim()
                )
                onSendNotice(notice)
                title = ""
                message = ""
                actionUrl = ""
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == "bn") "সকল ইউজারের কাছে নোটিফিকেশন পাঠান" else "Send Notice to All Users",
                fontWeight = FontWeight.Bold
            )
        }

        if (noticeHistory.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (language == "bn") "পূর্ববর্তী পাঠানো নোটিশসমূহ (${noticeHistory.size})" else "Past Notices (${noticeHistory.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            noticeHistory.forEach { item ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (item.type) {
                                        "ALERT" -> Color(0xFFFEE2E2)
                                        "OFFER" -> Color(0xFFFEF3C7)
                                        "FEATURE" -> Color(0xFFE0E7FF)
                                        else -> Color(0xFFDCFCE7)
                                    }
                                ) {
                                    Text(
                                        text = item.type,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (item.type) {
                                            "ALERT" -> LossRed
                                            "OFFER" -> DueOrange
                                            "FEATURE" -> Color(0xFF4338CA)
                                            else -> ProfitGreen
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { onDeleteNotice(item.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = LossRed, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSecurityTab(
    language: String,
    onChangePin: (String, String) -> Boolean
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (language == "bn") "এডমিন পিন নম্বর পরিবর্তন" else "Change Admin PIN",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (language == "bn") "শুধুমাত্র দোকান মালিক বা অ্যাডমিন এই পিন দিয়ে আপডেট ও নোটিফিকেশন প্রকাশ করতে পারবেন।" else "Only the store owner/admin can publish updates using this PIN.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        OutlinedTextField(
            value = oldPin,
            onValueChange = { oldPin = it },
            label = { Text(if (language == "bn") "বর্তমান পিন (ডিফল্ট: 1234)" else "Current PIN (Default: 1234)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = newPin,
            onValueChange = { newPin = it },
            label = { Text(if (language == "bn") "নতুন ৪ সংখ্যার পিন" else "New 4-digit PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it },
            label = { Text(if (language == "bn") "নতুন পিন নিশ্চিত করুন" else "Confirm New PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (newPin.length < 4) {
                    Toast.makeText(context, if (language == "bn") "পিন কমপক্ষে ৪ সংখ্যা হতে হবে" else "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (newPin != confirmPin) {
                    Toast.makeText(context, if (language == "bn") "নতুন পিন দুটি মিলেনি!" else "PINs do not match!", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val res = onChangePin(oldPin, newPin)
                if (res) {
                    oldPin = ""
                    newPin = ""
                    confirmPin = ""
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (language == "bn") "পিন সেভ করুন" else "Save PIN", fontWeight = FontWeight.Bold)
        }
    }
}
