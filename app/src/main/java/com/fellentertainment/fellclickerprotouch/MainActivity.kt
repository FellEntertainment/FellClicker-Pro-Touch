package com.fellentertainment.fellclickerprotouch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BgDark = Color(0xFF0B0B10)
val CardDark = Color(0xFF15151F)
val BorderDark = Color(0xFF262638)
val AccentPurple = Color(0xFF6C5CE7)
val AccentCyan = Color(0xFF00E5FF)
val AccentGreen = Color(0xFF10B981)
val AccentRed = Color(0xFFEF4444)
val TextPrimary = Color(0xFFF3F4F6)
val TextSecondary = Color(0xFF9CA3AF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = BgDark,
                    surface = CardDark,
                    primary = AccentPurple
                )
            ) {
                FellClickerScreen()
            }
        }
    }
}

@Composable
fun FellClickerScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var intervalText by remember { mutableStateOf(ConfigManager.currentConfig.intervalMs.toString()) }
    var randomText by remember { mutableStateOf(ConfigManager.currentConfig.randomOffsetMs.toString()) }
    var holdText by remember { mutableStateOf(ConfigManager.currentConfig.holdDurationMs.toString()) }
    var doubleClick by remember { mutableStateOf(ConfigManager.currentConfig.doubleClick) }
    var maxClicksText by remember { mutableStateOf(ConfigManager.currentConfig.maxClicks.toString()) }
    var maxTimeText by remember { mutableStateOf(ConfigManager.currentConfig.maxTimeSeconds.toString()) }
    var posXText by remember { mutableStateOf(ConfigManager.currentConfig.targetX.toString()) }
    var posYText by remember { mutableStateOf(ConfigManager.currentConfig.targetY.toString()) }

    var profileName by remember { mutableStateOf("") }
    var savedProfiles by remember { mutableStateOf(ConfigManager.getProfiles(context)) }

    var hasOverlayPerm by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasAccessPerm by remember { mutableStateOf(AutoClickService.instance != null) }

    fun syncToConfig() {
        ConfigManager.currentConfig = ClickProfile(
            name = profileName.ifBlank { "Standard" },
            intervalMs = intervalText.toLongOrNull()?.coerceAtLeast(20L) ?: 100L,
            randomOffsetMs = randomText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
            holdDurationMs = holdText.toLongOrNull()?.coerceAtLeast(10L) ?: 15L,
            doubleClick = doubleClick,
            maxClicks = maxClicksText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            maxTimeSeconds = maxTimeText.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            targetX = posXText.toIntOrNull() ?: 500,
            targetY = posYText.toIntOrNull() ?: 1000
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("FellClicker Pro", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "TOUCH",
                        color = BgDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(AccentCyan, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text("by FellEntertainment • Mobile Edition", color = TextSecondary, fontSize = 12.sp)
            }
            Button(
                onClick = {
                    hasOverlayPerm = Settings.canDrawOverlays(context)
                    hasAccessPerm = AutoClickService.instance != null
                    posXText = ConfigManager.currentConfig.targetX.toString()
                    posYText = ConfigManager.currentConfig.targetY.toString()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CardDark)
            ) {
                Text("↻ Sync", color = AccentCyan, fontSize = 12.sp)
            }
        }

        FellCard(title = "1. System-Status & Berechtigungen") {
            PermissionRow(
                label = "Über anderen Apps einblenden (Overlay)",
                granted = hasOverlayPerm,
                onClick = {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
            Spacer(Modifier.height(8.dp))
            PermissionRow(
                label = "Bedienungshilfe (Klick-Service)",
                granted = hasAccessPerm,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }

        FellCard(title = "2. Klick-Intervall & Randomizer") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FellNumberField("Intervall (ms, min 20)", intervalText, { intervalText = it; syncToConfig() }, Modifier.weight(1f))
                FellNumberField("Randomizer (± ms)", randomText, { randomText = it; syncToConfig() }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FellNumberField("Haltedauer (ms)", holdText, { holdText = it; syncToConfig() }, Modifier.weight(1f))
                Row(
                    modifier = Modifier.weight(1f).clickable { doubleClick = !doubleClick; syncToConfig() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = doubleClick, onCheckedChange = { doubleClick = it; syncToConfig() })
                    Text("Doppelklick", color = TextPrimary, fontSize = 14.sp)
                }
            }
        }

        FellCard(title = "3. Sicherheits-Limits (0 = Unendlich)") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FellNumberField("Max. Klicks", maxClicksText, { maxClicksText = it; syncToConfig() }, Modifier.weight(1f))
                FellNumberField("Max. Zeit (Sek.)", maxTimeText, { maxTimeText = it; syncToConfig() }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Hardware Not-Aus: Drücke jederzeit die 'Leiser'-Taste am Handy, um den Clicker sofort zu stoppen.",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        FellCard(title = "4. Ziel-Koordinaten & Profile") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FellNumberField("Position X (px)", posXText, { posXText = it; syncToConfig() }, Modifier.weight(1f))
                FellNumberField("Position Y (px)", posYText, { posYText = it; syncToConfig() }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profilname", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        if (profileName.isBlank()) {
                            Toast.makeText(context, "Bitte Profilnamen eingeben", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        syncToConfig()
                        ConfigManager.saveProfile(context, ConfigManager.currentConfig.copy(name = profileName))
                        savedProfiles = ConfigManager.getProfiles(context)
                        Toast.makeText(context, "Profil '$profileName' gespeichert", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Speichern", color = Color.White)
                }
            }

            if (savedProfiles.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                savedProfiles.forEach { prof ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(BgDark, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prof.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                "${prof.intervalMs}ms (±${prof.randomOffsetMs}) | X:${prof.targetX} Y:${prof.targetY}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = {
                                ConfigManager.currentConfig = prof
                                profileName = prof.name
                                intervalText = prof.intervalMs.toString()
                                randomText = prof.randomOffsetMs.toString()
                                holdText = prof.holdDurationMs.toString()
                                doubleClick = prof.doubleClick
                                maxClicksText = prof.maxClicks.toString()
                                maxTimeText = prof.maxTimeSeconds.toString()
                                posXText = prof.targetX.toString()
                                posYText = prof.targetY.toString()
                                Toast.makeText(context, "Profil geladen", Toast.LENGTH_SHORT).show()
                            }) {
                                Text("Laden", color = AccentCyan)
                            }
                            TextButton(onClick = {
                                ConfigManager.deleteProfile(context, prof.name)
                                savedProfiles = ConfigManager.getProfiles(context)
                            }) {
                                Text("✕", color = AccentRed)
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    hasOverlayPerm = Settings.canDrawOverlays(context)
                    hasAccessPerm = AutoClickService.instance != null
                    if (!hasOverlayPerm || !hasAccessPerm) {
                        Toast.makeText(
                            context,
                            "Bitte zuerst oben beide Berechtigungen aktivieren!",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }
                    syncToConfig()
                    context.startService(Intent(context, FloatingOverlayService::class.java))
                },
                modifier = Modifier.weight(1f).height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("OVERLAY STARTEN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Button(
                onClick = {
                    AutoClickService.instance?.stopClicking()
                    context.stopService(Intent(context, FloatingOverlayService::class.java))
                },
                modifier = Modifier.height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("BEENDEN", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FellCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(14.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(title, color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun PermissionRow(label: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (if (granted) "● " else "○ ") + label,
            color = if (granted) AccentGreen else TextPrimary,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) BorderDark else AccentPurple
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(if (granted) "Aktiv" else "Freigeben", fontSize = 12.sp, color = Color.White)
        }
    }
}

@Composable
fun FellNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() }) },
        label = { Text(label, color = TextSecondary, fontSize = 11.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}