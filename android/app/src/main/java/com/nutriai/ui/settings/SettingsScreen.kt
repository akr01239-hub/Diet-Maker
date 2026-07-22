package com.nutriai.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutriai.BuildConfig
import com.nutriai.ui.theme.BrandGreen
import com.nutriai.ui.theme.BrandGreenDeep

@Composable
fun SettingsScreen(
    onEditProfile: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDelete by remember { mutableStateOf(false) }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete account?") },
            text = { Text("This permanently deletes your account and all your data from our servers. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    viewModel.deleteAccount(onLoggedOut)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
    ) {
        item { SettingsHero(name = state.user?.firstName, email = state.user?.email) }

        item {
            SettingTile(icon = "🧍", title = "Edit health profile", subtitle = "Height, weight, goal, diet & conditions", onClick = onEditProfile)
        }

        item {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BrandGreen)
                    OutlinedButton(onClick = { viewModel.logout(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Log out")
                    }
                    OutlinedButton(
                        onClick = { showDelete = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text("NutriAI v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Educational guidance, not medical advice — consult a professional.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SettingsHero(name: String?, email: String?) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(BrandGreen, BrandGreenDeep))).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) { Text((name?.firstOrNull() ?: '👤').toString(), style = MaterialTheme.typography.headlineSmall, color = Color.White) }
                Column {
                    Text(name?.ifBlank { "Your account" } ?: "Your account", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    if (!email.isNullOrBlank()) {
                        Text(email, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingTile(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text(icon) }
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
