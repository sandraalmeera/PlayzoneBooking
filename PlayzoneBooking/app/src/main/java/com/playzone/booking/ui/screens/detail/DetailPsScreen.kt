package com.playzone.booking.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playzone.booking.data.model.Review
import com.playzone.booking.ui.theme.*
import com.playzone.booking.ui.viewmodel.AuthViewModel
import com.playzone.booking.ui.viewmodel.BookingViewModel
import com.playzone.booking.ui.viewmodel.ReviewViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailPsScreen(
    psUnitId: String,
    onBack: () -> Unit,
    onBooking: (String) -> Unit,
    bookingViewModel: BookingViewModel = hiltViewModel(),
    reviewViewModel: ReviewViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val psUnits by bookingViewModel.psUnits.collectAsStateWithLifecycle()
    val psUnit = psUnits.find { it.id == psUnitId }
    val reviews by reviewViewModel.getReviews(psUnitId).collectAsStateWithLifecycle(initialValue = emptyList())
    val reviewUiState by reviewViewModel.uiState.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userProfile?.uid) {
        userProfile?.uid?.let { uid ->
            reviewViewModel.checkCanReview(uid, psUnitId)
        }
    }

    val avgRating = if (reviews.isEmpty()) 0f else reviews.map { it.rating }.average().toFloat()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail PS", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            if (psUnit != null) {
                Column(Modifier.fillMaxWidth().background(SurfaceDark).padding(16.dp)) {
                    if (reviewUiState.canReview) {
                        OutlinedButton(
                            onClick = { showReviewDialog = true },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent)
                        ) {
                            Icon(Icons.Default.Star, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tulis Review")
                        }
                    }
                    Button(
                        onClick = { onBooking(psUnitId) },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                    ) {
                        Text("Booking Sekarang", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (psUnit == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PurpleLight)
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Header
            item {
                Box(
                    Modifier.fillMaxWidth().height(180.dp)
                        .background(Brush.verticalGradient(listOf(PurplePrimary.copy(0.8f), DarkBackground)))
                ) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Gamepad, null, tint = TextPrimary, modifier = Modifier.size(56.dp))
                        Text(psUnit.name, fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 20.sp)
                        Text(psUnit.type, color = TextSecondary, fontSize = 13.sp)
                    }
                    Surface(Modifier.align(Alignment.TopEnd).padding(12.dp), shape = RoundedCornerShape(8.dp),
                        color = (if (psUnit.isAvailable) GreenSuccess else GoldAccent).copy(0.2f)) {
                        Text(if (psUnit.isAvailable) "✅ Tersedia" else "⏳ Sedang Dipakai",
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = if (psUnit.isAvailable) GreenSuccess else GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Harga + Rating
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Harga Sewa", color = TextSecondary, fontSize = 12.sp)
                            Text("Rp ${"%,d".format(psUnit.pricePerHour)}", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text("per jam", color = TextSecondary, fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Rating", color = TextSecondary, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                Text(if (reviews.isEmpty()) "-" else "%.1f".format(avgRating), color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            }
                            Text("${reviews.size} ulasan", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Fasilitas
            if (psUnit.facilities.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Fasilitas", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(psUnit.facilities) { f ->
                                Surface(shape = RoundedCornerShape(8.dp), color = PurplePrimary.copy(0.2f)) {
                                    Text("✓ $f", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = PurpleLight, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            // Reviews section
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Ulasan Pelanggan", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    if (reviewUiState.hasReviewed) {
                        Text("✅ Sudah review", color = GreenSuccess, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (reviews.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Belum ada ulasan. Jadilah yang pertama!", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                items(reviews) { review ->
                    ReviewCard(review = review, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }

            item { Spacer(Modifier.height(120.dp)) }
        }
    }

    // Dialog tulis review
    if (showReviewDialog) {
        WriteReviewDialog(
            psUnitId = psUnitId,
            psUnitName = psUnit?.name ?: "",
            userId = userProfile?.uid ?: "",
            userName = userProfile?.name ?: "",
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                reviewViewModel.submitReview(psUnitId, psUnit?.name ?: "", userProfile?.uid ?: "", userProfile?.name ?: "", rating, comment)
                showReviewDialog = false
            }
        )
    }
}

@Composable
fun ReviewCard(review: Review, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(PurplePrimary.copy(0.3f)), contentAlignment = Alignment.Center) {
                    Text(review.userName.take(1).uppercase(), color = PurpleLight, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(review.userName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                    Row {
                        repeat(5) { i ->
                            Icon(if (i < review.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder, null,
                                tint = GoldAccent, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            if (review.comment.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(review.comment, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun WriteReviewDialog(psUnitId: String, psUnitName: String, userId: String, userName: String, onDismiss: () -> Unit, onSubmit: (Float, String) -> Unit) {
    var rating by remember { mutableFloatStateOf(5f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Tulis Ulasan", fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Unit: $psUnitName", color = TextSecondary, fontSize = 13.sp)
                Text("Rating:", color = TextPrimary, fontWeight = FontWeight.Medium)
                Row {
                    (1..5).forEach { i ->
                        IconButton(onClick = { rating = i.toFloat() }, modifier = Modifier.size(36.dp)) {
                            Icon(if (i <= rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder, null, tint = GoldAccent)
                        }
                    }
                }
                OutlinedTextField(
                    value = comment, onValueChange = { comment = it },
                    label = { Text("Komentar (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurpleLight)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(rating, comment) }, colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) {
                Text("Kirim Ulasan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}