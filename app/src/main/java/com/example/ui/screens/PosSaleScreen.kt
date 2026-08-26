package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.ui.components.toIntOrNull
import com.example.ui.theme.*
import com.example.ui.viewmodel.ShopViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosSaleScreen(
    viewModel: ShopViewModel,
    onSaleCompleted: (String) -> Unit
) {
    val products by viewModel.products.collectAsState()
    val customCategories by viewModel.customCategories.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val customerName by viewModel.cartCustomerName.collectAsState()
    val customerPhone by viewModel.cartCustomerPhone.collectAsState()
    val discount by viewModel.cartDiscount.collectAsState()
    val paidAmount by viewModel.cartPaidAmount.collectAsState()
    val paymentMethod by viewModel.cartPaymentMethod.collectAsState()
    val note by viewModel.cartNote.collectAsState()
    val shopInfo by viewModel.shopInfo.collectAsState()
    val language by viewModel.language.collectAsState()
    val currency = shopInfo.currency

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("সব") }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showAddCustomerInPosDialog by remember { mutableStateOf(false) }
    var customerSearchQuery by remember { mutableStateOf("") }

    val categories = remember(customCategories, language) {
        listOf(if (language == "bn") "সব" else "All") + customCategories
    }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true)
            val matchCat = selectedCategory == "সব" || p.category == selectedCategory
            matchQuery && matchCat
        }
    }

    val grossSubtotal = remember(cartItems) { cartItems.sumOf { it.total } }
    val netPayable = remember(grossSubtotal, discount) { (grossSubtotal - discount).coerceAtLeast(0.0) }
    val calculatedDue = remember(netPayable, paidAmount) { (netPayable - paidAmount).coerceAtLeast(0.0) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Sales Content
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top POS Header & Search
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(if (language == "bn") "পণ্য খুঁজুন বা সিলেক্ট করুন..." else "Search product to add...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Two Sections: Available Quick Products (Top) & Cart Ledger (Bottom)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Add Product Grid / Cards
                item {
                    Text(
                        text = if (language == "bn") "পণ্য তালিকা (ক্লিক করে কার্টে যোগ করুন)" else "Products (Tap to add to cart)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts.take(12)) { prod ->
                            Card(
                                modifier = Modifier
                                    .width(145.dp)
                                    .clickable { viewModel.addToCart(prod) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = prod.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "$currency${prod.sellPrice.toIntOrNull() ?: prod.sellPrice}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${prod.stockQuantity.toIntOrNull() ?: prod.stockQuantity} ${prod.unit}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (prod.stockQuantity <= 3) LossRed else MaterialTheme.colorScheme.outline
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = EmeraldPrimary.copy(alpha = 0.15f),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "Add",
                                                tint = EmeraldPrimary,
                                                modifier = Modifier.padding(3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Cart Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${if (language == "bn") "বিক্রয় কার্ট" else "Cart Items"} (${cartItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (cartItems.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearCart() }) {
                                Text(if (language == "bn") "কার্ট খালি করুন" else "Clear", color = LossRed)
                            }
                        }
                    }
                }

                if (cartItems.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (language == "bn") "কার্ট খালি! পণ্য সিলেক্ট করুন।" else "Cart is empty. Select products above.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(cartItems) { item ->
                        PosCartItemRow(
                            item = item,
                            currency = currency,
                            language = language,
                            onQuantityChange = { newQ -> viewModel.updateCartItemQuantity(item.product.id, newQ) },
                            onPriceChange = { newP -> viewModel.updateCartItemPrice(item.product.id, newP) },
                            onRemove = { viewModel.removeFromCart(item.product.id) }
                        )
                    }

                    // Customer Details Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == "bn") "ক্রেতার তথ্য" else "Customer Info",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    TextButton(onClick = { showCustomerPicker = true }) {
                                        Text(if (language == "bn") "খাতা থেকে নির্বাচন" else "Select Customer")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customerName,
                                        onValueChange = { viewModel.cartCustomerName.value = it },
                                        label = { Text(if (language == "bn") "ক্রেতার নাম" else "Name") },
                                        placeholder = { Text(if (language == "bn") "ক্যাশ কাস্টমার" else "Cash Customer") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = customerPhone,
                                        onValueChange = { viewModel.cartCustomerPhone.value = it },
                                        label = { Text(if (language == "bn") "মোবাইল নং" else "Phone") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Payment Calculation & Checkout Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = if (language == "bn") "পেমেন্ট ও বিলিং" else "Payment & Billing",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Subtotal
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (language == "bn") "মোট মূল্য:" else "Subtotal:", color = Color(0xFF475569))
                                    Text("$currency${grossSubtotal.toIntOrNull() ?: grossSubtotal}", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Discount Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (language == "bn") "ছাড় / ডিসকাউন্ট:" else "Discount:", color = ProfitGreen)
                                    OutlinedTextField(
                                        value = if (discount > 0) discount.toString().replace(".0", "") else "",
                                        onValueChange = { viewModel.setDiscount(it.toDoubleOrNull() ?: 0.0) },
                                        placeholder = { Text("0") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(110.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Grand Total
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        if (language == "bn") "সর্বমোট প্রদেয়:" else "Grand Total:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "$currency${netPayable.toIntOrNull() ?: netPayable}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = EmeraldPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Payment Method Selection Chips
                                Text(
                                    text = if (language == "bn") "পেমেন্ট মাধ্যম:" else "Payment Method:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "CASH" to if (language == "bn") "নগদ ক্যাশ" else "Cash",
                                        "BKASH" to "bKash",
                                        "NAGAD" to "Nagad",
                                        "DUE" to if (language == "bn") "সম্পূর্ণ বাকি" else "Full Due"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = paymentMethod == key,
                                            onClick = {
                                                viewModel.cartPaymentMethod.value = key
                                                if (key == "DUE") {
                                                    viewModel.cartPaidAmount.value = 0.0
                                                    // If customer is not selected or is generic cash customer, prompt selection
                                                    if (customerName.isBlank() || customerName == "ক্যাশ কাস্টমার" || customerName == "Cash Customer") {
                                                        showCustomerPicker = true
                                                    }
                                                } else {
                                                    viewModel.cartPaidAmount.value = netPayable
                                                }
                                            },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Paid Amount Input
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (language == "bn") "জমা / নগদ গ্রহণ:" else "Paid Amount:",
                                        fontWeight = FontWeight.Bold,
                                        color = ProfitGreen
                                    )
                                    OutlinedTextField(
                                        value = paidAmount.toString().replace(".0", ""),
                                        onValueChange = { viewModel.cartPaidAmount.value = it.toDoubleOrNull() ?: 0.0 },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(130.dp)
                                    )
                                }

                                // Due Balance display
                                if (calculatedDue > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (language == "bn") "বাকি হিসাব যোগ হবে:" else "Due will be added:",
                                            color = LossRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$currency${calculatedDue.toIntOrNull() ?: calculatedDue}",
                                            color = LossRed,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Confirm Sale Button
                                Button(
                                    onClick = {
                                        viewModel.checkoutSale { invoiceNo ->
                                            onSaleCompleted(invoiceNo)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (language == "bn") "বিক্রি সম্পন্ন করুন ও মেমো দেখুন" else "Confirm Sale & Generate Memo",
                                        style = MaterialTheme.typography.titleMedium,
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

    // Enhanced Customer Picker Modal with Search & Add Customer
    if (showCustomerPicker) {
        val filteredCustomers = remember(customers, customerSearchQuery) {
            customers.filter {
                customerSearchQuery.isBlank() ||
                        it.name.contains(customerSearchQuery, ignoreCase = true) ||
                        it.phone.contains(customerSearchQuery, ignoreCase = true) ||
                        it.address.contains(customerSearchQuery, ignoreCase = true)
            }
        }

        Dialog(onDismissRequest = { showCustomerPicker = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == "bn") "বাকি / কাস্টমার নির্বাচন" else "Select Customer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = { showCustomerPicker = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // + Add New Customer Button
                    Button(
                        onClick = { showAddCustomerInPosDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DueOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == "bn") "+ নতুন কাস্টমার যোগ করুন" else "+ Add New Customer",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Customer field
                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = { customerSearchQuery = it },
                        placeholder = { Text(if (language == "bn") "নাম, মোবাইল বা ঠিকানা খুঁজুন..." else "Search name, phone, address...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Customer List
                    if (filteredCustomers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (language == "bn") "কোনো কাস্টমার পাওয়া যায়নি" else "No customers found",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                TextButton(onClick = { showAddCustomerInPosDialog = true }) {
                                    Text(if (language == "bn") "নতুন কাস্টমার হিসেবে তৈরি করুন" else "Create New Customer")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredCustomers) { c ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, if (c.totalDue > 0) DueOrange.copy(alpha = 0.3f) else Color.Transparent),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.cartCustomerName.value = c.name
                                            viewModel.cartCustomerPhone.value = c.phone
                                            showCustomerPicker = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (c.totalDue > 0) Color(0xFFFFEDD5) else Color(0xFFDCFCE7),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = c.name.take(1).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (c.totalDue > 0) DueOrange else ProfitGreen
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(c.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                                if (c.phone.isNotBlank()) {
                                                    Text(c.phone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                                }
                                                if (c.address.isNotBlank()) {
                                                    Text(c.address, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                                                }
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            if (c.totalDue > 0) {
                                                Text(
                                                    "বাকি: $currency${c.totalDue.toIntOrNull() ?: c.totalDue}",
                                                    color = DueOrange,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            } else {
                                                Text(
                                                    if (language == "bn") "পরিশোধিত" else "Settled",
                                                    color = ProfitGreen,
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
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

    // In-place Add Customer Dialog inside POS Sale Screen
    if (showAddCustomerInPosDialog) {
        AddCustomerDialog(
            currency = currency,
            language = language,
            onDismiss = { showAddCustomerInPosDialog = false },
            onSave = { name, phone, address, initialDue, imageUri ->
                viewModel.addCustomer(name, phone, address, initialDue, imageUri)
                // Automatically assign this newly created customer to the active cart!
                viewModel.cartCustomerName.value = name
                viewModel.cartCustomerPhone.value = phone
                showAddCustomerInPosDialog = false
                showCustomerPicker = false
            }
        )
    }
}

@Composable
fun PosCartItemRow(
    item: CartItem,
    currency: String,
    language: String,
    onQuantityChange: (Double) -> Unit,
    onPriceChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${if (language == "bn") "দর: " else "Rate: "}$currency${item.customPrice.toIntOrNull() ?: item.customPrice} / ${item.product.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Stepper for quantity
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChange(item.quantity - 1.0) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${item.quantity.toIntOrNull() ?: item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1.0) },
                    modifier = Modifier
                        .size(32.dp)
                        .background(EmeraldPrimary.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                }
            }

            // Total price & delete button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$currency${item.total.toIntOrNull() ?: item.total}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
