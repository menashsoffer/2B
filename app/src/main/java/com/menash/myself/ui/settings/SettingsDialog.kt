package com.menash.myself.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menash.myself.BuildConfig
import com.menash.myself.R
import com.menash.myself.ui.shell.GalleryIndex
import com.menash.myself.core.designsystem.theme.AppType
import com.menash.myself.core.designsystem.theme.appColors
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.external.hebcal.IsraeliCity
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun SettingsDialog(
    currentRate: Double,
    onDismiss: () -> Unit,
    onSaveRate: (Double) -> Unit,
    onOpenLegacyScreen: (String) -> Unit = {}
) {
    var rateInput by remember { mutableStateOf(currentRate.toString()) }
    val scope = rememberCoroutineScope()
    val appContainer = LocalAppContainer.current

    var showCityDropdown by remember { mutableStateOf(false) }
    var showDesignGallery by remember { mutableStateOf(false) }
    val boiRateState by appContainer.exchangeRateRepository.currentRateFlow.collectAsStateWithLifecycle()
    val selectedCityState by appContainer.hebcalRepository.selectedCityFlow.collectAsStateWithLifecycle()
    var useManualRate by remember(boiRateState?.isManualOverride) { mutableStateOf(boiRateState?.isManualOverride == true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.ui_str_241), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Hebcal City Picker Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCityDropdown = true }
                        .testTag("hebcal_city_picker_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("עיר לזמני שבת וחגים:", fontSize = 12.sp, color = Color.Gray)
                            Text(
                                text = selectedCityState?.nameHebrew ?: "בחר עיר",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (selectedCityState == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Text("נתונים: Hebcal.com", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                DropdownMenu(
                    expanded = showCityDropdown,
                    onDismissRequest = { showCityDropdown = false }
                ) {
                    IsraeliCity.DEFAULT_CITIES.forEach { city ->
                        DropdownMenuItem(
                            text = { Text(city.nameHebrew) },
                            onClick = {
                                showCityDropdown = false
                                scope.launch { appContainer.hebcalRepository.selectCity(city) }
                            }
                        )
                    }
                }


                // BOI USD/ILS Rate Section
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("בנק ישראל, שער יציג:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    boiRateState?.let { r ->
                        val officialDateStr = r.officialRateDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "טרם התקבל"
                        val officialStr = if (r.officialRate != null) "₪${r.officialRate} ($officialDateStr)" else "טרם התקבל"
                        if (r.isManualOverride && r.manualRate != null) {
                            Text("שער ידני: ₪${r.manualRate} · שער רשמי: $officialStr", fontSize = 12.sp)
                            Text("דריסה ידנית פעילה", fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        } else {
                            Text("שער רשמי: $officialStr", fontSize = 12.sp)
                        }
                        if (com.menash.myself.core.external.boi.BoiStalenessPolicy.isStale(r.officialRateDate, r.officialFetchedAt, java.time.Instant.now())) {
                            Text("לא עודכן לאחרונה", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    } ?: Text("לא הוגדר שער", fontSize = 12.sp, color = Color.Gray)
                }

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text(stringResource(R.string.ui_usd_247)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("usd_rate_input"),
                    supportingText = { Text(stringResource(R.string.ui_str_251)) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = useManualRate,
                        onCheckedChange = { useManualRate = it },
                        modifier = Modifier.testTag("use_manual_rate_checkbox")
                    )
                    Text("השתמש בדריסה ידנית לשער הדולר", fontSize = 13.sp)
                }

                if (boiRateState?.isManualOverride == true) {
                    TextButton(
                        onClick = {
                            scope.launch { appContainer.exchangeRateRepository.clearManualRate() }
                        },
                        modifier = Modifier.testTag("return_to_official_rate_button")
                    ) {
                        Text("חזור לשער הרשמי של בנק ישראל")
                    }
                }

                Text(
                    text = "גרסה ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = AppType.caption,
                    color = MaterialTheme.appColors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_version_line")
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = { showDesignGallery = true })
                        },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rate = rateInput.toDoubleOrNull() ?: currentRate
                    onSaveRate(rate)
                    scope.launch {
                        if (useManualRate) {
                            appContainer.exchangeRateRepository.setManualRate(rate)
                        } else {
                            appContainer.exchangeRateRepository.clearManualRate()
                        }
                    }
                },
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text(stringResource(R.string.ui_str_242))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_settings_button")
            ) {
                Text(stringResource(R.string.ui_str_2))
            }
        }
    )
    if (showDesignGallery) {
        Dialog(
            onDismissRequest = { showDesignGallery = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            GalleryIndex(onClose = { showDesignGallery = false }, onOpenLegacy = { route ->
                showDesignGallery = false
                onOpenLegacyScreen(route)
            })
        }
    }
}
