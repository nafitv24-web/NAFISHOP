package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LossRed
import com.example.ui.theme.ProfitGreen
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

enum class ScannerMode {
    BARCODE,        // Barcode and QR code scanning
    PACKET_TEXT     // OCR: scans text/brand printed on product packaging
}

/**
 * Intelligent Camera Product Scanner for POS:
 * 1. Barcode / QR Code Scanner (অটোমেটিক বারকোড শনাক্তকরণ)
 * 2. Packet Text / Label OCR (বারকোড না থাকলে প্যাকেটের গায়ের নাম/লেখা পড়ে স্বয়ংক্রিয় শনাক্তকরণ)
 * 3. Quick Visual Unbarcoded Products Drawer (বারকোড ছাড়া সব পণ্যের এক-ক্লিকে কার্টে যোগ)
 * 4. Voice Search (মুখে বলে পণ্য শনাক্তকরণ)
 */
@Composable
fun PosCameraBarcodeScannerDialog(
    products: List<Product>,
    cartItems: List<CartItem> = emptyList(),
    currency: String,
    language: String,
    onProductScanned: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var scannerMode by remember { mutableStateOf(ScannerMode.BARCODE) }
    var isTorchOn by remember { mutableStateOf(false) }
    var continuousScan by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var lastScannedCode by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableLongStateOf(0L) }

    // Feedback states
    var recentlyAddedProduct by remember { mutableStateOf<Product?>(null) }
    var notFoundBarcode by remember { mutableStateOf<String?>(null) }
    var manualInputCode by remember { mutableStateOf("") }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var showUnbarcodedDrawer by remember { mutableStateOf(false) }

    // OCR detected text and matched candidates
    var detectedLabelMatches by remember { mutableStateOf<List<Product>>(emptyList()) }
    var lastDetectedRawText by remember { mutableStateOf("") }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }
    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Voice recognition launcher
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenWords = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenWords.isNullOrBlank()) {
                val matched = products.find {
                    it.name.contains(spokenWords, ignoreCase = true) ||
                    spokenWords.contains(it.name, ignoreCase = true) ||
                    it.category.contains(spokenWords, ignoreCase = true)
                }
                if (matched != null) {
                    recentlyAddedProduct = matched
                    onProductScanned(matched)
                } else {
                    notFoundBarcode = spokenWords
                }
            }
        }
    }

    fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                if (language == "bn") "পণ্যের নাম বলুন..." else "Speak product name..."
            )
        }
        try {
            voiceSearchLauncher.launch(intent)
        } catch (_: Exception) {
        }
    }

    // Auto-clear notification toast
    LaunchedEffect(recentlyAddedProduct) {
        if (recentlyAddedProduct != null) {
            delay(2500)
            recentlyAddedProduct = null
        }
    }
    LaunchedEffect(notFoundBarcode) {
        if (notFoundBarcode != null) {
            delay(3000)
            notFoundBarcode = null
        }
    }

    fun triggerFeedback(success: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (success) {
                    VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE)
                } else {
                    VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80), -1)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (success) 70L else 180L)
            }

            if (success) {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
            }
        } catch (_: Exception) {
        }
    }

    fun handleScannedBarcode(rawBarcode: String) {
        val trimmed = rawBarcode.trim()
        if (trimmed.isEmpty()) return

        val now = System.currentTimeMillis()
        if (trimmed == lastScannedCode && (now - lastScannedTime) < 1400L) {
            return
        }

        lastScannedCode = trimmed
        lastScannedTime = now

        val matchedProduct = products.find {
            it.barcode.trim().equals(trimmed, ignoreCase = true) ||
            (it.barcode.isBlank() && it.id.toString() == trimmed)
        }

        if (matchedProduct != null) {
            triggerFeedback(true)
            recentlyAddedProduct = matchedProduct
            notFoundBarcode = null
            onProductScanned(matchedProduct)

            if (!continuousScan) {
                onDismiss()
            }
        } else {
            triggerFeedback(false)
            notFoundBarcode = trimmed
            recentlyAddedProduct = null
        }
    }

    fun handleRecognizedText(extractedText: String) {
        val clean = extractedText.replace("\n", " ").trim()
        if (clean.length < 3 || clean == lastDetectedRawText) return
        lastDetectedRawText = clean

        // Look for any products whose name or parts are in the detected packet text
        val matchingCandidates = products.filter { prod ->
            val nameWords = prod.name.split(" ").filter { it.length >= 3 }
            val directMatch = clean.contains(prod.name, ignoreCase = true) ||
                    prod.name.contains(clean, ignoreCase = true)
            val wordMatch = nameWords.any { clean.contains(it, ignoreCase = true) }
            directMatch || wordMatch
        }.take(4)

        if (matchingCandidates.isNotEmpty()) {
            detectedLabelMatches = matchingCandidates
        }
    }

    // Scanner animation
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    val unbarcodedProducts = remember(products) {
        products.filter { it.barcode.isBlank() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera Preview
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            @OptIn(ExperimentalGetImage::class)
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                if (scannerMode == ScannerMode.BARCODE) {
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            for (b in barcodes) {
                                                val value = b.rawValue
                                                if (!value.isNullOrBlank()) {
                                                    handleScannedBarcode(value)
                                                    break
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    // Text Recognition Mode (OCR for packages without barcodes)
                                    textRecognizer.process(inputImage)
                                        .addOnSuccessListener { visionText ->
                                            if (visionText.text.isNotBlank()) {
                                                handleRecognizedText(visionText.text)
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Dark semi-transparent framing mask
            Column(modifier = Modifier.fillMaxSize()) {
                // Top shade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black.copy(alpha = 0.55f))
                )

                // Middle scanning window
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )

                    // Reticle Box
                    Box(
                        modifier = Modifier
                            .size(270.dp)
                            .border(
                                2.dp,
                                if (scannerMode == ScannerMode.BARCODE) EmeraldPrimary.copy(alpha = 0.8f) else Color(0xFF38BDF8),
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        ) {
                            // Animated Laser Beam
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .offset(y = (laserPosition * 250).dp)
                                    .background(if (scannerMode == ScannerMode.BARCODE) EmeraldPrimary else Color(0xFF38BDF8))
                            )
                        }

                        // Reticle Center Guide Text
                        Text(
                            text = if (scannerMode == ScannerMode.BARCODE) {
                                if (language == "bn") "বারকোড বা কিউআর কোড এখানে ধরুন" else "Align Barcode in frame"
                            } else {
                                if (language == "bn") "প্যাকেটের গায়ের নাম বা লেখার উপর ধরুন" else "Align product name/text in frame"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp)
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black.copy(alpha = 0.55f))
                    )
                }

                // Bottom shade
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.3f)
                        .background(Color.Black.copy(alpha = 0.55f))
                )
            }

            // Top Header & Mode Switcher
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color.Black.copy(alpha = 0.82f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Close",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "ক্যামেরা পণ্য স্ক্যানার" else "Smart Product Scanner",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (scannerMode == ScannerMode.BARCODE) {
                                        if (language == "bn") "বারকোড ধরলে অটো যোগ হবে" else "Barcode auto adds to cart"
                                    } else {
                                        if (language == "bn") "প্যাকেটের লেখা পড়ে শনাক্ত করবে" else "Reads text printed on packet"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (scannerMode == ScannerMode.BARCODE) EmeraldPrimary else Color(0xFF38BDF8)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Flashlight Toggle
                            IconButton(
                                onClick = {
                                    isTorchOn = !isTorchOn
                                    camera?.cameraControl?.enableTorch(isTorchOn)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isTorchOn) EmeraldPrimary else Color.White.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Torch",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Voice Input Button
                            IconButton(
                                onClick = { launchVoiceSearch() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Continuous scan toggle
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (continuousScan) EmeraldPrimary.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, if (continuousScan) EmeraldPrimary else Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { continuousScan = !continuousScan }
                            ) {
                                Text(
                                    text = if (continuousScan) (if (language == "bn") "একটানা" else "Cont.") else (if (language == "bn") "একক" else "Single"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (continuousScan) EmeraldPrimary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scanner Mode Tabs: Barcode vs Packet Text (OCR)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (scannerMode == ScannerMode.BARCODE) EmeraldPrimary else Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (scannerMode == ScannerMode.BARCODE) EmeraldPrimary else Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { scannerMode = ScannerMode.BARCODE }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == "bn") "বারকোড স্ক্যান" else "Barcode Scan",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (scannerMode == ScannerMode.PACKET_TEXT) Color(0xFF0284C7) else Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, if (scannerMode == ScannerMode.PACKET_TEXT) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .weight(1.2f)
                                .clickable { scannerMode = ScannerMode.PACKET_TEXT }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == "bn") "প্যাকেটের নাম/লেখা স্ক্যান" else "Packet Name/Text",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // Central Scan Feedback HUD Banners
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 20.dp)
                    .offset(y = 160.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success Scan Notification Toast
                AnimatedVisibility(
                    visible = recentlyAddedProduct != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -30 })
                ) {
                    recentlyAddedProduct?.let { prod ->
                        val inCartCount = cartItems.find { it.product.id == prod.id }?.quantity ?: 1.0
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldPrimary,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${prod.name} কার্টে যোগ হয়েছে!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "$currency${prod.sellPrice} | কার্টে মোট: ${inCartCount.toIntOrNull() ?: inCartCount} ${prod.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }

                // OCR Text Mode: Live Matching Candidates Chips
                if (scannerMode == ScannerMode.PACKET_TEXT && detectedLabelMatches.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (language == "bn") "🔍 প্যাকেটের লেখায় পাওয়া পণ্য (ট্যাপ করে যোগ করুন):" else "🔍 Detected Products on Packet:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8)
                                )
                                IconButton(
                                    onClick = { detectedLabelMatches = emptyList() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(detectedLabelMatches) { prod ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                        modifier = Modifier.clickable {
                                            triggerFeedback(true)
                                            recentlyAddedProduct = prod
                                            onProductScanned(prod)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!prod.imageUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = prod.imageUri,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Column {
                                                Text(
                                                    text = prod.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.widthIn(max = 110.dp)
                                                )
                                                Text(
                                                    text = "$currency${prod.sellPrice} + যোগ করুন",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ProfitGreen,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Not Found Warning Toast
                AnimatedVisibility(
                    visible = notFoundBarcode != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { 30 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { -30 })
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = LossRed,
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (language == "bn") "পণ্য পাওয়া যায়নি!" else "Product Not Found!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$notFoundBarcode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Bar: Cart Summary & Quick Actions
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.90f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    val totalCartQty = cartItems.sumOf { it.quantity }
                    val totalCartAmount = cartItems.sumOf { it.product.sellPrice * it.quantity }

                    // Row of Quick Non-Barcode Actions:
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Unbarcoded Items Drawer Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showUnbarcodedDrawer = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (language == "bn") "বারকোড ছাড়া পণ্য (${unbarcodedProducts.size})" else "No Barcode (${unbarcodedProducts.size})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }

                        // Voice Search Button
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                            modifier = Modifier.clickable { launchVoiceSearch() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color(0xFFA78BFA),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "মুখে বলুন" else "Voice",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDDD6FE)
                                )
                            }
                        }

                        // Manual Code Entry
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.clickable { showManualInputDialog = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "কোড লিখুন" else "Type",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Cart Summary Row & Finish Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == "bn") "কার্ট:" else "Cart:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${totalCartQty.toIntOrNull() ?: totalCartQty} টি পণ্য",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "মোট: $currency${totalCartAmount.toIntOrNull() ?: totalCartAmount}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (language == "bn") "কার্টে যান ✓" else "Done ✓",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Drawer Sheet for Products without Barcode
            if (showUnbarcodedDrawer) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.65f)
                        .align(Alignment.BottomCenter),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (language == "bn") "বারকোড ছাড়া পণ্যসমূহ" else "Products Without Barcode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (language == "bn") "ট্যাপ করলেই কার্টে যোগ হবে" else "Tap to add to cart",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldPrimary
                                )
                            }
                            IconButton(onClick = { showUnbarcodedDrawer = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (unbarcodedProducts.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (language == "bn") "বারকোডহীন কোনো পণ্য স্টকে নেই" else "No unbarcoded products found",
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(unbarcodedProducts) { prod ->
                                    val inCart = cartItems.find { it.product.id == prod.id }?.quantity ?: 0.0
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                triggerFeedback(true)
                                                recentlyAddedProduct = prod
                                                onProductScanned(prod)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                if (!prod.imageUri.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = prod.imageUri,
                                                        contentDescription = prod.name,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(42.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                    )
                                                } else {
                                                    Surface(
                                                        shape = RoundedCornerShape(8.dp),
                                                        color = EmeraldPrimary.copy(alpha = 0.15f),
                                                        modifier = Modifier.size(42.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(Icons.Default.Category, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                                        }
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = prod.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    Text(
                                                        text = "$currency${prod.sellPrice} | স্টক: ${prod.stockQuantity.toIntOrNull() ?: prod.stockQuantity} ${prod.unit}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.outline
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (inCart > 0) {
                                                    Text(
                                                        text = "কার্টে: ${inCart.toIntOrNull() ?: inCart}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = EmeraldPrimary,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                }
                                                Surface(
                                                    shape = CircleShape,
                                                    color = EmeraldPrimary,
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Manual Barcode / Name Entry Dialog
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = {
                Text(
                    text = if (language == "bn") "বারকোড বা পণ্যের নাম লিখুন" else "Enter Barcode or Name",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = manualInputCode,
                        onValueChange = { manualInputCode = it },
                        placeholder = { Text(if (language == "bn") "যেমন: 89411001 বা ব্যাটারি" else "e.g. 89411001 or Battery") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualInputCode.isNotBlank()) {
                            val trimmed = manualInputCode.trim()
                            val matched = products.find {
                                it.barcode.equals(trimmed, ignoreCase = true) ||
                                it.name.contains(trimmed, ignoreCase = true) ||
                                it.id.toString() == trimmed
                            }
                            if (matched != null) {
                                triggerFeedback(true)
                                recentlyAddedProduct = matched
                                onProductScanned(matched)
                            } else {
                                handleScannedBarcode(manualInputCode)
                            }
                            manualInputCode = ""
                            showManualInputDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(if (language == "bn") "যোগ করুন" else "Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text(if (language == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }
}
