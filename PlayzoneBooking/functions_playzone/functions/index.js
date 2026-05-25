// functions/index.js
// Deploy ke Firebase: firebase deploy --only functions

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

// ── Trigger: Booking baru dibuat ─────────────────────────────
exports.onBookingCreated = functions.firestore
  .document("bookings/{bookingId}")
  .onCreate(async (snap, context) => {
    const booking = snap.data();
    const { userName, psUnitName, durationHours, totalPrice, fcmToken } = booking;

    if (!fcmToken) return null;

    const message = {
      token: fcmToken,
      notification: {
        title: "✅ Booking Berhasil!",
        body: `${psUnitName} dipesan selama ${durationHours} jam - Rp ${totalPrice.toLocaleString("id-ID")}`,
      },
      data: {
        bookingId: context.params.bookingId,
        type: "booking_created",
      },
      android: {
        priority: "high",
        notification: { channelId: "playzone_channel" },
      },
    };

    try {
      await messaging.send(message);
      console.log("Notif booking created sent to", fcmToken);
    } catch (err) {
      console.error("Error sending notif:", err);
    }

    return null;
  });

// ── Trigger: Status booking berubah ──────────────────────────
exports.onBookingStatusChanged = functions.firestore
  .document("bookings/{bookingId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    if (before.status === after.status) return null;

    const { fcmToken, psUnitName } = after;
    if (!fcmToken) return null;

    const statusMessages = {
      ACTIVE:    { title: "🎮 Mulai Main!", body: `${psUnitName} sudah aktif. Selamat bermain!` },
      COMPLETED: { title: "🏁 Sesi Selesai", body: `Sesi ${psUnitName} telah selesai. Terima kasih!` },
      CANCELLED: { title: "❌ Booking Dibatalkan", body: `Booking ${psUnitName} telah dibatalkan.` },
    };

    const notif = statusMessages[after.status];
    if (!notif) return null;

    const message = {
      token: fcmToken,
      notification: notif,
      data: {
        bookingId: context.params.bookingId,
        type: "status_changed",
        newStatus: after.status,
      },
      android: {
        priority: "high",
        notification: { channelId: "playzone_channel" },
      },
    };

    try {
      await messaging.send(message);
    } catch (err) {
      console.error("Error sending status notif:", err);
    }

    return null;
  });

// ── Trigger: Notifikasi broadcast ke semua user online ────────
exports.broadcastAvailability = functions.firestore
  .document("ps_units/{unitId}")
  .onUpdate(async (change) => {
    const before = change.before.data();
    const after = change.after.data();

    // Jika unit baru jadi tersedia, notif semua user
    if (!before.isAvailable && after.isAvailable) {
      const usersSnap = await db.collection("users").get();
      const tokens = usersSnap.docs
        .map(d => d.data().fcmToken)
        .filter(t => !!t);

      if (tokens.length === 0) return null;

      const multicastMessage = {
        tokens,
        notification: {
          title: "🎮 Unit PS Tersedia!",
          body: `${after.name} kini tersedia untuk dipesan!`,
        },
        android: {
          priority: "high",
          notification: { channelId: "playzone_channel" },
        },
      };

      try {
        await messaging.sendEachForMulticast(multicastMessage);
        console.log(`Broadcast sent to ${tokens.length} users`);
      } catch (err) {
        console.error("Broadcast error:", err);
      }
    }

    return null;
  });