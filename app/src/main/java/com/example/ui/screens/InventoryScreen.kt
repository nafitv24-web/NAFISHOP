package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: ShopViewModel,
    onNavigateToPos: () -> Unit
) {
    val products by viewModel.filteredProducts.collectAsState()
    val allProducts by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency = shopInfo.currency

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    var showStockInDialog by remember { mutableStateOf(false) }
    var stockInProduct by remember { mutableStateOf<Product?>(null) }

    var showStockOutDialog by remember { mutableStateOf(false) }
    var stockOutProduct by remember { mutableStateOf<Product?>(null) }

    var showBarcodeScannerModal by remember { mutableStateOf(false) }

    val categories = listOf("সব", "মুদি সামগ্রী", "চাল ও ডাল", "তেল ও ঘি", "চা ও পানীয়", "ডিম ও দুগ্ধজাত", "প্রসাধন", "পরিষ্কারক", "অন্যান্য")

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

                    Spacer(modifier = Modifier.height(12.dp))

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

            // Products Count & Summary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (language == "bn") "মোট প্রদর্শিত: " else "Showing: "}${products.size} ${if (language == "bn") "টি পণ্য" else "items"}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val totalVal = products.sumOf { it.stockQuantity * it.sellPrice }
                Text(
                    text = "${if (language == "bn") "মোট মূল্য: " else "Value: "}$currency${totalVal.toIntOrNull() ?: totalVal}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Product List
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (language == "bn") "কোনো পণ্য পাওয়া যায়নি" else "No products found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == "bn") "নিচের 'নতুন পণ্য' বাটনে চাপ দিয়ে পণ্য যোগ করুন" else "Tap 'Add Product' below to add items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(products, key = { it.id }) { product ->
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
            allProducts = products,
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

            Spacer(modifier = Modifier.height(10.dp))

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
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "মুদি সামগ্রী") }
    var buyPriceStr by remember { mutableStateOf(product?.buyPrice?.toString()?.replace(".0", "") ?: "") }
    var sellPriceStr by remember { mutableStateOf(product?.sellPrice?.toString()?.replace(".0", "") ?: "") }
    var stockStr by remember { mutableStateOf(product?.stockQuantity?.toString()?.replace(".0", "") ?: "10") }
    var unit by remember { mutableStateOf(product?.unit ?: "পিস") }
    var minStockStr by remember { mutableStateOf(product?.minStockAlert?.toString()?.replace(".0", "") ?: "5") }
    var imageUri by remember { mutableStateOf(product?.imageUri ?: "") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri.toString()
        }
    }

    val units = listOf("পিস", "কেজি", "গ্রাম", "লিটার", "বস্তা", "প্যাকেট", "ডজন", "বক্স", "হালি")
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

                // Category Chips Selector
                Text(
                    text = if (language == "bn") "ক্যাটাগরি" else "Category",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                                    imageUri = imageUri
                                ) ?: Product(
                                    name = name,
                                    barcode = barcode,
                                    category = category,
                                    buyPrice = buyP,
                                    sellPrice = sellP,
                                    stockQuantity = stockQ,
                                    unit = unit,
                                    minStockAlert = minS,
                                    imageUri = imageUri
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "স্টক ইন / নতুন ক্রয়" else "Stock In / Purchase",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StockBlue
                    )
                    if (allProducts.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { expandedProductSelector = !expandedProductSelector }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Change Product", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (language == "bn") "পণ্য পরিবর্তন" else "Change",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // If product selection dropdown is opened
                if (expandedProductSelector && allProducts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            OutlinedTextField(
                                value = productSearchQuery,
                                onValueChange = { productSearchQuery = it },
                                placeholder = { Text(if (language == "bn") "পণ্য খুঁজুন..." else "Search product...", style = MaterialTheme.typography.bodySmall) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 160.dp)
                            ) {
                                items(filteredProducts) { p ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (p.id == currentProduct.id) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                            .clickable {
                                                currentProduct = p
                                                expandedProductSelector = false
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text("${p.category} • স্টক: ${p.stockQuantity.toIntOrNull() ?: p.stockQuantity} ${p.unit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        if (p.id == currentProduct.id) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Current Selected Product Information Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(44.dp)
                        ) {
                            if (!currentProduct.imageUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = currentProduct.imageUri,
                                    contentDescription = currentProduct.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = StockBlue, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentProduct.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${if (language == "bn") "বর্তমান স্টক: " else "Current Stock: "}${currentProduct.stockQuantity.toIntOrNull() ?: currentProduct.stockQuantity} ${currentProduct.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StockBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(if (language == "bn") "যোগ করার পরিমাণ (${currentProduct.unit}) *" else "Quantity to Add *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = buyPriceStr,
                        onValueChange = { buyPriceStr = it },
                        label = { Text(if (language == "bn") "নতুন ক্রয়মূল্য ($currency)" else "Buy Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sellPriceStr,
                        onValueChange = { sellPriceStr = it },
                        label = { Text(if (language == "bn") "নতুন বিক্রয়মূল্য ($currency)" else "Sell Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (language == "bn") "নোট / মহাজন / বিবরণ" else "Note / Supplier") },
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
                            if (qty > 0) {
                                val buyP = buyPriceStr.toDoubleOrNull()
                                val sellP = sellPriceStr.toDoubleOrNull()
                                onConfirm(currentProduct, qty, buyP, sellP, note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StockBlue)
                    ) {
                        Text(if (language == "bn") "স্টক যোগ করুন" else "Add Stock")
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

    val reasons = listOf("নষ্ট / মেয়াদোত্তীর্ণ", "ভেঙে গেছে", "ব্যক্তিগত ব্যবহার", "হারিয়ে গেছে", "অন্যান্য")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (language == "bn") "স্টক বাদ / নষ্টের সমন্বয়" else "Stock Out / Damage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = LossRed
                )
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text(if (language == "bn") "বাদ দেওয়ার পরিমাণ (${product.unit})" else "Quantity to Remove") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (language == "bn") "কারণ নির্বাচন করুন:" else "Select Reason:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
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
                        Text(if (language == "bn") "স্টক কমান" else "Reduce Stock")
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
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(0.95f),
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
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated Optical Camera Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (language == "bn") "বারকোড স্ক্যান হচ্ছে..." else "Scanning Barcode...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA7F3D0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (language == "bn") "দ্রুত বারকোড নির্বাচন করুন (সিমুলেশন):" else "Select Barcode to match:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(products.filter { it.barcode.isNotBlank() }) { p ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectProduct(p.barcode) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "Code: ${p.barcode}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
