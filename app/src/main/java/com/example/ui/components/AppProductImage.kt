package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.ui.theme.EmeraldPrimary
import com.example.util.ImageStorageHelper

/**
 * Universal, highly resilient Product Image loader that supports:
 * - Local files (with automatic package path remapping and internal storage resolution)
 * - Base64 data URLs & raw Base64 strings (decoded to ByteArray)
 * - Content URIs & Network URLs
 * - Graceful category-aware visual placeholder fallback with zero blank areas
 */
@Composable
fun AppProductImage(
    imageUri: String?,
    productName: String = "",
    category: String = "",
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    showZoomButton: Boolean = false,
    onZoomClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val resolvedModel = remember(imageUri) {
        ImageStorageHelper.resolveImageModel(context, imageUri)
    }

    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedModel != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(resolvedModel)
                    .crossfade(true)
                    .build(),
                contentDescription = productName.ifBlank { "পণ্যের ছবি" },
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = EmeraldPrimary
                        )
                    }
                },
                error = {
                    // Fallback to stylized category icon if the file was deleted or invalid
                    ProductCategoryPlaceholder(
                        productName = productName,
                        category = category
                    )
                },
                success = {
                    SubcomposeAsyncImageContent(modifier = Modifier.fillMaxSize())

                    if (showZoomButton && onZoomClick != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(5.dp)
                                .size(24.dp)
                                .clickable { onZoomClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.ZoomIn,
                                    contentDescription = "বড় করে দেখুন",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            )
        } else {
            // No image specified: show handsome category-based avatar
            ProductCategoryPlaceholder(
                productName = productName,
                category = category
            )
        }
    }
}

/**
 * Stylish visual placeholder for products without an image or when an image fails to load.
 */
@Composable
fun ProductCategoryPlaceholder(
    productName: String,
    category: String,
    modifier: Modifier = Modifier
) {
    val (icon, bgColors) = remember(productName, category) {
        getCategoryVisuals(productName, category)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = bgColors
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (productName.isNotBlank()) {
                val initial = productName.trim().firstOrNull()?.toString() ?: ""
                if (initial.isNotBlank()) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun getCategoryVisuals(productName: String, category: String): Pair<ImageVector, List<Color>> {
    val text = "$productName $category".lowercase()

    return when {
        text.contains("কাভার") || text.contains("cover") || text.contains("case") -> {
            Icons.Default.PhoneAndroid to listOf(Color(0xFF0F766E), Color(0xFF115E59))
        }
        text.contains("চার্জার") || text.contains("charger") || text.contains("adapter") -> {
            Icons.Default.Bolt to listOf(Color(0xFF0369A1), Color(0xFF075985))
        }
        text.contains("কেবল") || text.contains("cable") || text.contains("usb") || text.contains("data") -> {
            Icons.Default.Cable to listOf(Color(0xFF4338CA), Color(0xFF3730A3))
        }
        text.contains("গেরিলা") || text.contains("গ্লাস") || text.contains("glass") || text.contains("protector") -> {
            Icons.Default.Security to listOf(Color(0xFF6D28D9), Color(0xFF5B21B6))
        }
        text.contains("ব্যাটারি") || text.contains("battery") -> {
            Icons.Default.BatteryChargingFull to listOf(Color(0xFF047857), Color(0xFF065F46))
        }
        text.contains("হেডফোন") || text.contains("headphone") || text.contains("earphone") || text.contains("airpod") -> {
            Icons.Default.Headphones to listOf(Color(0xFFBE185D), Color(0xFF9D174D))
        }
        text.contains("ডিসপ্লে") || text.contains("display") || text.contains("lcd") || text.contains("touch") -> {
            Icons.Default.Smartphone to listOf(Color(0xFFB45309), Color(0xFF92400E))
        }
        else -> {
            Icons.Default.Inventory2 to listOf(Color(0xFF334155), Color(0xFF1E293B))
        }
    }
}
