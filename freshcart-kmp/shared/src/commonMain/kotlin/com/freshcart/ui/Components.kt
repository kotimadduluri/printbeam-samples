package com.freshcart.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The shared module deliberately has no icon-font dependency, so the few glyphs the app
// needs are drawn here — a magnifier and a cart — plus the badge shapes both screens reuse.

@Composable
fun MagnifierIcon(color: Color, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        val radius = size.minDimension * 0.32f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(color = color, radius = radius, center = center, style = stroke)
        val handleStart = center + Offset(radius * 0.72f, radius * 0.72f)
        drawLine(
            color = color,
            start = handleStart,
            end = Offset(size.width * 0.88f, size.height * 0.88f),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun CartIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.075f
        // Basket: handle stub, slanted body, two wheels.
        drawLine(color, Offset(w * 0.08f, h * 0.18f), Offset(w * 0.24f, h * 0.18f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.24f, h * 0.18f), Offset(w * 0.36f, h * 0.62f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.36f, h * 0.62f), Offset(w * 0.78f, h * 0.62f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.78f, h * 0.62f), Offset(w * 0.9f, h * 0.28f), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.9f, h * 0.28f), Offset(w * 0.3f, h * 0.28f), stroke, StrokeCap.Round)
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.42f, h * 0.82f))
        drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.72f, h * 0.82f))
    }
}

@Composable
fun PrinterIcon(color: Color, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = w * 0.075f
        val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        // Sheet feeding in on top, printer body, receipt coming out below.
        drawRect(
            color = color,
            topLeft = Offset(w * 0.3f, h * 0.1f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.24f),
            style = stroke,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.12f, h * 0.34f),
            size = androidx.compose.ui.geometry.Size(w * 0.76f, h * 0.32f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f),
            style = stroke,
        )
        drawRect(
            color = color,
            topLeft = Offset(w * 0.3f, h * 0.66f),
            size = androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.24f),
            style = stroke,
        )
    }
}

/** The solid accent circle used for connected/success confirmation moments. */
@Composable
fun CheckBadge(background: Color, tint: Color, size: Dp = 48.dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "✓",
            color = tint,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.5f).sp,
        )
    }
}

@Composable
fun EmptyBadge(glyph: String, size: Dp = 64.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
