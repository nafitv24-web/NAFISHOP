package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.ui.theme.*

const val NAFI_SHOP_OFFICIAL_LOGO_URL = "https://i.supaimg.com/83afe636-c743-4025-87ea-6f9f3f10d71f/15f12bae-0b0e-42d2-b8de-c83e8914e3fc.png"

/**
 * Official App Logo Badge (for Login Screen, Splash, Profiles)
 */
@Composable
fun NafiShopLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Surface(
        shape = RoundedCornerShape(size * 0.22f),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFFFDE68A),
                    Color(0xFFD97706),
                    Color(0xFFF59E0B)
                )
            )
        ),
        modifier = modifier.size(size)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(NAFI_SHOP_OFFICIAL_LOGO_URL)
                .error(R.drawable.app_logo)
                .placeholder(R.drawable.app_logo)
                .crossfade(true)
                .build(),
            contentDescription = "NAFI SHOP 24 Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size * 0.22f))
        )
    }
}

/**
 * Full Brand Header Card with Logo Banner and Typography
 * "NAFI SHOP 24 / নাফি শপ ২৪ / দোকানদারের হিসাবের বিশ্বস্ত সঙ্গী"
 */
@Composable
fun NafiShopFullBrandCard(
    modifier: Modifier = Modifier,
    tagline: String = "দোকানদারের হিসাবের বিশ্বস্ত সঙ্গী",
    showTagline: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upper Brand Graphic Logo Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3B0764),
                                Color(0xFF2E1065),
                                Color(0xFF1E0438)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFDE68A),
                                Color(0xFFD97706),
                                Color(0xFFFDE68A)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(NAFI_SHOP_OFFICIAL_LOGO_URL)
                        .error(R.drawable.app_logo)
                        .placeholder(R.drawable.app_logo)
                        .crossfade(true)
                        .build(),
                    contentDescription = "NAFI SHOP 24 Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lower Brand Identity: Logo Icon + Typography
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3B0764),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
                    modifier = Modifier.size(36.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(NAFI_SHOP_OFFICIAL_LOGO_URL)
                            .error(R.drawable.app_logo)
                            .placeholder(R.drawable.app_logo)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Logo Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "NAFI SHOP 24",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "নাফি শপ ২৪",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AmberTertiary
                    )
                }
            }

            if (showTagline) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Small inline logo for TopAppBar & Row headers
 */
@Composable
fun NafiShopSmallLogo(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    Surface(
        shape = RoundedCornerShape(size * 0.22f),
        color = Color(0xFF3B0764),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
        modifier = modifier.size(size)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(NAFI_SHOP_OFFICIAL_LOGO_URL)
                .error(R.drawable.app_logo)
                .placeholder(R.drawable.app_logo)
                .crossfade(true)
                .build(),
            contentDescription = "NAFI SHOP 24 Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size * 0.22f))
        )
    }
}
