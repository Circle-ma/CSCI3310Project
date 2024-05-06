package com.example.csci3310project

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    authController.user?.let { user ->
        var joinCode by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome ${user.name}!", style = MaterialTheme.typography.headlineSmall)
            TextField(value = joinCode,
                onValueChange = { joinCode = it },
                label = { Text("Enter Trip Join Code") })
            Button(onClick = {
                firestoreRepository.joinTripByCode(joinCode, user.id) { isSuccess ->
                    if (isSuccess) {
                        Toast.makeText(context, "Successfully joined trip", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, "Failed to join trip", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text("Join Trip")
            }

            Button(onClick = { navController.navigate("trip") }) {
                Text("Plan a Trip")
            }
            Button(onClick = onNavigateToFakeMap) {
                Text("Fake Map")
            }
            Button(onClick = { authController.logout() }) {
                Text("Logout")
            }
            TripsListView(firestoreRepository, user.id) { tripId ->
                navController.navigate("tripDetails/$tripId")
            }
        }
    } ?: Text("Please log in to view details.", style = MaterialTheme.typography.bodyLarge)
}

