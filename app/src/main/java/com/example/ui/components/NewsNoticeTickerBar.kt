package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppNotice
import com.example.ui.theme.*

/**
 * Top News & Notice Ticker Bar (সবার উপরে নিউজ নোটিশ ব্যানার)
 * Displays breaking news / notices published by the Admin in a dynamic rolling ticker format.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewsNoticeTickerBar(
    activeNotice: AppNotice?,
    language: String = "bn",
    onNoticeClick: (AppNotice) -> Unit,
    onOpenAdminPanel: (() -> Unit)? = null
) {
    // Blinking / Pulsing animation for the Live badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveAlpha"
    )

    val hasActiveNotice = activeNotice != null && activeNotice.isActive

    val (badgeBg, badgeText, badgeIcon, barBg) = when (activeNotice?.type) {
        "ALERT" -> Quad(
            Color(0xFFDC2626),
            if (language == "bn") "জরুরী সতর্কবার্তা" else "ALERT",
            Icons.Default.Warning,
            Color(0xFFFEF2F2)
        )
        "OFFER" -> Quad(
            Color(0xFFD97706),
            if (language == "bn") "অফার ও ছাড়" else "OFFER",
            Icons.Default.LocalOffer,
            Color(0xFFFFFBEB)
        )
        "FEATURE" -> Quad(
            Color(0xFF4F46E5),
            if (language == "bn") "নতুন ফিচার" else "UPDATE",
            Icons.Default.Star,
            Color(0xFFEEF2FF)
        )
        else -> Quad(
            Color(0xFFE11D48),
            if (language == "bn") "নিউজ নোটিশ" else "NEWS",
            Icons.Default.Campaign,
            Color(0xFFFFF1F2)
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (activeNotice != null) {
                    onNoticeClick(activeNotice)
                } else if (onOpenAdminPanel != null) {
                    onOpenAdminPanel()
                }
            },
        color = barBg,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Live / News Badge with blinking indicator
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeBg,
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = alphaAnim))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = badgeText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Scrolling Marquee News Text
            val newsText = if (hasActiveNotice) {
                "${activeNotice!!.title} : ${activeNotice.message}"
            } else {
                if (language == "bn") {
                    "সৌজন্যে নাফি এন্ড নাজমুল টেলিকম • আপনার ব্যবসার সকল হিসাব এক ক্লিকে রাখুন • https://nfiptvserver.blogspot.com/"
                } else {
                    "Powered by Nafi & Nazmul Telecom • Complete business khata management"
                }
            }

            Text(
                text = newsText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        velocity = 40.dp
                    )
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Tap to view / open button
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = badgeBg.copy(alpha = 0.12f),
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == "bn") "দেখুন" else "View",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeBg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "View",
                        tint = badgeBg,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
