package com.cardpulse.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardpulse.app.model.BankPreset
import com.cardpulse.app.ui.theme.AmberWarn
import com.cardpulse.app.ui.theme.EmeraldOk
import com.cardpulse.app.ui.theme.NeonCyan
import com.cardpulse.app.ui.theme.RoseUrgent
import java.io.File

@Composable
fun CreditCardVisual(
    bankPreset: BankPreset,
    bankName: String,
    cardLabel: String,
    last4: String,
    dueDateText: String,
    daysLeft: Long?,
    customImagePath: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val customBitmap = remember(customImagePath) {
        if (!customImagePath.isNullOrBlank() && File(customImagePath).exists()) {
            runCatching { BitmapFactory.decodeFile(customImagePath)?.asImageBitmap() }.getOrNull()
        } else {
            null
        }
    }

    val (badgeText, badgeColor) = when {
        daysLeft == null -> dueDateText to NeonCyan
        daysLeft < 0L -> "OVERDUE ${-daysLeft}D" to RoseUrgent
        daysLeft == 0L -> "DUE TODAY" to RoseUrgent
        daysLeft <= 2L -> "${daysLeft}D • URGENT" to AmberWarn
        daysLeft <= 5L -> "${daysLeft}D • SOON" to NeonCyan
        else -> "IN ${daysLeft} DAYS" to EmeraldOk
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.586f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = bankPreset.gradientColors,
                    start = Offset(0f, 0f),
                    end = Offset(900f, 600f)
                )
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = bankName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
            )
        }

        // Subtle holographic diagonal sheen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.08f),
                            NeonCyan.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1100f, 700f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Bank Name & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = bankName.ifBlank { bankPreset.displayName },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = cardLabel.ifBlank { bankPreset.defaultTag }.uppercase(),
                        color = Color.White.copy(alpha = 0.78f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .border(1.dp, badgeColor.copy(alpha = 0.65f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // Middle Row: Gold EMV Chip + Contactless Waves
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmvChipGraphic()
                ContactlessWavesGraphic()
            }

            // Bottom Row: Masked Number + Due Date + Network
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "••••  ••••  ••••  ${last4.ifBlank { "8888" }}",
                        color = Color.White.copy(alpha = 0.94f),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Due $dueDateText",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Text(
                    text = bankPreset.network,
                    color = Color.White.copy(alpha = 0.92f),
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun EmvChipGraphic() {
    Canvas(modifier = Modifier.size(width = 40.dp, height = 28.dp)) {
        val brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFFDE68A),
                Color(0xFFD97706),
                Color(0xFFFEF3C7),
                Color(0xFFB45309)
            )
        )
        drawRoundRect(
            brush = brush,
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        val lineColor = Color.Black.copy(alpha = 0.3f)
        val stroke = 1.dp.toPx()
        drawLine(lineColor, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), stroke)
        drawLine(lineColor, Offset(size.width * 0.34f, 0f), Offset(size.width * 0.34f, size.height), stroke)
        drawLine(lineColor, Offset(size.width * 0.66f, 0f), Offset(size.width * 0.66f, size.height), stroke)
    }
}

@Composable
private fun ContactlessWavesGraphic() {
    Canvas(modifier = Modifier.size(20.dp)) {
        val color = Color.White.copy(alpha = 0.72f)
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        listOf(0.32f, 0.56f, 0.80f).forEach { factor ->
            val diameter = size.width * factor
            val topLeft = Offset(
                x = (size.width - diameter) / 2f - 4.dp.toPx(),
                y = (size.height - diameter) / 2f
            )
            drawArc(
                color = color,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = stroke
            )
        }
    }
}
