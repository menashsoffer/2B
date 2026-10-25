package com.menash.myself.ui.catalog

import android.content.Intent
import android.widget.Toast
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
import com.menash.myself.data.firestore.HouseholdRepository
import com.menash.myself.data.firestore.LifeHubRepository
import com.menash.myself.data.io.CsvHelper
import com.menash.myself.data.models.DreamItem
import com.menash.myself.data.models.SpaceType
import com.menash.myself.data.models.TaskItem
import com.menash.myself.ui.common.CsvMappingDialog
import kotlinx.coroutines.launch

data class FileTypeItem(
    val title: String,
    val description: String,
    val feedsModule: String,
    val sampleTemplateCsv: String,
    val targetFields: List<String>,
    val isPhase2: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCatalogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { LifeHubRepository(context) }
    val householdRepo = remember { HouseholdRepository(context) }
    val household by householdRepo.observeHousehold().collectAsState(initial = null)

    var currentImportType by remember { mutableStateOf<FileTypeItem?>(null) }
    var showCsvMapping by remember { mutableStateOf(false) }
    var importedRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }
    var chosenSpaceForImport by remember { mutableStateOf(SpaceType.SHARED) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
            importedRows = CsvHelper.parseCsv(content)
            if (importedRows.isNotEmpty()) {
                showCsvMapping = true
            } else {
                Toast.makeText(context, "הקובץ שנבחר ריק", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val catalogItems = listOf(
        FileTypeItem(
            title = "קובץ חלומות ויעדים (Dreams CSV)",
            description = "מייבא רשימות קניות לבית, טיסות משפחה או רעיונות לפתח.",
            feedsModule = "חלומות ורשימות",
            sampleTemplateCsv = "title,list,cost,link,priority,status\nספה לסלון,לבית,4500,https://ikea.co.il,2,חוסכים/מתכננים\nחופשה באיטליה,טיסות משפחה,12000,,1,בבדיקה",
            targetFields = listOf("כותרת/שם החלום", "רשימה", "עלות משוערת", "קישור", "עדיפות (1-3)", "סטטוס")
        ),
        FileTypeItem(
            title = "קובץ משימות (Tasks CSV)",
            description = "מייבא משימות עם פעולה ו-Next Action מוגדר.",
            feedsModule = "משימות יומיות",
            sampleTemplateCsv = "task,next_action,date\nלהזמין טכנאי תמי 4,להתקשר לשירות לקוחות,28/10/2026\nלסדר חשבוניות רו\"ח,לפתוח תיקיית הורדות,30/10/2026",
            targetFields = listOf("שם המשימה", "צעד ראשון מיידי (Next Action)", "תאריך יעד")
        ),
        FileTypeItem(
            title = "קובץ מחשבונים (רכבים, מנויים, פנסיה)",
            description = "טבלאות מהמחשבונים הקיימים באפליקציה.",
            feedsModule = "מחשבונים פיננסיים",
            sampleTemplateCsv = "Name,PurchasePrice,Resale5Yr,YearlyEnergy,Insurance\nטויוטה קורולה,148000,88000,7200,4800",
            targetFields = listOf("דגם רכב", "מחיר קנייה", "מכירה בעוד 5 שנים")
        ),
        FileTypeItem(
            title = "ייצוא תנועות עו״ש בנקאי [שלב 2]",
            description = "קובץ תנועות בנקאי לסיווג הוצאות ותזרים מזומנים חיובי.",
            feedsModule = "תזרים פיננסי (Phase 2)",
            sampleTemplateCsv = "תאריך,תיאור,חובה,זכות,יתרה\n24/10/2026,סופרמרקט,450,,12500",
            targetFields = listOf("תאריך", "תיאור", "סכום"),
            isPhase2 = true
        ),
        FileTypeItem(
            title = "פירוט עסקאות כרטיס אשראי [שלב 2]",
            description = "קובץ פירוט כרטיסי אשראי חודשיים לבקרת הוצאות המשק.",
            feedsModule = "תקציב אשראי (Phase 2)",
            sampleTemplateCsv = "תאריך,שם בית עסק,סכום חיוב,קטגוריה\n24/10/2026,דלק,320,רכב",
            targetFields = listOf("תאריך", "שם עסק", "סכום"),
            isPhase2 = true
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("קטלוג קבצים — מה אפשר להעלות", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("catalog_back_button")) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("ייבוא קובצי CSV בקלות", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("הורד תבנית לדוגמה, מלא אותה והעלה ישירות לתוך המרחב הרצוי.", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            items(catalogItems) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (item.isPhase2) Color(0xFFF8FAFC) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (item.isPhase2) 0.dp else 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (item.isPhase2) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("בקרוב — שלב 2", fontSize = 10.sp) }
                                )
                            }
                        }

                        Text(item.description, fontSize = 12.sp, color = Color.DarkGray)
                        Text("מזין את: ${item.feedsModule}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Download / Share Template
                            OutlinedButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_TEXT, item.sampleTemplateCsv)
                                        putExtra(Intent.EXTRA_SUBJECT, "תבנית ${item.title}.csv")
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "שתף / שמור תבנית CSV"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("הורד תבנית", fontSize = 12.sp)
                            }

                            // Import Flow Button
                            Button(
                                onClick = {
                                    if (item.isPhase2) {
                                        Toast.makeText(context, "פיצ'ר זה ייפתח בשלב 2 של האפליקציה!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentImportType = item
                                        csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !item.isPhase2
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("העלה קובץ", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Mapping Dialog for Catalog Imports
    if (showCsvMapping && currentImportType != null && importedRows.isNotEmpty()) {
        val targetItem = currentImportType!!
        CsvMappingDialog(
            headers = importedRows.first(),
            previewRows = importedRows.drop(1),
            targetFields = targetItem.targetFields,
            onDismiss = {
                showCsvMapping = false
                currentImportType = null
            },
            onConfirmMap = { mapping ->
                scope.launch {
                    val dataRows = importedRows.drop(1)
                    if (targetItem.feedsModule == "משימות יומיות") {
                        val titleIdx = mapping["שם המשימה"] ?: -1
                        val nextIdx = mapping["צעד ראשון מיידי (Next Action)"] ?: -1
                        val dateIdx = mapping["תאריך יעד"] ?: -1
                        var count = 0
                        dataRows.forEach { row ->
                            val t = row.getOrNull(titleIdx)?.trim() ?: ""
                            if (t.isNotBlank()) {
                                repo.createTask(
                                    title = t,
                                    nextAction = row.getOrNull(nextIdx)?.trim() ?: "",
                                    dueDate = row.getOrNull(dateIdx)?.trim() ?: "",
                                    space = chosenSpaceForImport.id,
                                    household = household
                                )
                                count++
                            }
                        }
                        Toast.makeText(context, "יובאו $count משימות בהצלחה!", Toast.LENGTH_SHORT).show()
                    } else if (targetItem.feedsModule == "חלומות ורשימות") {
                        val titleIdx = mapping["כותרת/שם החלום"] ?: -1
                        val listIdx = mapping["רשימה"] ?: -1
                        val costIdx = mapping["עלות משוערת"] ?: -1
                        var count = 0
                        dataRows.forEach { row ->
                            val t = row.getOrNull(titleIdx)?.trim() ?: ""
                            if (t.isNotBlank()) {
                                repo.createDream(
                                    DreamItem(
                                        title = t,
                                        list = row.getOrNull(listIdx)?.trim() ?: "לבית",
                                        estimatedCost = row.getOrNull(costIdx)?.toDoubleOrNull() ?: 0.0,
                                        space = chosenSpaceForImport.id
                                    ),
                                    household = household
                                )
                                count++
                            }
                        }
                        Toast.makeText(context, "יובאו $count חלומות בהצלחה!", Toast.LENGTH_SHORT).show()
                    }
                    showCsvMapping = false
                    currentImportType = null
                }
            }
        )
    }
}
