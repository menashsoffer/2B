package com.menash.myself.ui.dreams

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.core.designsystem.component.SpaceBadge
import com.menash.myself.core.domain.model.Dream
import com.menash.myself.core.domain.model.DreamStatus
import java.text.NumberFormat
import java.util.Locale

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
@Composable
fun DreamCard(
    dream: Dream,
    viewerUid: String = "",
    onStatusChange: (String) -> Unit,
    onAddSavings: (Double) -> Unit,
    onOpenAppIdea: () -> Unit,
    onDelete: () -> Unit
) {
    val isAppIdea = dream.list == stringResource(R.string.ui_str_307)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DreamCardHeader(dream = dream, viewerUid = viewerUid, onDelete = onDelete)
            DreamStatusRow(currentStatus = dream.status, onStatusChange = onStatusChange)
            if (dream.estimatedCost.agoras > 0) {
                DreamSavingsProgressSection(dream = dream, onAddSavings = onAddSavings)
            }
            if (isAppIdea) {
                DreamAppIdeaButton(dream = dream, onOpenAppIdea = onOpenAppIdea)
            }
        }
    }
}

@Composable
private fun DreamCardHeader(
    dream: Dream,
    viewerUid: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(dream.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(dream.list, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("•", fontSize = 11.sp, color = Color.Gray)
                SpaceBadge(
                    forWhom = dream.forWhom,
                    ownerUid = dream.ownerUid.ifBlank { viewerUid },
                    viewerUid = viewerUid
                )
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.ui_str_260), tint = Color.LightGray)
        }
    }
}

@Composable
private fun DreamStatusRow(
    currentStatus: String,
    onStatusChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DreamStatus.entries.forEach { status ->
            val isCurrent = currentStatus == status.displayName
            FilterChip(
                selected = isCurrent,
                onClick = { onStatusChange(status.displayName) },
                label = { Text(status.displayName, fontSize = 10.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun DreamSavingsProgressSection(
    dream: Dream,
    onAddSavings: (Double) -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                stringResource(R.string.ui_fmtformatdreamsavedamountshekelstoint_fmtformatdreamestimatedcostshekelstoint_343, fmt.format(dream.savedAmount.shekels.toInt()), fmt.format(dream.estimatedCost.shekels.toInt())),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${(dream.progressPercent * 100).toInt()}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        LinearProgressIndicator(
            progress = { dream.progressPercent },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = Color(0xFFE2E8F0)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 2.dp)
        ) {
            OutlinedButton(
                onClick = { onAddSavings(100.0) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("+₪100", fontSize = 10.sp)
            }
            OutlinedButton(
                onClick = { onAddSavings(500.0) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("+₪500", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DreamAppIdeaButton(
    dream: Dream,
    onOpenAppIdea: () -> Unit
) {
    Button(
        onClick = onOpenAppIdea,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            if (dream.appIdeaQuestions.isNotEmpty()) stringResource(R.string.ui_7_341) else stringResource(R.string.ui_7_342),
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DreamBoardView(dreams: List<Dream>) {
    val statuses = DreamStatus.entries.map { it.displayName }
    val scrollState = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        statuses.forEach { status ->
            DreamBoardColumn(status = status, columnDreams = dreams.filter { it.status == status })
        }
    }
}

@Composable
private fun DreamBoardColumn(
    status: String,
    columnDreams: List<Dream>
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(status, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("${columnDreams.size}", fontSize = 12.sp, color = Color.Gray)
            }
            HorizontalDivider()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(columnDreams, key = { it.id }) { d ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(d.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(d.list, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            if (d.estimatedCost.agoras > 0) {
                                Text("₪${d.savedAmount.shekels.toInt()} / ₪${d.estimatedCost.shekels.toInt()}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
