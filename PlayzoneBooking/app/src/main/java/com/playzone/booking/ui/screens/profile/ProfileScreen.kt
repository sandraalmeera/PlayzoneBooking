package com.playzone.booking.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.playzone.booking.ui.theme.*
import com.playzone.booking.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var showLogoutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Saya", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {

            // Header
            Box(
                modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(SurfaceDark, DarkBackground))).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(Brush.radialGradient(listOf(PurpleLight, PurplePrimary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(userProfile?.name?.take(1)?.uppercase() ?: "U", fontSize = 32.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(userProfile?.name ?: "User", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(userProfile?.email ?: "", fontSize = 13.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    // Badge role
                    Surface(shape = RoundedCornerShape(20.dp), color = if (userProfile?.role == "admin") GoldAccent.copy(0.2f) else PurplePrimary.copy(0.2f)) {
                        Text(
                            if (userProfile?.role == "admin") "👑 Admin" else "🎮 Pelanggan",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            color = if (userProfile?.role == "admin") GoldAccent else PurpleLight,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Info akun
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Informasi Akun", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    Divider(Modifier.padding(vertical = 10.dp), color = PurplePrimary.copy(0.3f))
                    ProfileInfoRow(Icons.Default.Person, "Nama", userProfile?.name ?: "-")
                    ProfileInfoRow(Icons.Default.Phone, "No HP", userProfile?.phone ?: "-")
                    ProfileInfoRow(Icons.Default.Email, "Email", userProfile?.email ?: "-")
                    ProfileInfoRow(Icons.Default.Shield, "Role", if (userProfile?.role == "admin") "Admin" else "Pelanggan")
                }
            }

            Spacer(Modifier.weight(1f))

            // Logout
            Button(
                onClick = { showLogoutDialog = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedCancel.copy(0.15f))
            ) {
                Icon(Icons.Default.Logout, null, tint = RedCancel)
                Spacer(Modifier.width(8.dp))
                Text("Logout", color = RedCancel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Yakin ingin keluar dari akun?") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout(); onLogout() }) { Text("Ya, Logout", color = RedCancel) }
            },
            dismissButton = { TextButton(onClick = { showLogoutDialog = false }) { Text("Batal") } },
            containerColor = SurfaceDark
        )
    }
}

@Composable
fun ProfileInfoRow(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = PurpleLight, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextSecondary, fontSize = 13.sp)
        }
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}