package com.playzone.booking.data.local

import androidx.room.*
import com.playzone.booking.data.model.Booking
import com.playzone.booking.data.model.BookingStatus
import kotlinx.coroutines.flow.Flow

// ── Type Converters ────────────────────────────────────────────
class Converters {
    @TypeConverter
    fun fromStatus(status: BookingStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): BookingStatus = BookingStatus.valueOf(value)
}

// ── DAO ────────────────────────────────────────────────────────
@Dao
interface BookingDao {

    @Query("SELECT * FROM bookings WHERE userId = :userId ORDER BY createdAt DESC")
    fun getBookingsByUser(userId: String): Flow<List<Booking>>

    @Query("SELECT * FROM bookings ORDER BY createdAt DESC")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE status = 'ACTIVE' OR status = 'CONFIRMED'")
    fun getActiveBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getBookingById(id: String): Booking?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: Booking)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bookings: List<Booking>)

    @Update
    suspend fun updateBooking(booking: Booking)

    @Delete
    suspend fun deleteBooking(booking: Booking)

    @Query("DELETE FROM bookings WHERE userId = :userId")
    suspend fun deleteUserBookings(userId: String)
}

// ── Database ───────────────────────────────────────────────────
@Database(
    entities = [Booking::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookingDao(): BookingDao
}