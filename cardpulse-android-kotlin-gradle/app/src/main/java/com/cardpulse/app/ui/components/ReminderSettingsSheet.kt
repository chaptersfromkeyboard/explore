package com.cardpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardpulse.app.model.ReminderSettings
import com.cardpulse.app.ui.theme.AmberWarn
import com.cardpulse.app.ui.theme.EmeraldOk
import com.cardpulse.app.ui.theme.InputBg
import com.cardpulse.app.ui.theme.NeonCyan
import com.cardpulse.app.ui.theme.RoseUrgent
import com.cardpulse.app.ui.theme.SheetSurface
import com.cardpulse.app.ui.theme.SlateBorder
import com.cardpulse.app.ui.theme.TextPrimary
import com.cardpulse.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsSheet(
    settings: ReminderSettings,
    hasNotificationPermission: Boolean,
    onDismiss: () -> Unit,
    onToggleFiveDays: (Boolean) -> Unit,
    onToggleTwoDays: (Boolean) -> Unit,
    onToggleDueDate: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onTestReminder: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reminder Schedule & Alerts",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            ReminderToggleRow(
                tag = "5 DAYS BEFORE",
                tagColor = NeonCyan,
                title = "Payment coming up",
                checked = settings.fiveDaysBefore,
                onCheckedChange = onToggleFiveDays
            )

            ReminderToggleRow(
                tag = "2 DAYS BEFORE",
                tagColor = AmberWarn,
                title = "Payment still outstanding",
                checked = settings.twoDaysBefore,
                onCheckedChange = onToggleTwoDays
            )

            ReminderToggleRow(
                tag = "ON DUE DATE",
                tagColor = RoseUrgent,
                title = "Final payment reminder",
                checked = settings.onDueDate,
                onCheckedChange = onToggleDueDate
            )

            HorizontalDivider(color = SlateBorder, modifier = Modifier.padding(vertical = 4.dp))

            // Android Notification Permission + Master Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(InputBg)
                    .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Android Local Notifications",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (hasNotificationPermission) {
                            "System permission granted"
                        } else {
                            "Tap toggle to grant notification permission"
                        },
                        color = if (hasNotificationPermission) EmeraldOk else TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = settings.notificationsEnabled && hasNotificationPermission,
                    onCheckedChange = { enabled ->
                        if (enabled && !hasNotificationPermission) {
                            onRequestPermission()
                        } else {
                            onToggleNotifications(enabled)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF04121A),
                        checkedTrackColor = NeonCyan
                    )
                )
            }

            Button(
                onClick = {
                    if (!hasNotificationPermission) {
                        onRequestPermission()
                    }
                    onTestReminder()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.16f),
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Reminder Alert", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ReminderToggleRow(
    tag: String,
    tagColor: Color,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(InputBg)
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tag,
                color = tagColor,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF04121A),
                checkedTrackColor = NeonCyan
            )
        )
    }
}
