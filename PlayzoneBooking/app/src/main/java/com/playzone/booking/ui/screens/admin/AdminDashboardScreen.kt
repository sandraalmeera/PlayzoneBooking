package com.playzone.booking.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playzone.booking.data.model.Booking
import com.playzone.booking.data.model.BookingStatus
import com.playzone.booking.ui.theme.*
import com.playzone.booking.ui.viewmodel.AuthViewModel
import com.playzone.booking.ui.viewmodel.BookingViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    bookingViewModel: BookingViewModel = hiltViewModel()
) {
    val allBookings by bookingViewModel.getAllBookings()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val psUnits by bookingViewModel.psUnits.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf("Semua") }

    val statusFilters = listOf("Semua", "PENDING", "CONFIRMED", "ACTIVE", "COMPLETED", "CANCELLED")
    val filteredBookings = if (selectedStatus == "Semua") allBookings
    else allBookings.filter { it.status.name == selectedStatus }

    // Statistik
    val totalRevenue = allBookings.filter {
        it.status == BookingStatus.CONFIRMED || it.status == BookingStatus.COMPLETED
    }.sumOf { it.totalPrice }
    val totalBookings = allBookings.size
    val activeBookings = allBookings.count { it.status == BookingStatus.ACTIVE || it.status == BookingStatus.CONFIRMED }
    val availableUnits = psUnits.count { it.isAvailable }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Admin Dashboard", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Halo, ${userProfile?.name ?: "Admin"} 👋", fontSize = 12.sp, color = TextSecondary)
                    }
                },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, null, tint = RedCancel.copy(0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, null) },
                    label = { Text("Ringkasan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.List, null) },
                    label = { Text("Semua Booking") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Gamepad, null) },
                    label = { Text("Unit PS") }
                )
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> AdminSummaryTab(
                modifier = Modifier.padding(padding),
                totalRevenue = totalRevenue,
                totalBookings = totalBookings,
                activeBookings = activeBookings,
                availableUnits = availableUnits,
                recentBookings = allBookings.take(5),
                bookingViewModel = bookingViewModel
            )
            1 -> AdminBookingsTab(
                modifier = Modifier.padding(padding),
                bookings = filteredBookings,
                statusFilters = statusFilters,
                selectedStatus = selectedStatus,
                onStatusSelected = { selectedStatus = it },
                bookingViewModel = bookingViewModel
            )
            2 -> AdminUnitsTab(
                modifier = Modifier.padding(padding),
                psUnits = psUnits
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Yakin ingin keluar dari akun admin?") },
            confirmButton = {
                TextButton(onClick = { authViewModel.logout(); onLogout() }) {
                    Text("Ya, Logout", color = RedCancel)
                }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") } },
            containerColor = SurfaceDark
        )
    }
}

// ── Tab 1: Ringkasan ───────────────────────────────────
@Composable
fun AdminSummaryTab(
    modifier: Modifier,
    totalRevenue: Int,
    totalBookings: Int,
    activeBookings: Int,
    availableUnits: Int,
    recentBookings: List<Booking>,
    bookingViewModel: BookingViewModel
) {
    val currency = NumberFormat.getNumberInstance(Locale("in", "ID"))

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            // Banner admin
            Box(
                modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(PurplePrimary, PurpleLight)), RoundedCornerShape(16.dp)).padding(20.dp)
            ) {
                Column {
                    Text("📊 Dashboard Admin", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
                    Text("Kelola semua aktivitas Playzone di sini", color = TextPrimary.copy(0.8f), fontSize = 13.sp)
                }
            }
        }

        item {
            // Grid statistik
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.AttachMoney, label = "Total Pendapatan", value = "Rp ${currency.format(totalRevenue)}", color = GoldAccent)
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.BookOnline, label = "Total Booking", value = "$totalBookings", color = PurpleLight)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.PlayCircle, label = "Booking Aktif", value = "$activeBookings", color = GreenSuccess)
                StatCard(modifier = Modifier.weight(1f), icon = Icons.Default.Gamepad, label = "Unit Tersedia", value = "$availableUnits", color = PurplePrimary)
            }
        }

        item {
            Text("Booking Terbaru", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
        }
        items(recentBookings) { booking ->
            AdminBookingCard(booking = booking, bookingViewModel = bookingViewModel)
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, icon: ImageVector, label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Black, color = color, fontSize = 18.sp)
            Text(label, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

// ── Tab 2: Semua Booking ───────────────────────────────
@Composable
fun AdminBookingsTab(
    modifier: Modifier,
    bookings: List<Booking>,
    statusFilters: List<String>,
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    bookingViewModel: BookingViewModel
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Filter status
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(statusFilters) { status ->
                FilterChip(
                    selected = selectedStatus == status,
                    onClick = { onStatusSelected(status) },
                    label = { Text(status, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PurplePrimary,
                        selectedLabelColor = TextPrimary
                    )
                )
            }
        }

        if (bookings.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada booking", color = TextSecondary)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(bookings, key = { it.id }) { booking ->
                    AdminBookingCard(booking = booking, bookingViewModel = bookingViewModel)
                }
            }
        }
    }
}

@Composable
fun AdminBookingCard(booking: Booking, bookingViewModel: BookingViewModel) {
    val currency = NumberFormat.getNumberInstance(Locale("in", "ID"))
    val (statusColor, statusLabel) = when (booking.status) {
        BookingStatus.CONFIRMED -> GreenSuccess to "Dikonfirmasi"
        BookingStatus.ACTIVE    -> PurpleLight to "Aktif"
        BookingStatus.COMPLETED -> TextSecondary to "Selesai"
        BookingStatus.CANCELLED -> RedCancel to "Dibatalkan"
        BookingStatus.PENDING   -> GoldAccent to "Menunggu"
    }

    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(booking.psUnitName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text("${booking.userName} • ${booking.userPhone}", color = TextSecondary, fontSize = 12.sp)
                }
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(0.15f)) {
                    Text(statusLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("📅 ${booking.bookingDate} ${booking.startTime}", color = TextSecondary, fontSize = 12.sp)
                Text("Rp ${currency.format(booking.totalPrice)}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text("ID: ${booking.id}", color = TextSecondary.copy(0.6f), fontSize = 10.sp)

            // Admin bisa ubah status booking
            if (booking.status == BookingStatus.PENDING || booking.status == BookingStatus.CONFIRMED) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (booking.status == BookingStatus.PENDING) {
                        OutlinedButton(
                            onClick = { bookingViewModel.updateBookingStatus(booking.id, BookingStatus.CONFIRMED) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenSuccess)
                        ) { Text("Konfirmasi", fontSize = 12.sp) }
                    }
                    if (booking.status == BookingStatus.CONFIRMED) {
                        OutlinedButton(
                            onClick = { bookingViewModel.updateBookingStatus(booking.id, BookingStatus.ACTIVE) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleLight)
                        ) { Text("Set Aktif", fontSize = 12.sp) }
                        OutlinedButton(
                            onClick = { bookingViewModel.updateBookingStatus(booking.id, BookingStatus.COMPLETED) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) { Text("Selesai", fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}

// ── Tab 3: Unit PS ─────────────────────────────────────
@Composable
fun AdminUnitsTab(modifier: Modifier, psUnits: List<com.playzone.booking.data.model.PsUnit>) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Semua Unit PS (${psUnits.size})", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
        }
        items(psUnits, key = { it.id }) { unit ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gamepad, null, tint = if (unit.isAvailable) PurpleLight else TextSecondary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(unit.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("${unit.type} • Rp ${"%,d".format(unit.pricePerHour)}/jam", color = TextSecondary, fontSize = 12.sp)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = (if (unit.isAvailable) GreenSuccess else RedCancel).copy(0.15f)) {
                        Text(
                            if (unit.isAvailable) "Tersedia" else "Dipakai",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (unit.isAvailable) GreenSuccess else RedCancel,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}