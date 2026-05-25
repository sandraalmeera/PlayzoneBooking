package com.playzone.booking.ui.screens.booking

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playzone.booking.data.model.PaymentMethod
import com.playzone.booking.service.LocalNotificationHelper
import com.playzone.booking.ui.theme.*
import com.playzone.booking.ui.viewmodel.AuthViewModel
import com.playzone.booking.ui.viewmodel.BookingViewModel
import java.text.NumberFormat
import java.util.*

// ═══════════════════════════════════════════════════════
// BOOKING CONFIRM SCREEN
// ═══════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmScreen(
    psUnitId: String,
    date: String,
    startTime: String,
    duration: Int,
    onBack: () -> Unit,
    onConfirm: (String) -> Unit,
    bookingViewModel: BookingViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by bookingViewModel.uiState.collectAsStateWithLifecycle()
    val psUnits by bookingViewModel.psUnits.collectAsStateWithLifecycle()
    val userProfile by authViewModel.userProfile.collectAsStateWithLifecycle()
    val psUnit = psUnits.find { it.id == psUnitId }
    val currency = NumberFormat.getNumberInstance(Locale("in", "ID"))
    var selectedPayment by remember { mutableStateOf(PaymentMethod.COD) }
    var hasNavigated by remember { mutableStateOf(false) }

    // Navigasi ke Payment + kirim notif lokal setelah booking berhasil
    LaunchedEffect(uiState.createdBookingId) {
        val bookingId = uiState.createdBookingId
        if (!bookingId.isNullOrEmpty() && !hasNavigated) {
            hasNavigated = true
            // Kirim notif lokal booking berhasil
            LocalNotificationHelper.sendBookingConfirmedNotif(
                context = context,
                bookingId = bookingId,
                psUnitName = psUnit?.name ?: "",
                date = date,
                startTime = startTime
            )
            bookingViewModel.clearMessage()
            onConfirm(bookingId)
        }
    }

    val startHour = startTime.split(":")[0].toIntOrNull() ?: 8
    val endTime = "${startHour + duration}:00"
    val totalPrice = (psUnit?.pricePerHour ?: 0) * duration

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfirmasi Booking", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            Box(Modifier.fillMaxWidth().background(SurfaceDark).padding(16.dp)) {
                Button(
                    onClick = {
                        if (psUnit != null && !uiState.isLoading && !hasNavigated) {
                            val parts = date.split("/")
                            val cal = Calendar.getInstance()
                            if (parts.size == 3) {
                                cal.set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt(), startHour, 0, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                            }
                            bookingViewModel.createBooking(
                                psUnit = psUnit,
                                date = date,
                                startTime = startTime,
                                startMillis = cal.timeInMillis,
                                durationHours = duration,
                                paymentMethod = selectedPayment,
                                userPhone = userProfile?.phone ?: ""
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !uiState.isLoading && psUnit != null && !hasNavigated,
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Memproses...")
                    } else {
                        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Konfirmasi Booking", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Detail Pesanan
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Detail Pesanan", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Divider(Modifier.padding(vertical = 10.dp), color = PurplePrimary.copy(0.3f))
                    InfoRow2(Icons.Default.Gamepad, "Unit PS", psUnit?.name ?: "-")
                    InfoRow2(Icons.Default.Category, "Tipe", psUnit?.type ?: "-")
                    InfoRow2(Icons.Default.CalendarToday, "Tanggal", date)
                    InfoRow2(Icons.Default.AccessTime, "Jam Main", "$startTime - $endTime")
                    InfoRow2(Icons.Default.Timer, "Durasi", "$duration jam")
                    InfoRow2(Icons.Default.Person, "Nama", userProfile?.name ?: "-")
                    InfoRow2(Icons.Default.Phone, "No HP", userProfile?.phone ?: "-")
                }
            }

            // Total Harga
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = PurplePrimary.copy(0.2f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total Bayar", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Text("Rp ${currency.format(totalPrice)}", fontWeight = FontWeight.Black, color = GoldAccent, fontSize = 20.sp)
                }
            }

            // Metode Pembayaran
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Metode Pembayaran", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Spacer(Modifier.height(10.dp))
                    PaymentMethod.values().forEach { method ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedPayment = method }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedPayment == method, onClick = { selectedPayment = method }, colors = RadioButtonDefaults.colors(selectedColor = PurpleLight))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(if (method == PaymentMethod.COD) "Bayar di Tempat (COD)" else "Transfer Bank", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(if (method == PaymentMethod.COD) "Bayar langsung ke kasir" else "Transfer ke rekening warnet", color = TextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = RedCancel.copy(0.15f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = RedCancel, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(uiState.error!!, color = RedCancel, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow2(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = TextSecondary, fontSize = 13.sp)
        }
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

// ═══════════════════════════════════════════════════════
// PAYMENT SCREEN
// ═══════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(bookingId: String, onBack: () -> Unit, onPaymentDone: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pembayaran", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            Icon(Icons.Default.Payment, null, tint = PurpleLight, modifier = Modifier.size(64.dp))
            Text("Instruksi Pembayaran", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 20.sp)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                Column(Modifier.padding(20.dp)) {
                    Text("ID Booking:", color = TextSecondary, fontSize = 12.sp)
                    Text(bookingId, fontWeight = FontWeight.Black, color = GoldAccent, fontSize = 24.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("💵 Bayar di Tempat (COD):", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("• Tunjukkan ID booking ke kasir\n• Bayar sesuai total yang tertera\n• Kasir akan konfirmasi booking", color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("🏦 Transfer Bank:", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("• BCA: 1234567890 a/n Playzone\n• Kirim bukti ke kasir via WA\n• Booking dikonfirmasi setelah bayar", color = TextSecondary, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onPaymentDone(bookingId) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.width(8.dp))
                Text("Saya Sudah Mengerti", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════
// BOOKING SUCCESS SCREEN
// ═══════════════════════════════════════════════════════
@Composable
fun BookingSuccessScreen(bookingId: String, onViewHistory: () -> Unit, onOrderSnack: () -> Unit, onBackHome: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(DarkBackground, SurfaceDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("🎉", fontSize = 72.sp)
            Text("Booking Berhasil!", fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 26.sp)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ID Booking", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(bookingId, fontWeight = FontWeight.Black, color = GoldAccent, fontSize = 26.sp)
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = GoldAccent.copy(0.15f)) {
                        Text("⏳ Menunggu Konfirmasi", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            Text("Cek notifikasi & tunjukkan ID booking ke kasir", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))

            // Tombol order snack setelah booking
            Button(
                onClick = onOrderSnack,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text("🍟", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Text("Order Snack Sekarang", fontWeight = FontWeight.Bold, color = DarkBackground)
            }

            Button(onClick = onViewHistory, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)) {
                Icon(Icons.Default.History, null)
                Spacer(Modifier.width(8.dp))
                Text("Lihat Riwayat Booking", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBackHome, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Home, null, tint = PurpleLight)
                Spacer(Modifier.width(8.dp))
                Text("Kembali ke Beranda", color = PurpleLight, fontWeight = FontWeight.Bold)
            }
        }
    }
}