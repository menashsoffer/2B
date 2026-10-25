package com.menash.myself.ui.inbox

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.menash.myself.core.designsystem.component.SpaceBadge
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.model.InboxItem
import com.menash.myself.feature.inbox.InboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxTriageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: InboxViewModel = viewModel(
        factory = InboxViewModel.factory(
            inboxRepository = appContainer.inboxRepository,
            taskRepository = appContainer.taskRepository,
            dreamRepository = appContainer.dreamRepository
        )
    )

    val inboxItems by viewModel.inboxItems.collectAsState()
    val viewerUid = remember { runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: "" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("מיון Inbox", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${inboxItems.size} פריטים ממתינים למיון", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("inbox_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (inboxItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Text("ה-Inbox שלך נקי!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("כל המחשבות מויינו. שקט מנטלי מובטח.", fontSize = 13.sp, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(inboxItems, key = { it.id }) { item ->
                    InboxItemCard(
                        item = item,
                        viewerUid = viewerUid,
                        onTriageToTask = {
                            viewModel.triageToTask(item) {
                                Toast.makeText(context, "הועבר למשימות!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTriageToDream = {
                            viewModel.triageToDream(item) {
                                Toast.makeText(context, "הועבר לחלומות!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTriageToIdea = {
                            viewModel.triageToIdea(item) {
                                Toast.makeText(context, "נשמר כרעיון לפיתוח!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTriageToNote = {
                            viewModel.triageToNote(item) {
                                Toast.makeText(context, "סומן כפתק!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = {
                            viewModel.deleteItem(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InboxItemCard(
    item: InboxItem,
    viewerUid: String = "",
    onTriageToTask: () -> Unit,
    onTriageToDream: () -> Unit,
    onTriageToIdea: () -> Unit,
    onTriageToNote: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpaceBadge(
                    forWhom = item.forWhom,
                    ownerUid = item.ownerUid.ifBlank { viewerUid },
                    viewerUid = viewerUid
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "מחק", tint = Color.LightGray)
                }
            }

            Text(
                text = item.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onTriageToTask,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("משימה", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTriageToDream,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("חלום", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTriageToIdea,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("רעיון", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onTriageToNote,
                    modifier = Modifier.weight(0.9f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("פתק", fontSize = 11.sp)
                }
            }
        }
    }
}
