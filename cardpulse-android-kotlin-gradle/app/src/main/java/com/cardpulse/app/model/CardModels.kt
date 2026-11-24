package com.cardpulse.app.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class RecurrenceUnit(val label: String, val singular: String) {
    DAYS("Days", "day"),
    WEEKS("Weeks", "week"),
    MONTHS("Months", "month")
}

enum class BankPreset(
    val displayName: String,
    val defaultTag: String,
    val network: String,
    val gradientColors: List<Color>,
    val accentColor: Color
) {
    HSBC(
        displayName = "HSBC",
        defaultTag = "HSBC PREMIER / RED",
        network = "Mastercard",
        gradientColors = listOf(Color(0xFF1F0609), Color(0xFF7F1D1D), Color(0xFFDC2626)),
        accentColor = Color(0xFFEF4444)
    ),
    CITI(
        displayName = "Citi",
        defaultTag = "CITI CASH BACK / REWARDS",
        network = "VISA",
        gradientColors = listOf(Color(0xFF04152D), Color(0xFF0C3B75), Color(0xFF0284C7)),
        accentColor = Color(0xFF38BDF8)
    ),
    MOX(
        displayName = "Mox Bank",
        defaultTag = "MOX CARD • DIGITAL",
        network = "Mastercard",
        gradientColors = listOf(Color(0xFF042F2E), Color(0xFF0D9488), Color(0xFF2DD4BF)),
        accentColor = Color(0xFF2DD4BF)
    ),
    STANDARD_CHARTERED(
        displayName = "Standard Chartered",
        defaultTag = "SC SIMPLY CASH / CATHAY",
        network = "Mastercard",
        gradientColors = listOf(Color(0xFF062038), Color(0xFF0F4C81), Color(0xFF15803D)),
        accentColor = Color(0xFF4ADE80)
    ),
    HANG_SENG(
        displayName = "Hang Seng Bank",
        defaultTag = "HANG SENG MMPOWER",
        network = "Mastercard",
        gradientColors = listOf(Color(0xFF032219), Color(0xFF065F46), Color(0xFF10B981)),
        accentColor = Color(0xFF34D399)
    ),
    ZA_BANK(
        displayName = "ZA Bank",
        defaultTag = "ZA CARD • CUSTOM",
        network = "VISA",
        gradientColors = listOf(Color(0xFF052E2B), Color(0xFF0F766E), Color(0xFF84CC16)),
        accentColor = Color(0xFFA3E635)
    ),
    CUSTOM(
        displayName = "Custom Bank",
        defaultTag = "OBSIDIAN EDITION",
        network = "VISA",
        gradientColors = listOf(Color(0xFF090D16), Color(0xFF1E293B), Color(0xFF0E7490)),
        accentColor = Color(0xFF22D3EE)
    )
}

data class CreditCardItem(
    val id: String,
    val bankPreset: BankPreset,
    val bankName: String,
    val cardLabel: String,
    val last4: String,
    val dueDateIso: String, // YYYY-MM-DD
    val paymentMethod: String,
    val recurrenceEvery: Int,
    val recurrenceUnit: RecurrenceUnit,
    val customImagePath: String? = null
) {
    fun dueDate(): LocalDate = runCatching { LocalDate.parse(dueDateIso) }
        .getOrElse { LocalDate.now() }

    fun daysUntilDue(today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, dueDate())

    fun formattedDueDate(): String =
        dueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))

    fun nextCycleDueDateIso(): String {
        val current = dueDate()
        val interval = recurrenceEvery.coerceAtLeast(1).toLong()
        val next = when (recurrenceUnit) {
            RecurrenceUnit.DAYS -> current.plusDays(interval)
            RecurrenceUnit.WEEKS -> current.plusWeeks(interval)
            RecurrenceUnit.MONTHS -> current.plusMonths(interval)
        }
        return next.toString()
    }

    fun recurrenceSummary(): String {
        val unitText = if (recurrenceEvery == 1) recurrenceUnit.singular else recurrenceUnit.label.lowercase()
        return "Every $recurrenceEvery $unitText"
    }
}

data class ReminderSettings(
    val fiveDaysBefore: Boolean = true,
    val twoDaysBefore: Boolean = true,
    val onDueDate: Boolean = true,
    val notificationsEnabled: Boolean = true
)

enum class ReminderSeverity {
    DUE, WARN, INFO
}

data class ActiveReminder(
    val card: CreditCardItem,
    val daysLeft: Long,
    val message: String,
    val severity: ReminderSeverity
)

object PaymentMethods {
    val defaults = listOf(
        "FPS (Faster Payment)",
        "Autopay (Direct Debit)",
        "Bank App Transfer",
        "PPS",
        "AlipayHK / WeChat Pay",
        "Manual Check"
    )
}
