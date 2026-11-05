package com.menash.myself.feature.importcatalog.ui

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
import com.menash.myself.feature.importcatalog.ui.CsvMappingDialog

import com.menash.myself.R
import androidx.compose.ui.res.stringResource
data class FileTypeItem(
    val title: String,
    val description: String,
    val feedsModule: String,
    val targetType: String,
    val sampleTemplateCsv: String,
    val targetFields: List<String>,
    val isPhase2: Boolean = false
)

@Composable
private fun getCatalogItems(): List<FileTypeItem> {
    return listOf(
        FileTypeItem(
            title = stringResource(R.string.ui_dreams_221),
            description = stringResource(R.string.ui_str_202),
            feedsModule = stringResource(R.string.ui_str_233),
            targetType = "dreams",
            sampleTemplateCsv = "title,list,cost,saved,next_action,space\nספה לסלון,לבית,4500,0,למדוד סלון,משותף\nחופשה באיטליה,טיסות משפחה,12000,2000,לבדוק תאריכים,משותף",
            targetFields = listOf("כותרת חלום", "רשימה", "עלות משוערת", "סכום שנחסך", "צעד הבא", "מרחב")
        ),
        FileTypeItem(
            title = stringResource(R.string.ui_tasks_csv_228),
            description = stringResource(R.string.ui_str_195),
            feedsModule = stringResource(R.string.ui_str_230),
            targetType = "tasks",
            sampleTemplateCsv = "task,next_action,date,space\nלהזמין טכנאי,להתקשר לשירות לקוחות,28/10/2026,משותף",
            targetFields = listOf("כותרת משימה", "צעד הבא", "תאריך", "מרחב")
        ),
        FileTypeItem(
            title = stringResource(R.string.ui_str_209),
            description = stringResource(R.string.ui_str_236),
            feedsModule = stringResource(R.string.ui_phase_2_226),
            targetType = "bank_phase2",
            sampleTemplateCsv = "תאריך,תיאור,חובה,זכות,יתרה\n24/10/2026,סופרמרקט,450,,12500",
            targetFields = listOf("תאריך", "תיאור", "סכום"),
            isPhase2 = true
        ),
        FileTypeItem(
            title = stringResource(R.string.ui_str_237),
            description = stringResource(R.string.ui_str_234),
            feedsModule = stringResource(R.string.ui_phase_2_207),
            targetType = "credit_phase2",
            sampleTemplateCsv = "תאריך,שם בית עסק,סכום חיוב,קטגוריה\n24/10/2026,דלק,320,רכב",
            targetFields = listOf("תאריך", "שם עסק", "סכום"),
            isPhase2 = true
        )
    )
}

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
    val catalogItems = getCatalogItems()

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() } ?: ""
            importedRows = CsvParser.parseCsv(content)
            if (importedRows.isNotEmpty()) {
                showCsvMapping = true
            } else {
                Toast.makeText(context, context.getString(R.string.ui_str_224), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ui_str_200), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("catalog_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ui_str_60))
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
                    items(catalogItems) { item ->
                        CatalogItemCard(
                            item = item,
                            onDownloadTemplate = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_TEXT, item.sampleTemplateCsv)
                                    putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.ui_itemtitlecsv_231, item.title))
                                }
                                context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.ui_csv_201)))
                            },
                            onUploadFile = {
                                if (item.isPhase2) {
                                    Toast.makeText(context, context.getString(R.string.ui_str_211), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(context, context.getString(R.string.ui_count_208, count), Toast.LENGTH_SHORT).show()
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
                Text(stringResource(R.string.ui_csv_239), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(stringResource(R.string.ui_str_199), fontSize = 12.sp, color = Color.DarkGray)
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
                    SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.ui_2_232), fontSize = 10.sp) })
                }
            }
            Text(item.description, fontSize = 12.sp, color = Color.DarkGray)
            Text(stringResource(R.string.ui_itemfeedsmodule_227, item.feedsModule), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDownloadTemplate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.ui_str_205), fontSize = 12.sp)
                }
                Button(
                    onClick = onUploadFile,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !item.isPhase2
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.ui_str_229), fontSize = 12.sp)
                }
            }
        }
    }
}
