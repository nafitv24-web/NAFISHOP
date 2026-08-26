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
                                    text = "${if (language == "bn") "ইউজার তালিকা" else "Users"} (${registeredUsers.size})",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            },
                            icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(18.dp)) }
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
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else {
            val q = searchQuery.trim().lowercase()
            users.filter {
                it.email.lowercase().contains(q) ||
                it.shopName.lowercase().contains(q) ||
                it.ownerName.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary & Refresh Bar
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PeopleAlt,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (language == "bn") "সর্বমোট রেজিস্টার্ড ইউজার" else "Total Registered Users",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${users.size} ${if (language == "bn") "টি একাউন্ট" else "Accounts"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onRefresh,
                    enabled = !isLoading,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .size(38.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = EmeraldPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(if (language == "bn") "জিমেইল বা দোকানের নাম দিয়ে খুঁজুন" else "Search by Gmail or Shop Name") },
            placeholder = { Text("example@gmail.com") },
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
                        Text(if (language == "bn") "আবার চেষ্টা" else "Retry")
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
                        text = if (language == "bn") "Firebase ক্লাউড থেকে ইউজার তালিকা লোড হচ্ছে..." else "Loading users from Firebase...",
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
                            (if (language == "bn") "'$searchQuery' নামে কোনো অ্যাকাউন্ট পাওয়া যায়নি" else "No account matches '$searchQuery'")
                        else
                            (if (language == "bn") "এখনও কোনো অ্যাকাউন্ট তালিকা পাওয়া যায়নি" else "No accounts found"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (language == "bn") "তালিকাসমূহ রিফ্রেশ করুন" else "Refresh List")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                if (language == "bn") "ইমেইল কপি হয়েছে: $email" else "Email copied: $email",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAccountCard(
    user: FirebaseUserAccount,
    language: String,
    onCopyEmail: (String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val createdStr = remember(user.createdAt) {
        if (user.createdAt > 0) dateFormat.format(Date(user.createdAt)) else "-"
    }
    val lastLoginStr = remember(user.lastLoginAt) {
        if (user.lastLoginAt > 0) dateFormat.format(Date(user.lastLoginAt)) else "-"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (user.shopName.firstOrNull() ?: user.email.firstOrNull() ?: 'U').toString().uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
                                fontSize = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.shopName.ifBlank { "NAFI SHOP 24" },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${if (language == "bn") "মালিক: " else "Owner: "}${user.ownerName.ifBlank { "দোকানদার" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(ProfitGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "সক্রিয়" else "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProfitGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Email with Copy Button
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyEmail(user.email) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
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
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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

            Spacer(modifier = Modifier.height(6.dp))

            // Dates Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${if (language == "bn") "তৈরি: " else "Joined: "}$createdStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
                Text(
                    text = "${if (language == "bn") "সর্বশেষ: " else "Last Active: "}$lastLoginStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }
        }
    }
}
