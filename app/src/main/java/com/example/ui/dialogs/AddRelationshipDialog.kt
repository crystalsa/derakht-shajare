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
fun AddRelationshipDialog(
    persons: List<Person>,
    preselectedP1: Long,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, String) -> Unit
) {
    var p1Id by remember { mutableStateOf(if (preselectedP1 != 0L) preselectedP1 else (persons.firstOrNull()?.id ?: 0L)) }
    var p2Id by remember { mutableStateOf(persons.getOrNull(1)?.id ?: (persons.firstOrNull()?.id ?: 0L)) }
    var relationType by remember { mutableStateOf("Spouse") } // "Spouse", "Parent-Child", "Divorced", "Adoptive-Parent-Child"

    var showP1Dropdown by remember { mutableStateOf(false) }
    var showP2Dropdown by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("تعریف رابطه فامیلی جدید", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selector Person 1
                Column {
                    Text("شخص اول (پدر/مادر یا همسر):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    val p1Name = persons.find { it.id == p1Id }?.fullName ?: "انتخاب کنید..."
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { showP1Dropdown = true }
                            .padding(12.dp)
                    ) {
                        Text(p1Name, color = textColor)
                        DropdownMenu(expanded = showP1Dropdown, onDismissRequest = { showP1Dropdown = false }, modifier = Modifier.background(Color.White)) {
                            persons.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.fullName, color = textColor) },
                                    onClick = {
                                        p1Id = p.id
                                        showP1Dropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Selector Person 2
                Column {
                    Text("شخص دوم (فرزند یا همسر دوم):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    val p2Name = persons.find { it.id == p2Id }?.fullName ?: "انتخاب کنید..."
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { showP2Dropdown = true }
                            .padding(12.dp)
                    ) {
                        Text(p2Name, color = textColor)
                        DropdownMenu(expanded = showP2Dropdown, onDismissRequest = { showP2Dropdown = false }, modifier = Modifier.background(Color.White)) {
                            persons.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.fullName, color = textColor) },
                                    onClick = {
                                        p2Id = p.id
                                        showP2Dropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Relation Type selector
                Column {
                    Text("نوع رابطه فامیلی:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    listOf(
                        "Spouse" to "همسر",
                        "Parent-Child" to "پدر یا مادر - فرزند",
                        "Divorced" to "طلاق / متارکه",
                        "Adoptive-Parent-Child" to "فرزندخواندگی"
                    ).forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { relationType = value }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = relationType == value, onClick = { relationType = value })
                            Text(label, color = textColor)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (p1Id != 0L && p2Id != 0L && p1Id != p2Id) {
                        onConfirm(p1Id, p2Id, relationType)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("ثبت رابطه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف", color = textColor)
            }
        }
    )
    }
}

// Dialog for detailed member view
enum class ProfileExportIntent { DOWNLOAD, SHARE }

