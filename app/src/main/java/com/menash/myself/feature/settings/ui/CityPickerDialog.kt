package com.menash.myself.feature.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.external.hebcal.IsraeliCity
import kotlinx.coroutines.launch

@Composable
fun CityPickerDialog(onDismiss: () -> Unit) {
    val appContainer = LocalAppContainer.current
    val selectedCity by appContainer.hebcalRepository.selectedCityFlow.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            IsraeliCity.DEFAULT_CITIES
        } else {
            IsraeliCity.DEFAULT_CITIES.filter {
                it.nameHebrew.contains(searchQuery, ignoreCase = true) ||
                        it.nameEnglish.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("בחר עיר לזמני שבת וחגים", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("חיפוש עיר...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("city_search_input"),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredCities) { city ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        appContainer.hebcalRepository.selectCity(city)
                                    }
                                    onDismiss()
                                }
                                .testTag("city_item_${city.geonameid}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCity?.geonameid == city.geonameid)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(city.nameHebrew, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(city.nameEnglish, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_city_picker_button")
            ) {
                Text("סגור")
            }
        }
    )
}
