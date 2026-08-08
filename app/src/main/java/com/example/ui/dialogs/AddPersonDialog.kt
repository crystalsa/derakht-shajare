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
import compose.icons.TablerIcons
import compose.icons.tablericons.*
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
fun AddPersonDialog(
    theme: String,
    textColor: Color,
    accentColor: Color,
    parentName: String? = null,
    groups: List<com.example.data.FamilyGroup> = emptyList(),
    defaultGroupId: Long? = null,
    availableSpouses: List<Person> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, String?, String?, String?, Boolean, String?, String?, Long?, Long?) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var hasBirthDate by remember { mutableStateOf(false) }
    var birthDateInput by remember { mutableStateOf("") }
    var birthPlace by remember { mutableStateOf("") }
    var hasDeathDate by remember { mutableStateOf(false) }
    var deathDateInput by remember { mutableStateOf("") }
    var deathPlace by remember { mutableStateOf("") }
    var isDeceased by remember { mutableStateOf(false) }
    var occupation by remember { mutableStateOf("") }
    var biography by remember { mutableStateOf("") }
    var selectedGroupIdForPerson by remember { mutableStateOf<Long?>(defaultGroupId) }
    var selectedSpouseId by remember(availableSpouses) { mutableStateOf<Long?>(availableSpouses.firstOrNull()?.id) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text(if (parentName != null) "افزودن فرزند برای $parentName" else "افزودن عضو جدید به شجره‌نامه", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AppTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "نام",
                        testTag = "add_first_name"
                    )
                }
                item {
                    AppTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "نام خانوادگی",
                        testTag = "add_last_name"
                    )
                }
                item {
                    Text("جنسیت:", fontWeight = FontWeight.Bold, color = textColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Male", onClick = { gender = "Male" })
                            Text("آقا", modifier = Modifier.clickable { gender = "Male" }, color = textColor)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = gender == "Female", onClick = { gender = "Female" })
                            Text("خانم", modifier = Modifier.clickable { gender = "Female" }, color = textColor)
                        }
                    }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasBirthDate, onCheckedChange = { hasBirthDate = it })
                        Text("ثبت تاریخ تولد و سن", modifier = Modifier.clickable { hasBirthDate = !hasBirthDate }, color = textColor)
                    }
                }
                if (hasBirthDate) {
                    item {
                        InlineFarsiDatePicker(
                            label = "تاریخ تولد:",
                            initialDate = birthDateInput,
                            onDateChanged = { birthDateInput = it }
                        )
                    }
                }
                item {
                    AppTextField(
                        value = birthPlace,
                        onValueChange = { birthPlace = it },
                        label = "محل تولد / زندگی"
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isDeceased, onCheckedChange = { isDeceased = it })
                        Text("عضو فوت شده است", modifier = Modifier.clickable { isDeceased = !isDeceased }, color = textColor)
                    }
                }
                if (isDeceased) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasDeathDate, onCheckedChange = { hasDeathDate = it })
                            Text("ثبت تاریخ فوت", modifier = Modifier.clickable { hasDeathDate = !hasDeathDate }, color = textColor)
                        }
                    }
                    if (hasDeathDate) {
                        item {
                            InlineFarsiDatePicker(
                                label = "تاریخ فوت:",
                                initialDate = deathDateInput,
                                onDateChanged = { deathDateInput = it }
                            )
                        }
                    }
                    item {
                        AppTextField(
                            value = deathPlace,
                            onValueChange = { deathPlace = it },
                            label = "محل فوت"
                        )
                    }
                }
                item {
                    AppTextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = "شغل / پیشه"
                    )
                }
                item {
                    AppTextField(
                        value = biography,
                        onValueChange = { biography = it },
                        label = "شرح حال / بیوگرافی کوتاه",
                        maxLines = 10
                    )
                }
                
                // Group selection dropdown
                item {
                    Text("گروه فامیلی:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    var showGroupDropdown by remember { mutableStateOf(false) }
                    val selectedGroupName = groups.find { it.id == selectedGroupIdForPerson }?.name ?: "انتخاب گروه فامیلی"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { showGroupDropdown = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedGroupName, color = textColor, fontSize = 14.sp)
                            Icon(TablerIcons.ChevronDown, contentDescription = null, tint = accentColor)
                        }
                        DropdownMenu(
                            expanded = showGroupDropdown,
                            onDismissRequest = { showGroupDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                        ) {
                            groups.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.name, fontSize = 13.sp, color = textColor) },
                                    onClick = {
                                        selectedGroupIdForPerson = g.id
                                        showGroupDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Spouse / co-parent selection dropdown
                if (availableSpouses.isNotEmpty()) {
                    item {
                        Text("انتخاب همسر والد (والد دوم):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                        var showSpouseDropdown by remember { mutableStateOf(false) }
                        val selectedSpouse = availableSpouses.find { it.id == selectedSpouseId }
                        val selectedSpouseName = selectedSpouse?.fullName ?: "بدون والد دوم (فقط ثبت برای والد فعلی)"
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { showSpouseDropdown = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                              ) {
                                Text(selectedSpouseName, color = textColor, fontSize = 14.sp)
                                Icon(TablerIcons.ChevronDown, contentDescription = null, tint = accentColor)
                            }
                            DropdownMenu(
                                expanded = showSpouseDropdown,
                                onDismissRequest = { showSpouseDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("بدون والد دوم (فقط ثبت برای والد فعلی)", fontSize = 13.sp, color = textColor) },
                                    onClick = {
                                        selectedSpouseId = null
                                        showSpouseDropdown = false
                                    }
                                )
                                availableSpouses.forEach { sp ->
                                    DropdownMenuItem(
                                        text = { Text(sp.fullName, fontSize = 13.sp, color = textColor) },
                                        onClick = {
                                            selectedSpouseId = sp.id
                                            showSpouseDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    if (firstName.trim().isBlank() || lastName.trim().isBlank()) {
                        Toast.makeText(context, "تکمیل کادرهای نام و نام خانوادگی اجباری است", Toast.LENGTH_LONG).show()
                    } else if (isDeceased && hasDeathDate && hasBirthDate && !validateBirthAndDeathDates(birthDateInput, deathDateInput)) {
                        Toast.makeText(context, "تاریخ فوت نمی‌تواند کوچکتر از تاریخ تولد باشد", Toast.LENGTH_LONG).show()
                    } else {
                        onConfirm(
                            firstName.trim(),
                            lastName.trim(),
                            gender,
                            if (hasBirthDate) birthDateInput.ifBlank { null } else null,
                            birthPlace.ifBlank { null },
                            if (isDeceased && hasDeathDate) deathDateInput.ifBlank { null } else null,
                            if (isDeceased) deathPlace.ifBlank { null } else null,
                            isDeceased,
                            occupation.ifBlank { null },
                            biography.ifBlank { null },
                            selectedGroupIdForPerson,
                            selectedSpouseId
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("ثبت عضو")
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

