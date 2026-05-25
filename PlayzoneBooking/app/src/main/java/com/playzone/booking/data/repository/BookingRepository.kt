package com.playzone.booking.data.repository

import com.playzone.booking.data.local.BookingDao
import com.playzone.booking.data.model.*
import com.playzone.booking.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookingRepository @Inject constructor(
    private val firebaseService: FirebaseService,
    private val bookingDao: BookingDao
) {
    val currentUser get() = firebaseService.currentUser

    suspend fun signIn(email: String, password: String) = firebaseService.signIn(email, password)
    suspend fun register(email: String, password: String, name: String, phone: String) =
        firebaseService.register(email, password, name, phone)
    fun signOut() = firebaseService.signOut()
    suspend fun getUserProfile(uid: String) = firebaseService.getUserProfile(uid)
    suspend fun updateFcmToken(uid: String) = firebaseService.updateFcmToken(uid)

    fun getPsUnits(): Flow<List<PsUnit>> = firebaseService.getPsUnitsFlow()

    // Snack menu real-time dari Firestore (dikelola admin)
    fun getSnackMenu(): Flow<List<SnackMenuItem>> = firebaseService.getSnackMenuFlow()

    // Submit snack order ke Firestore → masuk ke admin real-time
    suspend fun createSnackOrder(order: SnackOrder): Result<String> =
        firebaseService.createSnackOrder(order)

    fun getMyBookings(userId: String): Flow<List<Booking>> =
        firebaseService.getBookingsFlow(userId).onEach { bookingDao.insertAll(it) }

    fun getAllBookings(): Flow<List<Booking>> =
        firebaseService.getBookingsFlow(null).onEach { bookingDao.insertAll(it) }

    fun getCachedBookings(userId: String): Flow<List<Booking>> =
        bookingDao.getBookingsByUser(userId)

    suspend fun createBookingFull(booking: Booking): Result<Unit> =
        firebaseService.createBooking(booking).also {
            if (it.isSuccess) bookingDao.insertBooking(booking)
        }

    suspend fun cancelBooking(booking: Booking): Result<Unit> =
        firebaseService.cancelBooking(booking).also {
            if (it.isSuccess) bookingDao.updateBooking(booking.copy(status = BookingStatus.CANCELLED))
        }

    suspend fun updateStatus(bookingId: String, status: BookingStatus) =
        firebaseService.updateBookingStatus(bookingId, status)

    suspend fun isTimeSlotAvailable(psUnitId: String, startMillis: Long, endMillis: Long) =
        firebaseService.isTimeSlotAvailable(psUnitId, startMillis, endMillis)

    suspend fun getBookedSlotsByUnit(psUnitId: String, date: String) =
        firebaseService.getBookedSlotsByUnit(psUnitId, date)

    // Reviews
    suspend fun addReview(review: Review) = firebaseService.addReview(review)
    fun getReviewsByUnit(psUnitId: String) = firebaseService.getReviewsByUnit(psUnitId)
    suspend fun hasUserBookedUnit(userId: String, psUnitId: String) =
        firebaseService.hasUserBookedUnit(userId, psUnitId)
    suspend fun hasUserReviewedUnit(userId: String, psUnitId: String) =
        firebaseService.hasUserReviewedUnit(userId, psUnitId)
}
