package com.menash.myself.ui.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.calendar.CalendarHelper
import com.menash.myself.core.designsystem.component.EmptyState
import com.menash.myself.core.designsystem.component.OfflineIndicator
import com.menash.myself.core.domain.model.CalendarEvent

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(CalendarHelper.hasCalendarPermission(context)) }
    var events by remember { mutableStateOf(if (hasPermission) CalendarHelper.getTodayEvents(context) else emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            events = CalendarHelper.getTodayEvents(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_str_401), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calendar_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_str_60))
                    }
                },
                actions = {
                    if (hasPermission) {
                        IconButton(onClick = { events = CalendarHelper.getTodayEvents(context) }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.ui_str_407))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineIndicator(isOffline = false)
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!hasPermission) {
                    CalendarPermissionCard(onRequest = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) })
                } else if (events.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.EventAvailable,
                        title = stringResource(R.string.ui_str_408),
                        description = stringResource(R.string.ui_str_405)
                    )
                } else {
                    Text(stringResource(R.string.ui_eventssize_404, events.size), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(events, key = { it.id }) { event ->
                            CalendarEventRowCard(event = event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarPermissionCard(onRequest: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Text(stringResource(R.string.ui_str_403), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                stringResource(R.string.ui_str_402),
                fontSize = 12.sp,
                color = Color.DarkGray
            )
            Button(onClick = onRequest, shape = RoundedCornerShape(10.dp)) {
                Text(stringResource(R.string.ui_str_406))
            }
        }
    }
}

@Composable
private fun CalendarEventRowCard(event: CalendarEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (event.isAllDay) stringResource(R.string.ui_str_366) else event.startTime,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!event.isAllDay && event.endTime.isNotBlank()) {
                    Text(text = event.endTime, fontSize = 11.sp, color = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            VerticalDivider(modifier = Modifier.height(36.dp), color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (event.location.isNotBlank()) {
                    Text("📍 ${event.location}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}
