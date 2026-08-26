package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * High-fidelity representation of the "NAFI SHOP 24 / নাফি শপ ২৪" Logo
 * Featuring the purple ledger notebook, golden pen, calculator medallion badge,
 * growth chart, and stylized open book branding.
 */
@Composable
fun NafiShopLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Card(
        shape = RoundedCornerShape(size * 0.22f),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3B0764),
                            Color(0xFF2E1065),
                            Color(0xFF1E0438)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.08f)) {
                drawNafiShopIcon(this)
            }
        }
    }
}

/**
 * Full Brand Header Card with Logo and Typography
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Upper Graphic Canvas (Ledger + Pen + Calculator + Growth Chart)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF4C1D95),
                                Color(0xFF3B0764),
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
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    drawNafiShopFullGraphic(this)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lower Brand Identity: Open Book Icon + Typography
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Stylized Open Book Icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF3B0764),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = "Logo Icon",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "NAFI SHOP 24",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF1E1B4B),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "নাফি শপ ২৪",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706)
                    )
                }
            }

            if (showTagline) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF475569),
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
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF3B0764),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706)),
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                drawNafiShopIcon(this)
            }
        }
    }
}

private fun drawNafiShopIcon(scope: DrawScope) {
    with(scope) {
        val w = size.width
        val h = size.height

        // White Notepad Body
        val sheetLeft = w * 0.15f
        val sheetTop = h * 0.15f
        val sheetW = w * 0.70f
        val sheetH = h * 0.70f

        // Shadow & Notepad
        drawRoundRect(
            color = Color(0x30000000),
            topLeft = Offset(sheetLeft + w * 0.03f, sheetTop + h * 0.03f),
            size = Size(sheetW, sheetH),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawRoundRect(
            color = Color(0xFFFDFBF7),
            topLeft = Offset(sheetLeft, sheetTop),
            size = Size(sheetW, sheetH),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )

        // Ruled lines
        val lineStroke = Stroke(width = w * 0.02f, cap = StrokeCap.Round)
        val lineCount = 4
        for (i in 1..lineCount) {
            val y = sheetTop + (sheetH / (lineCount + 1)) * i
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(sheetLeft + sheetW * 0.18f, y),
                end = Offset(sheetLeft + sheetW * 0.85f, y),
                strokeWidth = lineStroke.width
            )
        }

        // Calculator Badge in bottom left
        val calcRadius = w * 0.20f
        val calcCenter = Offset(w * 0.30f, h * 0.70f)
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = calcRadius,
            center = calcCenter
        )
        drawCircle(
            color = Color(0xFF3B0764),
            radius = calcRadius * 0.88f,
            center = calcCenter
        )
        // Calculator screen
        drawRoundRect(
            color = Color(0xFFFDE68A),
            topLeft = Offset(calcCenter.x - calcRadius * 0.55f, calcCenter.y - calcRadius * 0.55f),
            size = Size(calcRadius * 1.1f, calcRadius * 0.35f),
            cornerRadius = CornerRadius(2f, 2f)
        )

        // Pen on right
        val penStart = Offset(w * 0.82f, h * 0.20f)
        val penEnd = Offset(w * 0.58f, h * 0.65f)
        drawLine(
            color = Color(0xFFF59E0B),
            start = penStart,
            end = penEnd,
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF4C1D95),
            start = penStart,
            end = Offset(w * 0.65f, h * 0.53f),
            strokeWidth = w * 0.05f,
            cap = StrokeCap.Round
        )
    }
}

private fun drawNafiShopFullGraphic(scope: DrawScope) {
    with(scope) {
        val w = size.width
        val h = size.height

        // 1. Center Notepad
        val padW = w * 0.52f
        val padH = h * 0.82f
        val padX = (w - padW) / 2
        val padY = (h - padH) / 2

        // Notepad Shadow & Base
        drawRoundRect(
            color = Color(0x40000000),
            topLeft = Offset(padX + 4, padY + 4),
            size = Size(padW, padH),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = Color(0xFFFAF5FF),
            topLeft = Offset(padX, padY),
            size = Size(padW, padH),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Notepad Grid Table Header
        drawRoundRect(
            color = Color(0xFFF3E8FF),
            topLeft = Offset(padX + 6, padY + 6),
            size = Size(padW - 12, padH * 0.22f),
            cornerRadius = CornerRadius(4f, 4f)
        )

        // Ruled grid lines inside notebook
        val cols = 4
        val colStep = (padW - 12) / cols
        for (c in 1 until cols) {
            drawLine(
                color = Color(0xFFD8B4FE),
                start = Offset(padX + 6 + colStep * c, padY + 6),
                end = Offset(padX + 6 + colStep * c, padY + padH - 8),
                strokeWidth = 1f
            )
        }
        val rows = 5
        val rowStep = (padH - 8 - padH * 0.22f) / rows
        for (r in 1..rows) {
            val y = padY + 6 + padH * 0.22f + rowStep * (r - 1)
            drawLine(
                color = Color(0xFFE9D5FF),
                start = Offset(padX + 6, y),
                end = Offset(padX + padW - 6, y),
                strokeWidth = 1f
            )
        }

        // 2. Calculator Medallion (Bottom Left)
        val calcRadius = h * 0.26f
        val calcCenter = Offset(w * 0.22f, h * 0.65f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFD97706)),
                center = calcCenter,
                radius = calcRadius
            ),
            radius = calcRadius,
            center = calcCenter
        )
        drawCircle(
            color = Color(0xFF3B0764),
            radius = calcRadius * 0.88f,
            center = calcCenter
        )
        // Mini calc screen & buttons
        drawRoundRect(
            color = Color(0xFFFDE68A),
            topLeft = Offset(calcCenter.x - calcRadius * 0.55f, calcCenter.y - calcRadius * 0.60f),
            size = Size(calcRadius * 1.1f, calcRadius * 0.35f),
            cornerRadius = CornerRadius(2f, 2f)
        )
        // + - x = markers
        drawCircle(color = Color.White, radius = 2f, center = Offset(calcCenter.x - calcRadius * 0.3f, calcCenter.y + calcRadius * 0.05f))
        drawCircle(color = Color.White, radius = 2f, center = Offset(calcCenter.x + calcRadius * 0.3f, calcCenter.y + calcRadius * 0.05f))
        drawCircle(color = Color.White, radius = 2f, center = Offset(calcCenter.x - calcRadius * 0.3f, calcCenter.y + calcRadius * 0.45f))
        drawCircle(color = Color(0xFFF59E0B), radius = 2f, center = Offset(calcCenter.x + calcRadius * 0.3f, calcCenter.y + calcRadius * 0.45f))

        // 3. Upward Profit Growth Bars (Bottom Right)
        val barBaseX = w * 0.72f
        val barBaseY = h * 0.82f
        val barW = w * 0.035f
        val heights = listOf(h * 0.16f, h * 0.24f, h * 0.32f)
        val colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFDE68A))

        heights.forEachIndexed { index, bHeight ->
            drawRoundRect(
                color = colors[index],
                topLeft = Offset(barBaseX + index * (barW + 5), barBaseY - bHeight),
                size = Size(barW, bHeight),
                cornerRadius = CornerRadius(2f, 2f)
            )
        }
        // Trend arrow curve
        val arrowPath = Path().apply {
            moveTo(barBaseX - 8, barBaseY - 4)
            quadraticBezierTo(barBaseX + 15, barBaseY - 20, barBaseX + 38, barBaseY - h * 0.36f)
        }
        drawPath(
            path = arrowPath,
            color = Color(0xFFFDE68A),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // 4. Sleek Fountain Pen (Diagonal across ledger)
        val penLength = h * 0.70f
        val penStartX = w * 0.72f
        val penStartY = h * 0.18f
        val penEndX = w * 0.56f
        val penEndY = h * 0.65f

        // Pen body
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF4C1D95), Color(0xFF6D28D9), Color(0xFF3B0764))
            ),
            start = Offset(penStartX, penStartY),
            end = Offset(penEndX + 8, penEndY - 8),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        // Pen Gold Trim Rings
        drawLine(
            color = Color(0xFFFDE68A),
            start = Offset(penStartX - 6, penStartY + 6),
            end = Offset(penStartX - 2, penStartY + 10),
            strokeWidth = 7f
        )
        // Pen Gold Nib
        drawLine(
            color = Color(0xFFF59E0B),
            start = Offset(penEndX + 8, penEndY - 8),
            end = Offset(penEndX, penEndY),
            strokeWidth = 4f,
            cap = StrokeCap.Round
        )
    }
}
