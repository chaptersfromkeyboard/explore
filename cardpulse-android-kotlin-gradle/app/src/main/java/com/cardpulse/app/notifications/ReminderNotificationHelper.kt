package com.cardpulse.app.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.cardpulse.app.MainActivity
import com.cardpulse.app.R
import com.cardpulse.app.data.CardRepository
import com.cardpulse.app.model.ActiveReminder
import com.cardpulse.app.model.CreditCardItem
import com.cardpulse.app.model.ReminderSettings
import com.cardpulse.app.model.ReminderSeverity
import java.util.concurrent.TimeUnit

object ReminderNotificationHelper {

    const val CHANNEL_ID = "cardpulse_payment_reminders"
    private const val WORK_NAME = "cardpulse_daily_reminder_check"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Credit Card Payment Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts 5 days before, 2 days before, and on your credit card due date."
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun evaluateReminders(
        cards: List<CreditCardItem>,
        settings: ReminderSettings
    ): List<ActiveReminder> {
        return cards.mapNotNull { card ->
            val days = card.daysUntilDue()
            when {
                days < 0 -> ActiveReminder(
                    card = card,
                    daysLeft = days,
                    message = "Overdue by ${-days}d — Immediate payment required",
                    severity = ReminderSeverity.DUE
                )
                days == 0L && settings.onDueDate -> ActiveReminder(
                    card = card,
                    daysLeft = 0L,
                    message = "Final payment reminder (Due today)",
                    severity = ReminderSeverity.DUE
                )
                days in 1L..2L && settings.twoDaysBefore -> ActiveReminder(
                    card = card,
                    daysLeft = days,
                    message = "Payment still outstanding (${days}d left)",
                    severity = ReminderSeverity.WARN
                )
                days in 3L..5L && settings.fiveDaysBefore -> ActiveReminder(
                    card = card,
                    daysLeft = days,
                    message = "Payment coming up (${days}d left)",
                    severity = ReminderSeverity.INFO
                )
                else -> null
            }
        }.sortedBy { it.daysLeft }
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun sendNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ): Boolean {
        ensureChannel(context)
        if (!canPostNotifications(context)) return false

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_card)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }

    fun schedulePeriodicChecks(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<PaymentReminderWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

class PaymentReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = CardRepository(applicationContext)
        val settings = repo.loadSettings()
        if (!settings.notificationsEnabled) return Result.success()

        val cards = repo.loadCards()
        val active = ReminderNotificationHelper.evaluateReminders(cards, settings)
        active.forEachIndexed { index, reminder ->
            val title = "CardPulse • ${reminder.card.bankName}"
            val body = "${reminder.message} via ${reminder.card.paymentMethod}"
            ReminderNotificationHelper.sendNotification(
                context = applicationContext,
                notificationId = 2000 + index,
                title = title,
                body = body
            )
        }
        return Result.success()
    }
}
