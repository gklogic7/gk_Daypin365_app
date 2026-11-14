package com.example.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.permissions.PermissionHelper
import com.example.permissions.PermissionStatusSnapshot

@Composable
fun PermissionOnboardingDialog(
    snapshot: PermissionStatusSnapshot,
    onRefreshPermissions: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    Dialog(
        onDismissRequest = onFinish,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PermissionActionRow(
                    icon = Icons.Filled.NotificationsActive,
                    title = "Alarm Notifications",
                    subtitle = "Show alarm alerts and Snooze/Dismiss actions.",
                    granted = snapshot.notificationsGranted,
                    testTag = "perm_notifications_btn",
                    onGrantClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onRefreshPermissions()
                        }
                    }
                )

                PermissionActionRow(
                    icon = Icons.Filled.Layers,
                    title = "Sticky Note Screen Overlay",
                    subtitle = "Float pinned sticky notes cleanly over other tablet apps.",
                    granted = snapshot.overlayGranted,
                    testTag = "perm_overlay_btn",
                    onGrantClick = {
                        context.startActivity(PermissionHelper.buildOverlaySettingsIntent(context))
                    }
                )

                PermissionActionRow(
                    icon = Icons.Filled.Alarm,
                    title = "Exact 365-Day Alarms",
                    subtitle = "Wake the device accurately at your scheduled minute.",
                    granted = snapshot.exactAlarmGranted,
                    testTag = "perm_exact_alarm_btn",
                    onGrantClick = {
                        context.startActivity(PermissionHelper.buildExactAlarmSettingsIntent(context))
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    PermissionActionRow(
                        icon = Icons.Filled.Fullscreen,
                        title = "Full-Screen Alarm Display",
                        subtitle = "Show photo, video, and music alarms over the lock screen.",
                        granted = snapshot.fullScreenIntentGranted,
                        testTag = "perm_fullscreen_btn",
                        onGrantClick = {
                            context.startActivity(PermissionHelper.buildFullScreenIntentSettingsIntent(context))
                        }
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onFinish,
                        modifier = Modifier.testTag("onboarding_skip_btn")
                    ) {
                        Text("Later (Settings)")
                    }
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.testTag("onboarding_done_btn")
                    ) {
                        Text(if (snapshot.allGranted) "All Set — Continue" else "Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    testTag: String,
    onGrantClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Granted",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Ready",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = onGrantClick,
                    modifier = Modifier.testTag(testTag)
                ) {
                    Text("Allow")
                }
            }
        }
    }
}
