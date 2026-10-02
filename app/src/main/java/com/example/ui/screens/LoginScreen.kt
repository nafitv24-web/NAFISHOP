package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.auth.AuthResult
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel

/**
 * Authentic "টং খাতা" (Tong Khata) Login & Registration Screen
 * Features:
 * - Rich Crimson & Maroon twilight aesthetic
 * - Custom vector illustration of Bangladeshi village tea-stall ("টং দোকান") with palm trees,
 *   kettle & steam, moon glow and flying birds
 * - Top status bar with Language Switcher (বাংলা / English) and 24/7 Helpline button
 * - Mobile number login (১১ ডিজিট মোবাইল নম্বর) + Gmail login + New Shop Account registration
 * - Google Drive 1-tap restore & Offline Guest Mode
 * - Helpline modal dialog
 */

enum class LoginInputMode {
    SIGN_IN,
    REGISTER
}

// Custom crimson palette aligned with Tong Khata design
val TongCrimsonDark = Color(0xFF450A0A)
val TongCrimsonDeep = Color(0xFF7F1D1D)
val TongCrimsonPrimary = Color(0xFF991B1B)
val TongCrimsonBright = Color(0xFFDC2626)
val TongCrimsonLight = Color(0xFFEF4444)
val TongGold = Color(0xFFF59E0B)
val TongGoldLight = Color(0xFFFDE68A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ShopViewModel,
    onLoginSuccess: () -> Unit
) {
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val isDark = isSystemInDarkTheme()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val isBn = language == "bn"

    var inputMode by remember { mutableStateOf(LoginInputMode.SIGN_IN) }

    val lastSavedEmail = remember { viewModel.getLastSavedEmail() }

    // Form inputs: Gmail authentication for reliable Google Drive & Cloud backup
    var email by remember {
        mutableStateOf(
            if (shopInfo.userEmail.isNotBlank() && shopInfo.userEmail.contains("@")) shopInfo.userEmail
            else if (lastSavedEmail.isNotBlank() && lastSavedEmail.contains("@")) lastSavedEmail
            else ""
        )
    }
    var password by remember { mutableStateOf("") }
    var shopName by remember {
        mutableStateOf(
            if (shopInfo.shopName.isNotBlank() && shopInfo.shopName != "আমার দোকান") shopInfo.shopName
            else if (isBn) "নাফি খাতা" else "NAFI KHATA"
        )
    }
    var ownerName by remember {
        mutableStateOf(
            if (shopInfo.ownerName.isNotBlank()) shopInfo.ownerName else "দোকানদার"
        )
    }

    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var suggestedEmail by remember { mutableStateOf<String?>(null) }
    var userNotFoundEmail by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Helpline and Forgot Password dialogs
    var showHelplineDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isResettingPassword by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TongCrimsonDark,
                        TongCrimsonDeep,
                        TongCrimsonPrimary
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // -------------------------------------------------------------
            // TOP BAR: Language Selector & Helpline
            // -------------------------------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Switcher Chip (Globe + Bengali/English)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.clickable {
                        viewModel.toggleLanguage()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Language",
                            tint = TongGoldLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "বাংলা (BN)" else "English (EN)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Helpline Support Chip
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, TongGold.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        showHelplineDialog = true
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = "Support",
                            tint = TongGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBn) "হেল্পলাইন" else "Helpline",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TongGoldLight
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // SCROLLABLE BODY
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Area with Bengali Tea Stall Village Illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Custom Canvas Illustration of Tong Dokan & Village
                    TongDokanArtCanvas(
                        modifier = Modifier.fillMaxSize()
                    )

                    // Logo & App Name Overlay
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        // Steaming Tea Glass Logo Emblem
                        TongKhataEmblem(size = 64.dp)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isBn) "নাফি খাতা" else "NAFI KHATA",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = if (isBn) "দোকানের হিসাবের বিশ্বস্ত সঙ্গী" else "Complete Digital Business Khata",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TongGoldLight
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ---------------------------------------------------------
                // MAIN WHITE / LIGHT CURVED CONTAINER SHEET
                // ---------------------------------------------------------
                Surface(
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = if (isDark) Color(0xFF131A2A) else Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Small handle indicator
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.3f))
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Welcome Heading
                        Text(
                            text = if (isBn) "স্বাগতম!" else "Welcome!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )

                        Text(
                            text = if (inputMode == LoginInputMode.SIGN_IN) {
                                if (isBn) "গুগল ড্রাইভ ও ক্লাউড সিঙ্কের জন্য জিমেইল দিয়ে লগইন করুন"
                                else "Sign in with Gmail for Google Drive & Cloud backup"
                            } else {
                                if (isBn) "দোকানের হিসাব নিরাপদ রাখতে নতুন ফ্রি অ্যাকাউন্ট খুলুন"
                                else "Create a free shop account to secure your business khata"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                        )

                        // -------------------------------------------------
                        // Segmented Input Tabs: Sign In vs New Account
                        // -------------------------------------------------
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                            ) {
                                // Tab 1: Sign In
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (inputMode == LoginInputMode.SIGN_IN) TongCrimsonPrimary else Color.Transparent
                                        )
                                        .clickable {
                                            inputMode = LoginInputMode.SIGN_IN
                                            errorMessage = null
                                            successMessage = null
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isBn) "লগইন করুন" else "Sign In",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (inputMode == LoginInputMode.SIGN_IN) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                    )
                                }

                                // Tab 2: Register
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (inputMode == LoginInputMode.REGISTER) TongCrimsonPrimary else Color.Transparent
                                        )
                                        .clickable {
                                            inputMode = LoginInputMode.REGISTER
                                            errorMessage = null
                                            successMessage = null
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isBn) "নতুন অ্যাকাউন্ট তৈরি" else "New Account",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (inputMode == LoginInputMode.REGISTER) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Google Drive & Cloud Backup Notice Pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = TongGold.copy(alpha = if (isDark) 0.15f else 0.12f),
                            border = BorderStroke(1.dp, TongGold.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = TongGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "গুগল ড্রাইভে অটো ব্যাকআপ সক্রিয় থাকবে (Gmail একাউন্ট)"
                                           else "Auto-backup to Google Drive is linked with your Gmail",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) TongGoldLight else Color(0xFF92400E),
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // -------------------------------------------------
                        // FORM INPUTS (GMAIL BASED)
                        // -------------------------------------------------

                        // Additional fields for Registration (Shop Name & Owner Name)
                        AnimatedVisibility(
                            visible = inputMode == LoginInputMode.REGISTER,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                OutlinedTextField(
                                    value = shopName,
                                    onValueChange = { shopName = it },
                                    label = { Text(if (isBn) "দোকানের নাম (Shop Name)" else "Shop Name") },
                                    placeholder = { Text("যেমন: ভাই ভাই ভ্যারাইটিজ স্টোর") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Storefront, contentDescription = null, tint = TongCrimsonPrimary)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = ownerName,
                                    onValueChange = { ownerName = it },
                                    label = { Text(if (isBn) "মালিকের নাম (Owner Name)" else "Owner Name") },
                                    placeholder = { Text("যেমন: মোঃ নাফি") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = TongCrimsonPrimary)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        // Gmail Address Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                errorMessage = null
                                suggestedEmail = null
                                userNotFoundEmail = null
                            },
                            label = { Text(if (isBn) "জিমেইল ঠিকানা (Gmail ID)" else "Gmail Address") },
                            placeholder = { Text("yourname@gmail.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = null, tint = TongCrimsonPrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Quick Chip for Last Saved Email
                        if (lastSavedEmail.isNotBlank() && email != lastSavedEmail) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isBn) "পূর্বের জিমেইল:" else "Previous:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = TongCrimsonPrimary.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, TongCrimsonPrimary.copy(alpha = 0.4f)),
                                    modifier = Modifier.clickable {
                                        email = lastSavedEmail
                                        errorMessage = null
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            tint = TongCrimsonPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = lastSavedEmail,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TongCrimsonPrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMessage = null
                            },
                            label = { Text(if (isBn) "পাসওয়ার্ড (Password)" else "Password") },
                            placeholder = { Text("••••••••") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = TongCrimsonPrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Forgot Password Link
                        if (inputMode == LoginInputMode.SIGN_IN) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        resetEmail = email.trim()
                                        showForgotPasswordDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBn) "পাসওয়ার্ড ভুলে গেছেন?" else "Forgot Password?",
                                        color = TongCrimsonPrimary,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // -------------------------------------------------
                        // ERROR & SUGGESTION BANNER
                        // -------------------------------------------------
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            if (suggestedEmail != null) {
                                // Typo Suggestion Card
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBn) "পূর্বের অ্যাকাউন্ট পাওয়া গেছে!" else "Account Detected!",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF78350F)
                                        )
                                        Button(
                                            onClick = {
                                                val target = suggestedEmail!!
                                                email = target
                                                suggestedEmail = null
                                                errorMessage = null
                                                isLoading = true
                                                viewModel.firebaseSignIn(target, password) { res ->
                                                    isLoading = false
                                                    when (res) {
                                                        is AuthResult.Success -> {
                                                            Toast.makeText(context, if (isBn) "লগইন সফল!" else "Login successful!", Toast.LENGTH_SHORT).show()
                                                            onLoginSuccess()
                                                        }
                                                        is AuthResult.Error -> {
                                                            errorMessage = res.errorMessage
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TongCrimsonPrimary),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isBn) "${suggestedEmail} দিয়ে লগইন করুন" else "Sign In with $suggestedEmail",
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = LossRed.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, LossRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LossRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LossRed
                                        )
                                    }
                                }
                            }
                        }

                        // Success Message
                        if (successMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ProfitGreen.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = successMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ProfitGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // -------------------------------------------------
                        // PRIMARY CTA: Large Bold Crimson Pill Button
                        // -------------------------------------------------
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                errorMessage = null
                                successMessage = null

                                // Clean Gmail validation for Google Drive & Cloud backup
                                val cleanEmail = email.trim().lowercase()
                                if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                    errorMessage = if (isBn) "সঠিক জিমেইল আইডি লিখুন (যেমন: name@gmail.com)" else "Please enter a valid Gmail address"
                                    return@Button
                                }

                                val cleanPass = password.trim()
                                if (cleanPass.length < 6) {
                                    errorMessage = if (isBn) "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে!" else "Password must be at least 6 characters!"
                                    return@Button
                                }

                                isLoading = true

                                if (inputMode == LoginInputMode.REGISTER) {
                                    // Sign Up with Gmail
                                    val sName = shopName.ifBlank { if (isBn) "নাফি খাতা" else "NAFI KHATA" }
                                    val oName = ownerName.ifBlank { "দোকানদার" }
                                    viewModel.firebaseSignUp(cleanEmail, cleanPass, sName, oName) { res ->
                                        isLoading = false
                                        when (res) {
                                            is AuthResult.Success -> {
                                                successMessage = if (isBn) "অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে!" else "Account created successfully!"
                                                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }
                                            is AuthResult.Error -> {
                                                errorMessage = res.errorMessage
                                            }
                                            else -> {}
                                        }
                                    }
                                } else {
                                    // Sign In with Gmail
                                    viewModel.firebaseSignIn(cleanEmail, cleanPass) { res ->
                                        isLoading = false
                                        when (res) {
                                            is AuthResult.Success -> {
                                                successMessage = if (isBn) "লগইন সফল! আপনার খাতার হিসাব ও ক্লাউড ডাটা লোড হয়েছে।" else "Signed in successfully!"
                                                Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }
                                            is AuthResult.Suggestion -> {
                                                suggestedEmail = res.suggestedEmail
                                                errorMessage = res.message
                                            }
                                            is AuthResult.UserNotFound -> {
                                                userNotFoundEmail = res.email
                                                errorMessage = if (isBn) "এই জিমেইল দিয়ে কোনো অ্যাকাউন্ট পাওয়া যায়নি। অনুগ্রহ করে 'নতুন অ্যাকাউন্ট তৈরি' ট্যাবে গিয়ে একাউন্ট খুলুন।"
                                                               else res.message
                                            }
                                            is AuthResult.Error -> {
                                                errorMessage = res.errorMessage
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TongCrimsonPrimary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isBn) "যাচাই করা হচ্ছে..." else "Verifying...",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (inputMode == LoginInputMode.REGISTER) {
                                            if (isBn) "নতুন অ্যাকাউন্ট তৈরি করুন" else "Create New Account"
                                        } else {
                                            if (isBn) "লগইন করুন (এগিয়ে যান)" else "Sign In (Continue)"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Divider with "অথবা" (Or)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )
                            Text(
                                text = if (isBn) " অথবা " else " OR ",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // -------------------------------------------------
                        // GOOGLE / DRIVE RESTORE BUTTON
                        // -------------------------------------------------
                        OutlinedButton(
                            onClick = {
                                val targetEmail = if (email.isNotBlank()) email.trim().lowercase() else lastSavedEmail
                                if (targetEmail.isBlank()) {
                                    errorMessage = if (isBn) "গুগল ড্রাইভ রিস্টোর করতে উপরে আপনার জিমেইল আইডি লিখুন" else "Enter your Gmail to restore from Google Drive"
                                    return@OutlinedButton
                                }
                                isLoading = true
                                errorMessage = null
                                viewModel.importFromGoogleDriveCloud(context, customEmail = targetEmail) { res ->
                                    isLoading = false
                                    if (res.success) {
                                        Toast.makeText(
                                            context,
                                            if (isBn) "✅ গুগল ড্রাইভ থেকে সকল হিসাব ও ডাটা সফলভাবে রিস্টোর হয়েছে!" else "✅ Restored from Google Drive!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        onLoginSuccess()
                                    } else {
                                        errorMessage = res.message
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Stylized Google G Icon badge
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "G",
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF4285F4),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = if (isBn) "Google Drive ব্যাকআপ থেকে প্রবেশ" else "Continue with Google Drive",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) Color.White else Color(0xFF1E293B),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // -------------------------------------------------
                        // GUEST / OFFLINE MODE
                        // -------------------------------------------------
                        TextButton(
                            onClick = {
                                val guestIdentity = email.trim().ifBlank { "guest@tongkhata.app" }
                                viewModel.loginAsGuest(customEmail = guestIdentity) {
                                    onLoginSuccess()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "লগইন ছাড়াই ব্যবহার করুন (অফলাইন মোড)" else "Use without login (Offline Guest)",
                                    color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // -------------------------------------------------
                        // TERMS & HELPLINE FOOTER
                        // -------------------------------------------------
                        Text(
                            text = if (isBn)
                                "এগিয়ে যাওয়ার মাধ্যমে আপনি নাফি খাতার ব্যবহারের শর্তাবলী ও গোপনীয়তা নীতিতে সম্মতি দিচ্ছেন।"
                            else
                                "By continuing you agree to the Nafi Khata Terms of Service & Privacy Policy.",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Helpline Direct Call Row
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showHelplineDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = TongCrimsonPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBn) "সহায়তা প্রয়োজন? কল করুন: ০১৬০০-০০০০০০" else "Need help? Call: 01600-000000",
                                style = MaterialTheme.typography.labelSmall,
                                color = TongCrimsonPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------
    // HELPLINE SUPPORT DIALOG
    // -----------------------------------------------------------------
    if (showHelplineDialog) {
        AlertDialog(
            onDismissRequest = { showHelplineDialog = false },
            icon = {
                Surface(
                    shape = CircleShape,
                    color = TongCrimsonPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = null,
                            tint = TongCrimsonPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            },
            title = {
                Text(
                    text = if (isBn) "নাফি খাতা গ্রাহক সহায়তা" else "Nafi Khata Customer Care",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isBn)
                            "হিসাব সংরক্ষণ, ক্লাউড ব্যাকআপ বা অ্যাপ সম্পর্কিত যেকোনো প্রয়োজনে আমাদের কাস্টমার কেয়ারে যোগাযোগ করুন।"
                        else
                            "Contact our customer care for any queries regarding accounting, cloud backup, or app usage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Phone 1
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01600000000"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Call: 01600000000", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TongCrimsonPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBn) "হটলাইন নম্বর (সকাল ৯টা - রাত ১০টা)" else "Hotline (9 AM - 10 PM)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "০১৬০০-০০০০০০ / 01700-000000",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TongCrimsonPrimary
                                )
                            }
                        }
                    }

                    // Email Support
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, tint = StockBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isBn) "ইমেইল সহায়তা" else "Email Support",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "support@tongkhata.app",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelplineDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TongCrimsonPrimary)
                ) {
                    Text(if (isBn) "ঠিক আছে" else "Close")
                }
            }
        )
    }

    // -----------------------------------------------------------------
    // FORGOT PASSWORD DIALOG
    // -----------------------------------------------------------------
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResettingPassword) showForgotPasswordDialog = false
            },
            icon = {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = TongCrimsonPrimary, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = if (isBn) "পাসওয়ার্ড রিসেট লিংক পাঠান" else "Reset Password Link",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBn)
                            "আপনার নিবন্ধিত জিমেইল আইডি লিখুন। পাসওয়ার্ড পরিবর্তন করার লিংক আপনার ইনবক্সে পাঠানো হবে।"
                        else
                            "Enter your registered Gmail. A password reset link will be sent to your email.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text(if (isBn) "জিমেইল ঠিকানা" else "Gmail Address") },
                        placeholder = { Text("yourname@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = resetEmail.trim()
                        if (!clean.contains("@") || !clean.contains(".")) {
                            Toast.makeText(
                                context,
                                if (isBn) "সঠিক জিমেইল ঠিকানা দিন!" else "Please enter a valid Gmail!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        isResettingPassword = true
                        viewModel.firebaseResetPassword(clean) { res ->
                            isResettingPassword = false
                            showForgotPasswordDialog = false
                            when (res) {
                                is AuthResult.Success -> {
                                    Toast.makeText(
                                        context,
                                        if (isBn) "পাসওয়ার্ড রিসেট লিংক আপনার জিমেইলে পাঠানো হয়েছে!" else "Password reset link sent to your email!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                is AuthResult.Error -> {
                                    Toast.makeText(context, res.errorMessage, Toast.LENGTH_LONG).show()
                                }
                                else -> {}
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TongCrimsonPrimary),
                    enabled = !isResettingPassword
                ) {
                    if (isResettingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (isBn) "লিংক পাঠান" else "Send Reset Link")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showForgotPasswordDialog = false },
                    enabled = !isResettingPassword
                ) {
                    Text(if (isBn) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

/**
 * Custom Tong Khata Logo Badge:
 * Traditional steaming Bangladeshi tea glass + digital ledger notebook motif
 */
@Composable
fun TongKhataEmblem(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 64.dp
) {
    Surface(
        shape = CircleShape,
        color = Color(0xFF6B1111),
        border = BorderStroke(2.dp, Brush.linearGradient(listOf(TongGoldLight, TongGold, TongGoldLight))),
        shadowElevation = 6.dp,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                drawTongCupAndSteam(this)
            }
        }
    }
}

/**
 * Draws stylized tea glass and rising aromatic steam
 */
private fun drawTongCupAndSteam(drawScope: DrawScope) {
    with(drawScope) {
        val w = size.width
        val h = size.height

        // Tea glass body
        val glassPath = Path().apply {
            moveTo(w * 0.28f, h * 0.40f)
            lineTo(w * 0.72f, h * 0.40f)
            lineTo(w * 0.65f, h * 0.88f)
            lineTo(w * 0.35f, h * 0.88f)
            close()
        }
        drawPath(glassPath, color = Color(0xFFB45309)) // Chai amber-brown

        // Glass glass outline
        drawPath(
            glassPath,
            color = Color(0xFFFEF3C7),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Tea level highlight
        drawLine(
            color = Color(0xFFFDE68A),
            start = Offset(w * 0.32f, h * 0.48f),
            end = Offset(w * 0.68f, h * 0.48f),
            strokeWidth = 2.dp.toPx()
        )

        // Rising Steam Spirals
        val steam1 = Path().apply {
            moveTo(w * 0.38f, h * 0.35f)
            cubicTo(w * 0.34f, h * 0.26f, w * 0.44f, h * 0.20f, w * 0.38f, h * 0.12f)
        }
        drawPath(
            steam1,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        val steam2 = Path().apply {
            moveTo(w * 0.50f, h * 0.34f)
            cubicTo(w * 0.56f, h * 0.24f, w * 0.46f, h * 0.18f, w * 0.52f, h * 0.10f)
        }
        drawPath(
            steam2,
            color = Color(0xFFFDE68A).copy(alpha = 0.9f),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )

        val steam3 = Path().apply {
            moveTo(w * 0.62f, h * 0.35f)
            cubicTo(w * 0.58f, h * 0.26f, w * 0.68f, h * 0.20f, w * 0.62f, h * 0.12f)
        }
        drawPath(
            steam3,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Authentic Bangladeshi Village & Tea Stall ("টং দোকান") Landscape Art Canvas:
 * Features coconut / palm trees (তালগাছ/নারিকেল গাছ), cozy rustic shop hut with tin roof,
 * glowing crescent moon/sun, river curve, and flying birds across the crimson sky.
 */
@Composable
fun TongDokanArtCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Soft Warm Moon / Twilight Sun Glow in background
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    TongGoldLight.copy(alpha = 0.40f),
                    TongGold.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = Offset(w * 0.22f, h * 0.35f),
                radius = w * 0.35f
            ),
            center = Offset(w * 0.22f, h * 0.35f),
            radius = w * 0.35f
        )

        // Golden Crescent / Sun
        drawCircle(
            color = TongGoldLight.copy(alpha = 0.75f),
            center = Offset(w * 0.22f, h * 0.35f),
            radius = 16.dp.toPx()
        )

        // 2. Flying Birds in twilight sky
        drawBirdSilhouette(this, Offset(w * 0.70f, h * 0.22f), scale = 1.0f)
        drawBirdSilhouette(this, Offset(w * 0.78f, h * 0.18f), scale = 0.8f)
        drawBirdSilhouette(this, Offset(w * 0.85f, h * 0.25f), scale = 0.65f)

        // 3. Distant Village Tree Canopy / Horizons (Layer 1)
        val distantHills = Path().apply {
            moveTo(0f, h * 0.82f)
            cubicTo(w * 0.25f, h * 0.74f, w * 0.50f, h * 0.86f, w * 0.75f, h * 0.76f)
            cubicTo(w * 0.88f, h * 0.72f, w * 0.95f, h * 0.78f, w, h * 0.80f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(distantHills, color = Color(0xFF380606).copy(alpha = 0.6f))

        // 4. Village River / Ground Foreground (Layer 2)
        val groundPath = Path().apply {
            moveTo(0f, h * 0.90f)
            cubicTo(w * 0.30f, h * 0.82f, w * 0.65f, h * 0.95f, w, h * 0.88f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(groundPath, color = Color(0xFF260404))

        // 5. Left Side: Majestic Palm / Coconut Trees (তালগাছ / নারিকেল গাছ)
        drawPalmTree(this, base = Offset(w * 0.08f, h * 0.92f), height = h * 0.70f, lean = 15f)
        drawPalmTree(this, base = Offset(w * 0.16f, h * 0.90f), height = h * 0.58f, lean = -10f)

        // 6. Right Side: Rustic Village Tea Stall ("টং দোকান") Hut Silhouette
        drawTongDokanHut(this, base = Offset(w * 0.80f, h * 0.92f), width = w * 0.32f, height = h * 0.52f)
    }
}

/**
 * Draws a soaring bird silhouette
 */
private fun drawBirdSilhouette(drawScope: DrawScope, center: Offset, scale: Float) {
    with(drawScope) {
        val path = Path().apply {
            val r = 7.dp.toPx() * scale
            moveTo(center.x - r, center.y)
            cubicTo(center.x - r * 0.5f, center.y - r * 0.8f, center.x - r * 0.2f, center.y - r * 0.3f, center.x, center.y)
            cubicTo(center.x + r * 0.2f, center.y - r * 0.3f, center.x + r * 0.5f, center.y - r * 0.8f, center.x + r, center.y)
            cubicTo(center.x + r * 0.4f, center.y - r * 0.2f, center.x - r * 0.4f, center.y - r * 0.2f, center.x - r, center.y)
            close()
        }
        drawPath(path, color = Color(0xFFFDE68A).copy(alpha = 0.7f))
    }
}

/**
 * Draws an iconic Bangladeshi palm / coconut tree silhouette
 */
private fun drawPalmTree(drawScope: DrawScope, base: Offset, height: Float, lean: Float) {
    with(drawScope) {
        val top = Offset(base.x + lean, base.y - height)

        // Trunk
        val trunk = Path().apply {
            moveTo(base.x - 3.dp.toPx(), base.y)
            lineTo(base.x + 3.dp.toPx(), base.y)
            lineTo(top.x + 1.5.dp.toPx(), top.y)
            lineTo(top.x - 1.5.dp.toPx(), top.y)
            close()
        }
        drawPath(trunk, color = Color(0xFF260404))

        // Fronds radiating in arches
        val frondAngles = listOf(-80f, -50f, -20f, 15f, 50f, 85f, 115f, 150f)
        val frondLen = height * 0.35f

        for (angle in frondAngles) {
            val rad = Math.toRadians(angle.toDouble())
            val endX = (top.x + Math.cos(rad) * frondLen).toFloat()
            val endY = (top.y + Math.sin(rad) * frondLen * 0.65f).toFloat()

            val fPath = Path().apply {
                moveTo(top.x, top.y)
                quadraticTo(
                    (top.x + endX) / 2f,
                    top.y - 12.dp.toPx(),
                    endX,
                    endY
                )
            }
            drawPath(
                fPath,
                color = Color(0xFF1E0303),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Draws the rustic Bangladeshi Village Tea Stall ("টং দোকান") Hut Silhouette
 */
private fun drawTongDokanHut(drawScope: DrawScope, base: Offset, width: Float, height: Float) {
    with(drawScope) {
        val hutColor = Color(0xFF220303)
        val roofColor = Color(0xFF1A0202)

        // Hut Walls
        val left = base.x - width * 0.5f
        val right = base.x + width * 0.5f
        val wallTop = base.y - height * 0.6f

        drawRect(
            color = hutColor,
            topLeft = Offset(left + width * 0.1f, wallTop),
            size = Size(width * 0.8f, base.y - wallTop)
        )

        // Corrugated Sloped Tin Roof
        val roofPath = Path().apply {
            moveTo(left - width * 0.08f, wallTop + height * 0.05f)
            lineTo(base.x, base.y - height)
            lineTo(right + width * 0.08f, wallTop + height * 0.05f)
            close()
        }
        drawPath(roofPath, color = roofColor)

        // Wooden Stall Window / Serving Counter Opening
        drawRect(
            color = TongGold.copy(alpha = 0.25f),
            topLeft = Offset(left + width * 0.25f, wallTop + height * 0.15f),
            size = Size(width * 0.45f, height * 0.28f)
        )

        // Tea Kettle on Stall Counter
        val kettleX = left + width * 0.38f
        val kettleY = wallTop + height * 0.32f
        drawCircle(
            color = Color(0xFF0F0202),
            center = Offset(kettleX, kettleY),
            radius = 4.dp.toPx()
        )

        // Delicate Steam Wisp from Kettle
        val kettleSteam = Path().apply {
            moveTo(kettleX, kettleY - 4.dp.toPx())
            cubicTo(
                kettleX + 3.dp.toPx(), kettleY - 12.dp.toPx(),
                kettleX - 3.dp.toPx(), kettleY - 18.dp.toPx(),
                kettleX + 1.dp.toPx(), kettleY - 26.dp.toPx()
            )
        }
        drawPath(
            kettleSteam,
            color = Color(0xFFFDE68A).copy(alpha = 0.8f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
