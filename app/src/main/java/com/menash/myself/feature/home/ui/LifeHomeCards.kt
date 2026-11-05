package com.menash.myself.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.designsystem.component.SpaceBadge
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.Task
import com.menash.myself.core.domain.model.VisibilityFilter
import java.text.NumberFormat
import java.util.Locale

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@Composable
fun HomeVisibilityFilterRow(
    visibilityFilter: VisibilityFilter,
    onFilterChange: (VisibilityFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = visibilityFilter == VisibilityFilter.MINE_AND_BOTH,
            onClick = { onFilterChange(VisibilityFilter.MINE_AND_BOTH) },
            label = { Text(stringResource(R.string.ui_str_283), fontSize = 11.sp) },
            modifier = Modifier.testTag("home_filter_mine_and_both")
        )
        FilterChip(
            selected = visibilityFilter == VisibilityFilter.ALL,
            onClick = { onFilterChange(VisibilityFilter.ALL) },
            label = { Text(stringResource(R.string.ui_str_289), fontSize = 11.sp) },
            modifier = Modifier.testTag("home_filter_all")
        )
    }
}

@Composable
fun HomeSectionHeader(
    title: String,
    subtitle: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        TextButton(
            onClick = onActionClick,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(actionText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun TopTaskItemCard(
    task: Task,
    viewerUid: String = "",
    onToggle: () -> Unit,
    onCardClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("top_task_${task.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isDone) Color.Gray else Color.Black
                )
                if (task.nextAction.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.ui_tasknextaction_381, task.nextAction),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            SpaceBadge(
                forWhom = task.forWhom,
                ownerUid = task.ownerUid.ifBlank { viewerUid },
                viewerUid = viewerUid
            )
        }
    }
}

@Composable
fun FeaturedDreamCard(
    dream: Dream,
    onClick: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val hasMoneyGoal = dream.estimatedCost.agoras > 0
    val progress = dream.progressPercent

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("featured_dream_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(dream.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = dream.status,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            if (hasMoneyGoal) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.ui_fmtformatdreamsavedamountshekelstoint_fmtformatdreamestimatedcostshekelstoint_343, fmt.format(dream.savedAmount.shekels.toInt()), fmt.format(dream.estimatedCost.shekels.toInt())),
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                        Text("${(progress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            } else if (dream.nextAction.isNotEmpty()) {
                Text(stringResource(R.string.ui_dreamnextaction_382, dream.nextAction), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun QuickNavButton(
    icon: ImageVector,
    label: String,
    badge: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(badge, fontSize = 9.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RoadmapItem(phase: String, desc: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(phase, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(desc, fontSize = 11.sp, color = Color.DarkGray, lineHeight = 16.sp)
    }
}
