package com.menash.myself.ui.household

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.menash.myself.auth.AuthManager
import com.menash.myself.data.firestore.HouseholdRepository
import com.menash.myself.data.models.Household
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { HouseholdRepository(context) }
    val currentUser = Firebase.auth.currentUser

    var household by remember { mutableStateOf<Household?>(null) }
    var partnerEmailInput by remember { mutableStateOf("") }
    var isInviting by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        household = repo.getOrCreateHousehold()
        repo.observeHousehold().collect { h ->
            if (h != null) household = h
        }
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Signed-in user card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().testTag("user_profile_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "משתמש מחובר",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Spaces explanation card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("spaces_info_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "3 המרחבים של הבית",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "לכל פריט באפליקציה יש שיוך ברור למרחב כדי למנוע עומס ובלאגן:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )

                    SpaceInfoRow(
                        icon = Icons.Default.Person,
                        color = Color(0xFF2563EB),
                        title = "שלי (פרטי)",
                        desc = "רק אתה רואה ועורך. מתאים למשימות אישיות ורעיונות לפיתוח."
                    )
                    SpaceInfoRow(
                        icon = Icons.Default.Favorite,
                        color = Color(0xFFEC4899),
                        title = "של אשתי (פרטי)",
                        desc = "רק אשתך רואה ועורכת כשהיא מחוברת מחשבונה."
                    )
                    SpaceInfoRow(
                        icon = Icons.Default.Groups,
                        color = Color(0xFF16A34A),
                        title = "משותף (לשנינו)",
                        desc = "משימות לבית, טיסות, קניות, וכל מה שמנוהל יחד."
                    )
                }
            }

            // Partner Invitation Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().testTag("partner_invite_card")
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("חיבור בן/בת הזוג לבית", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    val partnerEmail = household?.partnerEmail
                    val partnerUid = household?.partnerUid

                    if (!partnerEmail.isNullOrEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (!partnerUid.isNullOrEmpty()) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (!partnerUid.isNullOrEmpty()) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = if (!partnerUid.isNullOrEmpty()) Color(0xFF059669) else Color(0xFFD97706)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (!partnerUid.isNullOrEmpty()) "מחובר/ת למשק הבית:" else "הזמנה ממתינה לחיבור:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!partnerUid.isNullOrEmpty()) Color(0xFF065F46) else Color(0xFF92400E)
                                    )
                                    Text(text = partnerEmail, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Text(
                        text = "הזן את כתובת ה-Google של אשתך. ברגע שהיא תתחבר לאפליקציה מחשבונה, היא תצטרף מיידית למשק הבית ותראה את כל הפריטים המשותפים.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = partnerEmailInput,
                        onValueChange = { partnerEmailInput = it },
                        label = { Text("אימייל Google של בת הזוג") },
                        placeholder = { Text("wife@gmail.com") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("partner_email_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            val email = partnerEmailInput.trim()
                            if (email.isEmpty() || !email.contains("@")) {
                                Toast.makeText(context, "נא להזין כתובת אימייל תקינה", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val hid = household?.id
                            if (hid.isNullOrEmpty()) {
                                Toast.makeText(context, "שגיאה בטעינת משק הבית", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isInviting = true
                            scope.launch {
                                try {
                                    repo.invitePartner(hid, email)
                                    Toast.makeText(context, "ההזמנה נשמרה בהצלחה!", Toast.LENGTH_SHORT).show()
                                    partnerEmailInput = ""
                                } catch (e: Exception) {
                                    Toast.makeText(context, "שגיאה בשמירת הזמנה: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally {
                                    isInviting = false
                                }
                            }
                        },
                        enabled = !isInviting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("send_invite_button")
                    ) {
                        if (isInviting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("שלח הזמנה לחשבון Google")
                        }
                    }
                }
            }

            // Sign out button
            OutlinedButton(
                onClick = { showSignOutConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("התנתק מחשבון Google")
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

@Composable
private fun SpaceInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
            Text(desc, fontSize = 11.sp, color = Color.DarkGray)
        }
    }
}
