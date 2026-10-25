package com.menash.myself.ui.catalog

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.UploadFile
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
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.OfflineIndicator
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.util.CsvParser
import com.menash.myself.feature.importcatalog.FileCatalogViewModel
import com.menash.myself.ui.common.CsvMappingDialog

data class FileTypeItem(
    val title: String,
    val description: String,
    val feedsModule: String,
    val targetType: String,
    val sampleTemplateCsv: String,
    val targetFields: List<String>,
    val isPhase2: Boolean = false
)

private val defaultCatalogItems = listOf(
    FileTypeItem(
        title = "קובץ חלומות ויעדים (Dreams CSV)",
        description = "מייבא רשימות קניות לבית, טיסות משפחה או רעיונות לפתח.",
        feedsModule = "חלומות ורשימות",
        targetType = "dreams",
        sampleTemplateCsv = "title,list,cost,saved,next_action,space\nספה לסלון,לבית,4500,0,למדוד סלון,משותף\nחופשה באיטליה,טיסות משפחה,12000,2000,לבדוק תאריכים,משותף",
        targetFields = listOf("כותרת חלום", "רשימה", "עלות משוערת", "סכום שנחסך", "צעד הבא", "מרחב")
    ),
    FileTypeItem(
        title = "קובץ משימות (Tasks CSV)",
        description = "מייבא משימות עם פעולה ו-Next Action מוגדר.",
        feedsModule = "משימות יומיות",
        targetType = "tasks",
        sampleTemplateCsv = "task,next_action,date,space\nלהזמין טכנאי תמי 4,להתקשר לשירות לקוחות,28/10/2026,משותף\nלסדר חשבוניות רו\"ח,לפתוח תיקיית הורדות,30/10/2026,שלי",
        targetFields = listOf("כותרת משימה", "צעד הבא", "תאריך יעד", "מרחב (שלי/של אשתי/משותף)")
    ),
    FileTypeItem(
        title = "קובץ מחשבונים (רכבים, מנויים, פנסיה)",
        description = "טבלאות מהמחשבונים הקיימים באפליקציה.",
        feedsModule = "מחשבונים פיננסיים",
        targetType = "calculators",
        sampleTemplateCsv = "Name,PurchasePrice,Resale5Yr,YearlyEnergy,Insurance\nטויוטה קורולה,148000,88000,7200,4800",
        targetFields = listOf("דגם רכב", "מחיר קנייה", "מכירה בעוד 5 שנים")
    ),
    FileTypeItem(
        title = "ייצוא תנועות עו״ש בנקאי [שלב 2]",
        description = "קובץ תנועות בנקאי לסיווג הוצאות ותזרים מזומנים חיובי.",
        feedsModule = "תזרים פיננסי (Phase 2)",
        targetType = "bank_phase2",
        sampleTemplateCsv = "תאריך,תיאור,חובה,זכות,יתרה\n24/10/2026,סופרמרקט,450,,12500",
        targetFields = listOf("תאריך", "תיאור", "סכום"),
        isPhase2 = true
    ),
    FileTypeItem(
        title = "פירוט עסקאות כרטיס אשראי [שלב 2]",
        description = "קובץ פירוט כרטיסי אשראי חודשיים לבקרת הוצאות המשק.",
        feedsModule = "תקציב אשראי (Phase 2)",
        targetType = "credit_phase2",
        sampleTemplateCsv = "תאריך,שם בית עסק,סכום חיוב,קטגוריה\n24/10/2026,דלק,320,רכב",
        targetFields = listOf("תאריך", "שם עסק", "סכום"),
        isPhase2 = true
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCatalogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appContainer = LocalAppContainer.current
    val viewModel: FileCatalogViewModel = viewModel(
        factory = FileCatalogViewModel.factory(
            taskRepository = appContainer.taskRepository,
            dreamRepository = appContainer.dreamRepository,
            errorReporter = appContainer.errorReporter
        )
    )

    val errorState by viewModel.errorState.collectAsState()
    var currentImportType by remember { mutableStateOf<FileTypeItem?>(null) }
    var showCsvMapping by remember { mutableStateOf(false) }
    var importedRows by remember { mutableStateOf<List<List<String>>>(emptyList()) }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
            importedRows = CsvParser.parseCsv(content)
            if (importedRows.isNotEmpty()) {
                showCsvMapping = true
            } else {
                Toast.makeText(context, "הקובץ שנבחר ריק", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineIndicator(isOffline = false)
            if (errorState != null) {
                ErrorState(message = errorState ?: "", onRetry = { viewModel.clearError() })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { CatalogIntroBanner() }
                    items(defaultCatalogItems) { item ->
                        CatalogItemCard(
                            item = item,
                            onDownloadTemplate = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_TEXT, item.sampleTemplateCsv)
                                    putExtra(Intent.EXTRA_SUBJECT, "תבנית ${item.title}.csv")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "שתף / שמור תבנית CSV"))
                            },
                            onUploadFile = {
                                if (item.isPhase2) {
                                    Toast.makeText(context, "פיצ'ר זה ייפתח בשלב 2 של האפליקציה!", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentImportType = item
                                    csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                                }
                            }
                        )
                    }
                }
            }
        }
    }

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
                viewModel.importCsvRows(
                    selectedTargetType = targetItem.targetType,
                    importedRows = importedRows,
                    mapping = mapping
                ) { count ->
                    Toast.makeText(context, "יובאו $count רשומות בהצלחה!", Toast.LENGTH_SHORT).show()
                }
                showCsvMapping = false
                currentImportType = null
            }
        )
    }
}

@Composable
private fun CatalogIntroBanner() {
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

@Composable
private fun CatalogItemCard(
    item: FileTypeItem,
    onDownloadTemplate: () -> Unit,
    onUploadFile: () -> Unit
) {
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
                    SuggestionChip(onClick = {}, label = { Text("בקרוב — שלב 2", fontSize = 10.sp) })
                }
            }
            Text(item.description, fontSize = 12.sp, color = Color.DarkGray)
            Text("מזין את: ${item.feedsModule}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDownloadTemplate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("הורד תבנית", fontSize = 12.sp)
                }
                Button(
                    onClick = onUploadFile,
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
