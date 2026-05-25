package com.playzone.booking.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.playzone.booking.R
import com.playzone.booking.data.model.BookingStatus
import com.playzone.booking.data.model.PsUnit
import com.playzone.booking.ui.theme.*
import com.playzone.booking.ui.viewmodel.AuthViewModel
import com.playzone.booking.ui.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDetailPs: (String) -> Unit,
    onMyBookings: () -> Unit,
    onProfile: () -> Unit,
    onSnackOrder: () -> Unit,
    onLogout: () -> Unit,
    bookingViewModel: BookingViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val psUnits by bookingViewModel.psUnits.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val allBookings by bookingViewModel.getAllBookings().collectAsStateWithLifecycle(initialValue = emptyList())
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("Semua") }
    var selectedSort by remember { mutableStateOf("Default") }

    val types = listOf("Semua", "PS4", "PS5")
    val sorts = listOf("Default", "Harga ↑", "Harga ↓")
    val today = remember { SimpleDateFormat("dd/MM/yyyy", Locale("in", "ID")).format(Date()) }

    fun getBusyHours(psUnitId: String): List<Int> {
        return allBookings.filter { booking ->
            booking.psUnitId == psUnitId &&
                    booking.bookingDate == today &&
                    booking.status != BookingStatus.CANCELLED &&
                    booking.status != BookingStatus.COMPLETED
        }.flatMap { booking ->
            val startHour = booking.startTime.split(":")[0].toIntOrNull() ?: 0
            (startHour until startHour + booking.durationHours).toList()
        }
    }

    fun getAvailableHoursToday(psUnitId: String): List<String> {
        val busyHours = getBusyHours(psUnitId)
        return (8..22).filter { it !in busyHours }.map { "$it:00" }
    }

    fun canBook(psUnitId: String): Boolean = getAvailableHoursToday(psUnitId).isNotEmpty()

    val filteredUnits = psUnits
        .filter { it.type != "PS3" }
        .filter { if (selectedType == "Semua") true else it.type == selectedType }
        .let { list ->
            when (selectedSort) {
                "Harga ↑" -> list.sortedBy { it.pricePerHour }
                "Harga ↓" -> list.sortedByDescending { it.pricePerHour }
                else -> list
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // ── Logo kecil di TopAppBar ────────────────────────────
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(R.drawable.logo_playzone)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Halo, ${userProfile?.name ?: "Gamer"} 👋",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Text("Mau main PS hari ini?", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onMyBookings) {
                        Icon(Icons.Default.History, contentDescription = "Riwayat Booking", tint = PurpleLight)
                    }
                    IconButton(onClick = onProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profil", tint = TextSecondary)
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Keluar", tint = RedCancel.copy(0.7f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { BannerPromo() }

            item { SnackOrderBanner(onClick = onSnackOrder) }

            item {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShortcutButton(
                        icon = Icons.Default.History,
                        label = "Riwayat Booking",
                        modifier = Modifier.weight(1f),
                        onClick = onMyBookings
                    )
                    ShortcutButton(
                        icon = Icons.Default.Person,
                        label = "Profil Saya",
                        modifier = Modifier.weight(1f),
                        onClick = onProfile
                    )
                }
            }

            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("🎮 Pilih Unit PS", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("Tipe PS", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        types.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurplePrimary,
                                    selectedLabelColor = TextPrimary
                                )
                            )
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Urutkan", fontWeight = FontWeight.SemiBold, color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sorts.forEach { sort ->
                            FilterChip(
                                selected = selectedSort == sort,
                                onClick = { selectedSort = sort },
                                label = { Text(sort) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldAccent,
                                    selectedLabelColor = DarkBackground
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val bookableCount = filteredUnits.count { canBook(it.id) }
                    Text(
                        "Tersedia untuk booking: $bookableCount/${filteredUnits.size} unit",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenSuccess
                    )
                }
            }

            if (filteredUnits.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PurpleLight)
                    }
                }
            } else {
                items(filteredUnits, key = { it.id }) { unit ->
                    val availableHours = getAvailableHoursToday(unit.id)
                    val isCurrentlyBusy = !unit.isAvailable
                    PsUnitCard(
                        unit = unit,
                        availableHours = availableHours,
                        isCurrentlyBusy = isCurrentlyBusy,
                        onClick = { onDetailPs(unit.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Yakin ingin keluar dari akun?") },
            confirmButton = {
                TextButton(onClick = {
                    authViewModel.logout()
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Ya, Logout", color = RedCancel) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun BannerPromo() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(PurplePrimary, PurpleLight)))
            .padding(20.dp)
    ) {
        Column {
            Text("🎮 Promo Weekday!", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 18.sp)
            Text("Booking PS4 jam 10-14, diskon 20%", color = TextPrimary.copy(0.85f), fontSize = 13.sp)
        }
    }
}

@Composable
fun SnackOrderBanner(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GoldAccent.copy(0.15f)),
                contentAlignment = Alignment.Center
            ) { Text("🍟", fontSize = 26.sp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Order Snack & Minuman", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                Text("Pesan makanan tanpa ke kasir, diantar ke tempatmu!", color = TextSecondary, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = GoldAccent, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun ShortcutButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = PurpleLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PsUnitCard(
    unit: PsUnit,
    availableHours: List<String>,
    isCurrentlyBusy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasAvailableSlots = availableHours.isNotEmpty()
    val statusColor = when {
        hasAvailableSlots && !isCurrentlyBusy -> GreenSuccess
        hasAvailableSlots && isCurrentlyBusy  -> GoldAccent
        else -> RedCancel
    }
    val statusLabel = when {
        hasAvailableSlots && !isCurrentlyBusy -> "Tersedia"
        hasAvailableSlots && isCurrentlyBusy  -> "Ada Slot"
        else -> "Penuh"
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.radialGradient(listOf(PurpleLight.copy(0.4f), CardDark))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Gamepad,
                        null,
                        tint = if (hasAvailableSlots) PurpleLight else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(unit.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text(unit.type, color = TextSecondary, fontSize = 12.sp)
                    Text("Rp ${"%,d".format(unit.pricePerHour)}/jam", color = GoldAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isCurrentlyBusy && hasAvailableSlots) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = GoldAccent.copy(0.1f)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = GoldAccent, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sedang dipakai, tapi masih bisa booking jam lain", color = GoldAccent, fontSize = 11.sp)
                    }
                }
            }

            if (hasAvailableSlots) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = PurplePrimary.copy(0.2f))
                Spacer(Modifier.height(8.dp))
                Text("⏰ Jam Tersedia Hari Ini:", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableHours.take(6).forEach { hour ->
                        Surface(shape = RoundedCornerShape(6.dp), color = GreenSuccess.copy(0.15f)) {
                            Text(
                                hour,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = GreenSuccess, fontSize = 11.sp, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (availableHours.size > 6) {
                        Surface(shape = RoundedCornerShape(6.dp), color = TextSecondary.copy(0.15f)) {
                            Text(
                                "+${availableHours.size - 6} lagi",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = TextSecondary, fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Ketuk untuk detail & booking →", color = PurpleLight, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            } else {
                Spacer(Modifier.height(6.dp))
                Text("Semua slot hari ini sudah penuh", color = RedCancel.copy(0.7f), fontSize = 11.sp)
                Text("Ketuk untuk cek jadwal hari lain →", color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}
