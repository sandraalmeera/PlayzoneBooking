package com.playzone.booking.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.playzone.booking.data.model.*
import com.playzone.booking.data.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class BookingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val createdBookingId: String? = null
)

data class SnackOrderUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val orderId: String? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookingViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    private val _snackOrderState = MutableStateFlow(SnackOrderUiState())
    val snackOrderState: StateFlow<SnackOrderUiState> = _snackOrderState.asStateFlow()

    private val _bookedSlots = MutableStateFlow<List<Pair<Long, Long>>>(emptyList())
    val bookedSlots: StateFlow<List<Pair<Long, Long>>> = _bookedSlots.asStateFlow()

    // userId sebagai StateFlow — diupdate via FirebaseAuth listener
    private val _currentUserId = MutableStateFlow(repository.currentUser?.uid ?: "")
    val currentUserIdFlow: StateFlow<String> = _currentUserId.asStateFlow()

    val currentUserId: String get() = repository.currentUser?.uid ?: ""
    val currentUserName: String
        get() = repository.currentUser?.displayName
            ?: repository.currentUser?.email?.substringBefore("@") ?: "User"

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            _currentUserId.value = auth.currentUser?.uid ?: ""
        }
    }

    val psUnits: StateFlow<List<PsUnit>> = repository.getPsUnits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val snackMenu: StateFlow<List<SnackMenuItem>> = repository.getSnackMenu()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // FIX: myBookings sebagai StateFlow permanen di ViewModel
    // flatMapLatest: setiap kali userId berubah, subscribe ke Flow booking yang baru
    // StateFlow ini TIDAK pernah cancel selama ViewModel hidup
    val myBookings: StateFlow<List<Booking>> = _currentUserId
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(emptyList())
            else repository.getMyBookings(uid).catch { emit(emptyList()) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun getAllBookings(): Flow<List<Booking>> = repository.getAllBookings()
        .catch { emit(emptyList()) }

    // Tetap ada untuk kompatibilitas, tapi MyBookingScreen pakai myBookings langsung
    fun getMyBookings(userId: String): Flow<List<Booking>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return repository.getMyBookings(userId).catch { emit(emptyList()) }
    }

    fun loadBookedSlots(psUnitId: String, date: String) {
        viewModelScope.launch {
            _bookedSlots.value = repository.getBookedSlotsByUnit(psUnitId, date)
        }
    }

    fun updateBookingStatus(bookingId: String, status: BookingStatus) {
        viewModelScope.launch { repository.updateStatus(bookingId, status) }
    }

    fun createBooking(
        psUnit: PsUnit, date: String, startTime: String, startMillis: Long,
        durationHours: Int, paymentMethod: PaymentMethod, userPhone: String
    ) {
        viewModelScope.launch {
            _uiState.value = BookingUiState(isLoading = true)

            val uid = repository.currentUser?.uid ?: ""
            if (uid.isEmpty()) {
                _uiState.value = BookingUiState(error = "Sesi habis. Silakan login ulang.")
                return@launch
            }

            val endMillis = startMillis + (durationHours * 3600 * 1000L)
            val isAvailable = try {
                repository.isTimeSlotAvailable(psUnit.id, startMillis, endMillis)
            } catch (e: Exception) { true }
            if (!isAvailable) {
                _uiState.value = BookingUiState(error = "Jadwal ini sudah dipesan! Pilih jam lain.")
                return@launch
            }

            val fcmToken = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrDefault("")
            val bookingId = "PZ${UUID.randomUUID().toString().take(6).uppercase()}"
            val userName = repository.currentUser?.displayName
                ?: repository.currentUser?.email?.substringBefore("@") ?: "User"

            val booking = Booking(
                id = bookingId, userId = uid, userName = userName, userPhone = userPhone,
                psUnitId = psUnit.id, psUnitName = psUnit.name, psUnitType = psUnit.type,
                bookingDate = date, startTime = startTime, startTimeMillis = startMillis,
                endTimeMillis = endMillis, durationHours = durationHours,
                totalPrice = psUnit.pricePerHour * durationHours, status = BookingStatus.CONFIRMED,
                paymentMethod = paymentMethod, createdAt = System.currentTimeMillis(), fcmToken = fcmToken
            )

            repository.createBookingFull(booking)
                .onSuccess { _uiState.value = BookingUiState(successMessage = "Booking berhasil!", createdBookingId = bookingId) }
                .onFailure { _uiState.value = BookingUiState(error = "Booking gagal: ${it.message}") }
        }
    }

    fun cancelBooking(booking: Booking) {
        viewModelScope.launch {
            _uiState.value = BookingUiState(isLoading = true)
            repository.cancelBooking(booking)
                .onSuccess { _uiState.value = BookingUiState(successMessage = "Booking dibatalkan") }
                .onFailure { _uiState.value = BookingUiState(error = it.message) }
        }
    }

    fun submitSnackOrder(
        bookingId: String,
        psUnitName: String,
        cartItems: List<SnackCartItem>
    ) {
        viewModelScope.launch {
            if (cartItems.isEmpty()) {
                _snackOrderState.value = SnackOrderUiState(error = "Pilih minimal 1 item!")
                return@launch
            }
            val uid = repository.currentUser?.uid ?: ""
            if (uid.isEmpty()) {
                _snackOrderState.value = SnackOrderUiState(error = "Sesi habis. Silakan login ulang.")
                return@launch
            }

            _snackOrderState.value = SnackOrderUiState(isLoading = true)

            val orderItems = cartItems.map { cart ->
                SnackOrderItem(
                    name = cart.menuItem.name,
                    emoji = cart.menuItem.emoji,
                    qty = cart.qty,
                    price = cart.menuItem.price
                )
            }
            val totalPrice = cartItems.sumOf { it.menuItem.price * it.qty }
            val userName = repository.currentUser?.displayName
                ?: repository.currentUser?.email?.substringBefore("@") ?: "User"

            val order = SnackOrder(
                bookingId = bookingId,
                userId = uid,
                userName = userName,
                psUnitName = psUnitName,
                items = orderItems,
                totalPrice = totalPrice,
                status = SnackOrderStatus.WAITING,
                createdAt = System.currentTimeMillis()
            )

            repository.createSnackOrder(order)
                .onSuccess { orderId ->
                    _snackOrderState.value = SnackOrderUiState(isSuccess = true, orderId = orderId)
                }
                .onFailure {
                    _snackOrderState.value = SnackOrderUiState(error = "Gagal mengirim pesanan: ${it.message}")
                }
        }
    }

    fun clearSnackOrderState() {
        _snackOrderState.value = SnackOrderUiState()
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            error = null, successMessage = null, createdBookingId = null, isLoading = false
        )
    }
}