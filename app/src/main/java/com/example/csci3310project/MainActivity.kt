package com.example.csci3310project

import FirestoreRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.csci3310project.ui.theme.CSCI3310ProjectTheme
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CSCI3310ProjectTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun AppNavigator() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val authController = remember { AuthController(navController) }
    val firestoreRepository = remember { FirestoreRepository(Firebase.firestore) }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginView(authController, context)
        }
        composable("home") {
//            HomeView(authController) {
//                navController.navigate("trip")
//            }
            HomeView(authController,
                onNavigateToTrip = { navController.navigate("trip") },
                onNavigateToFakeMap = { navController.navigate("map") }
            )
        }
        composable("trip") {
            TripView(firestoreRepository)
        }
        composable("map") {
            MapWithMarkers(null)
        }
    }
}





