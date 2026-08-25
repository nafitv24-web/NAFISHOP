package com.example.ui.screens

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Product
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: ShopViewModel,
    onNavigateToPos: () -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.filteredProducts.collectAsState()
    val allProducts by viewModel.products.collectAsState()
    val expiringProducts by viewModel.expiringProducts.collectAsState()
    val expiredProducts by viewModel.expiredProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency = shopInfo.currency

    var currentViewTab by remember { mutableStateOf(0) } // 0: All Products, 1: Low Stock, 2: Expiring & Expired

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    var showStockInDialog by remember { mutableStateOf(false) }
    var stockInProduct by remember { mutableStateOf<Product?>(null) }

    var showStockOutDialog by remember { mutableStateOf(false) }
    var stockOutProduct by remember { mutableStateOf<Product?>(null) }

    var showBarcodeScannerModal by remember { mutableStateOf(false) }

    val defaultCategories = listOf("সব", "মুদি সামগ্রী", "চাল ও ডাল", "তেল ও ঘি", "চা ও পানীয়", "ডিম ও দুগ্ধজাত", "প্রসাধন", "পরিষ্কারক", "অন্যান্য")
    val categories = remember(allProducts) {
        val customCats = allProducts.map { it.category.trim() }.filter { it.isNotBlank() && it != "সব" }
        (listOf("সব") + (customCats + defaultCategories.filter { it != "সব" }).distinct())
    }

    val lowStockProducts = remember(allProducts) {
        allProducts.filter { it.stockQuantity <= it.minStockAlert }
    }

    val allExpiringCombined = remember(expiringProducts, expiredProducts) {
        (expiredProducts + expiringProducts).distinctBy { it.id }.sortedBy { it.expiryDate }
    }

    val displayedProducts = remember(currentViewTab, products, lowStockProducts, allExpiringCombined) {
        when (currentViewTab) {
            1 -> lowStockProducts
            2 -> allExpiringCombined
            else -> products
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == "bn") "নতুন পণ্য" else "Add Product",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Barcode Scanner Button
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text(if (language == "bn") "পণ্যের নাম বা বারকোড খুঁজুন..." else "Search name or barcode...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        FilledTonalIconButton(
                            onClick = { showBarcodeScannerModal = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary View Mode Filter Tabs (All, Low Stock, Expiring/Expired)
                    TabRow(
                        selectedTabIndex = currentViewTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        divider = {}
                    ) {
                        Tab(
                            selected = currentViewTab == 0,
                            onClick = { currentViewTab = 0 },
                            text = {
                                Text(
                                    text = if (language == "bn") "সব পণ্য (${allProducts.size})" else "All (${allProducts.size})",
                                    fontWeight = if (currentViewTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = currentViewTab == 1,
                            onClick = { currentViewTab = 1 },
                            text = {
                                Text(
                                    text = if (language == "bn") "কম স্টক (${lowStockProducts.size})" else "Low Stock (${lowStockProducts.size})",
                                    color = if (lowStockProducts.isNotEmpty()) DueOrange else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (currentViewTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                        Tab(
                            selected = currentViewTab == 2,
                            onClick = { currentViewTab = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (language == "bn") "মেয়াদ শেষ (${allExpiringCombined.size})" else "Expiring (${allExpiringCombined.size})",
                                        color = if (allExpiringCombined.isNotEmpty()) LossRed else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (currentViewTab == 2) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        )
                    }

                    if (currentViewTab == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        // Horizontal Category Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { viewModel.selectedCategory.value = cat },
                                    label = { Text(cat) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Banner for Expiring Products when Tab 2 is active
            if (currentViewTab == 2) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border = BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = LossRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (language == "bn") "মেয়াদ উত্তীর্ণ ও শেষ পর্যায় পণ্য" else "Expiring & Expired Products",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LossRed
                                    )
                                    val totalExpVal = allExpiringCombined.sumOf { it.stockQuantity * it.buyPrice }
                                    Text(
                                        text = "${if (language == "bn") "মেয়াদোত্তীর্ণ: " else "Expired: "}${expiredProducts.size}টি | ${if (language == "bn") "৩০ দিনের মধ্যে: " else "In 30 days: "}${expiringProducts.size}টি (ক্রয়মূল্য: $currency${totalExpVal.toIntOrNull() ?: totalExpVal})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dedicated PDF Export Button for Expiring Products
                        Button(
                            onClick = {
                                val pdfFile = PdfGenerator.generateExpiringProductsPdf(
                                    context = context,
                                    shopName = shopInfo.shopName,
                                    products = allExpiringCombined,
                                    currency = currency
                                )
                                if (pdfFile != null) {
                                    PdfGenerator.openOrSharePdf(
                                        context = context,
                                        file = pdfFile,
                                        chooserTitle = if (language == "bn") "মেয়াদ শেষ পণ্যের তালিকা PDF শেয়ার করুন" else "Share Expiring Products PDF"
                                    )
                                } else {
                                    Toast.makeText(context, "PDF তৈরি করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (language == "bn") "মেয়াদ শেষ তালিকা PDF ডাউনলোড / শেয়ার" else "Export Expiring List PDF",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                // Products Count & Summary Bar with General PDF download button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${if (language == "bn") "মোট প্রদর্শিত: " else "Showing: "}${displayedProducts.size} ${if (language == "bn") "টি পণ্য" else "items"}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val totalVal = displayedProducts.sumOf { it.stockQuantity * it.sellPrice }
                        Text(
                            text = "${if (language == "bn") "মোট মূল্য: " else "Value: "}$currency${totalVal.toIntOrNull() ?: totalVal}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Download Products PDF Button
                    FilledTonalButton(
                        onClick = {
                            val pdfFile = PdfGenerator.generateAllProductsPdf(
                                context = context,
                                shopName = shopInfo.shopName,
                                products = displayedProducts,
                                currency = currency
                            )
                            if (pdfFile != null) {
                                PdfGenerator.openOrSharePdf(
                                    context = context,
                                    file = pdfFile,
                                    chooserTitle = if (language == "bn") "পণ্য তালিকা PDF ডাউনলোড / শেয়ার করুন" else "Download / Share Products PDF"
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    if (language == "bn") "PDF তৈরি করতে সমস্যা হয়েছে" else "Failed to generate PDF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Download PDF", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == "bn") "পণ্য PDF" else "Product PDF",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Product List
            if (displayedProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            if (currentViewTab == 2) Icons.Default.CheckCircle else Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = if (currentViewTab == 2) ProfitGreen else MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (currentViewTab == 2) (if (language == "bn") "কোনো মেয়াদ উত্তীর্ণ বা মেয়াদ শেষ পর্যায়ের পণ্য নেই" else "No expiring or expired products found")
                            else (if (language == "bn") "কোনো পণ্য পাওয়া যায়নি" else "No products found"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (currentViewTab == 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (language == "bn") "নিচের 'নতুন পণ্য' বাটনে চাপ দিয়ে পণ্য যোগ করুন" else "Tap 'Add Product' below to add items",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedProducts, key = { it.id }) { product ->
                        ProductItemCard(
                            product = product,
                            currency = currency,
                            language = language,
                            onStockIn = {
                                stockInProduct = product
                                showStockInDialog = true
                            },
                            onStockOut = {
                                stockOutProduct = product
                                showStockOutDialog = true
                            },
                            onEdit = {
                                editingProduct = product
                                showAddEditDialog = true
                            },
                            onDelete = {
                                viewModel.deleteProduct(product)
                            },
                            onAddToCart = {
                                viewModel.addToCart(product)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            product = editingProduct,
            currency = currency,
            language = language,
            onDismiss = { showAddEditDialog = false },
            onSave = { savedProd ->
                viewModel.saveProduct(savedProd)
                showAddEditDialog = false
            }
        )
    }

    // Stock In Dialog
    if (showStockInDialog && stockInProduct != null) {
        StockInDialog(
            product = stockInProduct!!,
            allProducts = allProducts,
            currency = currency,
            language = language,
            onDismiss = { showStockInDialog = false },
            onConfirm = { targetProd, qty, buyP, sellP, note ->
                viewModel.stockIn(targetProd.id, qty, buyP, sellP, note)
                showStockInDialog = false
            }
        )
    }

    // Stock Out / Damage Dialog
    if (showStockOutDialog && stockOutProduct != null) {
        StockOutDialog(
            product = stockOutProduct!!,
            language = language,
            onDismiss = { showStockOutDialog = false },
            onConfirm = { qty, reason ->
                viewModel.stockOutManual(stockOutProduct!!.id, qty, reason)
                showStockOutDialog = false
            }
        )
    }

    // Barcode Scan / Simulation Modal
    if (showBarcodeScannerModal) {
        BarcodeScannerDialog(
            products = allProducts,
            language = language,
            onDismiss = { showBarcodeScannerModal = false },
            onSelectProduct = { barcode ->
                viewModel.searchQuery.value = barcode
                showBarcodeScannerModal = false
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    currency: String,
    language: String,
    onStockIn: () -> Unit,
    onStockOut: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToCart: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    val isLowStock = product.stockQuantity <= product.minStockAlert
    val isOutOfStock = product.stockQuantity <= 0.0

    val now = System.currentTimeMillis()
    val hasExpiry = product.expiryDate > 0L
    val isExpired = hasExpiry && product.expiryDate < now
    val daysRemaining = if (hasExpiry) ((product.expiryDate - now) / (1000 * 60 * 60 * 24)).toInt() else 999
    val isExpiringSoon = hasExpiry && !isExpired && daysRemaining in 0..30

    val profitMargin = if (product.buyPrice > 0) {
        ((product.sellPrice - product.buyPrice) / product.buyPrice * 100).toInt()
    } else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product Thumbnail / Image
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(52.dp)
                ) {
                    if (!product.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = product.imageUri,
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                when (product.category) {
                                    "মুদি সামগ্রী" -> Icons.Default.LocalGroceryStore
                                    "চাল ও ডাল" -> Icons.Default.Grass
                                    "তেল ও ঘি" -> Icons.Default.Opacity
                                    "চা ও পানীয়" -> Icons.Default.EmojiFoodBeverage
                                    "ডিম ও দুগ্ধজাত" -> Icons.Default.Egg
                                    "প্রসাধন" -> Icons.Default.Face
                                    "পরিষ্কারক" -> Icons.Default.CleaningServices
                                    else -> Icons.Default.Inventory2
                                },
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (product.barcode.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Code: ${product.barcode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                // Stock Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isOutOfStock -> Color(0xFFFEE2E2)
                        isLowStock -> Color(0xFFFEF3C7)
                        else -> Color(0xFFDCFCE7)
                    }
                ) {
                    Text(
                        text = "${if (language == "bn") "স্টক: " else "Stock: "}${product.stockQuantity.toIntOrNull() ?: product.stockQuantity} ${product.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isOutOfStock -> LossRed
                            isLowStock -> DueOrange
                            else -> ProfitGreen
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { expandedMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (language == "bn") "এডিট করুন" else "Edit") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (language == "bn") "স্টক বাদ/নষ্ট" else "Stock Out / Damage") },
                            leadingIcon = { Icon(Icons.Default.RemoveCircleOutline, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                onStockOut()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (language == "bn") "মুছে ফেলুন" else "Delete", color = LossRed) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = LossRed) },
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Expiry Date Badge if configured
            if (hasExpiry) {
                Spacer(modifier = Modifier.height(6.dp))
                val expiryDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(product.expiryDate))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when {
                        isExpired -> Color(0xFFFEE2E2)
                        isExpiringSoon -> Color(0xFFFEF3C7)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when {
                                isExpired -> Icons.Default.Warning
                                isExpiringSoon -> Icons.Default.HourglassBottom
                                else -> Icons.Default.Event
                            },
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = when {
                                isExpired -> LossRed
                                isExpiringSoon -> DueOrange
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isExpired -> "${if (language == "bn") "মেয়াদ উত্তীর্ণ! (" else "Expired! ("}$expiryDateStr)"
                                isExpiringSoon -> "${if (language == "bn") "মেয়াদ শেষ হতে $daysRemaining দিন বাকি (" else "Expiring in $daysRemaining days ("}$expiryDateStr)"
                                else -> "${if (language == "bn") "মেয়াদ: " else "Expiry: "}$expiryDateStr"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isExpired -> LossRed
                                isExpiringSoon -> Color(0xFFB45309)
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pricing Info & Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${if (language == "bn") "বিক্রয়: " else "Sell: "}$currency${product.sellPrice.toIntOrNull() ?: product.sellPrice}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${if (language == "bn") "ক্রয়: " else "Buy: "}$currency${product.buyPrice.toIntOrNull() ?: product.buyPrice}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (profitMargin > 0) {
                        Text(
                            text = "${if (language == "bn") "মুনাফা: " else "Margin: "}+$profitMargin%",
                            style = MaterialTheme.typography.labelSmall,
                            color = ProfitGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Quick Stock In button
                    OutlinedButton(
                        onClick = onStockIn,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Stock In", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "bn") "স্টক ইন" else "In", style = MaterialTheme.typography.labelSmall)
                    }

                    // Sell / POS add
                    Button(
                        onClick = onAddToCart,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (language == "bn") "বিক্রি" else "Sell", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditProductDialog(
    product: Product?,
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "মুদি সামগ্রী") }
    var buyPriceStr by remember { mutableStateOf(product?.buyPrice?.toString()?.replace(".0", "") ?: "") }
    var sellPriceStr by remember { mutableStateOf(product?.sellPrice?.toString()?.replace(".0", "") ?: "") }
    var stockStr by remember { mutableStateOf(product?.stockQuantity?.toString()?.replace(".0", "") ?: "10") }
    var unit by remember { mutableStateOf(product?.unit ?: "পিস") }
    var minStockStr by remember { mutableStateOf(product?.minStockAlert?.toString()?.replace(".0", "") ?: "5") }
    var imageUri by remember { mutableStateOf(product?.imageUri ?: "") }
    var expiryTimestamp by remember { mutableStateOf(product?.expiryDate ?: 0L) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    val categories = listOf("মুদি সামগ্রী", "চাল ও ডাল", "তেল ও ঘি", "চা ও পানীয়", "ডিম ও দুগ্ধজাত", "প্রসাধন", "পরিষ্কারক", "অন্যান্য")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (product == null) (if (language == "bn") "নতুন পণ্য যোগ করুন" else "Add New Product")
                    else (if (language == "bn") "পণ্য সম্পাদনা" else "Edit Product"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Product Image Selector Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(64.dp)
                        ) {
                            if (imageUri.isNotBlank()) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Product Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Add Photo",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == "bn") "পণ্যের ছবি" else "Product Photo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (imageUri.isNotBlank()) (if (language == "bn") "বদলান" else "Change") else (if (language == "bn") "ছবি দিন" else "Choose"),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (imageUri.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { imageUri = "" },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(if (language == "bn") "মুছুন" else "Remove", style = MaterialTheme.typography.labelSmall, color = LossRed)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (language == "bn") "পণ্যের নাম *" else "Product Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Text Input & Chips
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(if (language == "bn") "ক্যাটাগরি লিখুন বা বাছাই করুন" else "Category Name (Type or Select)") },
                    placeholder = { Text(if (language == "bn") "যেমন: মুদি, ফার্মেসি, প্রসাধন..." else "e.g. Grocery, Stationery...") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = EmeraldPrimary) },
                    trailingIcon = {
                        if (category.isNotBlank()) {
                            IconButton(onClick = { category = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (language == "bn") "জনপ্রিয় ক্যাটাগরি তালিকা (ট্যাপ করুন):" else "Quick suggestions (Tap to select):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category.trim().equals(cat.trim(), ignoreCase = true),
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text(if (language == "bn") "বারকোড/কোড" else "Barcode/Code") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { barcode = (10000000..99999999).random().toString() },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Auto Code", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buy & Sell Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = buyPriceStr,
                        onValueChange = { buyPriceStr = it },
                        label = { Text(if (language == "bn") "ক্রয়মূল্য ($currency)" else "Buy Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPriceStr,
                        onValueChange = { sellPriceStr = it },
                        label = { Text(if (language == "bn") "বিক্রয়মূল্য ($currency)" else "Sell Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stock & Unit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text(if (language == "bn") "স্টক পরিমাণ" else "Stock Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(if (language == "bn") "একক (পিস/কেজি)" else "Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = minStockStr,
                    onValueChange = { minStockStr = it },
                    label = { Text(if (language == "bn") "কম স্টক অ্যালার্ট লিমিট" else "Low Stock Alert Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Product Expiry Date (পণ্যের মেয়াদ শেষ হওয়ার তারিখ)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = DueOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (language == "bn") "পণ্যের মেয়াদ (Expiry Date)" else "Expiry Date",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (expiryTimestamp > 0L) {
                                TextButton(
                                    onClick = { expiryTimestamp = 0L },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(if (language == "bn") "মেয়াদ বাদ দিন" else "Clear", color = LossRed, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (expiryTimestamp > 0L) {
                            val expDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(expiryTimestamp))
                            Text(
                                text = "${if (language == "bn") "নির্বাচিত মেয়াদ শেষ: " else "Selected Expiry: "}$expDateStr",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            Text(
                                text = if (language == "bn") "মেয়াদ যুক্ত করা হয়নি (ঐচ্ছিক)" else "No expiry date set (Optional)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Quick duration chips & DatePicker trigger
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MONTH, 1)
                                    expiryTimestamp = cal.timeInMillis
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+১ মাস", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MONTH, 3)
                                    expiryTimestamp = cal.timeInMillis
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+৩ মাস", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MONTH, 6)
                                    expiryTimestamp = cal.timeInMillis
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+৬ মাস", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    if (expiryTimestamp > 0L) {
                                        cal.timeInMillis = expiryTimestamp
                                    }
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val picked = Calendar.getInstance()
                                            picked.set(y, m, d, 23, 59, 59)
                                            expiryTimestamp = picked.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (language == "bn") "বাতিল" else "Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val buyP = buyPriceStr.toDoubleOrNull() ?: 0.0
                                val sellP = sellPriceStr.toDoubleOrNull() ?: 0.0
                                val stockQ = stockStr.toDoubleOrNull() ?: 0.0
                                val minS = minStockStr.toDoubleOrNull() ?: 5.0

                                val newProd = product?.copy(
                                    name = name,
                                    barcode = barcode,
                                    category = category,
                                    buyPrice = buyP,
                                    sellPrice = sellP,
                                    stockQuantity = stockQ,
                                    unit = unit,
                                    minStockAlert = minS,
                                    imageUri = imageUri,
                                    expiryDate = expiryTimestamp
                                ) ?: Product(
                                    name = name,
                                    barcode = barcode,
                                    category = category,
                                    buyPrice = buyP,
                                    sellPrice = sellP,
                                    stockQuantity = stockQ,
                                    unit = unit,
                                    minStockAlert = minS,
                                    imageUri = imageUri,
                                    expiryDate = expiryTimestamp
                                )
                                onSave(newProd)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text(if (language == "bn") "সংরক্ষণ করুন" else "Save")
                    }
                }
            }
        }
    }
}

@Composable
fun StockInDialog(
    product: Product,
    allProducts: List<Product> = emptyList(),
    currency: String,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (Product, Double, Double?, Double?, String) -> Unit
) {
    var currentProduct by remember(product) { mutableStateOf(product) }
    var quantityStr by remember { mutableStateOf("10") }
    var buyPriceStr by remember(currentProduct) { mutableStateOf(currentProduct.buyPrice.toString().replace(".0", "")) }
    var sellPriceStr by remember(currentProduct) { mutableStateOf(currentProduct.sellPrice.toString().replace(".0", "")) }
    var note by remember { mutableStateOf("নতুন মাল ক্রয়") }

    var expandedProductSelector by remember { mutableStateOf(false) }
    var productSearchQuery by remember { mutableStateOf("") }

    val filteredProducts = remember(productSearchQuery, allProducts) {
        if (productSearchQuery.isBlank()) allProducts
        else allProducts.filter { it.name.contains(productSearchQuery, ignoreCase = true) || it.barcode.contains(productSearchQuery) }
    }

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
                    text = if (language == "bn") "স্টক ইন / মাল ক্রয় যোগ" else "Stock In / Add Purchase",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Product selector
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentProduct.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${if (language == "bn") "বর্তমান স্টক: " else "Current Stock: "}${currentProduct.stockQuantity.toIntOrNull() ?: currentProduct.stockQuantity} ${currentProduct.unit}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            if (allProducts.size > 1) {
                                TextButton(onClick = { expandedProductSelector = !expandedProductSelector }) {
                                    Text(if (language == "bn") "পণ্য পরিবর্তন" else "Change")
                                }
                            }
                        }

                        if (expandedProductSelector) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = productSearchQuery,
                                onValueChange = { productSearchQuery = it },
                                placeholder = { Text(if (language == "bn") "অন্য পণ্য খুঁজুন..." else "Search other product...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                                items(filteredProducts.take(10)) { p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                currentProduct = p
                                                expandedProductSelector = false
                                            }
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("${p.stockQuantity.toIntOrNull() ?: p.stockQuantity} ${p.unit}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(if (language == "bn") "নতুন আগত পরিমাণ (${currentProduct.unit}) *" else "Quantity (${currentProduct.unit}) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = buyPriceStr,
                        onValueChange = { buyPriceStr = it },
                        label = { Text(if (language == "bn") "ক্রয়মূল্য ($currency)" else "Buy Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPriceStr,
                        onValueChange = { sellPriceStr = it },
                        label = { Text(if (language == "bn") "বিক্রয়মূল্য ($currency)" else "Sell Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "নোট বা সরবরাহকারীর নাম" else "Note / Supplier") },
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
                            val qty = quantityStr.toDoubleOrNull() ?: 0.0
                            val buyP = buyPriceStr.toDoubleOrNull()
                            val sellP = sellPriceStr.toDoubleOrNull()
                            if (qty > 0) {
                                onConfirm(currentProduct, qty, buyP, sellP, note)
                            }
                        }
                    ) {
                        Text(if (language == "bn") "স্টক যুক্ত করুন" else "Add to Stock")
                    }
                }
            }
        }
    }
}

@Composable
fun StockOutDialog(
    product: Product,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var quantityStr by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("নষ্ট / মেয়াদোত্তীর্ণ") }

    val reasons = listOf("নষ্ট / মেয়াদোত্তীর্ণ", "ভাঙা / ক্ষতিগ্রস্থ", "নিজ ব্যবহারের জন্য", "হারিয়ে গেছে", "অন্যান্য")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "স্টক বাদ / নষ্ট হিসাব" else "Stock Out / Damage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LossRed
                )
                Text(
                    text = "${product.name} (বর্তমান স্টক: ${product.stockQuantity.toIntOrNull() ?: product.stockQuantity} ${product.unit})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(if (language == "bn") "বাদ দেওয়ার পরিমাণ (${product.unit}) *" else "Quantity to Remove *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (language == "bn") "কারণ বাছাই করুন:" else "Select Reason:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(reasons) { r ->
                        FilterChip(
                            selected = reason == r,
                            onClick = { reason = r },
                            label = { Text(r, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

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
                            val qty = quantityStr.toDoubleOrNull() ?: 0.0
                            if (qty > 0) {
                                onConfirm(qty, reason)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                    ) {
                        Text(if (language == "bn") "স্টক থেকে বাদ দিন" else "Remove Stock")
                    }
                }
            }
        }
    }
}

@Composable
fun BarcodeScannerDialog(
    products: List<Product>,
    language: String,
    onDismiss: () -> Unit,
    onSelectProduct: (String) -> Unit
) {
    var manualBarcode by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "বারকোড স্ক্যানার" else "Barcode Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label = { Text(if (language == "bn") "বারকোড লিখুন বা স্ক্যান করুন" else "Enter / Scan Barcode") },
                    leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (language == "bn") "বারকোড যুক্ত বিদ্যমান পণ্যসমূহ (ট্যাপ করুন):" else "Products with Barcode:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )

                val withBarcode = remember(products) { products.filter { it.barcode.isNotBlank() } }

                if (withBarcode.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (language == "bn") "কোনো বারকোড যুক্ত পণ্য নেই" else "No products with barcode", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(withBarcode) { prod ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable { onSelectProduct(prod.barcode) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(prod.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                                    Text(prod.barcode, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (manualBarcode.isNotBlank()) {
                            onSelectProduct(manualBarcode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (language == "bn") "বারকোড দিয়ে খুঁজুন" else "Search Barcode")
                }
            }
        }
    }
}
