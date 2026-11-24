package com.cardpulse.app.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cardpulse.app.model.BankPreset
import com.cardpulse.app.model.CreditCardItem
import com.cardpulse.app.model.PaymentMethods
import com.cardpulse.app.model.RecurrenceUnit
import com.cardpulse.app.ui.theme.InputBg
import com.cardpulse.app.ui.theme.NeonCyan
import com.cardpulse.app.ui.theme.RoseUrgent
import com.cardpulse.app.ui.theme.SheetSurface
import com.cardpulse.app.ui.theme.SlateBorder
import com.cardpulse.app.ui.theme.TextPrimary
import com.cardpulse.app.ui.theme.TextSecondary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorSheet(
    editingCard: CreditCardItem?,
    onDismiss: () -> Unit,
    onImportImage: (android.net.Uri) -> String?,
    onSave: (CreditCardItem) -> Unit,
    onDelete: (String) -> Unit
) {
    var bankPreset by remember { mutableStateOf(editingCard?.bankPreset ?: BankPreset.HSBC) }
    var bankName by remember { mutableStateOf(editingCard?.bankName ?: BankPreset.HSBC.displayName) }
    var cardLabel by remember { mutableStateOf(editingCard?.cardLabel ?: "") }
    var last4 by remember { mutableStateOf(editingCard?.last4 ?: "") }
    var dueDate by remember {
        mutableStateOf(
            editingCard?.dueDate() ?: LocalDate.now().plusDays(7)
        )
    }

    val initialIsPresetMethod = editingCard == null || PaymentMethods.defaults.contains(editingCard.paymentMethod)
    var selectedMethod by remember {
        mutableStateOf(
            if (initialIsPresetMethod) editingCard?.paymentMethod ?: PaymentMethods.defaults.first()
            else "CUSTOM"
        )
    }
    var customMethod by remember {
        mutableStateOf(if (initialIsPresetMethod) "" else editingCard?.paymentMethod ?: "")
    }

    var recurrenceEveryText by remember {
        mutableStateOf((editingCard?.recurrenceEvery ?: 1).toString())
    }
    var recurrenceUnit by remember {
        mutableStateOf(editingCard?.recurrenceUnit ?: RecurrenceUnit.MONTHS)
    }
    var customImagePath by remember { mutableStateOf(editingCard?.customImagePath) }
    var showDatePicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val savedPath = onImportImage(uri)
            if (savedPath != null) {
                customImagePath = savedPath
            }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = NeonCyan,
        unfocusedBorderColor = SlateBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary,
        focusedContainerColor = InputBg,
        unfocusedContainerColor = InputBg,
        cursorColor = NeonCyan,
        focusedLabelColor = NeonCyan,
        unfocusedLabelColor = TextSecondary
    )

    val formattedPreviewDate = dueDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SheetSurface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editingCard != null) "Edit Credit Card" else "Add New Credit Card",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            // Live Google Pay Card Preview
            Text(
                text = "LIVE GOOGLE PAY CARD PREVIEW",
                color = TextSecondary,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
            CreditCardVisual(
                bankPreset = bankPreset,
                bankName = bankName,
                cardLabel = cardLabel,
                last4 = last4,
                dueDateText = formattedPreviewDate,
                daysLeft = null,
                customImagePath = customImagePath
            )

            // Bank Preset Chips
            Text(
                text = "Bank Card Theme",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BankPreset.entries.forEach { preset ->
                    val active = preset == bankPreset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) NeonCyan.copy(alpha = 0.2f) else InputBg)
                            .border(
                                1.dp,
                                if (active) NeonCyan else SlateBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val oldPresetName = bankPreset.displayName
                                bankPreset = preset
                                if (preset != BankPreset.CUSTOM) {
                                    bankName = preset.displayName
                                } else if (bankName == oldPresetName) {
                                    bankName = ""
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = preset.displayName,
                            color = if (active) NeonCyan else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // Custom Card Image Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(InputBg)
                        .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 12.dp, horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (customImagePath != null) "Replace Card Image" else "Upload Custom Card Image",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (customImagePath != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoseUrgent.copy(alpha = 0.15f))
                            .border(1.dp, RoseUrgent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable { customImagePath = null }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text("Reset", color = RoseUrgent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Bank Name & Card Tier
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = cardLabel,
                    onValueChange = { cardLabel = it },
                    label = { Text("Card Label") },
                    placeholder = { Text("e.g. Red Card") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // Due Date Picker & Last 4 Digits
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(InputBg)
                        .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Column {
                        Text("Next Due Date", color = TextSecondary, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formattedPreviewDate,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = last4,
                    onValueChange = { input -> last4 = input.filter { it.isDigit() }.take(4) },
                    label = { Text("Last 4") },
                    placeholder = { Text("8888") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(0.9f)
                )
            }

            // Payment Method Selection
            Text(
                text = "Payment Method",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                (PaymentMethods.defaults + "CUSTOM").forEach { method ->
                    val active = selectedMethod == method
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) NeonCyan.copy(alpha = 0.2f) else InputBg)
                            .border(1.dp, if (active) NeonCyan else SlateBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedMethod = method }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (method == "CUSTOM") "Custom..." else method,
                            color = if (active) NeonCyan else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            if (selectedMethod == "CUSTOM") {
                OutlinedTextField(
                    value = customMethod,
                    onValueChange = { customMethod = it },
                    label = { Text("Custom Payment Method") },
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Custom Recurrence (Days / Weeks / Months)
            Text(
                text = "Repeat Every (Custom Recurrence)",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = recurrenceEveryText,
                    onValueChange = { input ->
                        recurrenceEveryText = input.filter { it.isDigit() }.take(3)
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.width(84.dp)
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(InputBg)
                        .border(1.dp, SlateBorder, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RecurrenceUnit.entries.forEach { unit ->
                        val active = unit == recurrenceUnit
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) NeonCyan else Color.Transparent)
                                .clickable { recurrenceUnit = unit }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = unit.label,
                                color = if (active) Color(0xFF04121A) else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Action Buttons (Delete + Save)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (editingCard != null) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoseUrgent.copy(alpha = 0.15f))
                            .border(1.dp, RoseUrgent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable { onDelete(editingCard.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = RoseUrgent)
                    }
                }

                Button(
                    onClick = {
                        val resolvedName = bankName.trim().ifBlank { bankPreset.displayName }
                        val resolvedMethod = if (selectedMethod == "CUSTOM") {
                            customMethod.trim().ifBlank { "Manual Payment" }
                        } else {
                            selectedMethod
                        }
                        val interval = recurrenceEveryText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val item = CreditCardItem(
                            id = editingCard?.id ?: UUID.randomUUID().toString(),
                            bankPreset = bankPreset,
                            bankName = resolvedName,
                            cardLabel = cardLabel.trim().ifBlank { "Credit Card" },
                            last4 = last4.ifBlank { "8888" },
                            dueDateIso = dueDate.toString(),
                            paymentMethod = resolvedMethod,
                            recurrenceEvery = interval,
                            recurrenceUnit = recurrenceUnit,
                            customImagePath = customImagePath
                        )
                        onSave(item)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF04121A)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = if (editingCard != null) "Save Changes" else "Add Card to Wallet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = dueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
