package com.example.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.*
import com.example.ui.theme.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTailorOrderDialog(
    initialOrder: TailorOrder? = null,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (TailorOrder) -> Unit
) {
    val context = LocalContext.current
    val isEdit = initialOrder != null

    var customerName by remember { mutableStateOf(initialOrder?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(initialOrder?.customerPhone ?: "") }
    var customerAddress by remember { mutableStateOf(initialOrder?.customerAddress ?: "") }

    val defaultOrderNumber = remember {
        if (initialOrder != null && initialOrder.orderNumber.isNotBlank()) {
            initialOrder.orderNumber
        } else {
            "T-${SimpleDateFormat("MMdd-HHmm", Locale.getDefault()).format(Date())}"
        }
    }
    var orderNumber by remember { mutableStateOf(defaultOrderNumber) }

    var orderDate by remember { mutableLongStateOf(initialOrder?.orderDate ?: System.currentTimeMillis()) }
    var deliveryDate by remember {
        mutableLongStateOf(initialOrder?.deliveryDate ?: (System.currentTimeMillis() + 86400000L * 5))
    }

    var fabricType by remember { mutableStateOf(initialOrder?.fabricType ?: "কাস্টমারের কাপড়") }
    var designNotes by remember { mutableStateOf(initialOrder?.designNotes ?: "") }
    var paymentMethod by remember { mutableStateOf(initialOrder?.paymentMethod ?: "CASH") }

    // List of garments added to this order
    var garmentsList by remember {
        mutableStateOf(
            if (initialOrder != null) {
                initialOrder.parseGarments()
            } else {
                // By default start with 1 Kamiz
                listOf(
                    GarmentMeasurementItem(
                        garmentType = GarmentType.KAMIZ.name,
                        garmentName = "কামিজ",
                        quantity = 1,
                        stitchingRate = 250.0,
                        measurements = mutableMapOf()
                    )
                )
            }
        )
    }

    // Auto-calculate default total bill
    val calculatedTotal = remember(garmentsList) {
        garmentsList.sumOf { it.totalRate }
    }

    var manualTotalAmountText by remember {
        mutableStateOf(
            if (initialOrder != null) {
                if (initialOrder.totalAmount % 1.0 == 0.0) initialOrder.totalAmount.toInt().toString() else initialOrder.totalAmount.toString()
            } else {
                if (calculatedTotal % 1.0 == 0.0) calculatedTotal.toInt().toString() else calculatedTotal.toString()
            }
        )
    }

    // Sync manualTotalAmountText when calculatedTotal changes if user hasn't manually overridden it or starting fresh
    LaunchedEffect(calculatedTotal) {
        if (!isEdit && manualTotalAmountText.isBlank() || manualTotalAmountText == "0") {
            manualTotalAmountText = if (calculatedTotal % 1.0 == 0.0) calculatedTotal.toInt().toString() else calculatedTotal.toString()
        }
    }

    var advancePaidText by remember {
        mutableStateOf(
            if (initialOrder != null) {
                if (initialOrder.advancePaid % 1.0 == 0.0) initialOrder.advancePaid.toInt().toString() else initialOrder.advancePaid.toString()
            } else ""
        )
    }

    val finalTotal = manualTotalAmountText.toDoubleOrNull() ?: calculatedTotal
    val finalAdvance = advancePaidText.toDoubleOrNull() ?: 0.0
    val finalDue = (finalTotal - finalAdvance).coerceAtLeast(0.0)

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.95f)
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Straighten,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isEdit) "অর্ডার ও মাপ এডিট করুন" else "নতুন দর্জি অর্ডার ও মাপ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "কামিজ, সেলোয়ার, ফ্রক, পেটিকোট, ব্লাউজ ইত্যাদি",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: Customer Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "👤 কাস্টমারের তথ্য",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )

                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text("কাস্টমারের নাম *") },
                                placeholder = { Text("উদাঃ নাসরিন আক্তার") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { customerPhone = it },
                                    label = { Text("মোবাইল নম্বর") },
                                    placeholder = { Text("017xxxxxxxx") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = orderNumber,
                                    onValueChange = { orderNumber = it },
                                    label = { Text("রসিদ / অর্ডার নং") },
                                    singleLine = true,
                                    modifier = Modifier.weight(0.8f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            OutlinedTextField(
                                value = customerAddress,
                                onValueChange = { customerAddress = it },
                                label = { Text("ঠিকানা (ঐচ্ছিক)") },
                                placeholder = { Text("গ্রাম / রোড / এলাকা") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // SECTION 2: Dates & Delivery
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "📅 ডেলিভারি তারিখ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "ডেলিভারি দেওয়ার দিন:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateFormat.format(Date(deliveryDate)),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = LossRed
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        val cal = Calendar.getInstance().apply { timeInMillis = deliveryDate }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val selCal = Calendar.getInstance().apply {
                                                    set(y, m, d, 18, 0, 0)
                                                }
                                                deliveryDate = selCal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("তারিখ পরিবর্তন")
                                }
                            }

                            // Quick delivery day chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(3 to "+৩ দিন", 5 to "+৫ দিন", 7 to "+৭ দিন", 10 to "+১০ দিন", 15 to "+১৫ দিন").forEach { (days, label) ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            deliveryDate = System.currentTimeMillis() + (86400000L * days)
                                        },
                                        label = { Text(label, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 3: Garments & Measurements Selection
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✂️ পোশাকের মাপসমূহ (${garmentsList.size}টি)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )

                                Text(
                                    text = "নিচে পোশাক যোগ করুন ⬇️",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fast Add Garment Chip Bar
                            Text(
                                text = "পোশাক নির্বাচন করুন:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val garmentButtons = listOf(
                                    GarmentType.KAMIZ to "👗 কামিজ",
                                    GarmentType.SALWAR to "👖 সেলোয়ার",
                                    GarmentType.FROCK to "👧 বাচ্চাদের ফ্রক",
                                    GarmentType.PETTICOAT to "🥻 পেটিকোট",
                                    GarmentType.BLOUSE to "👚 ব্লাউজ",
                                    GarmentType.PANJABI to "👔 পাঞ্জাবি",
                                    GarmentType.SHIRT to "👕 শার্ট",
                                    GarmentType.PANT to "👖 প্যান্ট",
                                    GarmentType.MAXI to "👗 ম্যাক্সি",
                                    GarmentType.BURQA to "🧕 বোরকা",
                                    GarmentType.OTHER to "✂️ অন্যান্য"
                                )

                                items(garmentButtons) { (gType, label) ->
                                    FilledTonalButton(
                                        onClick = {
                                            val defaultRate = when (gType) {
                                                GarmentType.KAMIZ -> 250.0
                                                GarmentType.SALWAR -> 150.0
                                                GarmentType.FROCK -> 200.0
                                                GarmentType.PETTICOAT -> 100.0
                                                GarmentType.BLOUSE -> 180.0
                                                GarmentType.PANJABI -> 300.0
                                                GarmentType.SHIRT -> 250.0
                                                GarmentType.PANT -> 300.0
                                                GarmentType.MAXI -> 180.0
                                                GarmentType.BURQA -> 350.0
                                                GarmentType.OTHER -> 150.0
                                            }
                                            garmentsList = garmentsList + GarmentMeasurementItem(
                                                garmentType = gType.name,
                                                garmentName = gType.bnName,
                                                quantity = 1,
                                                stitchingRate = defaultRate,
                                                measurements = mutableMapOf()
                                            )
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                                            contentColor = EmeraldPrimary
                                        )
                                    ) {
                                        Text("+ $label", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // List of added garment cards
                            if (garmentsList.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "কোনো পোশাক যোগ করা হয়নি। উপরের বাটনগুলো থেকে কামিজ, সেলোয়ার, ফ্রক ইত্যাদি নির্বাচন করুন।",
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                garmentsList.forEachIndexed { index, garment ->
                                    GarmentMeasurementEditorCard(
                                        index = index,
                                        garment = garment,
                                        currency = currency,
                                        onUpdate = { updated ->
                                            val newList = garmentsList.toMutableList()
                                            newList[index] = updated
                                            garmentsList = newList
                                        },
                                        onDelete = {
                                            val newList = garmentsList.toMutableList()
                                            newList.removeAt(index)
                                            garmentsList = newList
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }

                    // SECTION 4: Design & Fabric Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🎨 কাপড়ের ধরন ও বিশেষ ডিজাইন",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )

                            // Quick Fabric Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("কাস্টমারের কাপড়", "দোকানের কাপড়", "সুতি", "জর্জেট", "সিল্ক").forEach { f ->
                                    FilterChip(
                                        selected = fabricType == f,
                                        onClick = { fabricType = f },
                                        label = { Text(f, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = designNotes,
                                onValueChange = { designNotes = it },
                                label = { Text("ডিজাইন / গলার স্টাইল / অতিরিক্ত নোট") },
                                placeholder = { Text("উদাঃ পান গলা, হাতায় পাইপিং, পেছনের চেইন, বুক কুচি ইত্যাদি") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // SECTION 5: Billing & Advance Payment
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "💵 সেলাই বিল ও পেমেন্ট",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualTotalAmountText,
                                    onValueChange = { manualTotalAmountText = it },
                                    label = { Text("মোট বিল ($currency) *") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                OutlinedTextField(
                                    value = advancePaidText,
                                    onValueChange = { advancePaidText = it },
                                    label = { Text("অগ্রিম জমা ($currency)") },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            // Due Amount Display
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = if (finalDue > 0) LossRed.copy(alpha = 0.1f) else EmeraldPrimary.copy(alpha = 0.1f)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (finalDue > 0) "বাকি টাকা থাকবে:" else "সব টাকা পরিশোধ:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (finalDue > 0) LossRed else EmeraldPrimary
                                    )
                                    Text(
                                        text = "$currency${finalDue.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (finalDue > 0) LossRed else EmeraldPrimary
                                    )
                                }
                            }

                            // Payment Method
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("CASH" to "ক্যাশ", "BKASH" to "বিকাশ", "NAGAD" to "নগদ").forEach { (method, label) ->
                                    FilterChip(
                                        selected = paymentMethod == method,
                                        onClick = { paymentMethod = method },
                                        label = { Text(label, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Bottom Save Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("বাতিল")
                    }

                    Button(
                        onClick = {
                            if (customerName.isBlank()) {
                                return@Button
                            }

                            val garmentsJson = JSONArray().apply {
                                garmentsList.forEach { put(it.toJson()) }
                            }.toString()

                            val orderToSave = TailorOrder(
                                id = initialOrder?.id ?: 0L,
                                orderNumber = orderNumber.ifBlank { "T-${System.currentTimeMillis() % 10000}" },
                                customerName = customerName.trim(),
                                customerPhone = customerPhone.trim(),
                                customerAddress = customerAddress.trim(),
                                orderDate = orderDate,
                                deliveryDate = deliveryDate,
                                fabricType = fabricType,
                                totalAmount = finalTotal,
                                advancePaid = finalAdvance,
                                dueAmount = finalDue,
                                status = initialOrder?.status ?: "RECEIVED",
                                paymentMethod = paymentMethod,
                                designNotes = designNotes.trim(),
                                garmentsJson = garmentsJson,
                                createdAt = initialOrder?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(orderToSave)
                        },
                        enabled = customerName.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isEdit) "পরিবর্তন সংরক্ষণ" else "অর্ডার সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Individual Garment Measurement Editor Card
 */
@Composable
private fun GarmentMeasurementEditorCard(
    index: Int,
    garment: GarmentMeasurementItem,
    currency: String,
    onUpdate: (GarmentMeasurementItem) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val gType = GarmentType.fromId(garment.garmentType)
    val templateFields = remember(gType) {
        TailorMeasurementTemplates.getFieldsForGarmentType(gType)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Card Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { expanded = !expanded }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = garment.garmentName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = LossRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Quantity & Stitching Rate row
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (garment.quantity > 0) garment.quantity.toString() else "",
                    onValueChange = {
                        val q = it.toIntOrNull() ?: 1
                        onUpdate(garment.copy(quantity = q))
                    },
                    label = { Text("পিস সংখ্যা") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = if (garment.stitchingRate > 0) {
                        if (garment.stitchingRate % 1.0 == 0.0) garment.stitchingRate.toInt().toString() else garment.stitchingRate.toString()
                    } else "",
                    onValueChange = {
                        val rate = it.toDoubleOrNull() ?: 0.0
                        onUpdate(garment.copy(stitchingRate = rate))
                    },
                    label = { Text("সেলাই দর ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        text = "📐 জামার মাপের ঘরসমূহ (ইঞ্চিতে লিখুন):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2-column Grid of Measurement Inputs
                    val fieldRows = templateFields.chunked(2)
                    fieldRows.forEach { rowFields ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowFields.forEach { field ->
                                val curVal = garment.measurements[field.key] ?: ""
                                OutlinedTextField(
                                    value = curVal,
                                    onValueChange = { newVal ->
                                        val newMap = garment.measurements.toMutableMap()
                                        if (newVal.isBlank()) {
                                            newMap.remove(field.key)
                                        } else {
                                            newMap[field.key] = newVal
                                        }
                                        onUpdate(garment.copy(measurements = newMap))
                                    },
                                    label = { Text(field.bnLabel, fontSize = 12.sp) },
                                    placeholder = { Text(field.hint, fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            if (rowFields.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = garment.notes,
                        onValueChange = { onUpdate(garment.copy(notes = it)) },
                        label = { Text("এই পোশাকের বিশেষ নোট (ঐচ্ছিক)") },
                        placeholder = { Text("উদাঃ হাতা পাইপিং, চেইন পেছনে, কুচি ৩ ইঞ্চি ইত্যাদি") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}
