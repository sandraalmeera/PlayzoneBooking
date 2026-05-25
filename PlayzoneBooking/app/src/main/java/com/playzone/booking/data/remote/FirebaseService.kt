package com.playzone.booking.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.playzone.booking.data.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseService @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser get() = auth.currentUser

    // ── Auth ───────────────────────────────────────────
    suspend fun signIn(email: String, password: String): Result<String> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await().user?.uid ?: throw Exception("Login gagal")
    }

    suspend fun register(email: String, password: String, name: String, phone: String): Result<String> = runCatching {
        val uid = auth.createUserWithEmailAndPassword(email, password).await().user?.uid ?: throw Exception("Register gagal")
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrDefault("")
        firestore.collection("users").document(uid).set(
            UserProfile(uid = uid, name = name, email = email, phone = phone, fcmToken = token)
        ).await()
        uid
    }

    fun signOut() = auth.signOut()

    suspend fun updateFcmToken(uid: String) {
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrDefault("")
        if (token.isNotEmpty()) firestore.collection("users").document(uid).update("fcmToken", token).await()
    }

    suspend fun getUserProfile(uid: String): UserProfile? = try {
        firestore.collection("users").document(uid).get().await().toObject(UserProfile::class.java)
    } catch (e: Exception) { null }

    // ── PS Units (real-time dari Firestore, sinkron dengan admin) ──
    fun getPsUnitsFlow(): Flow<List<PsUnit>> = callbackFlow {
        val listener = firestore.collection("ps_units").addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            val units = snap?.documents?.mapNotNull { doc ->
                try {
                    PsUnit(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        type = doc.getString("type") ?: "PS4",
                        pricePerHour = (doc.getLong("pricePerHour") ?: 0L).toInt(),
                        isAvailable = doc.getBoolean("isAvailable") ?: true,
                        imageUrl = doc.getString("imageUrl") ?: "",
                        facilities = (doc.get("facilities") as? List<*>)?.map { it.toString() } ?: emptyList(),
                        description = doc.getString("description") ?: ""
                    )
                } catch (e: Exception) { null }
            } ?: emptyList()
            trySend(units)
        }
        awaitClose { listener.remove() }
    }

    // ── Snack Menu (real-time dari Firestore, dikelola admin) ──────
    fun getSnackMenuFlow(): Flow<List<SnackMenuItem>> = callbackFlow {
        val listener = firestore.collection("snack_menu")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val items = snap?.documents?.mapNotNull { doc ->
                    try {
                        SnackMenuItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            emoji = doc.getString("emoji") ?: "🍟",
                            price = (doc.getLong("price") ?: 0L).toInt(),
                            isAvailable = doc.getBoolean("isAvailable") ?: true,
                            category = doc.getString("category") ?: "food"
                        )
                    } catch (e: Exception) { null }
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    // ── Create Snack Order (simpan ke Firestore → real-time ke admin) ─
    suspend fun createSnackOrder(order: SnackOrder): Result<String> = runCatching {
        val ref = firestore.collection("snack_orders").document()
        val itemsData = order.items.map { item ->
            mapOf(
                "name" to item.name,
                "emoji" to item.emoji,
                "qty" to item.qty,
                "price" to item.price
            )
        }
        val data = hashMapOf<String, Any>(
            "bookingId" to order.bookingId,
            "userId" to order.userId,
            "userName" to order.userName,
            "psUnitName" to order.psUnitName,
            "items" to itemsData,
            "totalPrice" to order.totalPrice,
            "status" to SnackOrderStatus.WAITING.name,
            "createdAt" to System.currentTimeMillis()
        )
        ref.set(data).await()
        ref.id
    }

    // ── Bookings ───────────────────────────────────────
    suspend fun createBooking(booking: Booking): Result<Unit> = runCatching {
        val data = hashMapOf<String, Any>(
            "id" to booking.id, "userId" to booking.userId, "userName" to booking.userName,
            "userPhone" to booking.userPhone, "psUnitId" to booking.psUnitId,
            "psUnitName" to booking.psUnitName, "psUnitType" to booking.psUnitType,
            "bookingDate" to booking.bookingDate, "startTime" to booking.startTime,
            "startTimeMillis" to booking.startTimeMillis, "endTimeMillis" to booking.endTimeMillis,
            "durationHours" to booking.durationHours, "totalPrice" to booking.totalPrice,
            "status" to booking.status.name, "paymentMethod" to booking.paymentMethod.name,
            "createdAt" to booking.createdAt, "fcmToken" to booking.fcmToken
        )
        firestore.collection("bookings").document(booking.id).set(data).await()
    }

    fun getBookingsFlow(userId: String? = null): Flow<List<Booking>> = callbackFlow {
        if (userId != null && userId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // PENTING: jangan pakai whereEqualTo + orderBy bersamaan tanpa composite index di Firebase
        // Sort dilakukan di memory agar tidak perlu composite index
        val query = if (userId != null)
            firestore.collection("bookings").whereEqualTo("userId", userId)
        else
            firestore.collection("bookings")

        val listener = query.addSnapshotListener { snap, err ->
            // Jika error, LOG saja — jangan kirim emptyList agar data lama tetap tampil
            if (err != null) {
                // Coba lagi dengan ignore error (data cache Firestore masih bisa dipakai)
                return@addSnapshotListener
            }
            val bookings = snap?.documents?.mapNotNull { doc ->
                try {
                    Booking(
                        id = doc.getString("id") ?: doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "",
                        userPhone = doc.getString("userPhone") ?: "",
                        psUnitId = doc.getString("psUnitId") ?: "",
                        psUnitName = doc.getString("psUnitName") ?: "",
                        psUnitType = doc.getString("psUnitType") ?: "",
                        bookingDate = doc.getString("bookingDate") ?: "",
                        startTime = doc.getString("startTime") ?: "",
                        startTimeMillis = doc.getLong("startTimeMillis") ?: 0L,
                        endTimeMillis = doc.getLong("endTimeMillis") ?: 0L,
                        durationHours = (doc.getLong("durationHours") ?: 1L).toInt(),
                        totalPrice = (doc.getLong("totalPrice") ?: 0L).toInt(),
                        status = try { BookingStatus.valueOf(doc.getString("status") ?: "PENDING") } catch (e: Exception) { BookingStatus.PENDING },
                        paymentMethod = try { PaymentMethod.valueOf(doc.getString("paymentMethod") ?: "COD") } catch (e: Exception) { PaymentMethod.COD },
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        fcmToken = doc.getString("fcmToken") ?: ""
                    )
                } catch (e: Exception) { null }
            }
            // Sort descending by createdAt di memory — tidak perlu composite index
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()

            trySend(bookings)
        }
        awaitClose { listener.remove() }
    }

    suspend fun cancelBooking(booking: Booking): Result<Unit> = runCatching {
        firestore.collection("bookings").document(booking.id).update("status", BookingStatus.CANCELLED.name).await()
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Result<Unit> = runCatching {
        firestore.collection("bookings").document(bookingId).update("status", status.name).await()
    }

    suspend fun isTimeSlotAvailable(psUnitId: String, startMillis: Long, endMillis: Long): Boolean {
        return try {
            val snap = firestore.collection("bookings").whereEqualTo("psUnitId", psUnitId).get().await()
            snap.documents.none { doc ->
                val status = doc.getString("status") ?: "CANCELLED"
                if (status == BookingStatus.CANCELLED.name || status == BookingStatus.COMPLETED.name) return@none false
                val s = doc.getLong("startTimeMillis") ?: 0L
                val e = doc.getLong("endTimeMillis") ?: 0L
                startMillis < e && endMillis > s
            }
        } catch (e: Exception) { true }
    }

    suspend fun getBookedSlotsByUnit(psUnitId: String, date: String): List<Pair<Long, Long>> {
        return try {
            firestore.collection("bookings")
                .whereEqualTo("psUnitId", psUnitId)
                .whereEqualTo("bookingDate", date)
                .get().await().documents.mapNotNull { doc ->
                    val status = doc.getString("status") ?: "CANCELLED"
                    if (status == BookingStatus.CANCELLED.name || status == BookingStatus.COMPLETED.name) return@mapNotNull null
                    val s = doc.getLong("startTimeMillis") ?: return@mapNotNull null
                    val e = doc.getLong("endTimeMillis") ?: return@mapNotNull null
                    Pair(s, e)
                }
        } catch (e: Exception) { emptyList() }
    }

    // ── Reviews ────────────────────────────────────────
    suspend fun addReview(review: Review): Result<Unit> = runCatching {
        val ref = firestore.collection("reviews").document()
        val withId = review.copy(id = ref.id)
        ref.set(withId.toMap()).await()
    }

    fun getReviewsByUnit(psUnitId: String): Flow<List<Review>> = callbackFlow {
        val listener = firestore.collection("reviews")
            .whereEqualTo("psUnitId", psUnitId)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val reviews = snap?.documents?.mapNotNull { doc ->
                    try {
                        Review(
                            id = doc.id,
                            psUnitId = doc.getString("psUnitId") ?: "",
                            psUnitName = doc.getString("psUnitName") ?: "",
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            rating = (doc.getDouble("rating") ?: 5.0).toFloat(),
                            comment = doc.getString("comment") ?: "",
                            bookingId = doc.getString("bookingId") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    } catch (e: Exception) { null }
                }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
                trySend(reviews)
            }
        awaitClose { listener.remove() }
    }

    suspend fun hasUserBookedUnit(userId: String, psUnitId: String): Boolean {
        return try {
            val snap = firestore.collection("bookings")
                .whereEqualTo("userId", userId)
                .whereEqualTo("psUnitId", psUnitId)
                .get().await()
            snap.documents.any { doc ->
                val status = doc.getString("status") ?: ""
                status == BookingStatus.COMPLETED.name || status == BookingStatus.CONFIRMED.name
            }
        } catch (e: Exception) { false }
    }

    suspend fun hasUserReviewedUnit(userId: String, psUnitId: String): Boolean {
        return try {
            val snap = firestore.collection("reviews")
                .whereEqualTo("userId", userId)
                .whereEqualTo("psUnitId", psUnitId)
                .get().await()
            snap.documents.isNotEmpty()
        } catch (e: Exception) { false }
    }
}
