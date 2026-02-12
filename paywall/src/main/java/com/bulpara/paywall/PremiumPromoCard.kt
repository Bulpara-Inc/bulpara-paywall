package com.bulpara.paywall

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class PremiumPromoConfig(
    val title: String,
    val gradientColors: List<Color>,
    val icon: ImageVector = Icons.Filled.Star,
)

@Composable
fun PremiumPromoCard(
    billingManager: BillingManager,
    config: PremiumPromoConfig,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPremium by billingManager.isPremium.collectAsState()
    if (isPremium) return

    val products by billingManager.products.collectAsState()
    val annualProduct = products.find { it.productId == billingManager.productIds.annual }

    val subtitle = when {
        annualProduct == null -> null
        billingManager.hasFreeTrial(annualProduct) -> {
            val trial = billingManager.getTrialPeriod(annualProduct)
            val price = billingManager.getFormattedPrice(annualProduct)
            "$trial free trial \u2022 $price/year"
        }
        else -> {
            val price = billingManager.getFormattedPrice(annualProduct)
            "$price/year"
        }
    }

    val gradient = Brush.horizontalGradient(config.gradientColors)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = config.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color.White,
                    )
                    Text(
                        text = config.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                } else {
                    ShimmerPlaceholder()
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View plans",
                modifier = Modifier.size(28.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun ShimmerPlaceholder() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val offsetX by transition.animateFloat(
        initialValue = -200f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(14.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.15f),
                    ),
                    start = Offset(offsetX, 0f),
                    end = Offset(offsetX + 200f, 0f),
                ),
            ),
    )
}
