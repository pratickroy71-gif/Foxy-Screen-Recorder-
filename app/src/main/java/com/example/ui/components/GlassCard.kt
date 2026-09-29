package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.FoxyBorder
import com.example.ui.theme.FoxySurface
import com.example.ui.theme.FoxySurfaceVariant

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    border: BorderStroke? = BorderStroke(1.dp, FoxyBorder),
    backgroundColor: Color? = null,
    gradientColors: List<Color> = listOf(FoxySurface, FoxySurfaceVariant),
    contentPadding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickableModifier = if (onClick != null) {
        modifier
            .clip(shape)
            .clickable(onClick = onClick)
    } else {
        modifier.clip(shape)
    }

    Surface(
        modifier = clickableModifier,
        shape = shape,
        border = border,
        color = Color.Transparent,
        tonalElevation = 2.dp
    ) {
        val bgModifier = if (backgroundColor != null) {
            Modifier.background(backgroundColor)
        } else {
            Modifier.background(
                Brush.linearGradient(colors = gradientColors)
            )
        }

        Box(
            modifier = Modifier
                .then(bgModifier)
                .padding(contentPadding),
            content = content
        )
    }
}
