package com.bulpara.paywall.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GlassWhite = Color.White.copy(alpha = 0.15f)
private val GlassWhiteSelected = Color.White.copy(alpha = 0.25f)

@Composable
internal fun TierCard(
    name: String,
    badge: String?,
    price: String,
    period: String,
    creditsLabel: String?,
    benefits: List<String>,
    isSelected: Boolean,
    isCurrentPlan: Boolean,
    onClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(CornerRadius.lg)
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) GlassWhiteSelected else GlassWhite,
        label = "tier_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) accentColor else Color.White.copy(alpha = 0.2f),
        label = "tier_border",
    )

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bgColor)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = shape,
                )
                .clickable(onClick = onClick)
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            val priceFontSize = when {
                price.length > 12 -> 16.sp
                price.length > 8 -> 20.sp
                else -> 24.sp
            }
            Text(
                text = price,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = priceFontSize,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = period,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )

            if (creditsLabel != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = creditsLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                )
            }

            if (benefits.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
                ) {
                    benefits.forEach { benefit ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = accentColor,
                            )
                            Spacer(modifier = Modifier.width(Spacing.xxs))
                            Text(
                                text = benefit,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                            )
                        }
                    }
                }
            }
        }

        val displayBadge = when {
            isCurrentPlan -> "Current Plan"
            badge != null -> badge
            else -> null
        }
        if (displayBadge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .clip(RoundedCornerShape(CornerRadius.full))
                    .background(accentColor)
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
            ) {
                Text(
                    text = displayBadge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            }
        }
    }
}
