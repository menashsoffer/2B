package com.menash.myself.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.domain.model.CalendarEvent
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.FitnessEntry
import com.menash.myself.core.domain.model.Task

@Composable
fun HomeTop3TasksSection(
    top3Tasks: List<Task>,
    activeTasksCount: Int,
    viewerUid: String,
    onToggleTaskDone: (Task) -> Unit,
    onOpenTasks: () -> Unit
) {
    HomeSectionHeader(
        title = "3 המשימות של היום (Top 3)",
        subtitle = "רק מה שקריטי היום, ללא הסחות דעת",
        actionText = "כל המשימות ($activeTasksCount)",
        onActionClick = onOpenTasks
    )
    if (top3Tasks.isEmpty()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().clickable { onOpenTasks() }.padding(top = 4.dp)
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Checklist, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(10.dp))
                Text("לא נבחרו משימות Top 3 להיום. לחץ לבחירת משימות ממוקדות.", fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    } else {
        Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            top3Tasks.forEach { task ->
                TopTaskItemCard(
                    task = task,
                    viewerUid = viewerUid,
                    onToggle = { onToggleTaskDone(task) },
                    onCardClick = onOpenTasks
                )
            }
        }
    }
}

@Composable
fun HomeTodayCalendarSection(
    hasCalendarPermission: Boolean,
    calendarEvents: List<CalendarEvent>,
    onRequestPermission: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    HomeSectionHeader(
        title = "אירועים ביומן להיום",
        subtitle = "מתוך יומן המכשיר",
        actionText = "יומן מלא",
        onActionClick = onOpenCalendar
    )
    if (!hasCalendarPermission) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEFF6FF),
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF2563EB))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "אפשר גישה ליומן כדי לראות פגישות להיום מול העיניים.",
                    fontSize = 12.sp,
                    color = Color(0xFF1E3A8A),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onRequestPermission,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("אפשר", fontSize = 11.sp)
                }
            }
        }
    } else if (calendarEvents.isEmpty()) {
        Text("היום פנוי מאירועים ביומן", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
    } else {
        Column(modifier = Modifier.padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            calendarEvents.take(2).forEach { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (event.isAllDay) "כל היום" else "${event.startTime} - ${event.endTime}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = event.title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun HomeDailyFitnessSection(
    todayFitness: FitnessEntry?,
    streakDays: Int,
    onSaveTodayFitness: (Boolean, Int, String) -> Unit,
    onOpenFitness: () -> Unit
) {
    HomeSectionHeader(
        title = "כושר ותנועה יומית",
        subtitle = if (streakDays > 0) "רצף של $streakDays ימים פעילים!" else "בדיקה יומית מהירה",
        actionText = "מעקב שבועי",
        onActionClick = onOpenFitness
    )
    val didMove = todayFitness?.moved == true
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (didMove) Color(0xFFF0FDF4) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).clickable { onOpenFitness() }.testTag("home_fitness_card")
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (didMove) Icons.AutoMirrored.Filled.DirectionsRun else Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = if (didMove) Color(0xFF16A34A) else Color.Gray,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (didMove) "הזזת את הגוף היום! (${todayFitness?.minutes ?: 0} דק׳ ${todayFitness?.type ?: ""})" else "האם הזזת את הגוף היום?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (didMove) Color(0xFF15803D) else Color.DarkGray
                )
                Text(if (didMove) "כל הכבוד, המשך כך!" else "לחץ לסימון מהיר של הפעילות", fontSize = 11.sp, color = Color.Gray)
            }
            Switch(
                checked = didMove,
                onCheckedChange = { checked ->
                    onSaveTodayFitness(
                        checked,
                        if (checked && (todayFitness?.minutes ?: 0) == 0) 30 else (todayFitness?.minutes ?: 0),
                        todayFitness?.type ?: "הליכה"
                    )
                },
                modifier = Modifier.testTag("home_fitness_quick_switch")
            )
        }
    }
}

@Composable
fun HomeFeaturedDreamAndQuickNavSection(
    featuredDream: Dream?,
    dreamsCount: Int,
    roadmapExpanded: Boolean,
    onToggleRoadmap: () -> Unit,
    onNavigate: (String) -> Unit
) {
    HomeSectionHeader(
        title = "חלום ממוקד",
        subtitle = "חלום אחד בתהליך חסכון או תכנון",
        actionText = "כל החלומות ($dreamsCount)",
        onActionClick = { onNavigate("dreams") }
    )
    if (featuredDream == null) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth().clickable { onNavigate("dreams") }.padding(top = 4.dp)
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(10.dp))
                Text("אין כרגע חלום מוגדר. לחץ להוספת חלום או יעד משפחתי.", fontSize = 12.sp, color = Color.DarkGray)
            }
        }
    } else {
        FeaturedDreamCard(dream = featuredDream, onClick = { onNavigate("dreams") })
    }
    Text("גישה מהירה לכל התחומים", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        QuickNavButton(Icons.Default.Calculate, "המחשבונים שלי", "8", Modifier.weight(1f)) { onNavigate("calculators") }
        QuickNavButton(Icons.Default.FolderOpen, "קטלוג וקבצים", "CSV", Modifier.weight(1f)) { onNavigate("catalog") }
        QuickNavButton(Icons.AutoMirrored.Filled.ListAlt, "חלומות ורשימות", "$dreamsCount", Modifier.weight(1f)) { onNavigate("dreams") }
    }
    HomeRoadmapCard(roadmapExpanded = roadmapExpanded, onToggleRoadmap = onToggleRoadmap)
}

@Composable
private fun HomeRoadmapCard(roadmapExpanded: Boolean, onToggleRoadmap: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(Color(0xFFE2E8F0))),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("roadmap_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleRoadmap() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Upcoming, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("מפת דרכים — בקרוב (שלבים 2 ו-3)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Icon(if (roadmapExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = Color.Gray)
            }
            AnimatedVisibility(visible = roadmapExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RoadmapItem("שלב 2 — כספים וחשבונות", "תזרים מזומנים חודשי אוטומטי מייבוא בנקים ואשראי, חלוקה לקטגוריות, ניהול הוראות קבע, ויעד יתרה חיובית מקושר לחיסכון החלומות.")
                    RoadmapItem("שלב 2 — בריאות", "סנכרון נתוני הליכה ופעילות אוטומטית מחיישני הטלפון לתוך המעקב היומי.")
                    RoadmapItem("שלב 3 — תפעול החיים", "ארכיון מסמכים ותעודות אחריות, ניהול מלאי ביתי וציוד, ביקורת מנויים תקופתית, ותחזוקה שוטפת של הבית והרכב.")
                }
            }
        }
    }
}
