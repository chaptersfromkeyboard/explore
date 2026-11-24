package com.cardpulse.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cardpulse.app.model.ActiveReminder
import com.cardpulse.app.model.CreditCardItem
import com.cardpulse.app.model.ReminderSeverity
import com.cardpulse.app.notifications.ReminderNotificationHelper
import com.cardpulse.app.ui.components.CardEditorSheet
import com.cardpulse.app.ui.components.CreditCardVisual
import com.cardpulse.app.ui.components.ReminderSettingsSheet
import com.cardpulse.app.ui.theme.AmberWarn
import com.cardpulse.app.ui.theme.CardFooterBg
import com.cardpulse.app.ui.theme.CardSurface
import com.cardpulse.app.ui.theme.ElectricBlue
import com.cardpulse.app.ui.theme.InputBg
import com.cardpulse.app.ui.theme.NeonCyan
import com.cardpulse.app.ui.theme.ObsidianBg
import com.cardpulse.app.ui.theme.RoseUrgent
import com.cardpulse.app.ui.theme.SlateBorder
import com.cardpulse.app.ui.theme.TextMuted
import com.cardpulse.app.ui.theme.TextPrimary
import com.cardpulse.app.ui.theme.TextSecondary
import com.cardpulse.app.viewmodel.CardFilter
import com.cardpulse.app.viewmodel.CardPulseViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardPulseApp(
    viewModel: CardPulseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCardItem?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var hasNotifPermission by remember {
        mutableStateOf(ReminderNotificationHelper.canPostNotifications(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPermission = granted
        if (granted) {
            viewModel.updateSettings { it.copy(notificationsEnabled = true) }
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBg.copy(alpha = 0.96f)
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(listOf(NeonCyan, ElectricBlue))
                                )
                                .padding(1.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.5.dp))
                                    .background(ObsidianBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CreditCard,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "CardPulse",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonCyan.copy(alpha = 0.14f))
                                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "PAY",
                                        color = NeonCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Text(
                                text = "Smart Billing Telemetry",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                actions = {
                    // Bell button with reminder count badge
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable { showSettingsSheet = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(InputBg)
                                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Reminders",
                                tint = TextPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        if (uiState.activeReminders.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 3.dp, y = (-3).dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(RoseUrgent),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.activeReminders.size.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Settings gear button
                    Box(
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(InputBg)
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            .clickable { showSettingsSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCard = null
                    showEditorSheet = true
                },
                containerColor = NeonCyan,
                contentColor = Color(0xFF04121A),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Credit Card",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Telemetry Overview Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryMetricCard(
                        label = "ACTIVE CARDS",
                        value = uiState.cards.size.toString(),
                        valueColor = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMetricCard(
                        label = "NEXT DUE",
                        value = uiState.nextDueSummary,
                        valueColor = NeonCyan,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMetricCard(
                        label = "DUE ≤ 7D",
                        value = uiState.urgentCount.toString(),
                        valueColor = AmberWarn,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 2. Active Reminders Banner
            if (uiState.activeReminders.isNotEmpty()) {
                item {
                    ActiveRemindersBanner(
                        reminders = uiState.activeReminders,
                        onConfigureClick = { showSettingsSheet = true }
                    )
                }
            }

            // 3. Filter Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WALLET STACK • BY DUE DATE",
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(InputBg)
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChipPill(
                            text = "All (${uiState.cards.size})",
                            selected = uiState.filter == CardFilter.ALL,
                            selectedColor = NeonCyan,
                            onClick = { viewModel.setFilter(CardFilter.ALL) }
                        )
                        FilterChipPill(
                            text = "≤ 7d (${uiState.urgentCount})",
                            selected = uiState.filter == CardFilter.URGENT_7D,
                            selectedColor = AmberWarn,
                            onClick = { viewModel.setFilter(CardFilter.URGENT_7D) }
                        )
                    }
                }
            }

            // 4. Credit Card Stack
            if (uiState.visibleCards.isEmpty()) {
                item {
                    EmptyWalletPlaceholder()
                }
            } else {
                items(uiState.visibleCards, key = { it.id }) { card ->
                    CreditCardTrackerItem(
                        card = card,
                        onEdit = {
                            editingCard = card
                            showEditorSheet = true
                        },
                        onMarkPaid = {
                            val updated = viewModel.markCardPaid(card)
                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "${card.bankName} moved to ${updated.formattedDueDate()}",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoLastMarkPaid()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showEditorSheet) {
        CardEditorSheet(
            editingCard = editingCard,
            onDismiss = { showEditorSheet = false },
            onImportImage = { uri -> viewModel.importCardImage(uri) },
            onSave = { item ->
                viewModel.saveCard(item)
                showEditorSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("${item.bankName} saved to wallet")
                }
            },
            onDelete = { id ->
                viewModel.deleteCard(id)
                showEditorSheet = false
                scope.launch {
                    snackbarHostState.showSnackbar("Card removed")
                }
            }
        )
    }

    if (showSettingsSheet) {
        ReminderSettingsSheet(
            settings = uiState.settings,
            hasNotificationPermission = hasNotifPermission,
            onDismiss = { showSettingsSheet = false },
            onToggleFiveDays = { enabled ->
                viewModel.updateSettings { it.copy(fiveDaysBefore = enabled) }
            },
            onToggleTwoDays = { enabled ->
                viewModel.updateSettings { it.copy(twoDaysBefore = enabled) }
            },
            onToggleDueDate = { enabled ->
                viewModel.updateSettings { it.copy(onDueDate = enabled) }
            },
            onToggleNotifications = { enabled ->
                viewModel.updateSettings { it.copy(notificationsEnabled = enabled) }
            },
            onRequestPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    hasNotifPermission = true
                    viewModel.updateSettings { it.copy(notificationsEnabled = true) }
                }
            },
            onTestReminder = {
                val (title, body) = viewModel.triggerTestNotification()
                scope.launch {
                    snackbarHostState.showSnackbar("$title: $body")
                }
            }
        )
    }
}

@Composable
private fun TelemetryMetricCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardSurface)
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = valueColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun ActiveRemindersBanner(
    reminders: List<ActiveReminder>,
    onConfigureClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        AmberWarn.copy(alpha = 0.12f),
                        RoseUrgent.copy(alpha = 0.10f),
                        CardSurface
                    )
                )
            )
            .border(1.dp, AmberWarn.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = AmberWarn,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ACTIVE REMINDERS (${reminders.size})",
                    color = AmberWarn,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "Configure",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onConfigureClick() }
            )
        }

        reminders.forEach { reminder ->
            val msgColor = when (reminder.severity) {
                ReminderSeverity.DUE -> RoseUrgent
                ReminderSeverity.WARN -> AmberWarn
                ReminderSeverity.INFO -> NeonCyan
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(ObsidianBg.copy(alpha = 0.65f))
                    .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reminder.card.bankName,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.4f)
                )
                Text(
                    text = reminder.message,
                    color = msgColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) selectedColor.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (selected) selectedColor.copy(alpha = 0.4f) else Color.Transparent,
                shape = RoundedCornerShape(9.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (selected) selectedColor else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CreditCardTrackerItem(
    card: CreditCardItem,
    onEdit: () -> Unit,
    onMarkPaid: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface)
            .border(1.dp, SlateBorder, RoundedCornerShape(22.dp))
    ) {
        CreditCardVisual(
            bankPreset = card.bankPreset,
            bankName = card.bankName,
            cardLabel = card.cardLabel,
            last4 = card.last4,
            dueDateText = card.formattedDueDate(),
            daysLeft = card.daysUntilDue(),
            customImagePath = card.customImagePath,
            onClick = onEdit
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardFooterBg)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.paymentMethod,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Repeat,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.recurrenceSummary(),
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(InputBg)
                        .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                        .clickable { onEdit() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit Card",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyan.copy(alpha = 0.16f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onMarkPaid() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Mark Paid",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyWalletPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(CardSurface.copy(alpha = 0.6f))
            .border(1.dp, SlateBorder, RoundedCornerShape(22.dp))
            .padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.CreditCard,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "No credit cards in this view",
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap the + button below to add your bank card and payment cycle.",
            color = TextSecondary,
            fontSize = 12.sp
        )
    }
}
