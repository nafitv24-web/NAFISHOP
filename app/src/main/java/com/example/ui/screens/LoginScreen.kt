package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ShopViewModel,
    onLoginSuccess: () -> Unit
) {
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()

    var email by remember { mutableStateOf(shopInfo.userEmail.ifBlank { "" }) }
    var password by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf(if (shopInfo.shopName != "আমার দোকান") shopInfo.shopName else "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Logo & Branding
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = EmeraldPrimary.copy(alpha = 0.15f),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Cloud Logo",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (language == "bn") "দোকান খাতা ক্লাউড" else "Shop Khata Cloud",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = if (language == "bn") "গুগল ড্রাইভ ও জিমেইল সংযুক্ত অ্যাকাউন্ট" else "Google Drive & Gmail Connected Account",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Login Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.95f)),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (language == "bn") "জিমেইল দিয়ে প্রবেশ করুন" else "Sign In with Gmail",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (language == "bn") "গুগল ড্রাইভে স্বয়ংক্রিয় ব্যাকআপ ও ডাটা সুরক্ষিত রাখতে লগিন করুন"
                        else "Sign in to keep your shop data safe on Google Drive",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Gmail Input Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text(if (language == "bn") "জিমেইল আইডি (Gmail Address)" else "Gmail Address") },
                        placeholder = { Text("yourname@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldPrimary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Field
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text(if (language == "bn") "পাসওয়ার্ড (Password)" else "Password") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Shop Name (Optional)
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text(if (language == "bn") "দোকানের নাম (ঐচ্ছিক)" else "Shop Name (Optional)") },
                        placeholder = { Text(if (language == "bn") "যেমন: ভাই ভাই স্টোর" else "e.g. Bhai Bhai Store") },
                        leadingIcon = {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = StockBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = StockBlue,
                            unfocusedBorderColor = Color(0xFF475569),
                            focusedLabelColor = StockBlue,
                            unfocusedLabelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = LossRed.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LossRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = errorMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LossRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Login / Sign-In Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val cleanEmail = email.trim().lowercase()
                            val cleanPass = password.trim()

                            // Strict Gmail validation check: Must be a legitimate Gmail address
                            val gmailRegex = "^[a-zA-Z0-9._%+-]{4,30}@gmail\\.com$".toRegex()
                            if (!cleanEmail.matches(gmailRegex)) {
                                errorMessage = if (language == "bn")
                                    "সঠিক জিমেইল ঠিকানা দিন (যেমন: yourname@gmail.com)!"
                                else
                                    "Please enter a valid Gmail address (e.g. yourname@gmail.com)!"
                                return@Button
                            }

                            // Prevent random keyboard mashing (e.g., vuggghc, asdfg, 12345)
                            val namePart = cleanEmail.substringBefore("@")
                            if (namePart.length < 5) {
                                errorMessage = if (language == "bn")
                                    "জিমেইল নাম ন্যূনতম ৫ অক্ষরের হতে হবে!"
                                else
                                    "Gmail username must be at least 5 characters!"
                                return@Button
                            }

                            if (cleanPass.length < 6) {
                                errorMessage = if (language == "bn")
                                    "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে!"
                                else
                                    "Password must be at least 6 characters!"
                                return@Button
                            }

                            isLoggingIn = true
                            viewModel.loginUser(
                                email = cleanEmail,
                                password = cleanPass,
                                customShopName = shopName.trim()
                            )
                            onLoginSuccess()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "লগইন হচ্ছে..." else "Logging in...",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (language == "bn") "লগিন ও গুগল ড্রাইভ সংযোগ" else "Sign In & Connect Drive",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cloud Security Feature Notice
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "১০০% নিরাপদ ও ক্লাউড সিঙ্ক" else "100% Secure & Cloud Synced",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (language == "bn")
                                        "লগিন করার পর আপনার সব পণ্য, কাস্টমার বাকি খাতা ও বিক্রয় সরাসরি আপনার গুগল অ্যাকাউন্টের সাথে যুক্ত হবে।"
                                    else
                                        "After sign-in, all products, dues, and sales will be linked to your Google Account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Guest / Skip Mode
                    TextButton(
                        onClick = {
                            viewModel.loginAsGuest()
                            onLoginSuccess()
                        }
                    ) {
                        Text(
                            text = if (language == "bn") "গেস্ট হিসেবে প্রবেশ করুন (পরে লগিন করব)" else "Continue as Guest (Login later)",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
