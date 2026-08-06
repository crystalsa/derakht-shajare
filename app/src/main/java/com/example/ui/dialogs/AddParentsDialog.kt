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
fun AddParentsDialog(
    child: Person,
    groupId: Long?,
    textColor: Color,
    accentColor: Color,
    existingFather: Person? = null,
    existingMother: Person? = null,
    onDismiss: () -> Unit,
    onConfirm: (Person?, Person?) -> Unit
) {
    // Father states
    val context = androidx.compose.ui.platform.LocalContext.current
    var addFather by remember { mutableStateOf(existingFather == null) }
    var fFirstName by remember { mutableStateOf("") }
    var fLastName by remember { mutableStateOf(child.lastName) }
    var fHasBirthDate by remember { mutableStateOf(false) }
    var fBirthDateInput by remember { mutableStateOf("") }
    var fBirthPlace by remember { mutableStateOf("") }
    var fHasDeathDate by remember { mutableStateOf(false) }
    var fDeathDateInput by remember { mutableStateOf("") }
    var fDeathPlace by remember { mutableStateOf("") }
    var fIsDeceased by remember { mutableStateOf(false) }
    var fOccupation by remember { mutableStateOf("") }
    var fBiography by remember { mutableStateOf("") }

    // Mother states
    var addMother by remember { mutableStateOf(existingMother == null) }
    var mFirstName by remember { mutableStateOf("") }
    var mLastName by remember { mutableStateOf("") }
    var mHasBirthDate by remember { mutableStateOf(false) }
    var mBirthDateInput by remember { mutableStateOf("") }
    var mBirthPlace by remember { mutableStateOf("") }
    var mHasDeathDate by remember { mutableStateOf(false) }
    var mDeathDateInput by remember { mutableStateOf("") }
    var mDeathPlace by remember { mutableStateOf("") }
    var mIsDeceased by remember { mutableStateOf(false) }
    var mOccupation by remember { mutableStateOf("") }
    var mBiography by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("ثبت و افزودن والدین برای ${child.fullName}", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // --- FATHER SECTION ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBF9)),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("مشخصات پدر", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor)
                                    if (existingFather == null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = addFather, onCheckedChange = { addFather = it })
                                            Text("ثبت پدر", fontSize = 12.sp, color = textColor)
                                        }
                                    } else {
                                        Text("پدر قبلاً ثبت شده است", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                if (existingFather != null) {
                                    Text(
                                        "پدر: ${existingFather.fullName}",
                                        fontSize = 14.sp,
                                        color = textColor.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else if (addFather) {
                                    AppTextField(value = fFirstName, onValueChange = { fFirstName = it }, label = "نام پدر")
                                    AppTextField(value = fLastName, onValueChange = { fLastName = it }, label = "نام خانوادگی")
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = fHasBirthDate, onCheckedChange = { fHasBirthDate = it })
                                        Text("ثبت تاریخ تولد و سن پدر", modifier = Modifier.clickable { fHasBirthDate = !fHasBirthDate }, color = textColor)
                                    }
                                    if (fHasBirthDate) {
                                        InlineFarsiDatePicker(
                                            label = "تاریخ تولد پدر:",
                                            initialDate = fBirthDateInput,
                                            onDateChanged = { fBirthDateInput = it }
                                        )
                                    }
                                    
                                    AppTextField(value = fBirthPlace, onValueChange = { fBirthPlace = it }, label = "محل زندگی / تولد")
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = fIsDeceased, onCheckedChange = { fIsDeceased = it })
                                        Text("پدر فوت شده است", modifier = Modifier.clickable { fIsDeceased = !fIsDeceased }, color = textColor)
                                    }
                                    if (fIsDeceased) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = fHasDeathDate, onCheckedChange = { fHasDeathDate = it })
                                            Text("ثبت تاریخ فوت پدر", modifier = Modifier.clickable { fHasDeathDate = !fHasDeathDate }, color = textColor)
                                        }
                                        if (fHasDeathDate) {
                                            InlineFarsiDatePicker(
                                                label = "تاریخ فوت پدر:",
                                                initialDate = fDeathDateInput,
                                                onDateChanged = { fDeathDateInput = it }
                                            )
                                        }
                                        AppTextField(value = fDeathPlace, onValueChange = { fDeathPlace = it }, label = "محل فوت")
                                    }
                                    AppTextField(value = fOccupation, onValueChange = { fOccupation = it }, label = "شغل / پیشه")
                                    AppTextField(value = fBiography, onValueChange = { fBiography = it }, label = "شرح حال / بیوگرافی", maxLines = 10)
                                }
                            }
                        }
                    }

                    // --- MOTHER SECTION ---
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9FA)),
                            border = BorderStroke(1.dp, Color(0xFFF8BBD0).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("مشخصات مادر", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFC2185B))
                                    if (existingMother == null) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = addMother, onCheckedChange = { addMother = it })
                                            Text("ثبت مادر", fontSize = 12.sp, color = textColor)
                                        }
                                    } else {
                                        Text("مادر قبلاً ثبت شده است", fontSize = 12.sp, color = Color(0xFFC2185B), fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                if (existingMother != null) {
                                    Text(
                                        "مادر: ${existingMother.fullName}",
                                        fontSize = 14.sp,
                                        color = textColor.copy(alpha = 0.8f),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else if (addMother) {
                                    AppTextField(value = mFirstName, onValueChange = { mFirstName = it }, label = "نام مادر")
                                    AppTextField(value = mLastName, onValueChange = { mLastName = it }, label = "نام خانوادگی مادر")
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = mHasBirthDate, onCheckedChange = { mHasBirthDate = it })
                                        Text("ثبت تاریخ تولد و سن مادر", modifier = Modifier.clickable { mHasBirthDate = !mHasBirthDate }, color = textColor)
                                    }
                                    if (mHasBirthDate) {
                                        InlineFarsiDatePicker(
                                            label = "تاریخ تولد مادر:",
                                            initialDate = mBirthDateInput,
                                            onDateChanged = { mBirthDateInput = it }
                                        )
                                    }
                                    
                                    AppTextField(value = mBirthPlace, onValueChange = { mBirthPlace = it }, label = "محل زندگی / تولد")
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = mIsDeceased, onCheckedChange = { mIsDeceased = it })
                                        Text("مادر فوت شده است", modifier = Modifier.clickable { mIsDeceased = !mIsDeceased }, color = textColor)
                                    }
                                    if (mIsDeceased) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(checked = mHasDeathDate, onCheckedChange = { mHasDeathDate = it })
                                            Text("ثبت تاریخ فوت مادر", modifier = Modifier.clickable { mHasDeathDate = !mHasDeathDate }, color = textColor)
                                        }
                                        if (mHasDeathDate) {
                                            InlineFarsiDatePicker(
                                                label = "تاریخ فوت مادر:",
                                                initialDate = mDeathDateInput,
                                                onDateChanged = { mDeathDateInput = it }
                                            )
                                        }
                                        AppTextField(value = mDeathPlace, onValueChange = { mDeathPlace = it }, label = "محل فوت")
                                    }
                                    AppTextField(value = mOccupation, onValueChange = { mOccupation = it }, label = "شغل / پیشه")
                                    AppTextField(value = mBiography, onValueChange = { mBiography = it }, label = "شرح حال / بیوگرافی", maxLines = 10)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fIsDeceased && fHasDeathDate && fHasBirthDate && !validateBirthAndDeathDates(fBirthDateInput, fDeathDateInput)) {
                            Toast.makeText(context, "تاریخ فوت پدر نمی‌تواند کوچکتر از تاریخ تولد او باشد", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (mIsDeceased && mHasDeathDate && mHasBirthDate && !validateBirthAndDeathDates(mBirthDateInput, mDeathDateInput)) {
                            Toast.makeText(context, "تاریخ فوت مادر نمی‌تواند کوچکتر از تاریخ تولد او باشد", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        val father = if (existingFather == null && addFather && fFirstName.isNotBlank()) {
                            Person(
                                firstName = fFirstName,
                                lastName = fLastName,
                                gender = "Male",
                                birthDate = if (fHasBirthDate) fBirthDateInput.ifBlank { null } else null,
                                birthPlace = fBirthPlace.ifBlank { null },
                                deathDate = if (fIsDeceased && fHasDeathDate) fDeathDateInput.ifBlank { null } else null,
                                deathPlace = if (fIsDeceased) fDeathPlace.ifBlank { null } else null,
                                isDeceased = fIsDeceased,
                                occupation = fOccupation.ifBlank { null },
                                biography = fBiography.ifBlank { null },
                                groupId = groupId
                            )
                        } else null

                        val mother = if (existingMother == null && addMother && mFirstName.isNotBlank()) {
                            Person(
                                firstName = mFirstName,
                                lastName = mLastName,
                                gender = "Female",
                                birthDate = if (mHasBirthDate) mBirthDateInput.ifBlank { null } else null,
                                birthPlace = mBirthPlace.ifBlank { null },
                                deathDate = if (mIsDeceased && mHasDeathDate) mDeathDateInput.ifBlank { null } else null,
                                deathPlace = if (mIsDeceased) mDeathPlace.ifBlank { null } else null,
                                isDeceased = mIsDeceased,
                                occupation = mOccupation.ifBlank { null },
                                biography = mBiography.ifBlank { null },
                                groupId = groupId
                            )
                        } else null

                        if (father != null || mother != null) {
                            onConfirm(father, mother)
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("ثبت و تایید والدین")
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

// Dialog Component for adding relations
