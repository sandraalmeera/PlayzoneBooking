package com.playzone.booking.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playzone.booking.ui.theme.*
import com.playzone.booking.ui.viewmodel.BookingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    psUnitId: String,
    onBack: () -> Unit,
    onNext: (String, String, String, Int) -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val bookedSlots by viewModel.bookedSlots.collectAsStateWithLifecycle()

    val dates = remember {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale("in", "ID"))
        val dayName = SimpleDateFormat("EEE", Locale("in", "ID"))
        val dayNum = SimpleDateFormat("dd", Locale("in", "ID"))
        val monthName = SimpleDateFormat("MMM", Locale("in", "ID"))
        (0..6).map { i ->
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }
            mapOf(
                "full" to sdf.format(cal.time),
                "day" to dayName.format(cal.time),
                "num" to dayNum.format(cal.time),
                "month" to monthName.format(cal.time)
            )
        }
    }

    val allHours = (8..22).map { "$it:00" }
    val durations = listOf(1, 2, 3, 4, 5, 6)

    var selectedDate by remember { mutableStateOf(dates[0]["full"]!!) }
    var selectedHour by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableIntStateOf(1) }
    var errorMessage by remember { mutableStateOf("") }

    // Load booked slots saat tanggal berubah, tapi JANGAN reset jam
    LaunchedEffect(selectedDate, psUnitId) {
        viewModel.loadBookedSlots(psUnitId, selectedDate)
        // TIDAK reset selectedHour di sini supaya jam tetap terpilih
    }

    // Cek apakah jam tertentu sudah dipesan
    fun isHourBooked(hour: String): Boolean {
        val h = hour.split(":")[0].toIntOrNull() ?: return false
        val parts = selectedDate.split("/")
        if (parts.size != 3) return false
        val cal = Calendar.getInstance()
        cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(), h, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val slotStart = cal.timeInMillis
        val slotEnd = slotStart + 3600_000L
        return bookedSlots.any { (start, end) -> slotStart < end && slotEnd > start }
    }

    // Validasi: kalau jam yang dipilih ternyata sudah terpesan di tanggal baru, tampilkan warning
    val selectedHourIsBooked = selectedHour.isNotEmpty() && isHourBooked(selectedHour)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Jadwal", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            Box(
                Modifier.fillMaxWidth().background(SurfaceDark).padding(16.dp)
            ) {
                Button(
                    onClick = {
                        when {
                            selectedHour.isEmpty() ->
                                errorMessage = "Pilih jam mulai terlebih dahulu"
                            selectedHourIsBooked ->
                                errorMessage = "Jam ini sudah dipesan di tanggal ini, pilih jam lain"
                            else -> {
                                errorMessage = ""
                                onNext(psUnitId, selectedDate, selectedHour, selectedDuration)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedHourIsBooked) RedCancel else PurplePrimary
                    )
                ) {
                    Text(
                        if (selectedHourIsBooked) "Jam Ini Sudah Dipesan" else "Lanjut ke Konfirmasi",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!selectedHourIsBooked) {
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, null)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Pilih Tanggal ──────────────────────────────────
            Column {
                Text(
                    "📅 Pilih Tanggal",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dates) { dateMap ->
                        val full = dateMap["full"]!!
                        val isSelected = selectedDate == full
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PurplePrimary else CardDark)
                                .clickable { selectedDate = full; errorMessage = "" }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                dateMap["day"]!!,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                dateMap["num"]!!,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            Text(
                                dateMap["month"]!!,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // ── Warning kalau jam yang dipilih sudah terpesan ──
            if (selectedHourIsBooked) {
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = RedCancel.copy(0.15f))
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            null,
                            tint = RedCancel,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Jam $selectedHour sudah dipesan di tanggal ini. Pilih jam lain atau ganti tanggal.",
                            color = RedCancel,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ── Legenda ────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem(color = PurplePrimary, label = "Dipilih")
                LegendItem(color = GreenSuccess, label = "Tersedia")
                LegendItem(color = RedCancel, label = "Sudah Dipesan")
            }

            // ── Pilih Jam ──────────────────────────────────────
            Column {
                Text(
                    "⏰ Pilih Jam Mulai",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                allHours.chunked(4).forEach { rowHours ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowHours.forEach { hour ->
                            val isSelected = selectedHour == hour
                            val isBooked = isHourBooked(hour)
                            val bgColor = when {
                                isSelected && isBooked -> RedCancel.copy(0.4f)
                                isSelected -> PurplePrimary
                                isBooked -> RedCancel.copy(0.2f)
                                else -> CardDark
                            }
                            val textColor = when {
                                isSelected -> TextPrimary
                                isBooked -> RedCancel
                                else -> GreenSuccess
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedHour = hour
                                        errorMessage = ""
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = bgColor
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        hour,
                                        color = textColor,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                    if (isBooked) {
                                        Text("Penuh", color = RedCancel, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                        repeat(4 - rowHours.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // ── Pilih Durasi ───────────────────────────────────
            Column {
                Text(
                    "⌛ Durasi Main",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    durations.forEach { dur ->
                        val isSelected = selectedDuration == dur
                        Surface(
                            modifier = Modifier.clickable { selectedDuration = dur },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GoldAccent else CardDark
                        ) {
                            Text(
                                "$dur jam",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = if (isSelected) DarkBackground else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ── Ringkasan ──────────────────────────────────────
            if (selectedHour.isNotEmpty()) {
                val endHour = (selectedHour.split(":")[0].toIntOrNull() ?: 8) + selectedDuration
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedHourIsBooked)
                            RedCancel.copy(0.1f) else PurplePrimary.copy(0.15f)
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "📋 Ringkasan Jadwal",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text("📅 Tanggal: $selectedDate", color = TextSecondary, fontSize = 13.sp)
                        Text(
                            "⏰ Mulai: $selectedHour ${if (selectedHourIsBooked) "⚠️ Sudah dipesan!" else ""}",
                            color = if (selectedHourIsBooked) RedCancel else TextSecondary,
                            fontSize = 13.sp
                        )
                        Text("⌛ Durasi: $selectedDuration jam", color = TextSecondary, fontSize = 13.sp)
                        Text("🏁 Selesai: $endHour:00", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = RedCancel, fontSize = 13.sp)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}