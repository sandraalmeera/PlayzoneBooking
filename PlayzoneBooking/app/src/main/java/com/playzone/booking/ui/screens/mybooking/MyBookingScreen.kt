package com.playzone.booking.ui.screens.mybooking

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playzone.booking.data.model.Booking
import com.playzone.booking.data.model.BookingStatus
import com.playzone.booking.service.LocalNotificationHelper
import com.playzone.booking.ui.theme.*
import com.playzone.booking.ui.viewmodel.BookingViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingScreen(
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userId by viewModel.currentUserIdFlow.collectAsStateWithLifecycle()
    val bookings by viewModel.myBookings.collectAsStateWithLifecycle()
    var cancelTarget by remember { mutableStateOf<Booking?>(null) }
    var selectedFilter by remember { mutableStateOf("Semua") }

    val filters = listOf("Semua", "Pending", "Aktif", "Selesai", "Batal")

    val filteredBookings = when (selectedFilter) {
        "Pending"   -> bookings.filter { it.status == BookingStatus.PENDING || it.status == BookingStatus.CONFIRMED }
        "Aktif"     -> bookings.filter { it.status == BookingStatus.ACTIVE }
        "Selesai"   -> bookings.filter { it.status == BookingStatus.COMPLETED }
        "Batal"     -> bookings.filter { it.status == BookingStatus.CANCELLED }
        else        -> bookings
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Riwayat Booking", color = TextPrimary, fontWeight = FontWeight.Bold)
                        if (bookings.isNotEmpty()) {
                            Text("${bookings.size} booking", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Filter chips ───────────────────────────────────────
            if (bookings.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filters.forEach { filter ->
                        val count = when (filter) {
                            "Pending"   -> bookings.count { it.status == BookingStatus.PENDING || it.status == BookingStatus.CONFIRMED }
                            "Aktif"     -> bookings.count { it.status == BookingStatus.ACTIVE }
                            "Selesai"   -> bookings.count { it.status == BookingStatus.COMPLETED }
                            "Batal"     -> bookings.count { it.status == BookingStatus.CANCELLED }
                            else        -> bookings.size
                        }
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    if (count > 0) "$filter ($count)" else filter,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PurplePrimary,
                                selectedLabelColor = TextPrimary,
                                containerColor = CardDark,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedFilter == filter,
                                selectedBorderColor = PurpleLight,
                                borderColor = SurfaceDark
                            )
                        )
                    }
                }
            }

            when {
                // Loading — userId belum tersedia
                userId.isBlank() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PurpleLight)
                            Spacer(Modifier.height(12.dp))
                            Text("Memuat riwayat booking...", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }

                // Empty state — belum ada booking sama sekali
                bookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(100.dp)
                                    .background(
                                        Brush.radialGradient(listOf(PurplePrimary.copy(0.2f), DarkBackground)),
                                        shape = RoundedCornerShape(50.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.SportsEsports,
                                    null,
                                    tint = PurpleLight,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Belum ada booking", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Yuk booking PS sekarang!", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }

                // Filter kosong
                filteredBookings.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FilterList, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Tidak ada booking $selectedFilter", color = TextSecondary, fontSize = 15.sp)
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredBookings, key = { it.id }) { booking ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically()
                            ) {
                                BookingCard(
                                    booking = booking,
                                    onCancel = { cancelTarget = booking }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialog konfirmasi batalkan ─────────────────────────────────
    cancelTarget?.let { booking ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            icon = { Icon(Icons.Default.Warning, null, tint = RedCancel) },
            title = { Text("Batalkan Booking?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Booking ${booking.psUnitName}\n" +
                    "Tanggal ${booking.bookingDate} jam ${booking.startTime}\n\n" +
                    "Booking ini akan dibatalkan dan tidak bisa dikembalikan."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelBooking(booking)
                        LocalNotificationHelper.sendBookingCancelledNotif(context, booking.psUnitName)
                        cancelTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedCancel)
                ) {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Batalkan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { cancelTarget = null }) { Text("Tidak") }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun BookingCard(booking: Booking, onCancel: () -> Unit) {
    val currency = NumberFormat.getNumberInstance(Locale("in", "ID"))
    val (statusColor, statusLabel, statusIcon) = when (booking.status) {
        BookingStatus.CONFIRMED -> Triple(GreenSuccess, "Dikonfirmasi", Icons.Default.CheckCircle)
        BookingStatus.ACTIVE    -> Triple(PurpleLight, "Sedang Aktif", Icons.Default.PlayCircle)
        BookingStatus.COMPLETED -> Triple(TextSecondary, "Selesai", Icons.Default.Done)
        BookingStatus.CANCELLED -> Triple(RedCancel, "Dibatalkan", Icons.Default.Cancel)
        BookingStatus.PENDING   -> Triple(GoldAccent, "Menunggu", Icons.Default.HourglassEmpty)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // ── Header: unit name + status badge ──────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(PurplePrimary.copy(0.2f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gamepad, null, tint = PurpleLight, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            booking.psUnitName,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "ID: ${booking.id}",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(statusLabel, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = PurplePrimary.copy(0.15f))

            // ── Info rows ──────────────────────────────────────────
            BookingInfoRow(Icons.Default.CalendarToday, "Tanggal", booking.bookingDate)
            BookingInfoRow(Icons.Default.AccessTime, "Jam", "${booking.startTime} (${booking.durationHours} jam)")
            BookingInfoRow(Icons.Default.AttachMoney, "Harga Sewa", "Rp ${currency.format(booking.totalPrice)}")
            BookingInfoRow(
                Icons.Default.Payment,
                "Pembayaran",
                if (booking.paymentMethod.name == "COD") "Bayar di Tempat" else "Transfer Bank"
            )

            // ── Tombol batalkan (hanya untuk pending/confirmed) ────
            if (booking.status == BookingStatus.CONFIRMED || booking.status == BookingStatus.PENDING) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedCancel),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Batalkan Booking", fontSize = 13.sp)
                }
            }

            // ── Label sesi aktif ───────────────────────────────────
            if (booking.status == BookingStatus.ACTIVE) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = PurpleLight.copy(0.12f)
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PlayCircle, null, tint = PurpleLight, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sesi sedang berjalan", color = PurpleLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun BookingInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextSecondary, fontSize = 12.sp)
        }
        Text(
            value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
