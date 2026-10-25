package com.menash.myself.ui.calendar

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.menash.myself.data.models.CalendarEvent

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
                title = { Text("יומן אירועים להיום", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calendar_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                actions = {
                    if (hasPermission) {
                        IconButton(onClick = { events = CalendarHelper.getTodayEvents(context) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "רענן")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!hasPermission) {
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
                        Text(
                            "חיבור ליומן המכשיר לקריאה בלבד",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "האפליקציה יכולה להציג לך את אירועי היום ישירות במסך הבית בלי לעבור בין אפליקציות.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("אשר קריאת יומן")
                        }
                    }
                }
            } else {
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("אין אירועים ביומן להיום", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("יום פנוי לעשייה ממוקדת ומנוחה.", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                } else {
                    Text("אירועי היום (${events.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(events, key = { it.id }) { event ->
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
                                            text = if (event.isAllDay) "כל היום" else event.startTime,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (!event.isAllDay && event.endTime.isNotBlank()) {
                                            Text(
                                                text = event.endTime,
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
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
                    }
                }
            }
        }
    }
}
