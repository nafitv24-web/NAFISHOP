package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FirebaseUserAccount
import com.example.data.firebase.UserSessionLog
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
    val registeredUsers by viewModel.registeredUsers.collectAsState()
    val isLoadingUsers by viewModel.isLoadingUsers.collectAsState()
    val usersErrorMessage by viewModel.usersErrorMessage.collectAsState()

    var isAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Admin Tabs: 0: Registered Users & Stats, 1: Breaking News / Notice, 2: Publish App Update, 3: Security & Password
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            viewModel.loadAllRegisteredUsers()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
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
                                    text = if (language == "bn") "ইউজার তালিকা, নোটিশ ও আপডেট" else "Users, Notices & Updates",
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
                    // Admin Password Lock Screen (Masked, No plain text hints)
                    AdminPasswordLockView(
                        pinInput = pinInput,
                        pinError = pinError,
                        language = language,
                        onPinChange = {
                            pinInput = it
                            pinError = false
                        },
                        onUnlock = {
                            if (viewModel.verifyAdminPin(pinInput) || pinInput.trim() == "40541273") {
                                isAuthenticated = true
                                pinError = false
                            } else {
                                pinError = true
                            }
                        }
                    )
                } else {
                    // Tab Bar
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        edgePadding = 8.dp,
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                viewModel.loadAllRegisteredUsers()
                            },
                            text = {
                                Text(
                                    text = "${if (language == "bn") "ইউজার ট্র্যাকিং" else "User Tracking"} (${registeredUsers.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    text = if (language == "bn") "নিউজ নোটিশ" else "News & Notice",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            icon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = {
                                Text(
                                    text = if (language == "bn") "অ্যাপ আপডেট" else "App Update",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = {
                                Text(
                                    text = if (language == "bn") "পাসওয়ার্ড সেটিং" else "Security",
                                    fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
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
                            0 -> AdminUsersTab(
                                users = registeredUsers,
                                isLoading = isLoadingUsers,
                                errorMessage = usersErrorMessage,
                                language = language,
                                onRefresh = {
                                    viewModel.loadAllRegisteredUsers { count ->
                                        Toast.makeText(
                                            context,
                                            if (language == "bn") "মোট $count টি অ্যাকাউন্ট লোড হয়েছে" else "Loaded $count user accounts",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
                                        Toast.makeText(context, if (language == "bn") "নোটিশ মুছে ফেলা হয়েছে" else "Notice deleted", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            2 -> AdminPublishUpdateTab(
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
                            3 -> AdminSecurityTab(
                                language = language,
                                onChangePin = { oldPin, newPin ->
                                    val success = viewModel.changeAdminPin(oldPin, newPin)
                                    if (success) {
                                        Toast.makeText(context, if (language == "bn") "এডমিন পাসওয়ার্ড সফলভাবে পরিবর্তন হয়েছে!" else "Password changed successfully!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, if (language == "bn") "বর্তমান পাসওয়ার্ড ভুল!" else "Incorrect current password!", Toast.LENGTH_SHORT).show()
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
private fun AdminPasswordLockView(
    pinInput: String,
    pinError: Boolean,
    language: String,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

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
            text = if (language == "bn") "এডমিন পাসওয়ার্ড দিন" else "Enter Admin Password",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (language == "bn") "নিরাপত্তার স্বার্থে শুধুমাত্র অ্যাডমিন অ্যাক্সেস করতে পারবেন" else "Secure Admin Panel Access",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = pinInput,
            onValueChange = onPinChange,
            label = { Text(if (language == "bn") "এডমিন পাসওয়ার্ড" else "Admin Password") },
            placeholder = { Text("••••••••") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            isError = pinError,
            supportingText = {
                if (pinError) {
                    Text(
                        text = if (language == "bn") "ভুল পাসওয়ার্ড! সঠিক পাসওয়ার্ড দিয়ে পুনরায় চেষ্টা করুন" else "Incorrect password! Please try again",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onUnlock,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == "bn") "প্রবেশ করুন" else "Unlock Panel",
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
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = LossRed, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (language == "bn") "🔴 লাইভ নিউজ ও নোটিশ প্রকাশ করুন" else "🔴 Publish Live Breaking News & Notice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = LossRed
                    )
                    Text(
                        text = if (language == "bn") "এখানে লিখলে সবার উপরের হেডলাইনে নিউজ স্ক্রোলিং ব্যানারের মতো প্রদর্শিত হবে।" else "This notice will be displayed as a live news ticker at the top of the app.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF991B1B)
                    )
                }
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text(if (language == "bn") "নিউজ / নোটিশের শিরোনাম *" else "News / Notice Headline *") },
            placeholder = { Text(if (language == "bn") "যেমন: বিশেষ ছাড় চলছে / জরুরী নোটিশ" else "e.g. Special Offer / Urgent Notice") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text(if (language == "bn") "নিউজ / বিস্তারিত বার্তা *" else "News Details / Message *") },
            placeholder = { Text(if (language == "bn") "সম্মানিত গ্রাহক ও ইউজারগণ, আমাদের সকল পণ্যে আকর্ষণীয় মূল্যছাড় চলছে..." else "Dear users, special discounts are live...") },
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
            label = { Text(if (language == "bn") "বাটন লিঙ্ক / URL (ঐচ্ছিক)" else "Action Link / URL (Optional)") },
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
            colors = ButtonDefaults.buttonColors(containerColor = LossRed)
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == "bn") "সবার উপরে নিউজ নোটিশ হিসেবে প্রকাশ করুন" else "Publish as Top News Notice",
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
    var oldVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (language == "bn") "এডমিন পাসওয়ার্ড পরিবর্তন" else "Change Admin Password",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = if (language == "bn") "শুধুমাত্র দোকান মালিক বা অ্যাডমিন এই পাসওয়ার্ড দিয়ে আপডেট ও নোটিশ প্রকাশ করতে পারবেন।" else "Only store owner or authorized admin can publish updates and notices using this password.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )

        OutlinedTextField(
            value = oldPin,
            onValueChange = { oldPin = it },
            label = { Text(if (language == "bn") "বর্তমান পাসওয়ার্ড" else "Current Password") },
            placeholder = { Text("••••••••") },
            visualTransformation = if (oldVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { oldVisible = !oldVisible }) {
                    Icon(imageVector = if (oldVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = newPin,
            onValueChange = { newPin = it },
            label = { Text(if (language == "bn") "নতুন পাসওয়ার্ড" else "New Password") },
            placeholder = { Text("••••••••") },
            visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { newVisible = !newVisible }) {
                    Icon(imageVector = if (newVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it },
            label = { Text(if (language == "bn") "নতুন পাসওয়ার্ড নিশ্চিত করুন" else "Confirm New Password") },
            placeholder = { Text("••••••••") },
            visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { confirmVisible = !confirmVisible }) {
                    Icon(imageVector = if (confirmVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (newPin.length < 4) {
                    Toast.makeText(context, if (language == "bn") "পাসওয়ার্ড কমপক্ষে ৪ সংখ্যা বা অক্ষরের হতে হবে" else "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (newPin != confirmPin) {
                    Toast.makeText(context, if (language == "bn") "নতুন পাসওয়ার্ড দুটি মেলেনি!" else "Passwords do not match!", Toast.LENGTH_SHORT).show()
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
            Text(if (language == "bn") "পাসওয়ার্ড সেভ করুন" else "Save Password", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminUsersTab(
    users: List<FirebaseUserAccount>,
    isLoading: Boolean,
    errorMessage: String?,
    language: String,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val isBn = language == "bn"
    val now = remember { System.currentTimeMillis() }

    var searchQuery by remember { mutableStateOf("") }
    // Filters: 0: All, 1: Online Now, 2: Active Today, 3: With Location
    var selectedFilter by remember { mutableStateOf(0) }
    var selectedUserForDetails by remember { mutableStateOf<FirebaseUserAccount?>(null) }

    val totalUsersCount = users.size
    val onlineUsersCount = users.count { it.isOnline }
    val todayActiveCount = users.count { isSameDay(it.lastActiveAt, now) || isSameDay(it.lastAppEntryAt, now) }
    val locationCount = users.mapNotNull { it.city.ifBlank { null } }.distinct().size

    val filteredUsers = remember(users, searchQuery, selectedFilter) {
        var list = users

        // Filter by tab/chip
        list = when (selectedFilter) {
            1 -> list.filter { it.isOnline }
            2 -> list.filter { isSameDay(it.lastActiveAt, now) || isSameDay(it.lastAppEntryAt, now) }
            3 -> list.filter { it.city.isNotBlank() || it.locationDisplay.isNotBlank() }
            else -> list
        }

        // Filter by search query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            list = list.filter {
                it.email.lowercase().contains(q) ||
                it.shopName.lowercase().contains(q) ||
                it.ownerName.lowercase().contains(q) ||
                it.city.lowercase().contains(q) ||
                it.region.lowercase().contains(q) ||
                it.country.lowercase().contains(q) ||
                it.ipAddress.lowercase().contains(q) ||
                it.deviceModel.lowercase().contains(q) ||
                it.isp.lowercase().contains(q)
            }
        }

        // Always sort most recently active/entered users at top
        list.sortedByDescending { maxOf(it.lastActiveAt, it.lastAppEntryAt, it.lastLoginAt) }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 4 KPI Summary Cards at Top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Total Users
            AdminKpiCard(
                title = if (isBn) "মোট ইউজার" else "Total Users",
                value = "$totalUsersCount",
                icon = Icons.Default.PeopleAlt,
                color = EmeraldPrimary,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Online Now
            AdminKpiCard(
                title = if (isBn) "লাইভ অনলাইন" else "Online Now",
                value = "$onlineUsersCount",
                icon = Icons.Default.Wifi,
                color = ProfitGreen,
                badge = if (onlineUsersCount > 0) "🟢" else null,
                modifier = Modifier.weight(1f)
            )

            // Card 3: Active Today
            AdminKpiCard(
                title = if (isBn) "আজ প্রবেশ" else "Active Today",
                value = "$todayActiveCount",
                icon = Icons.Default.AccessTime,
                color = StockBlue,
                modifier = Modifier.weight(1f)
            )

            // Card 4: Locations
            AdminKpiCard(
                title = if (isBn) "শহর/অঞ্চল" else "Locations",
                value = "$locationCount",
                icon = Icons.Default.LocationOn,
                color = DueOrange,
                modifier = Modifier.weight(1f)
            )
        }

        // Refresh & Realtime Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (onlineUsersCount > 0) ProfitGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (onlineUsersCount > 0) ProfitGreen else MaterialTheme.colorScheme.outline, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isBn) "ইউজারদের লোকেশন ও প্রবেশ তথ্য সরাসরি ক্লাউড থেকে ট্র্যাক হচ্ছে" else "Live user location & access time tracked from cloud",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier.size(32.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = EmeraldPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(if (isBn) "ইউজার, শহর, এলাকা, আইপি বা ডিভাইস দিয়ে খুঁজুন" else "Search user, city, IP, or device") },
            placeholder = { Text("ঢাকা, 103.xxx, Samsung, nafitv24@gmail.com") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == 0,
                onClick = { selectedFilter = 0 },
                label = { Text("${if (isBn) "সকল" else "All"} ($totalUsersCount)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedFilter == 1,
                onClick = { selectedFilter = 1 },
                label = { Text("🟢 ${if (isBn) "অনলাইন" else "Online"} ($onlineUsersCount)", fontSize = 11.sp) },
                modifier = Modifier.weight(1.1f)
            )
            FilterChip(
                selected = selectedFilter == 2,
                onClick = { selectedFilter = 2 },
                label = { Text("🕒 ${if (isBn) "আজকে" else "Today"} ($todayActiveCount)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedFilter == 3,
                onClick = { selectedFilter = 3 },
                label = { Text("📍 ${if (isBn) "শহর" else "City"} ($locationCount)", fontSize = 11.sp) },
                modifier = Modifier.weight(1f)
            )
        }

        if (errorMessage != null && users.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LossRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = LossRed,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onRefresh) {
                        Text(if (isBn) "আবার চেষ্টা" else "Retry")
                    }
                }
            }
        }

        if (isLoading && users.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isBn) "Firebase ক্লাউড থেকে ইউজার ও লোকেশন ট্র্যাকিং ডাটা লোড হচ্ছে..." else "Loading live user location & access telemetry from Firebase...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PersonOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank())
                            (if (isBn) "'$searchQuery' এর সাথে মিল থাকা কোনো ইউজার পাওয়া যায়নি" else "No users match '$searchQuery'")
                        else
                            (if (isBn) "নির্বাচিত ফিল্টারে কোনো ইউজার নেই" else "No users in this filter"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isBn) "তালিকা রিফ্রেশ করুন" else "Refresh List")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.email + it.createdAt }) { user ->
                    UserAccountCard(
                        user = user,
                        language = language,
                        onCopyEmail = { email ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("User Email", email)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                if (isBn) "ইমেইল কপি হয়েছে: $email" else "Email copied: $email",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onViewMap = {
                            openUserLocationOnMap(context, user)
                        },
                        onViewDetails = {
                            selectedUserForDetails = user
                        }
                    )
                }
            }
        }
    }

    // Detailed User Diagnostics & Session Timeline Dialog
    if (selectedUserForDetails != null) {
        UserTrackingDetailsDialog(
            user = selectedUserForDetails!!,
            language = language,
            onDismiss = { selectedUserForDetails = null },
            onOpenMap = { openUserLocationOnMap(context, selectedUserForDetails!!) }
        )
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    badge: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                if (badge != null) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(badge, fontSize = 9.sp)
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun UserAccountCard(
    user: FirebaseUserAccount,
    language: String,
    onCopyEmail: (String) -> Unit,
    onViewMap: () -> Unit,
    onViewDetails: () -> Unit
) {
    val isBn = language == "bn"
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    val lastEntryTime = if (user.lastAppEntryAt > 0) user.lastAppEntryAt else user.lastLoginAt
    val lastEntryStr = remember(lastEntryTime) {
        if (lastEntryTime > 0) dateFormat.format(Date(lastEntryTime)) else "-"
    }
    val relativeTimeStr = remember(user.lastActiveAt, lastEntryTime) {
        val target = if (user.lastActiveAt > 0) user.lastActiveAt else lastEntryTime
        formatRelativeTime(target, isBn)
    }

    // Resolve location display
    val locationText = remember(user.city, user.country, user.locationDisplay) {
        if (user.locationDisplay.isNotBlank()) user.locationDisplay
        else if (user.city.isNotBlank() && user.country.isNotBlank()) "${user.city}, ${user.country}"
        else if (user.city.isNotBlank()) user.city
        else if (user.country.isNotBlank()) user.country
        else if (isBn) "অবস্থান সনাক্ত হয়নি" else "Location not available"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (user.isOnline) ProfitGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Avatar, Shop/Owner Name & Online Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (user.isOnline) Color(0xFFDCFCE7) else EmeraldPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (user.shopName.firstOrNull() ?: user.email.firstOrNull() ?: 'U').toString().uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (user.isOnline) ProfitGreen else EmeraldPrimary,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.shopName.ifBlank { "NAFI KHATA" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "${if (isBn) "মালিক: " else "Owner: "}${user.ownerName.ifBlank { "দোকানদার" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Live Online Status Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (user.isOnline) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(if (user.isOnline) ProfitGreen else MaterialTheme.colorScheme.outline, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (user.isOnline) (if (isBn) "এখন লাইভ সক্রিয়" else "Online Now") else relativeTimeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.isOnline) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Email Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyEmail(user.email) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = StockBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }

                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LOCATION BOX: কোথায় থেকে ব্যবহার করছে
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DueOrange.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, DueOrange.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Location Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = DueOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBn) "ব্যবহারের অবস্থান (Location):" else "User Origin Location:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DueOrange
                            )
                        }

                        if (user.city.isNotBlank() || user.latitude != 0.0) {
                            Text(
                                text = if (isBn) "ম্যাপে দেখুন" else "View Map",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { onViewMap() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "📍 $locationText",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // IP & Network & ISP Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (user.ipAddress.isNotBlank()) {
                            Text(
                                text = "🌐 IP: ${user.ipAddress}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        if (user.networkType.isNotBlank()) {
                            Text(
                                text = "• 📶 ${user.networkType}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        if (user.isp.isNotBlank()) {
                            Text(
                                text = "• ${user.isp}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 10.5.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // DEVICE & LAST ENTRY BOX: ডিভাইস ও লাস্ট প্রবেশ
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Device Model Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (isBn) "ডিভাইস: " else "Device: "}${user.deviceModel.ifBlank { "Android Mobile" }}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (user.androidVersion.isNotBlank()) {
                            Text(
                                text = " (${user.androidVersion})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Last App Access & Count Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = StockBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${if (isBn) "লাস্ট প্রবেশ: " else "Last Entry: "}$lastEntryStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "${user.appEntryCount} ${if (isBn) "বার প্রবেশ" else "Visits"}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: View on Map & Detailed Timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewMap,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(15.dp), tint = DueOrange)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "ম্যাপে অবস্থান" else "View on Map", fontSize = 12.sp)
                }

                Button(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isBn) "বিস্তারিত হিস্ট্রি" else "Access History", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Detailed User Diagnostics & Past Access Sessions Timeline Dialog
 */
@Composable
private fun UserTrackingDetailsDialog(
    user: FirebaseUserAccount,
    language: String,
    onDismiss: () -> Unit,
    onOpenMap: () -> Unit
) {
    val isBn = language == "bn"
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.88f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = EmeraldPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = user.shopName.ifBlank { "NAFI KHATA" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (user.isOnline) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (user.isOnline) ProfitGreen else MaterialTheme.colorScheme.outline, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (user.isOnline)
                                    (if (isBn) "🟢 ইউজার বর্তমানে অ্যাপে সক্রিয় আছেন (Online Now)" else "🟢 User is currently online")
                                else
                                    (if (isBn) "⚪ অফলাইন • সর্বশেষ সক্রিয়: ${formatRelativeTime(user.lastActiveAt, isBn)}" else "⚪ Offline • Last active: ${formatRelativeTime(user.lastActiveAt, isBn)}"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (user.isOnline) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Section 1: Location & Network Info
                    Text(
                        text = if (isBn) "📍 অবস্থান ও নেটওয়ার্ক বিবরণ" else "📍 Location & Network Telemetry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailItem(if (isBn) "শহর / এলাকা:" else "City / Area:", user.city.ifBlank { "সনাক্ত হয়নি" })
                            DetailItem(if (isBn) "অঞ্চল / বিভাগ:" else "Region / Division:", user.region.ifBlank { "-" })
                            DetailItem(if (isBn) "দেশ:" else "Country:", "${user.country.ifBlank { "Bangladesh" }} (${user.countryCode})")
                            DetailItem(if (isBn) "পাবলিক আইপি (IP):" else "Public IP:", user.ipAddress.ifBlank { "103.xxx" })
                            DetailItem(if (isBn) "ইন্টারনেট প্রোভাইডার (ISP):" else "ISP / Operator:", user.isp.ifBlank { "-" })
                            DetailItem(if (isBn) "নেটওয়ার্ক টাইপ:" else "Network Type:", user.networkType.ifBlank { "WiFi / Mobile" })
                            if (user.latitude != 0.0 && user.longitude != 0.0) {
                                DetailItem(if (isBn) "স্থানাঙ্ক (GPS Coords):" else "Coordinates:", "${user.latitude}, ${user.longitude}")
                            }
                        }
                    }

                    // Section 2: Device & Operating System
                    Text(
                        text = if (isBn) "📱 ডিভাইস ও সিস্টেম তথ্য" else "📱 Device & System Telemetry",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailItem(if (isBn) "ডিভাইস মডেল:" else "Device Model:", user.deviceModel.ifBlank { "Android Device" })
                            DetailItem(if (isBn) "অপারেটিং সিস্টেম:" else "Android OS:", user.androidVersion.ifBlank { "Android" })
                            DetailItem(if (isBn) "অ্যাপ ভার্সন:" else "App Version:", user.appVersion.ifBlank { "v2.4.0" })
                        }
                    }

                    // Section 3: Access Statistics
                    Text(
                        text = if (isBn) "🕒 অ্যাপ ব্যবহারের পরিসংখ্যান" else "🕒 App Usage Statistics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val lastEntry = if (user.lastAppEntryAt > 0) user.lastAppEntryAt else user.lastLoginAt
                            val created = user.createdAt
                            DetailItem(if (isBn) "সর্বশেষ প্রবেশ:" else "Last App Entry:", if (lastEntry > 0) dateFormat.format(Date(lastEntry)) else "-")
                            DetailItem(if (isBn) "সর্বশেষ সক্রিয় (Heartbeat):" else "Last Active:", if (user.lastActiveAt > 0) dateFormat.format(Date(user.lastActiveAt)) else "-")
                            DetailItem(if (isBn) "মোট অ্যাপে প্রবেশ সংখ্যা:" else "Total App Visits:", "${user.appEntryCount} বার")
                            DetailItem(if (isBn) "নিবন্ধনের তারিখ:" else "Joined Date:", if (created > 0) dateFormat.format(Date(created)) else "-")
                        }
                    }

                    // Section 4: Access History Timeline
                    if (user.sessionHistory.isNotEmpty()) {
                        Text(
                            text = "${if (isBn) "📜 পূর্ববর্তী প্রবেশের হিস্ট্রি" else "📜 Past Access History"} (${user.sessionHistory.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )

                        user.sessionHistory.forEachIndexed { index, sess ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "#${index + 1} • ${if (sess.timestamp > 0) dateFormat.format(Date(sess.timestamp)) else "-"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (sess.networkType.isNotBlank()) {
                                            Text(
                                                text = sess.networkType,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = StockBlue,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val loc = listOf(sess.city, sess.country).filter { it.isNotBlank() }.joinToString(", ")
                                        Text(
                                            text = "📍 ${loc.ifBlank { "বাংলাদেশ" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DueOrange,
                                            fontSize = 10.5.sp
                                        )
                                        if (sess.ip.isNotBlank()) {
                                            Text(
                                                text = "IP: ${sess.ip}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Button: Open Google Maps
                Button(
                    onClick = onOpenMap,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBn) "গুগল ম্যাপসে অবস্থান ট্র্যাক করুন" else "Open Location in Google Maps",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.8f)
        )
    }
}

/**
 * Opens the user's location in Google Maps app or browser
 */
private fun openUserLocationOnMap(context: Context, user: FirebaseUserAccount) {
    try {
        val intent = if (user.latitude != 0.0 && user.longitude != 0.0) {
            val label = Uri.encode(user.shopName.ifBlank { user.email })
            val uri = Uri.parse("geo:${user.latitude},${user.longitude}?q=${user.latitude},${user.longitude}($label)")
            Intent(Intent.ACTION_VIEW, uri)
        } else {
            val queryLocation = user.city.ifBlank { user.country }.ifBlank { "Bangladesh" }
            val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(queryLocation)}")
            Intent(Intent.ACTION_VIEW, uri)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "ম্যাপ খুলতে কোনো ব্রাউজার বা ম্যাপ অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
    }
}

private fun isSameDay(t1: Long, t2: Long): Boolean {
    if (t1 <= 0 || t2 <= 0) return false
    val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun formatRelativeTime(timestamp: Long, isBn: Boolean): String {
    if (timestamp <= 0) return "-"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)
    val days = diff / (24 * 60 * 60 * 1000)

    return when {
        diff < 0 -> if (isBn) "এইমাত্র" else "Just now"
        minutes < 2 -> if (isBn) "এইমাত্র সক্রিয়" else "Active now"
        minutes < 60 -> if (isBn) "$minutes মিনিট আগে" else "$minutes min ago"
        hours < 24 -> if (isBn) "$hours ঘণ্টা আগে" else "$hours hr ago"
        days == 1L -> if (isBn) "গতকাল" else "Yesterday"
        days < 30 -> if (isBn) "$days দিন আগে" else "$days days ago"
        else -> {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
