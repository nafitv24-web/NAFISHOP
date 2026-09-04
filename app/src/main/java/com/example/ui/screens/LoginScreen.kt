package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.ui.components.NafiShopFullBrandCard
import com.example.ui.components.NafiShopLogoBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel

enum class AuthScreenMode {
    SIGN_IN,
    SIGN_UP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: ShopViewModel,
    onLoginSuccess: () -> Unit
) {
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var screenMode by remember { mutableStateOf(AuthScreenMode.SIGN_IN) }

    val lastSavedEmail = remember { viewModel.getLastSavedEmail() }
    var email by remember { mutableStateOf(shopInfo.userEmail.ifBlank { lastSavedEmail.ifBlank { "" } }) }
    var password by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf(if (shopInfo.shopName != "আমার দোকান") shopInfo.shopName else "NAFI SHOP 24") }
    var ownerName by remember { mutableStateOf(shopInfo.ownerName.ifBlank { "দোকানদার" }) }
    
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var suggestedEmail by remember { mutableStateOf<String?>(null) }
    var userNotFoundEmail by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var isResettingPassword by remember { mutableStateOf(false) }

    val isBn = language == "bn"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0F1D),
                        Color(0xFF131E33),
                        Color(0xFF0B132B)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Official App Logo Badge
            NafiShopLogoBadge(size = 84.dp)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isBn) "NAFI SHOP 24" else "NAFI SHOP 24",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Text(
                text = if (isBn) "নাফি শপ ২৪ • হিসাবের বিশ্বস্ত সঙ্গী" else "Complete Digital Business Khata",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFDE68A),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Auth Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF192238).copy(alpha = 0.95f)),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Segmented Tabs: Sign In vs Sign Up
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A),
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (screenMode == AuthScreenMode.SIGN_IN) EmeraldPrimary else Color.Transparent
                                    )
                                    .clickable {
                                        screenMode = AuthScreenMode.SIGN_IN
                                        errorMessage = null
                                        successMessage = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "লগইন করুন" else "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    color = if (screenMode == AuthScreenMode.SIGN_IN) Color.White else Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }

                            // Tab 2: Sign Up
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (screenMode == AuthScreenMode.SIGN_UP) EmeraldPrimary else Color.Transparent
                                    )
                                    .clickable {
                                        screenMode = AuthScreenMode.SIGN_UP
                                        errorMessage = null
                                        successMessage = null
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isBn) "নতুন অ্যাকাউন্ট তৈরি" else "Create Account",
                                    fontWeight = FontWeight.Bold,
                                    color = if (screenMode == AuthScreenMode.SIGN_UP) Color.White else Color(0xFF94A3B8),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (screenMode == AuthScreenMode.SIGN_IN) {
                            if (isBn) "আপনার জিমেইল ও পাসওয়ার্ড দিয়ে লগইন করুন" else "Sign in with your Gmail and password"
                        } else {
                            if (isBn) "দোকানের তথ্যাদি ও জিমেইল দিয়ে ফ্রি অ্যাকাউন্ট তৈরি করুন" else "Create a free shop account with Gmail"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sign Up Specific Fields (Shop Name & Owner Name)
                    AnimatedVisibility(
                        visible = screenMode == AuthScreenMode.SIGN_UP,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            OutlinedTextField(
                                value = shopName,
                                onValueChange = { shopName = it },
                                label = { Text(if (isBn) "দোকানের নাম (Shop Name)" else "Shop Name") },
                                placeholder = { Text("e.g. NAFI SHOP 24") },
                                leadingIcon = {
                                    Icon(Icons.Default.Storefront, contentDescription = null, tint = StockBlue)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text(if (isBn) "মালিক / প্রোপাইটরের নাম" else "Owner Name") },
                                placeholder = { Text("e.g. মোঃ নাফি") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = StockBlue)
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
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

                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // Gmail Input Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                            suggestedEmail = null
                            userNotFoundEmail = null
                        },
                        label = { Text(if (isBn) "জিমেইল আইডি (Gmail Address)" else "Gmail Address") },
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

                    // Quick chip to select last saved email if available and different
                    if (lastSavedEmail.isNotBlank() && email != lastSavedEmail) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBn) "আগের জিমেইল:" else "Previous:",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                                modifier = Modifier.clickable {
                                    email = lastSavedEmail
                                    errorMessage = null
                                    suggestedEmail = null
                                    userNotFoundEmail = null
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = lastSavedEmail,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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

                    // Forgot Password link (in Sign In mode)
                    if (screenMode == AuthScreenMode.SIGN_IN) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
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
                                    color = StockBlue,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Error Notification Banner with Instant Enter / Suggestion Options
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (suggestedEmail != null) {
                            // Smart Account Typo Suggestion Card
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBn) "পূর্বের অ্যাকাউন্ট পাওয়া গেছে!" else "Existing Account Detected!",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                    Text(
                                        text = errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                    // Primary Action: Log in directly with suggested email
                                    Button(
                                        onClick = {
                                            val targetEmail = suggestedEmail!!
                                            email = targetEmail
                                            suggestedEmail = null
                                            errorMessage = null
                                            isLoading = true
                                            viewModel.firebaseSignIn(targetEmail, password) { res ->
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
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "${suggestedEmail} দিয়ে এখনই লগইন করুন" else "Sign In with $suggestedEmail",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    // Secondary: Create new account with typed email
                                    OutlinedButton(
                                        onClick = {
                                            val createEmail = email.trim().lowercase()
                                            suggestedEmail = null
                                            errorMessage = null
                                            isLoading = true
                                            viewModel.firebaseSignUp(createEmail, password, shopName, ownerName) { res ->
                                                isLoading = false
                                                when (res) {
                                                    is AuthResult.Success -> {
                                                        Toast.makeText(context, if (isBn) "অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে!" else "Account created!", Toast.LENGTH_SHORT).show()
                                                        onLoginSuccess()
                                                    }
                                                    is AuthResult.Error -> {
                                                        errorMessage = res.errorMessage
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                                        border = BorderStroke(1.dp, Color(0xFF475569)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "${email.trim()} দিয়ে নতুন অ্যাকাউন্ট খুলুন" else "Create account with ${email.trim()}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        } else if (userNotFoundEmail != null) {
                            // User Not Found Card with 1-Tap Create Account Action
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.5.dp, StockBlue),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = StockBlue, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBn) "অ্যাকাউন্ট তৈরি নেই" else "Account Not Found",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Button(
                                        onClick = {
                                            val targetEmail = userNotFoundEmail!!
                                            userNotFoundEmail = null
                                            errorMessage = null
                                            isLoading = true
                                            viewModel.firebaseSignUp(targetEmail, password, shopName, ownerName) { res ->
                                                isLoading = false
                                                when (res) {
                                                    is AuthResult.Success -> {
                                                        Toast.makeText(context, if (isBn) "অ্যাকাউন্ট তৈরি ও লগইন সফল!" else "Account created & signed in!", Toast.LENGTH_SHORT).show()
                                                        onLoginSuccess()
                                                    }
                                                    is AuthResult.Error -> {
                                                        errorMessage = res.errorMessage
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "এই তথ্য দিয়ে নতুন অ্যাকাউন্ট খুলুন ও প্রবেশ করুন" else "Create Account & Enter Shop",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            // Standard Error card with Enter as Guest/Offline option
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = LossRed.copy(alpha = 0.12f),
                                border = CardDefaults.outlinedCardBorder(),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = LossRed, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = errorMessage!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LossRed
                                        )
                                    }

                                    // Quick Enter Button
                                    Button(
                                        onClick = {
                                            viewModel.updateShopInfo(
                                                name = shopName.ifBlank { "NAFI SHOP 24" },
                                                owner = ownerName.ifBlank { "মালিক" },
                                                phone = "",
                                                address = "",
                                                currency = "৳",
                                                email = email.ifBlank { "nafitv24@gmail.com" }
                                            )
                                            viewModel.loginAsGuest()
                                            onLoginSuccess()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isBn) "দোকানে প্রবেশ করুন (গুগল ড্রাইভ ও অফলাইন মোড)" else "Enter Shop (Drive & Offline Mode)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Success Notification Banner
                    if (successMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ProfitGreen.copy(alpha = 0.15f),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = successMessage!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Button (Sign In or Sign Up)
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val cleanEmail = email.trim().lowercase()
                            val cleanPass = password.trim()

                            if (!cleanEmail.contains("@") || !cleanEmail.contains(".")) {
                                errorMessage = if (isBn) "সঠিক জিমেইল ঠিকানা দিন (যেমন: name@gmail.com)" else "Please enter a valid Gmail address"
                                return@Button
                            }

                            if (cleanPass.length < 6) {
                                errorMessage = if (isBn) "পাসওয়ার্ড কমপক্ষে ৬ অক্ষরের হতে হবে!" else "Password must be at least 6 characters!"
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null
                            suggestedEmail = null
                            userNotFoundEmail = null
                            successMessage = null

                            if (screenMode == AuthScreenMode.SIGN_UP) {
                                viewModel.firebaseSignUp(
                                    email = cleanEmail,
                                    pass = cleanPass,
                                    shopName = shopName.trim(),
                                    ownerName = ownerName.trim()
                                ) { result ->
                                    isLoading = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            successMessage = if (isBn) "অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে!" else "Account created successfully!"
                                            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        }
                                        is AuthResult.Suggestion -> {
                                            suggestedEmail = result.suggestedEmail
                                            errorMessage = result.message
                                        }
                                        is AuthResult.UserNotFound -> {
                                            userNotFoundEmail = result.email
                                            errorMessage = result.message
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.errorMessage
                                        }
                                    }
                                }
                            } else {
                                viewModel.firebaseSignIn(
                                    email = cleanEmail,
                                    pass = cleanPass
                                ) { result ->
                                    isLoading = false
                                    when (result) {
                                        is AuthResult.Success -> {
                                            successMessage = if (isBn) "লগইন সফল! ক্লাউড থেকে পূর্বের সকল লেনদেন ও খাতা লোড হচ্ছে..." else "Signed in! Restoring previous transactions from Cloud..."
                                            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        }
                                        is AuthResult.Suggestion -> {
                                            suggestedEmail = result.suggestedEmail
                                            errorMessage = result.message
                                        }
                                        is AuthResult.UserNotFound -> {
                                            userNotFoundEmail = result.email
                                            errorMessage = result.message
                                        }
                                        is AuthResult.Error -> {
                                            errorMessage = result.errorMessage
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (screenMode == AuthScreenMode.SIGN_UP) {
                                    if (isBn) "অ্যাকাউন্ট তৈরি হচ্ছে..." else "Creating Account..."
                                } else {
                                    if (isBn) "লগইন হচ্ছে..." else "Signing in..."
                                },
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = if (screenMode == AuthScreenMode.SIGN_UP) Icons.Default.PersonAdd else Icons.Default.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (screenMode == AuthScreenMode.SIGN_UP) {
                                    if (isBn) "নতুন অ্যাকাউন্ট তৈরি করুন" else "Create New Account"
                                } else {
                                    if (isBn) "লগইন করুন" else "Sign In"
                                },
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
                                    text = if (isBn) "ক্লাউড ডেটা সুরক্ষা" else "Cloud Data Protection",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isBn)
                                        "লগইন বা রেজিস্ট্রেশন করার পর আপনার সকল পণ্যের হিসাব, কাস্টমার বাকি খাতা এবং ক্যাশ রিপোর্ট স্বয়ংক্রিয়ভাবে ক্লাউডে নিরাপদ থাকবে।"
                                    else
                                        "After sign-in, all products, dues, and cash reports are safely linked to your cloud account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Guest / Skip Mode
                    TextButton(
                        onClick = {
                            viewModel.loginAsGuest()
                            onLoginSuccess()
                        }
                    ) {
                        Text(
                            text = if (isBn) "গেস্ট হিসেবে প্রবেশ করুন (পরে একাউন্ট করব)" else "Continue as Guest (Sign in later)",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isResettingPassword) showForgotPasswordDialog = false
            },
            icon = {
                Icon(Icons.Default.LockReset, contentDescription = null, tint = StockBlue, modifier = Modifier.size(32.dp))
            },
            title = {
                Text(
                    text = if (isBn) "পাসওয়ার্ড রিসেট লিংক পাঠান" else "Send Password Reset Link",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isBn)
                            "আপনার জিমেইল আইডি লিখুন। পাসওয়ার্ড পরিবর্তন করার লিংক আপনার ইনবক্সে পাঠানো হবে।"
                        else
                            "Enter your registered Gmail. A password reset link will be emailed to you.",
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
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
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
