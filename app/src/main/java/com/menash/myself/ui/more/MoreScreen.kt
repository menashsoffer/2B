package com.menash.myself.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.designsystem.component.OfflineIndicator

data class MoreMenuItem(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String
)

private val moreMenuItems = listOf(
    MoreMenuItem(
        route = "inbox",
        title = "אינבוקס ומיון מהיר",
        subtitle = "מיון רעיונות ולכידות מהירות למשימות או חלומות",
        icon = Icons.Default.Inbox,
        testTag = "more_inbox_item"
    ),
    MoreMenuItem(
        route = "fitness",
        title = "כושר ותנועה יומית",
        subtitle = "מעקב תנועה יומי ורצף שבועי",
        icon = Icons.AutoMirrored.Filled.DirectionsRun,
        testTag = "more_fitness_item"
    ),
    MoreMenuItem(
        route = "calendar",
        title = "יומן אירועים",
        subtitle = "אירועי היום מתוך יומן המכשיר",
        icon = Icons.Default.CalendarMonth,
        testTag = "more_calendar_item"
    ),
    MoreMenuItem(
        route = "catalog",
        title = "קטלוג וקבצי CSV",
        subtitle = "ייבוא וייצוא תבניות נתונים לאפליקציה",
        icon = Icons.Default.FolderOpen,
        testTag = "more_catalog_item"
    ),
    MoreMenuItem(
        route = "household",
        title = "משק בית ומרחבים",
        subtitle = "ניהול שותפות, הזמנת בת הזוג וחשבון",
        icon = Icons.Default.Group,
        testTag = "more_household_item"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("עוד כלים והגדרות", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineIndicator(isOffline = false)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(moreMenuItems, key = { it.route }) { item ->
                    MoreMenuRowCard(item = item, onClick = { onNavigate(item.route) })
                }
            }
        }
    }
}

@Composable
private fun MoreMenuRowCard(
    item: MoreMenuItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(item.testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(item.subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.LightGray
            )
        }
    }
}
