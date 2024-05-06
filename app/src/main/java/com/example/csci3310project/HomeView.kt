package com.example.csci3310project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun HomeView(
    authController: AuthController,
    firestoreRepository: FirestoreRepository,
    navController: NavController,
    onNavigateToTrip: () -> Unit,
    onNavigateToFakeMap: () -> Unit
) {
    authController.user?.let { user ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome ${user.name}!", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { authController.logout() }) {
                Text("Logout")
            }
            Button(onClick = { navController.navigate("trip") }) {
                Text("Plan a Trip")
            }
            TripsListView(firestoreRepository, user.id) { tripId ->
                navController.navigate("tripDetails/$tripId")
            }
            Button(onClick = onNavigateToFakeMap) {
                Text("Fake Map")
            }
        }
    } ?: Text("Please log in to view details.", style = MaterialTheme.typography.bodyLarge)
}
