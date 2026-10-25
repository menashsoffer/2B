package com.menash.myself.ui.household

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.menash.myself.auth.AuthManager
import com.menash.myself.core.designsystem.component.ErrorState
import com.menash.myself.core.designsystem.component.LoadingState
import com.menash.myself.core.designsystem.component.OfflineIndicator
import com.menash.myself.core.di.LocalAppContainer
import com.menash.myself.core.domain.model.UiState
import com.menash.myself.feature.household.HouseholdViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appContainer = LocalAppContainer.current
    val viewModel: HouseholdViewModel = viewModel(
        factory = HouseholdViewModel.factory(
            householdRepository = appContainer.householdRepository,
            errorReporter = appContainer.errorReporter
        )
    )
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()

    val household by viewModel.household.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var partnerEmailInput by remember { mutableStateOf("") }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ניהול משק בית ומרחבים", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("household_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
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
            when (val state = uiState) {
                is UiState.Loading -> LoadingState()
                is UiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.clearError() })
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    UserProfileCard(
                        displayName = currentUser?.displayName ?: "משתמש מחובר",
                        email = currentUser?.email ?: ""
                    )
                    SpacesInfoCard()
                    PartnerInviteCard(
                        partnerEmail = household?.partnerEmail,
                        partnerUid = household?.partnerUid,
                        partnerEmailInput = partnerEmailInput,
                        onEmailChange = { partnerEmailInput = it },
                        onSendInvite = {
                            val email = partnerEmailInput.trim()
                            val hid = household?.id
                            if (email.isEmpty() || !email.contains("@")) {
                                Toast.makeText(context, "נא להזין כתובת אימייל תקינה", Toast.LENGTH_SHORT).show()
                            } else if (hid.isNullOrEmpty()) {
                                Toast.makeText(context, "שגיאה בטעינת משק הבית", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.invitePartner(hid, email)
                                Toast.makeText(context, "ההזמנה נשמרה בהצלחה!", Toast.LENGTH_SHORT).show()
                                partnerEmailInput = ""
                            }
                        }
                    )
                    OutlinedButton(
                        onClick = { showSignOutConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("התנתק מחשבון Google")
                    }
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("התנתקות מהחשבון") },
            text = { Text("האם אתה בטוח שברצונך להתנתק? תוכל להתחבר שוב בכל עת.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutConfirm = false
                        val credentialManager = androidx.credentials.CredentialManager.create(context)
                        AuthManager.signOut(
                            context = context,
                            credentialManager = credentialManager,
                            scope = scope,
                            onSignOutComplete = onSignOut
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("התנתק")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("ביטול")
                }
            }
        )
    }
}
