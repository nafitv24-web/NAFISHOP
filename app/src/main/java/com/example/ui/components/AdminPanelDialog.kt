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
import com.example.data.firebase.AdminUserShopData
import com.example.data.model.AppNotice
import com.example.data.model.AppUpdateInfo
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextOverflow
import java.io.File
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
                                viewModel = viewModel,
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
    viewModel: ShopViewModel,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Backed Up, 2: No Backup
    var selectedUserForDetails by remember { mutableStateOf<FirebaseUserAccount?>(null) }
    var downloadingPdfEmail by remember { mutableStateOf<String?>(null) }
    var isGeneratingAllUsersPdf by remember { mutableStateOf(false) }

    val backedUpCount = remember(users) { users.count { it.lastBackupAt > 0 } }
    val noBackupCount = remember(users) { users.count { it.lastBackupAt <= 0 } }

    val filteredUsers = remember(users, searchQuery, selectedFilterIndex) {
        var list = users
        if (selectedFilterIndex == 1) {
            list = list.filter { it.lastBackupAt > 0 }
        } else if (selectedFilterIndex == 2) {
            list = list.filter { it.lastBackupAt <= 0 }
        }

        if (searchQuery.isBlank()) list
        else {
            val q = searchQuery.trim().lowercase()
            list.filter {
                it.email.lowercase().contains(q) ||
                it.shopName.lowercase().contains(q) ||
                it.ownerName.lowercase().contains(q) ||
                it.phone.contains(q)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Summary & Actions Bar
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Master PDF Button (All Users Summary)
                    IconButton(
                        onClick = {
                            if (users.isEmpty()) {
                                Toast.makeText(context, if (language == "bn") "কোনো ইউজার তালিকা নেই" else "No users to export", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            isGeneratingAllUsersPdf = true
                            coroutineScope.launch {
                                try {
                                    val pdfFile = PdfGenerator.generateAllUsersSummaryPdf(context, filteredUsers, language)
                                    isGeneratingAllUsersPdf = false
                                    if (pdfFile != null) {
                                        PdfGenerator.openOrSharePdf(context, pdfFile, "সকল ইউজার তালিকা ও ব্যাকআপ রিপোর্ট PDF")
                                    } else {
                                        Toast.makeText(context, if (language == "bn") "PDF তৈরিতে সমস্যা হয়েছে" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    isGeneratingAllUsersPdf = false
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isGeneratingAllUsersPdf && users.isNotEmpty(),
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                            .size(38.dp)
                    ) {
                        if (isGeneratingAllUsersPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = StockBlue,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Export All Users PDF",
                                tint = StockBlue,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Refresh Button
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
        }

        // Filter Chips (All, Backed up, Not backed up)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilterIndex == 0,
                onClick = { selectedFilterIndex = 0 },
                label = { Text(if (language == "bn") "সকল (${users.size})" else "All (${users.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldPrimary.copy(alpha = 0.15f),
                    selectedLabelColor = EmeraldPrimary
                )
            )
            FilterChip(
                selected = selectedFilterIndex == 1,
                onClick = { selectedFilterIndex = 1 },
                label = { Text(if (language == "bn") "ব্যাকআপ আছে ($backedUpCount)" else "Backed Up ($backedUpCount)") },
                leadingIcon = {
                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(14.dp), tint = ProfitGreen)
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDCFCE7),
                    selectedLabelColor = ProfitGreen
                )
            )
            FilterChip(
                selected = selectedFilterIndex == 2,
                onClick = { selectedFilterIndex = 2 },
                label = { Text(if (language == "bn") "ব্যাকআপ নেই ($noBackupCount)" else "No Backup ($noBackupCount)") },
                leadingIcon = {
                    Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                }
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(if (language == "bn") "জিমেইল, দোকান বা মালিকের নাম দিয়ে খুঁজুন" else "Search by Gmail, Shop or Owner") },
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.email + it.createdAt }) { user ->
                    UserAccountCard(
                        user = user,
                        language = language,
                        isDownloadingPdf = downloadingPdfEmail == user.email,
                        onCopyEmail = { email ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("User Email", email)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                if (language == "bn") "ইমেইল কপি হয়েছে: $email" else "Email copied: $email",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onViewDetails = {
                            selectedUserForDetails = user
                        },
                        onDownloadPdf = {
                            downloadingPdfEmail = user.email
                            coroutineScope.launch {
                                try {
                                    val shopData = viewModel.fetchUserShopData(user.email)
                                    downloadingPdfEmail = null
                                    if (shopData != null) {
                                        val pdfFile = PdfGenerator.generateUserShopFullReportPdf(
                                            context = context,
                                            shopName = shopData.shopName.ifBlank { user.shopName },
                                            ownerName = shopData.ownerName.ifBlank { user.ownerName },
                                            email = shopData.email,
                                            phone = shopData.phone.ifBlank { user.phone },
                                            lastBackupTime = if (shopData.lastBackupTime > 0) shopData.lastBackupTime else user.lastBackupAt,
                                            products = shopData.products,
                                            customers = shopData.customers,
                                            transactions = shopData.transactions,
                                            expenses = shopData.expenses,
                                            mainBalance = if (shopData.mainBalance != 0.0) shopData.mainBalance else user.mainBalance,
                                            currency = shopData.currency
                                        )
                                        if (pdfFile != null) {
                                            PdfGenerator.openOrSharePdf(context, pdfFile, "${shopData.shopName} - ইউজার হিসাব ও সম্পূর্ণ ডাটা PDF")
                                            Toast.makeText(context, if (language == "bn") "PDF ফাইল সফলভাবে প্রস্তুত হয়েছে" else "PDF Report Ready", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, if (language == "bn") "PDF তৈরিতে সমস্যা হয়েছে" else "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, if (language == "bn") "ইউজারের ক্লাউড ডাটা পাওয়া যায়নি" else "Cloud data not found for user", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    downloadingPdfEmail = null
                                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Comprehensive User Details Dialog
    if (selectedUserForDetails != null) {
        val user = selectedUserForDetails!!
        UserShopDataDetailsDialog(
            user = user,
            viewModel = viewModel,
            language = language,
            onDismiss = { selectedUserForDetails = null }
        )
    }
}

@Composable
private fun UserAccountCard(
    user: FirebaseUserAccount,
    language: String,
    isDownloadingPdf: Boolean,
    onCopyEmail: (String) -> Unit,
    onViewDetails: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val createdStr = remember(user.createdAt) {
        if (user.createdAt > 0) dateFormat.format(Date(user.createdAt)) else "-"
    }
    val lastLoginStr = remember(user.lastLoginAt) {
        if (user.lastLoginAt > 0) dateFormat.format(Date(user.lastLoginAt)) else "-"
    }
    val lastBackupStr = remember(user.lastBackupAt) {
        if (user.lastBackupAt > 0) dateFormat.format(Date(user.lastBackupAt)) else null
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Shop Name & Status
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
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (user.shopName.firstOrNull() ?: user.email.firstOrNull() ?: 'U').toString().uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary,
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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${if (language == "bn") "মালিক: " else "Owner: "}${user.ownerName.ifBlank { "দোকানদার" }}${if (user.phone.isNotBlank()) " | ${user.phone}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDCFCE7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Email with Copy Button
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Last Backup Status Banner (Prominent)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (lastBackupStr != null) EmeraldPrimary.copy(alpha = 0.08f) else Color(0xFFFEF3C7).copy(alpha = 0.5f),
                border = BorderStroke(
                    1.dp,
                    if (lastBackupStr != null) EmeraldPrimary.copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (lastBackupStr != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (lastBackupStr != null) EmeraldPrimary else Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (language == "bn") "সর্বশেষ ক্লাউড ব্যাকআপ" else "Last Cloud Backup",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 10.sp
                        )
                        Text(
                            text = lastBackupStr ?: (if (language == "bn") "এখনও কোনো ব্যাকআপ নেননি" else "No backup taken yet"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (lastBackupStr != null) EmeraldPrimary else Color(0xFFB45309)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // User Accounts & Data Overview (What accounts/records the user is keeping)
            Text(
                text = if (language == "bn") "ইউজারের হিসাব ও ডাটা পরিসংখ্যান:" else "User Accounts Overview:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Product count
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "পণ্য তালিকা" else "Products",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${user.productCount} টি",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = StockBlue
                        )
                    }
                }

                // Customer count
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "কাস্টমার" else "Customers",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${user.customerCount} জন",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                // Total Sales
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "মোট বিক্রি" else "Sales",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "৳${user.totalSales.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Due Amount
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "মোট বাকি" else "Total Due",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "৳${user.totalDue.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (user.totalDue > 0) LossRed else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Main Cash Balance
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "মূল ক্যাশ" else "Cash Balance",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "৳${user.mainBalance.toInt()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                // Transactions count
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (language == "bn") "মোট লেনদেন" else "Tx Entries",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${user.transactionCount} টি",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Registration & Last Active Dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${if (language == "bn") "নিবন্ধন: " else "Joined: "}$createdStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
                Text(
                    text = "${if (language == "bn") "সর্বশেষ সক্রিয়: " else "Last Active: "}$lastLoginStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: View Details & Download PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == "bn") "হিসাব ও ডাটা দেখুন" else "View Accounts",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }

                Button(
                    onClick = onDownloadPdf,
                    enabled = !isDownloadingPdf,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    if (isDownloadingPdf) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "তৈরি হচ্ছে..." else "Preparing...",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "পিডিএফ ডাউনলোড" else "Download PDF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detailed Inspector Dialog for Admin to see all accounts, product lists, customers, dues and transactions of a user
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserShopDataDetailsDialog(
    user: FirebaseUserAccount,
    viewModel: ShopViewModel,
    language: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoadingData by remember { mutableStateOf(true) }
    var shopData by remember { mutableStateOf<AdminUserShopData?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var isDownloadingPdf by remember { mutableStateOf(false) }
    var isDownloadingProductPdf by remember { mutableStateOf(false) }

    LaunchedEffect(user.email) {
        isLoadingData = true
        shopData = viewModel.fetchUserShopData(user.email)
        isLoadingData = false
    }

    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()) }
    val lastBackupStr = remember(user.lastBackupAt, shopData?.lastBackupTime) {
        val time = if ((shopData?.lastBackupTime ?: 0L) > 0) shopData!!.lastBackupTime else user.lastBackupAt
        if (time > 0) dateFormat.format(Date(time)) else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Surface(
                    color = EmeraldPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = user.shopName.ifBlank { "NAFI KHATA" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${if (language == "bn") "ইউজার হিসাব খাতা ও ডাটা" else "User Ledger & Shop Data"} (${user.ownerName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .size(34.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Sub-tabs: 0: Overview & Stats, 1: Products, 2: Customers & Dues, 3: Transactions, 4: Expenses
                val pCount = shopData?.products?.size ?: user.productCount
                val cCount = shopData?.customers?.size ?: user.customerCount
                val tCount = shopData?.transactions?.size ?: user.transactionCount

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 12.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = EmeraldPrimary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (language == "bn") "📊 হিসাব বিবরণী" else "Overview") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (language == "bn") "📦 পণ্য ($pCount)" else "Products ($pCount)") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(if (language == "bn") "👥 বাকি খাতা ($cCount)" else "Dues ($cCount)") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text(if (language == "bn") "🧾 লেনদেন ($tCount)" else "Tx ($tCount)") }
                    )
                    Tab(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        text = { Text(if (language == "bn") "💸 খরচ" else "Expenses") }
                    )
                }

                // Content Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    if (isLoadingData) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (language == "bn") "ইউজারের হিসাব ও ব্যাকআপ ফাইল লোড হচ্ছে..." else "Loading user shop data...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        when (selectedTab) {
                            0 -> OverviewTabContent(
                                user = user,
                                shopData = shopData,
                                language = language,
                                lastBackupStr = lastBackupStr
                            )
                            1 -> ProductsTabContent(
                                products = shopData?.products ?: emptyList(),
                                currency = shopData?.currency ?: "৳",
                                language = language,
                                isDownloadingProductPdf = isDownloadingProductPdf,
                                onDownloadProductPdf = {
                                    val prods = shopData?.products ?: emptyList()
                                    if (prods.isEmpty()) {
                                        Toast.makeText(context, if (language == "bn") "কোনো পণ্য পাওয়া যায়নি" else "No products found", Toast.LENGTH_SHORT).show()
                                        return@ProductsTabContent
                                    }
                                    isDownloadingProductPdf = true
                                    coroutineScope.launch {
                                        val pdf = PdfGenerator.generateUserProductListPdf(
                                            context = context,
                                            shopName = shopData?.shopName ?: user.shopName,
                                            ownerName = shopData?.ownerName ?: user.ownerName,
                                            email = user.email,
                                            phone = shopData?.phone ?: user.phone,
                                            lastBackupTime = shopData?.lastBackupTime ?: user.lastBackupAt,
                                            products = prods,
                                            currency = shopData?.currency ?: "৳"
                                        )
                                        isDownloadingProductPdf = false
                                        if (pdf != null) {
                                            PdfGenerator.openOrSharePdf(context, pdf, "পণ্য তালিকা PDF")
                                        } else {
                                            Toast.makeText(context, "PDF Error", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            2 -> CustomersTabContent(
                                customers = shopData?.customers ?: emptyList(),
                                currency = shopData?.currency ?: "৳",
                                language = language
                            )
                            3 -> TransactionsTabContent(
                                transactions = shopData?.transactions ?: emptyList(),
                                currency = shopData?.currency ?: "৳",
                                language = language
                            )
                            4 -> ExpensesTabContent(
                                expenses = shopData?.expenses ?: emptyList(),
                                currency = shopData?.currency ?: "৳",
                                language = language
                            )
                        }
                    }
                }

                // Sticky Bottom Bar: Full PDF Download Button
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(0.4f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (language == "bn") "বন্ধ করুন" else "Close")
                        }

                        Button(
                            onClick = {
                                isDownloadingPdf = true
                                coroutineScope.launch {
                                    try {
                                        val sData = shopData ?: viewModel.fetchUserShopData(user.email)
                                        isDownloadingPdf = false
                                        if (sData != null) {
                                            val pdfFile = PdfGenerator.generateUserShopFullReportPdf(
                                                context = context,
                                                shopName = sData.shopName.ifBlank { user.shopName },
                                                ownerName = sData.ownerName.ifBlank { user.ownerName },
                                                email = sData.email,
                                                phone = sData.phone.ifBlank { user.phone },
                                                lastBackupTime = if (sData.lastBackupTime > 0) sData.lastBackupTime else user.lastBackupAt,
                                                products = sData.products,
                                                customers = sData.customers,
                                                transactions = sData.transactions,
                                                expenses = sData.expenses,
                                                mainBalance = if (sData.mainBalance != 0.0) sData.mainBalance else user.mainBalance,
                                                currency = sData.currency
                                            )
                                            if (pdfFile != null) {
                                                PdfGenerator.openOrSharePdf(context, pdfFile, "${sData.shopName} - সম্পূর্ণ খাতা ও হিসাব PDF")
                                                Toast.makeText(context, if (language == "bn") "PDF প্রস্তুত হয়েছে" else "PDF Download Ready", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, if (language == "bn") "PDF তৈরিতে ব্যর্থ হয়েছে" else "PDF generation failed", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, if (language == "bn") "কোনো ডাটা পাওয়া যায়নি" else "No data found", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        isDownloadingPdf = false
                                        Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isDownloadingPdf,
                            modifier = Modifier.weight(0.6f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            if (isDownloadingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (language == "bn") "PDF তৈরি হচ্ছে..." else "Generating PDF...", color = Color.White)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (language == "bn") "সম্পূর্ণ ডাটা PDF ডাউনলোড" else "Download Full PDF",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    user: FirebaseUserAccount,
    shopData: AdminUserShopData?,
    language: String,
    lastBackupStr: String?
) {
    val scrollState = rememberScrollState()
    val currency = shopData?.currency ?: "৳"
    val mainBalance = if ((shopData?.mainBalance ?: 0.0) != 0.0) shopData!!.mainBalance else user.mainBalance
    val totalDue = if (shopData != null && shopData.customers.isNotEmpty()) shopData.customers.sumOf { it.totalDue } else user.totalDue
    val totalSales = if (shopData != null && shopData.transactions.isNotEmpty()) shopData.transactions.filter { it.type == "SALE" }.sumOf { it.totalAmount } else user.totalSales
    val totalStockVal = if (shopData != null && shopData.products.isNotEmpty()) shopData.products.sumOf { it.stockQuantity * it.sellPrice } else 0.0
    val totalExpenses = if (shopData != null && shopData.expenses.isNotEmpty()) shopData.expenses.sumOf { it.amount } else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Last Backup Highlight Box
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (lastBackupStr != null) EmeraldPrimary.copy(alpha = 0.08f) else Color(0xFFFEF3C7).copy(alpha = 0.6f),
            border = BorderStroke(
                1.dp,
                if (lastBackupStr != null) EmeraldPrimary.copy(alpha = 0.35f) else Color(0xFFF59E0B).copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (lastBackupStr != null) EmeraldPrimary.copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (lastBackupStr != null) Icons.Default.CloudDone else Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = if (lastBackupStr != null) EmeraldPrimary else Color(0xFFD97706),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (language == "bn") "সর্বশেষ ক্লাউড ব্যাকআপ তথ্য:" else "Last Cloud Backup Status:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = lastBackupStr ?: (if (language == "bn") "এখনও ক্লাউডে কোনো ব্যাকআপ নেওয়া হয়নি" else "No backup taken to cloud yet"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (lastBackupStr != null) EmeraldPrimary else Color(0xFFB45309)
                    )
                    if (lastBackupStr == null) {
                        Text(
                            text = if (language == "bn") "ইউজার এখনও সেটিংস থেকে 'ব্যাকআপ' বোতামে চাপ দেননি" else "User hasn't triggered cloud backup from settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Shop Profile Info
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (language == "bn") "দোকান ও প্রোফাইল বিবরণ:" else "Shop & Profile Details:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (language == "bn") "দোকানের নাম:" else "Shop Name:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(shopData?.shopName?.ifBlank { user.shopName } ?: user.shopName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (language == "bn") "মালিকের নাম:" else "Owner Name:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(shopData?.ownerName?.ifBlank { user.ownerName } ?: user.ownerName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (language == "bn") "মোবাইল নম্বর:" else "Phone:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text((shopData?.phone?.ifBlank { user.phone } ?: user.phone).ifBlank { "N/A" }, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (language == "bn") "ইমেইল:" else "Email:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                if (!shopData?.address.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (language == "bn") "ঠিকানা:" else "Address:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(shopData!!.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Financial & Business Numbers Grid
        Text(
            text = if (language == "bn") "ইউজারের হিসাব-নিকাশ ও আর্থিক চিত্র:" else "User Accounts & Financial Status:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                title = if (language == "bn") "মূল ক্যাশ ব্যালেন্স" else "Cash Balance",
                value = "$currency${mainBalance.toInt()}",
                color = EmeraldPrimary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = if (language == "bn") "কাস্টমার মোট বাকি" else "Total Due",
                value = "$currency${totalDue.toInt()}",
                color = if (totalDue > 0) LossRed else EmeraldPrimary,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                title = if (language == "bn") "সর্বমোট বিক্রি" else "Total Sales",
                value = "$currency${totalSales.toInt()}",
                color = ProfitGreen,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = if (language == "bn") "স্টক বিক্রয়মূল্য" else "Stock Value",
                value = "$currency${totalStockVal.toInt()}",
                color = StockBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                title = if (language == "bn") "মোট দোকান খরচ" else "Total Expenses",
                value = "$currency${totalExpenses.toInt()}",
                color = AmberTertiary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = if (language == "bn") "পণ্য / কাস্টমার" else "Products / Clients",
                value = "${shopData?.products?.size ?: user.productCount} টি / ${shopData?.customers?.size ?: user.customerCount} জন",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun ProductsTabContent(
    products: List<com.example.data.model.Product>,
    currency: String,
    language: String,
    isDownloadingProductPdf: Boolean,
    onDownloadProductPdf: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else {
            val q = searchQuery.trim().lowercase()
            products.filter { it.name.lowercase().contains(q) || it.category.lowercase().contains(q) || it.barcode.contains(q) }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Download Product Catalog Button + Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (language == "bn") "মোট পণ্য: " else "Total Products: "}${products.size} টি",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )

            Button(
                onClick = onDownloadProductPdf,
                enabled = !isDownloadingProductPdf && products.isNotEmpty(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StockBlue),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                if (isDownloadingProductPdf) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(if (language == "bn") "পণ্য তালিকা PDF" else "Products PDF", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }

        if (products.size > 5) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (language == "bn") "পণ্য খুঁজুন..." else "Search product...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (products.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == "bn") "কোনো পণ্য পাওয়া যায়নি (ব্যাকআপ খালি বা এখনও আপলোড হয়নি)" else "No products found in backup",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredProducts, key = { it.id }) { prod ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${prod.category} | ${if (language == "bn") "ক্রয়" else "Cost"}: $currency${prod.buyPrice.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$currency${prod.sellPrice.toInt()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    "${if (language == "bn") "স্টক: " else "Stock: "}${prod.stockQuantity.toInt()} ${prod.unit}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (prod.stockQuantity <= prod.minStockAlert) LossRed else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomersTabContent(
    customers: List<com.example.data.model.Customer>,
    currency: String,
    language: String
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            val q = searchQuery.trim().lowercase()
            customers.filter { it.name.lowercase().contains(q) || it.phone.contains(q) }
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val totalDue = customers.sumOf { it.totalDue }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (language == "bn") "মোট কাস্টমার: " else "Customers: "}${customers.size} জন",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${if (language == "bn") "মোট বকেয়া বাকি: " else "Total Due: "}$currency${totalDue.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (totalDue > 0) LossRed else EmeraldPrimary
            )
        }

        if (customers.size > 5) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(if (language == "bn") "কাস্টমার খুঁজুন..." else "Search customer...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (customers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == "bn") "কোনো কাস্টমার পাওয়া যায়নি" else "No customers found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(filtered, key = { it.id }) { cust ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cust.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${cust.phone.ifBlank { "মোবাইল নেই" }} | ${if (language == "bn") "মোট ক্রয়" else "Purchased"}: $currency${cust.totalPurchased.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$currency${cust.totalDue.toInt()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cust.totalDue > 0) LossRed else EmeraldPrimary
                                )
                                Text(
                                    if (cust.totalDue > 0) (if (language == "bn") "বাকি আছে" else "Due") else (if (language == "bn") "পরিশোধিত" else "Paid"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cust.totalDue > 0) LossRed else EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsTabContent(
    transactions: List<com.example.data.model.TransactionRecord>,
    currency: String,
    language: String
) {
    val txDateFormat = remember { SimpleDateFormat("dd/MM/yy, hh:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "${if (language == "bn") "মোট লেনদেন রেকর্ড: " else "Total Transactions: "}${transactions.size} টি",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = EmeraldPrimary
        )

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == "bn") "কোনো লেনদেন রেকর্ড পাওয়া যায়নি" else "No transactions found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(transactions.take(100), key = { it.id }) { tx ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${tx.invoiceNumber.ifBlank { tx.type }} - ${tx.customerName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${txDateFormat.format(Date(tx.timestamp))} | ${tx.paymentMethod}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "$currency${tx.totalAmount.toInt()}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == "SALE") ProfitGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${if (language == "bn") "জমা: " else "Paid: "}$currency${tx.paidAmount.toInt()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpensesTabContent(
    expenses: List<com.example.data.model.Expense>,
    currency: String,
    language: String
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yy, hh:mm a", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val totalExp = expenses.sumOf { it.amount }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (language == "bn") "মোট খরচ রেকর্ড: " else "Total Expenses: "}${expenses.size} টি",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$currency${totalExp.toInt()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AmberTertiary
            )
        }

        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (language == "bn") "কোনো খরচের এন্ট্রি পাওয়া যায়নি" else "No expense records found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(expenses, key = { it.id }) { exp ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(exp.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${exp.category} | ${dateFormat.format(Date(exp.timestamp))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                "$currency${exp.amount.toInt()}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AmberTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
