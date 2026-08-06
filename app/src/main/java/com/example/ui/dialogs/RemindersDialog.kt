package com.example.ui.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.rememberAsyncImagePainter
import com.example.data.*
import com.example.ui.common.*
import com.example.ui.screens.calculateAge
import com.example.ui.screens.formatLifeDates
import com.example.ui.screens.formatLifeYearsOnlyLTR
import com.example.ui.screens.getCurrentJalaliYear
import com.example.ui.screens.validateBirthAndDeathDates
import com.example.ui.tree.*
import com.example.utils.*
import com.example.viewmodel.FamilyViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersDialog(
    events: List<com.example.viewmodel.FamilyEvent>,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("رویدادها و مناسبت‌های پیش‌رو (۳۰ روز آینده)", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیچ رویدادی در ۳۰ روز آینده یافت نشد.", color = textColor.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(events) { event ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (event.type) {
                                    "Birthday" -> Color(0xFFE8F5E9)
                                    else -> Color(0xFFECEFF1)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (event.type == "Birthday") Icons.Default.Cake else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (event.type == "Birthday") Color(0xFF4CAF50) else Color.DarkGray
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(event.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                        Text(event.description, fontSize = 11.sp, color = Color.DarkGray)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color.White, CircleShape)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (event.daysRemaining == 0) "امروز" else "${event.daysRemaining} روز",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                Text("بستن")
            }
        }
    )
    }
}

// Distance relation calculator dialogue
