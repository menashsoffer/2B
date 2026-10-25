package com.menash.myself.ui.dreams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.menash.myself.data.firestore.HouseholdRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.models.DreamItem
import com.menash.myself.data.models.DreamStatus
import com.menash.myself.data.models.SpaceType
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { LifeHubRepository(context) }
    val householdRepo = remember { HouseholdRepository(context) }

    val household by householdRepo.observeHousehold().collectAsState(initial = null)

    var selectedListFilter by remember { mutableStateOf<String?>(null) }
    var selectedSpaceFilter by remember { mutableStateOf<String?>(null) }
    var isBoardView by remember { mutableStateOf(false) }

    val dreams by repo.observeDreams(selectedListFilter, selectedSpaceFilter, household).collectAsState(initial = emptyList())

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedDreamForAppIdea by remember { mutableStateOf<DreamItem?>(null) }

    val defaultLists = listOf("הכל", "לבית", "טיסות משפחה", "פיתוח ואפליקציות")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("חלומות ורשימות", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("dreams_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזור")
                    }
                },
                actions = {
                    IconButton(onClick = { isBoardView = !isBoardView }) {
                        Icon(
                            if (isBoardView) Icons.Default.ViewList else Icons.Default.ViewKanban,
                            contentDescription = "החלף תצוגה"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_dream_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "הוסף חלום")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters Row: Lists & Spaces
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // List filter chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(defaultLists) { listName ->
                        FilterChip(
                            selected = (selectedListFilter == null && listName == "הכל") || selectedListFilter == listName,
                            onClick = { selectedListFilter = if (listName == "הכל") null else listName },
                            label = { Text(listName, fontSize = 12.sp) }
                        )
                    }
                }

                // Space filter chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val spaces = listOf("הכל", "משותף", "שלי", "של אשתי")
                    spaces.forEach { s ->
                        val isSelected = (selectedSpaceFilter == null && s == "הכל") || selectedSpaceFilter == s
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSpaceFilter = if (s == "הכל") null else s },
                            label = { Text(s, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Content: List View or Board View
            if (isBoardView) {
                DreamBoardView(
                    dreams = dreams,
                    onStatusChange = { dream, newStatus ->
                        scope.launch { repo.updateDreamStatus(dream.id, newStatus) }
                    },
                    onOpenAppIdea = { selectedDreamForAppIdea = it }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dreams, key = { it.id }) { dream ->
                        DreamCard(
                            dream = dream,
                            onStatusChange = { newStatus ->
                                scope.launch { repo.updateDreamStatus(dream.id, newStatus) }
                            },
                            onAddSavings = { amount ->
                                scope.launch { repo.updateDreamSavings(dream.id, dream.savedAmount + amount) }
                            },
                            onOpenAppIdea = { selectedDreamForAppIdea = dream },
                            onDelete = { scope.launch { repo.deleteDream(dream.id) } }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Create Dream Dialog
    if (showCreateDialog) {
        CreateDreamDialog(
            defaultList = selectedListFilter ?: "לבית",
            onDismiss = { showCreateDialog = false },
            onSave = { newDream ->
                scope.launch {
                    repo.createDream(newDream, household)
                    showCreateDialog = false
                }
            }
        )
    }

    // App Idea Template Dialog (7 questions + next action)
    selectedDreamForAppIdea?.let { dream ->
        AppIdeaTemplateDialog(
            dream = dream,
            onDismiss = { selectedDreamForAppIdea = null },
            onSave = { questions, nextAction ->
                scope.launch {
                    repo.updateDreamAppIdea(dream.id, questions, nextAction)
                    selectedDreamForAppIdea = null
                }
            }
        )
    }
}

@Composable
fun DreamCard(
    dream: DreamItem,
    onStatusChange: (String) -> Unit,
    onAddSavings: (Double) -> Unit,
    onOpenAppIdea: () -> Unit,
    onDelete: () -> Unit
) {
    val fmt = NumberFormat.getNumberInstance(Locale.US)
    val isAppIdea = dream.list == "פיתוח ואפליקציות"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header: Title, List tag, Space chip, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(dream.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 2.dp)) {
                        Text(dream.list, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("•", fontSize = 11.sp, color = Color.Gray)
                        Text(SpaceType.fromId(dream.space).displayName, fontSize = 11.sp, color = Color.Gray)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "מחק", tint = Color.LightGray)
                }
            }

            // Status Pipeline Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DreamStatus.entries.forEach { status ->
                    val isCurrent = dream.status == status.displayName
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

            // Money Dream Savings Progress Bar (if estimatedCost > 0)
            if (dream.estimatedCost > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "נחסך: ₪${fmt.format(dream.savedAmount.toInt())} מתוך ₪${fmt.format(dream.estimatedCost.toInt())}",
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

            // App Idea Template trigger (7 questions)
            if (isAppIdea) {
                Button(
                    onClick = onOpenAppIdea,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (dream.appIdeaQuestions.isNotEmpty()) "ערוך תבנית 7 שאלות לרעיון" else "מלא תבנית 7 שאלות לרעיון",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DreamBoardView(
    dreams: List<DreamItem>,
    onStatusChange: (DreamItem, String) -> Unit,
    onOpenAppIdea: (DreamItem) -> Unit
) {
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
            val columnDreams = dreams.filter { it.status == status }
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
                                    if (d.estimatedCost > 0) {
                                        Text("₪${d.savedAmount.toInt()} / ₪${d.estimatedCost.toInt()}", fontSize = 10.sp, color = Color.Gray)
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

@Composable
fun CreateDreamDialog(
    defaultList: String,
    onDismiss: () -> Unit,
    onSave: (DreamItem) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(if (defaultList == "הכל") "לבית" else defaultList) }
    var estimatedCostInput by remember { mutableStateOf("") }
    var space by remember {
        // Default: home and trips = shared; app ideas = mine
        mutableStateOf(if (list == "פיתוח ואפליקציות") SpaceType.MINE else SpaceType.SHARED)
    }

    val availableLists = listOf("לבית", "טיסות משפחה", "פיתוח ואפליקציות", "אישי")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("הוספת חלום / יעד", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("שם החלום / הפריט") },
                    modifier = Modifier.fillMaxWidth()
                )

                // List selector
                Text("רשימה:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableLists.forEach { l ->
                        FilterChip(
                            selected = list == l,
                            onClick = {
                                list = l
                                space = if (l == "פיתוח ואפליקציות") SpaceType.MINE else SpaceType.SHARED
                            },
                            label = { Text(l, fontSize = 11.sp) }
                        )
                    }
                }

                // Estimated cost
                OutlinedTextField(
                    value = estimatedCostInput,
                    onValueChange = { estimatedCostInput = it },
                    label = { Text("עלות משוערת (₪) - אופציונלי") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Space selector
                Text("מרחב:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SpaceType.entries.forEach { s ->
                        FilterChip(
                            selected = space == s,
                            onClick = { space = s },
                            label = { Text(s.displayName, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            DreamItem(
                                title = title.trim(),
                                list = list,
                                estimatedCost = estimatedCostInput.toDoubleOrNull() ?: 0.0,
                                space = space.id
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("שמור חלום")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}

@Composable
fun AppIdeaTemplateDialog(
    dream: DreamItem,
    onDismiss: () -> Unit,
    onSave: (questions: Map<String, String>, nextAction: String) -> Unit
) {
    val questionPrompts = listOf(
        "1. מה הבעיה הכואבת?",
        "2. למי זה מיועד בדיוק?",
        "3. מה הפתרון המינימלי (MVP)?",
        "4. מה היתרון הייחודי / הוגן?",
        "5. איך מרוויחים / מודל עסקי?",
        "6. מה הסיכון הכי גדול?",
        "7. מה המדד להצלחה ראשונית?"
    )

    val answers = remember {
        mutableStateMapOf<String, String>().apply {
            questionPrompts.forEach { q ->
                this[q] = dream.appIdeaQuestions[q] ?: ""
            }
        }
    }
    var nextActionInput by remember { mutableStateOf(dream.nextAction) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("תבנית 7 שאלות לרעיון + Next Action", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "ענה בקצרה ובדיוק — מונע התפזרות ועומס קוגניטיבי:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }

                items(questionPrompts) { question ->
                    OutlinedTextField(
                        value = answers[question] ?: "",
                        onValueChange = { answers[question] = it },
                        label = { Text(question, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }

                item {
                    OutlinedTextField(
                        value = nextActionInput,
                        onValueChange = { nextActionInput = it },
                        label = { Text("צעד ראשון מיידי (Next Action)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(answers.toMap(), nextActionInput.trim()) }) {
                Text("שמור תבנית")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("ביטול") }
        }
    )
}
