package com.playzone.booking.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.playzone.booking.MainActivity
import com.playzone.booking.R

/**
 * Helper untuk kirim notifikasi lokal tanpa Cloud Functions / Blaze plan.
 * Dipanggil langsung dari app setelah booking berhasil.
 */
object LocalNotificationHelper {

    fun sendBookingConfirmedNotif(context: Context, bookingId: String, psUnitName: String, date: String, startTime: String) {
        send(
            context = context,
            title = "✅ Booking Dikonfirmasi!",
            body = "$psUnitName\nTanggal: $date, Jam: $startTime\nID: $bookingId"
        )
    }

    fun sendBookingCancelledNotif(context: Context, psUnitName: String) {
        send(
            context = context,
            title = "❌ Booking Dibatalkan",
            body = "Booking $psUnitName telah dibatalkan."
        )
    }

    fun sendReviewPostedNotif(context: Context, psUnitName: String) {
        send(
            context = context,
            title = "⭐ Review Terposting!",
            body = "Review kamu untuk $psUnitName sudah dipublikasikan."
        )
    }

    private fun send(context: Context, title: String, body: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, "playzone_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}