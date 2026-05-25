package com.playzone.booking.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.playzone.booking.ui.screens.admin.AdminDashboardScreen
import com.playzone.booking.ui.screens.auth.LoginScreen
import com.playzone.booking.ui.screens.auth.RegisterScreen
import com.playzone.booking.ui.screens.booking.BookingConfirmScreen
import com.playzone.booking.ui.screens.booking.BookingSuccessScreen
import com.playzone.booking.ui.screens.booking.PaymentScreen
import com.playzone.booking.ui.screens.booking.ScheduleScreen
import com.playzone.booking.ui.screens.detail.DetailPsScreen
import com.playzone.booking.ui.screens.home.HomeScreen
import com.playzone.booking.ui.screens.mybooking.MyBookingScreen
import com.playzone.booking.ui.screens.profile.ProfileScreen
import com.playzone.booking.ui.screens.snack.SnackOrderScreen

sealed class Screen(val route: String) {
    object Login          : Screen("login")
    object Register       : Screen("register")
    object Home           : Screen("home")
    object AdminDashboard : Screen("admin_dashboard")
    object DetailPs       : Screen("detail/{psUnitId}") {
        fun createRoute(id: String) = "detail/$id"
    }
    object Schedule       : Screen("schedule/{psUnitId}") {
        fun createRoute(id: String) = "schedule/$id"
    }
    object Confirm        : Screen("confirm/{psUnitId}/{date}/{startTime}/{duration}") {
        fun createRoute(id: String, date: String, time: String, dur: Int): String {
            val safeDate = date.replace("/", "_")
            val safeTime = time.replace(":", "-")
            return "confirm/$id/$safeDate/$safeTime/$dur"
        }
    }
    object Payment        : Screen("payment/{bookingId}") {
        fun createRoute(id: String) = "payment/$id"
    }
    object BookingSuccess : Screen("success/{bookingId}") {
        fun createRoute(id: String) = "success/$id"
    }
    object MyBookings     : Screen("my_bookings")
    object Profile        : Screen("profile")
    object SnackOrder     : Screen("snack_order")
    object SnackOrderAfterBooking : Screen("snack_order_after/{bookingId}/{psUnitName}") {
        fun createRoute(id: String, unitName: String): String {
            val safeName = java.net.URLEncoder.encode(unitName, "UTF-8")
            return "snack_order_after/$id/$safeName"
        }
    }
}

private fun NavHostController.goHome() {
    navigate(Screen.Home.route) {
        popUpTo(Screen.Home.route) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun PlayzoneNavGraph(navController: NavHostController = rememberNavController()) {

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { isAdmin ->
                    val dest = if (isAdmin) Screen.AdminDashboard.route else Screen.Home.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onGoRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onDetailPs    = { navController.navigate(Screen.DetailPs.createRoute(it)) },
                onMyBookings  = { navController.navigate(Screen.MyBookings.route) },
                onProfile     = { navController.navigate(Screen.Profile.route) },
                onSnackOrder  = { navController.navigate(Screen.SnackOrder.route) },
                onLogout      = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            Screen.DetailPs.route,
            arguments = listOf(navArgument("psUnitId") { type = NavType.StringType })
        ) { back ->
            DetailPsScreen(
                psUnitId  = back.arguments?.getString("psUnitId") ?: "",
                onBack    = { navController.popBackStack() },
                onBooking = { navController.navigate(Screen.Schedule.createRoute(it)) }
            )
        }

        composable(
            Screen.Schedule.route,
            arguments = listOf(navArgument("psUnitId") { type = NavType.StringType })
        ) { back ->
            ScheduleScreen(
                psUnitId = back.arguments?.getString("psUnitId") ?: "",
                onBack   = { navController.popBackStack() },
                onNext   = { id, date, time, dur ->
                    navController.navigate(Screen.Confirm.createRoute(id, date, time, dur))
                }
            )
        }

        composable(
            Screen.Confirm.route,
            arguments = listOf(
                navArgument("psUnitId")   { type = NavType.StringType },
                navArgument("date")       { type = NavType.StringType },
                navArgument("startTime")  { type = NavType.StringType },
                navArgument("duration")   { type = NavType.IntType }
            )
        ) { back ->
            BookingConfirmScreen(
                psUnitId  = back.arguments?.getString("psUnitId") ?: "",
                date      = (back.arguments?.getString("date") ?: "").replace("_", "/"),
                startTime = (back.arguments?.getString("startTime") ?: "").replace("-", ":"),
                duration  = back.arguments?.getInt("duration") ?: 1,
                onBack    = { navController.popBackStack() },
                onConfirm = { bookingId ->
                    navController.navigate(Screen.Payment.createRoute(bookingId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            Screen.Payment.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { back ->
            PaymentScreen(
                bookingId    = back.arguments?.getString("bookingId") ?: "",
                onBack       = { navController.popBackStack() },
                onPaymentDone = { id ->
                    navController.navigate(Screen.BookingSuccess.createRoute(id)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            Screen.BookingSuccess.route,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType })
        ) { back ->
            val bookingId = back.arguments?.getString("bookingId") ?: ""
            BookingSuccessScreen(
                bookingId     = bookingId,
                onViewHistory = {
                    navController.navigate(Screen.MyBookings.route) {
                        popUpTo(Screen.BookingSuccess.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onOrderSnack  = {
                    navController.navigate(Screen.SnackOrderAfterBooking.createRoute(bookingId, "")) {
                        launchSingleTop = true
                    }
                },
                onBackHome    = { navController.goHome() }
            )
        }

        // FIX: Hapus parentEntry yang bisa crash — pakai ViewModel sendiri
        // myBookings menggunakan SharingStarted.Eagerly jadi data langsung diload
        composable(Screen.MyBookings.route) {
            MyBookingScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack   = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.SnackOrder.route) {
            SnackOrderScreen(
                bookingId = null,
                psUnitName = "",
                onBack    = { navController.popBackStack() }
            )
        }

        composable(
            Screen.SnackOrderAfterBooking.route,
            arguments = listOf(
                navArgument("bookingId") { type = NavType.StringType },
                navArgument("psUnitName") { type = NavType.StringType }
            )
        ) { back ->
            val rawName = back.arguments?.getString("psUnitName") ?: ""
            val decodedName = try { java.net.URLDecoder.decode(rawName, "UTF-8") } catch (e: Exception) { rawName }
            SnackOrderScreen(
                bookingId  = back.arguments?.getString("bookingId"),
                psUnitName = decodedName,
                onBack     = { navController.goHome() }
            )
        }
    }
}
