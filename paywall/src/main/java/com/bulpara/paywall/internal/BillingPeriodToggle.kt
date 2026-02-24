package com.bulpara.paywall.internal

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bulpara.paywall.BillingPeriod

private val ToggleShape = RoundedCornerShape(CornerRadius.full)
private val SegmentShape = RoundedCornerShape(CornerRadius.full)
private val GlassWhite = Color.White.copy(alpha = 0.15f)
private val SelectedBg = Color.White.copy(alpha = 0.25f)

@Composable
internal fun BillingPeriodToggle(
    selected: BillingPeriod,
    savingsPercent: Int?,
    onSelect: (BillingPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(ToggleShape)
            .background(GlassWhite)
            .border(1.dp, Color.White.copy(alpha = 0.2f), ToggleShape)
            .padding(Spacing.xxs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleSegment(
            text = "Monthly",
            isSelected = selected == BillingPeriod.MONTHLY,
            onClick = { onSelect(BillingPeriod.MONTHLY) },
        )
        ToggleSegment(
            text = buildString {
                append("Annual")
                if (savingsPercent != null && savingsPercent > 0) {
                    append(" (-$savingsPercent%)")
                }
            },
            isSelected = selected == BillingPeriod.ANNUAL,
            onClick = { onSelect(BillingPeriod.ANNUAL) },
        )
    }
}

@Composable
private fun ToggleSegment(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SelectedBg else Color.Transparent,
        label = "toggle_bg",
    )

    Box(
        modifier = Modifier
            .clip(SegmentShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
        )
    }
}
