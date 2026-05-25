package com.playzone.booking.data.model

import com.google.firebase.firestore.DocumentId

// ── Review Model ───────────────────────────────────────
data class Review(
    @DocumentId val id: String = "",
    val psUnitId: String = "",
    val psUnitName: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Float = 5f,         // 1-5 bintang
    val comment: String = "",
    val bookingId: String = "",     // hanya bisa review kalau pernah booking
    val createdAt: Long = System.currentTimeMillis()
)

fun Review.toMap(): Map<String, Any> = mapOf(
    "id" to id,
    "psUnitId" to psUnitId,
    "psUnitName" to psUnitName,
    "userId" to userId,
    "userName" to userName,
    "rating" to rating,
    "comment" to comment,
    "bookingId" to bookingId,
    "createdAt" to createdAt
)