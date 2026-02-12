package com.bulpara.paywall

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.bulpara.paywall.internal.IconSize
import com.bulpara.paywall.internal.PaywallPrimaryButton
import com.bulpara.paywall.internal.PaywallTextButton
import com.bulpara.paywall.internal.PlanCard
import com.bulpara.paywall.internal.Spacing
import kotlinx.coroutines.delay

@Composable
fun PaywallScreen(
    config: PaywallConfig,
    viewModel: PaywallViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    val showSuccess = uiState.isPremium && uiState.billingState is BillingState.PurchaseSuccess

    // Auto-dismiss after showing success screen
    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2500)
            onDismiss()
        }
    }

    val gradient = Brush.verticalGradient(config.branding.gradientColors)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        // Success overlay
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(animationSpec = tween(400)),
        ) {
            PurchaseSuccessContent(
                title = config.branding.title,
                planName = when (uiState.selectedPlan) {
                    PaywallPlan.ANNUAL -> "Annual Plan"
                    PaywallPlan.MONTHLY -> "Monthly Plan"
                },
                onDismiss = onDismiss,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Main paywall content (hidden when success is showing)
        AnimatedVisibility(
            visible = !showSuccess,
            exit = fadeOut(animationSpec = tween(300)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }

                    uiState.error != null && uiState.monthlyProduct == null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(Spacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = "Unable to load subscription info",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            PaywallPrimaryButton(
                                text = "Retry",
                                onClick = { viewModel.retryConnection() },
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            PaywallTextButton(
                                text = "Maybe Later",
                                onClick = onDismiss,
                            )
                        }
                    }

                    else -> {
                        PaywallContent(
                            config = config,
                            uiState = uiState,
                            onSelectPlan = viewModel::selectPlan,
                            onPurchase = {
                                (context as? Activity)?.let { activity ->
                                    viewModel.purchaseSelectedPlan(activity)
                                }
                            },
                            onDismiss = onDismiss,
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(Spacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaywallContent(
    config: PaywallConfig,
    uiState: PaywallUiState,
    onSelectPlan: (PaywallPlan) -> Unit,
    onPurchase: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .padding(top = 56.dp, bottom = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = config.branding.logoResId),
            contentDescription = null,
            modifier = Modifier.size(140.dp),
        )

        Text(
            text = config.branding.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            PlanCard(
                label = "Annual",
                price = uiState.annualPrice,
                period = "/year",
                badge = if (uiState.annualHasFreeTrial) "${uiState.trialPeriod} free" else null,
                subtitle = uiState.annualMonthlyEquivalent,
                isSelected = uiState.selectedPlan == PaywallPlan.ANNUAL,
                onClick = { onSelectPlan(PaywallPlan.ANNUAL) },
                accentColor = config.branding.accentColor,
                modifier = Modifier.weight(1f),
            )
            PlanCard(
                label = "Monthly",
                price = uiState.monthlyPrice,
                period = "/month",
                badge = null,
                subtitle = null,
                isSelected = uiState.selectedPlan == PaywallPlan.MONTHLY,
                onClick = { onSelectPlan(PaywallPlan.MONTHLY) },
                accentColor = config.branding.accentColor,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            config.benefits.forEach { benefit ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(IconSize.md),
                        tint = config.branding.accentColor,
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(
                        text = benefit,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xxl))

        PaywallPrimaryButton(
            text = uiState.ctaText,
            onClick = onPurchase,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Cancel anytime. Subscription auto-renews unless cancelled at least " +
                "24 hours before the end of the current period.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        PaywallTextButton(
            text = "Maybe Later",
            onClick = onDismiss,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Subscriptions on Google Play",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
        )

        if (config.termsUrl.isNotEmpty() || config.privacyUrl.isNotEmpty()) {
            val uriHandler = LocalUriHandler.current

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (config.termsUrl.isNotEmpty()) {
                    Text(
                        text = "Terms of Use",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { uriHandler.openUri(config.termsUrl) },
                    )
                }
                if (config.termsUrl.isNotEmpty() && config.privacyUrl.isNotEmpty()) {
                    Text(
                        text = " \u2022 ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                if (config.privacyUrl.isNotEmpty()) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { uriHandler.openUri(config.privacyUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseSuccessContent(
    title: String,
    planName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow,
                    ),
                ) + fadeIn(animationSpec = tween(300)),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.White,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(400, delayMillis = 200)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Welcome to $title!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = "Your $planName is active",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
