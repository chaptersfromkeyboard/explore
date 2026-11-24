package com.cardpulse.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.cardpulse.app.model.BankPreset
import com.cardpulse.app.model.CreditCardItem
import com.cardpulse.app.model.RecurrenceUnit
import com.cardpulse.app.model.ReminderSettings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID

class CardRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCards(): List<CreditCardItem> {
        val raw = prefs.getString(KEY_CARDS, null)
        if (raw.isNullOrBlank()) {
            val seeded = createInitialSeedCards()
            saveCards(seeded)
            return seeded
        }
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        CreditCardItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            bankPreset = runCatching {
                                BankPreset.valueOf(obj.optString("bankPreset", BankPreset.CUSTOM.name))
                            }.getOrDefault(BankPreset.CUSTOM),
                            bankName = obj.optString("bankName", "Credit Card"),
                            cardLabel = obj.optString("cardLabel", "Credit Card"),
                            last4 = obj.optString("last4", "8888"),
                            dueDateIso = obj.optString("dueDateIso", LocalDate.now().plusDays(7).toString()),
                            paymentMethod = obj.optString("paymentMethod", "FPS (Faster Payment)"),
                            recurrenceEvery = obj.optInt("recurrenceEvery", 1).coerceAtLeast(1),
                            recurrenceUnit = runCatching {
                                RecurrenceUnit.valueOf(obj.optString("recurrenceUnit", RecurrenceUnit.MONTHS.name))
                            }.getOrDefault(RecurrenceUnit.MONTHS),
                            customImagePath = obj.optString("customImagePath").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        }.getOrElse { createInitialSeedCards() }
    }

    fun saveCards(cards: List<CreditCardItem>) {
        val array = JSONArray()
        cards.forEach { card ->
            val obj = JSONObject().apply {
                put("id", card.id)
                put("bankPreset", card.bankPreset.name)
                put("bankName", card.bankName)
                put("cardLabel", card.cardLabel)
                put("last4", card.last4)
                put("dueDateIso", card.dueDateIso)
                put("paymentMethod", card.paymentMethod)
                put("recurrenceEvery", card.recurrenceEvery)
                put("recurrenceUnit", card.recurrenceUnit.name)
                put("customImagePath", card.customImagePath ?: "")
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_CARDS, array.toString()).apply()
    }

    fun loadSettings(): ReminderSettings {
        return ReminderSettings(
            fiveDaysBefore = prefs.getBoolean(KEY_REMIND_5D, true),
            twoDaysBefore = prefs.getBoolean(KEY_REMIND_2D, true),
            onDueDate = prefs.getBoolean(KEY_REMIND_DUE, true),
            notificationsEnabled = prefs.getBoolean(KEY_NOTIF_ENABLED, true)
        )
    }

    fun saveSettings(settings: ReminderSettings) {
        prefs.edit()
            .putBoolean(KEY_REMIND_5D, settings.fiveDaysBefore)
            .putBoolean(KEY_REMIND_2D, settings.twoDaysBefore)
            .putBoolean(KEY_REMIND_DUE, settings.onDueDate)
            .putBoolean(KEY_NOTIF_ENABLED, settings.notificationsEnabled)
            .apply()
    }

    /**
     * Copies and center-crops a picked gallery image to ISO 7810 ID-1 credit card aspect ratio (1.586:1)
     * inside app-private storage so it loads quickly and persists safely across reboots.
     */
    fun saveCustomCardImage(uri: Uri): String? {
        return runCatching {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val original = inputStream.use { BitmapFactory.decodeStream(it) } ?: return null

            val targetW = 793
            val targetH = 500
            val srcRatio = original.width.toFloat() / original.height.toFloat()
            val dstRatio = targetW.toFloat() / targetH.toFloat()

            val cropW: Int
            val cropH: Int
            val startX: Int
            val startY: Int
            if (srcRatio > dstRatio) {
                cropH = original.height
                cropW = (original.height * dstRatio).toInt().coerceAtMost(original.width)
                startX = (original.width - cropW) / 2
                startY = 0
            } else {
                cropW = original.width
                cropH = (original.width / dstRatio).toInt().coerceAtMost(original.height)
                startX = 0
                startY = (original.height - cropH) / 2
            }

            val cropped = Bitmap.createBitmap(original, startX, startY, cropW, cropH)
            val scaled = Bitmap.createScaledBitmap(cropped, targetW, targetH, true)

            val dir = File(context.filesDir, "card_art").apply { mkdirs() }
            val outFile = File(dir, "card_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outFile).use { fos ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 86, fos)
            }
            if (cropped !== original) cropped.recycle()
            if (scaled !== cropped) scaled.recycle()
            original.recycle()

            outFile.absolutePath
        }.getOrNull()
    }

    private fun createInitialSeedCards(): List<CreditCardItem> {
        val today = LocalDate.now()
        return listOf(
            CreditCardItem(
                id = "seed-hsbc",
                bankPreset = BankPreset.HSBC,
                bankName = "HSBC",
                cardLabel = "Red Mastercard",
                last4 = "4821",
                dueDateIso = today.toString(),
                paymentMethod = "FPS (Faster Payment)",
                recurrenceEvery = 1,
                recurrenceUnit = RecurrenceUnit.MONTHS
            ),
            CreditCardItem(
                id = "seed-mox",
                bankPreset = BankPreset.MOX,
                bankName = "Mox Bank",
                cardLabel = "Mox Credit",
                last4 = "9034",
                dueDateIso = today.plusDays(2).toString(),
                paymentMethod = "Autopay (Direct Debit)",
                recurrenceEvery = 1,
                recurrenceUnit = RecurrenceUnit.MONTHS
            ),
            CreditCardItem(
                id = "seed-citi",
                bankPreset = BankPreset.CITI,
                bankName = "Citi",
                cardLabel = "Cash Back Visa",
                last4 = "7719",
                dueDateIso = today.plusDays(5).toString(),
                paymentMethod = "Bank App Transfer",
                recurrenceEvery = 1,
                recurrenceUnit = RecurrenceUnit.MONTHS
            ),
            CreditCardItem(
                id = "seed-sc",
                bankPreset = BankPreset.STANDARD_CHARTERED,
                bankName = "Standard Chartered",
                cardLabel = "Simply Cash",
                last4 = "3310",
                dueDateIso = today.plusDays(14).toString(),
                paymentMethod = "FPS (Faster Payment)",
                recurrenceEvery = 1,
                recurrenceUnit = RecurrenceUnit.MONTHS
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "cardpulse_prefs_v1"
        private const val KEY_CARDS = "cards_json"
        private const val KEY_REMIND_5D = "remind_5d"
        private const val KEY_REMIND_2D = "remind_2d"
        private const val KEY_REMIND_DUE = "remind_due"
        private const val KEY_NOTIF_ENABLED = "notif_enabled"
    }
}
