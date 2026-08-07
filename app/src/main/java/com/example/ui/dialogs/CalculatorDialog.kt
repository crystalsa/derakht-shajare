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
fun CalculatorDialog(
    persons: List<Person>,
    relationships: List<Relationship>,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onCalculate: (Person, Person) -> Unit
) {
    var p1 by remember { mutableStateOf<Person?>(persons.firstOrNull()) }
    var p2 by remember { mutableStateOf<Person?>(persons.getOrNull(1) ?: persons.firstOrNull()) }

    var p1Dropdown by remember { mutableStateOf(false) }
    var p2Dropdown by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("محاسبه‌گر هوشمند نسبت فامیلی دور", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "دو شخص را در خانواده انتخاب کنید تا نسبت دقیق فامیلی آن‌ها را محاسبه و خط پیوندشان را ترسیم کنیم.",
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.7f)
                )

                // Selector 1
                Column {
                    Text("شخص اول:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { p1Dropdown = true }
                            .padding(12.dp)
                    ) {
                        Text(p1?.fullName ?: "انتخاب کنید...", color = textColor)
                        DropdownMenu(expanded = p1Dropdown, onDismissRequest = { p1Dropdown = false }, modifier = Modifier.background(Color.White)) {
                            persons.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.fullName, color = textColor) },
                                    onClick = {
                                        p1 = p
                                        p1Dropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Selector 2
                Column {
                    Text("شخص دوم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .clickable { p2Dropdown = true }
                            .padding(12.dp)
                    ) {
                        Text(p2?.fullName ?: "انتخاب کنید...", color = textColor)
                        DropdownMenu(expanded = p2Dropdown, onDismissRequest = { p2Dropdown = false }, modifier = Modifier.background(Color.White)) {
                            persons.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text(p.fullName, color = textColor) },
                                    onClick = {
                                        p2 = p
                                        p2Dropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (p1 != null && p2 != null) {
                    val computed = remember(p1, p2, persons, relationships) {
                        RelationshipCalculator.getRelationshipLabel(p1!!, p2!!, persons, relationships)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("نسبت فامیلی:", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(
                                computed.toFarsiNumbers(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (p1 != null && p2 != null) {
                Button(
                    onClick = { onCalculate(p1!!, p2!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("ترسیم و روشن کردن خط رابطه")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن", color = textColor)
            }
        }
    )
    }
}

