package com.playzone.booking.ui.screens.snack

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playzone.booking.data.model.SnackCartItem
import com.playzone.booking.data.model.SnackMenuItem
import com.playzone.booking.ui.theme.*
import com.playzone.booking.ui.viewmodel.BookingViewModel
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnackOrderScreen(
    bookingId: String?,
    psUnitName: String = "",
    onBack: () -> Unit,
    viewModel: BookingViewModel = hiltViewModel()
) {
    val snackMenu by viewModel.snackMenu.collectAsStateWithLifecycle()
    val snackOrderState by viewModel.snackOrderState.collectAsStateWithLifecycle()

    val cart = remember { mutableStateListOf<SnackCartItem>() }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var catatan by remember { mutableStateOf("") }

    // Kategori dinamis dari Firestore — map "food"→"Makanan", "drink"→"Minuman"
    val categoryLabels = remember(snackMenu) {
        val raw = snackMenu.map { it.category }.distinct()
        val mapped = raw.map { cat ->
            when (cat.lowercase()) {
                "food", "makanan" -> "Makanan"
                "drink", "minuman" -> "Minuman"
                "snack", "camilan" -> "Camilan"
                else -> cat.replaceFirstChar { it.uppercase() }
            }
        }.distinct()
        listOf("Semua") + mapped
    }

    fun categoryMatch(item: SnackMenuItem, selected: String): Boolean {
        if (selected == "Semua") return true
        val mapped = when (item.category.lowercase()) {
            "food", "makanan" -> "Makanan"
            "drink", "minuman" -> "Minuman"
            "snack", "camilan" -> "Camilan"
            else -> item.category.replaceFirstChar { it.uppercase() }
        }
        return mapped == selected
    }

    val filteredMenu = snackMenu.filter { categoryMatch(it, selectedCategory) }
    val currency = NumberFormat.getNumberInstance(Locale("in", "ID"))
    val totalItems = cart.sumOf { it.qty }
    val totalHarga = cart.sumOf { it.menuItem.price * it.qty }

    fun addToCart(item: SnackMenuItem) {
        val idx = cart.indexOfFirst { it.menuItem.id == item.id }
        if (idx >= 0) cart[idx] = cart[idx].copy(qty = cart[idx].qty + 1)
        else cart.add(SnackCartItem(item, 1))
    }

    fun removeFromCart(item: SnackMenuItem) {
        val idx = cart.indexOfFirst { it.menuItem.id == item.id }
        if (idx >= 0) {
            if (cart[idx].qty > 1) cart[idx] = cart[idx].copy(qty = cart[idx].qty - 1)
            else cart.removeAt(idx)
        }
    }

    fun getQty(id: String) = cart.find { it.menuItem.id == id }?.qty ?: 0

    // ── Layar Sukses ──────────────────────────────────────────────
    if (snackOrderState.isSuccess) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(DarkBackground, SurfaceDark))),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("✅", fontSize = 72.sp)
                Text(
                    "Pesanan Masuk!",
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    fontSize = 24.sp
                )
                Text(
                    "Pesanan snack kamu sudah diterima kasir. Tunggu sebentar ya!",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                if (!bookingId.isNullOrEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark)
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Untuk Booking:", color = TextSecondary, fontSize = 12.sp)
                            Text(bookingId, fontWeight = FontWeight.Black, color = GoldAccent, fontSize = 20.sp)
                        }
                    }
                }
                // Ringkasan pesanan
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Ringkasan Pesanan", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(Modifier.height(8.dp))
                        cart.forEach { item ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "${item.menuItem.emoji} ${item.menuItem.name} x${item.qty}",
                                    color = TextSecondary, fontSize = 13.sp
                                )
                                Text(
                                    "Rp ${currency.format(item.menuItem.price * item.qty)}",
                                    color = TextPrimary, fontSize = 13.sp
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp), color = PurplePrimary.copy(0.3f))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                "Rp ${currency.format(totalHarga)}",
                                fontWeight = FontWeight.Black,
                                color = GoldAccent,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                // Pesan lagi tombol
                OutlinedButton(
                    onClick = {
                        viewModel.clearSnackOrderState()
                        cart.clear()
                        catatan = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleLight)
                ) {
                    Icon(Icons.Default.ShoppingCart, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Pesan Lagi", fontWeight = FontWeight.Bold)
                }
                // Tombol kembali
                Button(
                    onClick = {
                        viewModel.clearSnackOrderState()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Icon(Icons.Default.Home, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (bookingId != null) "Selesai" else "Kembali ke Beranda",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

    // ── Error snackbar ────────────────────────────────────────────
    snackOrderState.error?.let { err ->
        LaunchedEffect(err) {
            // Tampilkan error sebentar lalu reset
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🍟 Order Snack", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                        if (!bookingId.isNullOrEmpty())
                            Text("Booking: $bookingId", fontSize = 11.sp, color = GoldAccent)
                        else
                            Text("Pesan diantar ke tempatmu", fontSize = 11.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground,
        bottomBar = {
            if (cart.isNotEmpty()) {
                Surface(color = SurfaceDark, shadowElevation = 8.dp) {
                    Column(Modifier.padding(16.dp)) {
                        if (snackOrderState.error != null) {
                            Text(
                                snackOrderState.error!!,
                                color = RedCancel,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("$totalItems item dipilih", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    "Rp ${currency.format(totalHarga)}",
                                    fontWeight = FontWeight.Black,
                                    color = GoldAccent,
                                    fontSize = 18.sp
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.submitSnackOrder(
                                        bookingId = bookingId ?: "",
                                        psUnitName = psUnitName,
                                        cartItems = cart.toList()
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                modifier = Modifier.height(48.dp),
                                enabled = !snackOrderState.isLoading
                            ) {
                                if (snackOrderState.isLoading) {
                                    CircularProgressIndicator(
                                        color = DarkBackground,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.ShoppingCart, null, tint = DarkBackground)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pesan Sekarang", fontWeight = FontWeight.Bold, color = DarkBackground)
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Banner info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PurplePrimary.copy(0.2f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = PurpleLight, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Snack akan diantar langsung ke unitmu. Bayar di kasir setelah selesai main.",
                            color = TextSecondary, fontSize = 12.sp
                        )
                    }
                }
            }

            // Filter kategori (dinamis dari Firestore)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categoryLabels) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldAccent,
                                selectedLabelColor = DarkBackground
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Loading state saat menu belum dimuat
            if (snackMenu.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PurpleLight, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Memuat menu...", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Daftar menu snack (dinamis dari Firestore)
            items(filteredMenu.chunked(2)) { row ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { item ->
                        val qty = getQty(item.id)
                        SnackMenuCard(
                            item = item,
                            qty = qty,
                            onAdd = { addToCart(item) },
                            onRemove = { removeFromCart(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // Field catatan
            item {
                Spacer(Modifier.height(12.dp))
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "📝 Catatan untuk Kasir",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = catatan,
                            onValueChange = { catatan = it },
                            placeholder = { Text("Contoh: jangan pedes, extra saus, dll", color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurpleLight,
                                unfocusedBorderColor = PurplePrimary.copy(0.3f),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SnackMenuCard(
    item: SnackMenuItem,
    qty: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = NumberFormat.getNumberInstance(Locale("in", "ID"))
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        elevation = CardDefaults.cardElevation(if (qty > 0) 4.dp else 2.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (qty > 0) GoldAccent.copy(0.2f) else PurplePrimary.copy(0.1f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) { Text(item.emoji, fontSize = 28.sp) }
                if (qty > 0) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(8.dp),
                        color = GoldAccent
                    ) {
                        Text(
                            "$qty",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = DarkBackground, fontSize = 11.sp, fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.name, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                fontSize = 12.sp, textAlign = TextAlign.Center, maxLines = 2
            )
            Text("Rp ${currency.format(item.price)}", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (qty == 0) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Tambah", fontSize = 12.sp)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(RedCancel.copy(0.15f))
                    ) { Icon(Icons.Default.Remove, null, tint = RedCancel, modifier = Modifier.size(16.dp)) }
                    Text("$qty", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    IconButton(
                        onClick = onAdd,
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(GreenSuccess.copy(0.15f))
                    ) { Icon(Icons.Default.Add, null, tint = GreenSuccess, modifier = Modifier.size(16.dp)) }
                }
            }
        }
    }
}
