package com.menash.myself.ui.household

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UserProfileCard(displayName: String, email: String) {
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
                Text(text = displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = email, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SpacesInfoCard() {
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
                title = "שלי",
                desc = "משימות ופריטים שמיועדים עבורך."
            )
            SpaceInfoRow(
                icon = Icons.Default.Favorite,
                color = Color(0xFFEC4899),
                title = "של אשתי",
                desc = "משימות ופריטים שמיועדים עבור בת הזוג."
            )
            SpaceInfoRow(
                icon = Icons.Default.Groups,
                color = Color(0xFF16A34A),
                title = "של שנינו (משותף)",
                desc = "משימות לבית, טיסות, קניות, וכל מה שמנוהל יחד."
            )
        }
    }
}

@Composable
fun PartnerInviteCard(
    partnerEmail: String?,
    partnerUid: String?,
    partnerEmailInput: String,
    onEmailChange: (String) -> Unit,
    onSendInvite: () -> Unit
) {
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
            if (!partnerEmail.isNullOrEmpty()) {
                PartnerStatusBanner(partnerEmail = partnerEmail, isConnected = !partnerUid.isNullOrEmpty())
            }
            Text(
                text = "הזן את כתובת ה-Google של אשתך. ברגע שהיא תתחבר לאפליקציה מחשבונה, היא תצטרף מיידית למשק הבית ותראה את כל הפריטים המשותפים.",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
            OutlinedTextField(
                value = partnerEmailInput,
                onValueChange = onEmailChange,
                label = { Text("אימייל Google של בת הזוג") },
                placeholder = { Text("wife@gmail.com") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().testTag("partner_email_input"),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = onSendInvite,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("send_invite_button")
            ) {
                Text("שלח הזמנה לחשבון Google")
            }
        }
    }
}

@Composable
private fun PartnerStatusBanner(partnerEmail: String, isConnected: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isConnected) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isConnected) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
                contentDescription = null,
                tint = if (isConnected) Color(0xFF059669) else Color(0xFFD97706)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = if (isConnected) "מחובר/ת למשק הבית:" else "הזמנה ממתינה לחיבור:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color(0xFF065F46) else Color(0xFF92400E)
                )
                Text(text = partnerEmail, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SpaceInfoRow(
    icon: ImageVector,
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
