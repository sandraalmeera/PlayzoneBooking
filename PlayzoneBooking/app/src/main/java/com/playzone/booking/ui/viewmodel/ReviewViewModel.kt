package com.playzone.booking.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.playzone.booking.data.model.Review
import com.playzone.booking.data.repository.BookingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val canReview: Boolean = false,
    val hasReviewed: Boolean = false
)

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val repository: BookingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun getReviews(psUnitId: String): Flow<List<Review>> =
        repository.getReviewsByUnit(psUnitId)

    fun checkCanReview(userId: String, psUnitId: String) {
        viewModelScope.launch {
            val hasBooked = repository.hasUserBookedUnit(userId, psUnitId)
            val hasReviewed = repository.hasUserReviewedUnit(userId, psUnitId)
            _uiState.value = _uiState.value.copy(
                canReview = hasBooked && !hasReviewed,
                hasReviewed = hasReviewed
            )
        }
    }

    fun submitReview(
        psUnitId: String,
        psUnitName: String,
        userId: String,
        userName: String,
        rating: Float,
        comment: String,
        bookingId: String = ""
    ) {
        viewModelScope.launch {
            _uiState.value = ReviewUiState(isLoading = true)
            val review = Review(
                psUnitId = psUnitId,
                psUnitName = psUnitName,
                userId = userId,
                userName = userName,
                rating = rating,
                comment = comment,
                bookingId = bookingId
            )
            repository.addReview(review)
                .onSuccess { _uiState.value = ReviewUiState(success = true, hasReviewed = true) }
                .onFailure { _uiState.value = ReviewUiState(error = it.message ?: "Gagal kirim review") }
        }
    }

    fun clearState() { _uiState.value = ReviewUiState() }
}