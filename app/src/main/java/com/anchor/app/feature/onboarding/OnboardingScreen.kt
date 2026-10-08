package com.anchor.app.feature.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.anchor.app.R
import com.anchor.app.core.design.components.Banner
import com.anchor.app.core.design.components.PrimaryButton
import com.anchor.app.core.design.components.ProgressTrack
import com.anchor.app.core.design.components.TextPill
import com.anchor.app.core.design.theme.AnchorColors
import com.anchor.app.core.design.theme.InterFamily
import com.anchor.app.core.design.theme.Radius
import com.anchor.app.core.design.theme.Spacing
import com.anchor.app.platform.usagestats.UsageAccessSettings

/**
 * 00 Splash & Onboarding (docs/superpowers/design/07-screens.md §2.3).
 *
 * Logo, two-line title, calm subtitle, and a soft 3D "Get started" CTA. No permissions
 * are requested here (PRD §10.10: onboarding never forces).
 */
@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(vertical = Spacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AnchorColors.textMutedAlt,
                modifier = Modifier.clickable(onClick = onSkip),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Logo avatar stack
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(AnchorColors.ink),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = AnchorColors.surface,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x8))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            Text(
                text = stringResource(R.string.onboarding_title_line1),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = AnchorColors.textPrimary,
            )
            Text(
                text = stringResource(R.string.onboarding_title_line2),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = AnchorColors.textPrimary,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.x4))

        Text(
            text = stringResource(R.string.onboarding_subtitle),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = AnchorColors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            label = stringResource(R.string.onboarding_get_started),
            onClick = onGetStarted,
        )
    }
}

/**
 * 00b Permission Onboarding (docs 07-screens.md §2.4).
 *
 * Step indicator, progress track, two optional permission cards, a local-only privacy
 * card, and Continue / Skip. Permissions are never forced; both cards are informational
 * in Sprint 1 and get live status wiring alongside Wellbeing (Sprint 2).
 */
@Composable
fun PermissionOnboardingScreen(
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var notificationsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU,
        )
    }
    var usageGranted by remember { mutableStateOf(false) }

    // Usage access is granted only in OS Settings, so re-check when we return (docs 08 §2).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageGranted = usageAccessGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Ask for notifications here (the "enable notifications" step most apps use first); if the
    // user declines, guide them to the notification settings screen.
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        notificationsGranted = granted
        if (!granted) openNotificationSettings(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorColors.surface)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal)
            .padding(vertical = Spacing.x6),
        verticalArrangement = Arrangement.spacedBy(Spacing.x4),
    ) {
        Text(
            text = stringResource(R.string.onboarding_perm_step, 1, 2),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = AnchorColors.textMutedAlt,
        )
        ProgressTrack(progress = 0.5f)

        Text(
            text = stringResource(R.string.onboarding_perm_headline),
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = AnchorColors.textPrimary,
        )
        Text(
            text = stringResource(R.string.onboarding_perm_sub),
            fontFamily = InterFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = AnchorColors.textSecondary,
        )

        PermissionCard(
            title = stringResource(R.string.onboarding_perm_usage_title),
            subtitle = stringResource(R.string.onboarding_perm_usage_sub),
            granted = usageGranted,
            onEnable = { UsageAccessSettings.open(context) },
        )
        PermissionCard(
            title = stringResource(R.string.onboarding_perm_notif_title),
            subtitle = stringResource(R.string.onboarding_perm_notif_sub),
            granted = notificationsGranted,
            onEnable = { requestNotifications(notifLauncher, context) },
        )

        Banner {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = AnchorColors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(Spacing.x2))
                Text(
                    text = stringResource(R.string.onboarding_privacy_card),
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = AnchorColors.textSecondary,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(label = stringResource(R.string.onboarding_continue), onClick = onContinue)
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.onboarding_skip_for_now),
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = AnchorColors.textMuted,
                modifier = Modifier.clickable(onClick = onSkip),
            )
        }
    }
}

/** Reads the PACKAGE_USAGE_STATS AppOps state (special access, granted in Settings). */
private fun usageAccessGranted(context: Context): Boolean = runCatching {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    @Suppress("DEPRECATION")
    val mode = appOps.checkOpNoThrow("android:get_usage_stats", Process.myUid(), context.packageName)
    mode == AppOpsManager.MODE_ALLOWED
}.getOrDefault(false)

private fun requestNotifications(
    launcher: androidx.activity.result.ActivityResultLauncher<String>,
    context: Context,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        openNotificationSettings(context)
    }
}

private fun openNotificationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    granted: Boolean,
    onEnable: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .border(1.dp, AnchorColors.border, RoundedCornerShape(Radius.card))
            .background(AnchorColors.surfaceCard)
            .padding(Spacing.cardHorizontal),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            Text(text = title, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = AnchorColors.textPrimary)
            Text(text = subtitle, fontFamily = InterFamily, fontSize = 12.sp, color = AnchorColors.textMuted)
        }
        Spacer(modifier = Modifier.width(Spacing.x2))
        if (granted) {
            TextPill(text = stringResource(R.string.onboarding_perm_enabled), highlighted = true)
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.chip))
                    .background(AnchorColors.ink)
                    .clickable(onClick = onEnable)
                    .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_perm_enable),
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = AnchorColors.surface,
                )
            }
        }
    }
}
