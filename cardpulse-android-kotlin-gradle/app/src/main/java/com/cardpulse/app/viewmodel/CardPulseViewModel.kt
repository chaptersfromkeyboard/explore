package com.cardpulse.app.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.ActiveReminder
import com.cardpulse.app.model.CreditCardItem
import com.cardpulse.app.model.ReminderSettings
import com.cardpulse.app.notifications.ReminderNotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class CardFilter { ALL, URGENT_7D }

data class CardPulseUiState(
    val cards: List<CreditCardItem> = emptyList(),
    val settings: ReminderSettings = ReminderSettings(),
    val filter: CardFilter = CardFilter.ALL,
    val activeReminders: List<ActiveReminder> = emptyList(),
    val lastUndoSnapshot: CreditCardItem? = null
) {
    val visibleCards: List<CreditCardItem>
        get() = cards
            .sortedBy { it.dueDateIso }
            .filter { card ->
                if (filter == CardFilter.URGENT_7D) card.daysUntilDue() <= 7L else true
            }

    val urgentCount: Int
        get() = cards.count { it.daysUntilDue() <= 7L }

    val nextDueSummary: String
        get() {
            val nearest = cards.minByOrNull { it.dueDateIso } ?: return "—"
            val diff = nearest.daysUntilDue()
            return when {
                diff < 0L -> "${-diff}d late"
                diff == 0L -> "Today"
                else -> "In ${diff}d"
            }
        }
}

class CardPulseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CardRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<CardPulseUiState> = _uiState.asStateFlow()

    init {
        ReminderNotificationHelper.ensureChannel(application.applicationContext)
        ReminderNotificationHelper.schedulePeriodicChecks(application.applicationContext)
    }

    private fun loadInitialState(): CardPulseUiState {
        val cards = repository.loadCards()
        val settings = repository.loadSettings()
        return CardPulseUiState(
            cards = cards,
            settings = settings,
            activeReminders = ReminderNotificationHelper.evaluateReminders(cards, settings)
        )
    }

    fun setFilter(filter: CardFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun saveCard(card: CreditCardItem) {
        _uiState.update { state ->
            val exists = state.cards.any { it.id == card.id }
            val updated = if (exists) {
                state.cards.map { if (it.id == card.id) card else it }
            } else {
                state.cards + card
            }
            repository.saveCards(updated)
            state.copy(
                cards = updated,
                activeReminders = ReminderNotificationHelper.evaluateReminders(updated, state.settings)
            )
        }
    }

    fun deleteCard(cardId: String) {
        _uiState.update { state ->
            val updated = state.cards.filterNot { it.id == cardId }
            repository.saveCards(updated)
            state.copy(
                cards = updated,
                activeReminders = ReminderNotificationHelper.evaluateReminders(updated, state.settings)
            )
        }
    }

    fun markCardPaid(card: CreditCardItem): CreditCardItem {
        val advanced = card.copy(dueDateIso = card.nextCycleDueDateIso())
        _uiState.update { state ->
            val updated = state.cards.map { if (it.id == card.id) advanced else it }
            repository.saveCards(updated)
            state.copy(
                cards = updated,
                lastUndoSnapshot = card,
                activeReminders = ReminderNotificationHelper.evaluateReminders(updated, state.settings)
            )
        }
        return advanced
    }

    fun undoLastMarkPaid() {
        val snapshot = _uiState.value.lastUndoSnapshot ?: return
        _uiState.update { state ->
            val updated = state.cards.map { if (it.id == snapshot.id) snapshot else it }
            repository.saveCards(updated)
            state.copy(
                cards = updated,
                lastUndoSnapshot = null,
                activeReminders = ReminderNotificationHelper.evaluateReminders(updated, state.settings)
            )
        }
    }

    fun updateSettings(transform: (ReminderSettings) -> ReminderSettings) {
        _uiState.update { state ->
            val updatedSettings = transform(state.settings)
            repository.saveSettings(updatedSettings)
            state.copy(
                settings = updatedSettings,
                activeReminders = ReminderNotificationHelper.evaluateReminders(state.cards, updatedSettings)
            )
        }
    }

    fun importCardImage(uri: Uri): String? {
        return repository.saveCustomCardImage(uri)
    }

    fun triggerTestNotification(): Pair<String, String> {
        val firstReminder = _uiState.value.activeReminders.firstOrNull()
        val title = if (firstReminder != null) {
            "CardPulse • ${firstReminder.card.bankName}"
        } else {
            "CardPulse • Payment coming up"
        }
        val body = if (firstReminder != null) {
            "${firstReminder.message} via ${firstReminder.card.paymentMethod}"
        } else {
            "HSBC Red Mastercard payment is due in 5 days via FPS (Faster Payment)."
        }

        ReminderNotificationHelper.sendNotification(
            context = getApplication(),
            notificationId = 9999,
            title = title,
            body = body
        )
        return title to body
    }
}
