package com.menash.myself.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.menash.myself.ui.settings.CityPickerDialog

@Composable
fun HomeHebcalCard(onOpenSettings: () -> Unit) {
    val appContainer = LocalAppContainer.current
    val selectedCity by appContainer.hebcalRepository.selectedCityFlow.collectAsStateWithLifecycle()
    val hebcalData by appContainer.hebcalRepository.hebcalDataFlow.collectAsStateWithLifecycle()

    var showCityPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showCityPicker = true }
            .testTag("home_hebcal_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "לוח עברי וזמני שבת",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "נתונים: Hebcal.com",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            if (selectedCity == null) {
                Text(
                    text = "בחר עיר",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                val cityText = selectedCity?.nameHebrew ?: ""
                val hebrewDate = hebcalData?.currentHebrewDate
                val headerText = if (hebrewDate != null) "$cityText | $hebrewDate" else cityText
                val candles = hebcalData?.upcomingShabbatCandles
                val havdalah = hebcalData?.upcomingShabbatHavdalah

                Text(
                    text = headerText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                if (candles != null) {
                    Text(text = candles, fontSize = 12.sp)
                }
                if (havdalah != null) {
                    Text(text = havdalah, fontSize = 12.sp)
                }
            }
        }
    }

    if (showCityPicker) {
        CityPickerDialog(onDismiss = { showCityPicker = false })
    }
}

