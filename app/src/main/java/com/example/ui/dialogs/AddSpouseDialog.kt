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
fun AddSpouseDialog(
    spouseOf: Person,
    groups: List<com.example.data.FamilyGroup>,
    allPersons: List<Person>,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, String?, String?, String?, Boolean, String?, String?, Long?, String) -> Unit,
    onConfirmExisting: (Long, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val defaultGender = if (spouseOf.gender == "Male") "Female" else "Male"
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf(spouseOf.lastName) }
    var gender by remember { mutableStateOf(defaultGender) }
    var hasBirthDate by remember { mutableStateOf(false) }
    var birthDateInput by remember { mutableStateOf("") }
    var birthPlace by remember { mutableStateOf("") }
    var hasDeathDate by remember { mutableStateOf(false) }
    var deathDateInput by remember { mutableStateOf("") }
    var deathPlace by remember { mutableStateOf("") }
    var isDeceased by remember { mutableStateOf(false) }
    var occupation by remember { mutableStateOf("") }
    var biography by remember { mutableStateOf("") }
    var selectedGroupIdForPerson by remember { mutableStateOf<Long?>(spouseOf.groupId) }
    var isSecondSpouse by remember { mutableStateOf(false) }
    var isDivorced by remember { mutableStateOf(false) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedExistingPerson by remember { mutableStateOf<Person?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("ثبت همسر برای ${spouseOf.fullName}", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column {
                    androidx.compose.material3.TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = accentColor,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        androidx.compose.material3.Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("شخص جدید", fontWeight = FontWeight.Bold) }
                        )
                        androidx.compose.material3.Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("انتخاب از درخت", fontWeight = FontWeight.Bold) }
                        )
                    }
                    
                    if (selectedTab == 0) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item { AppTextField(value = firstName, onValueChange = { firstName = it }, label = "نام") }
                            item { AppTextField(value = lastName, onValueChange = { lastName = it }, label = "نام خانوادگی") }
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
                                item { InlineFarsiDatePicker(label = "تاریخ تولد:", initialDate = birthDateInput, onDateChanged = { birthDateInput = it }) }
                            }
                            item { AppTextField(value = birthPlace, onValueChange = { birthPlace = it }, label = "محل زندگی / تولد") }
                            item {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = isSecondSpouse, onCheckedChange = { isSecondSpouse = it })
                                    Text("همسر دوم است", modifier = Modifier.clickable { isSecondSpouse = !isSecondSpouse }, color = textColor)
                                }
                            }
                            if (isSecondSpouse) {
                                item {
                                    androidx.compose.material3.Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFBC02D).copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("تنظیمات همسر دوم:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = isDivorced, onCheckedChange = { isDivorced = it })
                                                Text("مطلقه (جدا شده)", modifier = Modifier.clickable { isDivorced = !isDivorced }, color = textColor, fontSize = 12.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = isDeceased, onCheckedChange = { isDeceased = it })
                                                Text("فوت شده", modifier = Modifier.clickable { isDeceased = !isDeceased }, color = textColor, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isDeceased, onCheckedChange = { isDeceased = it })
                                        Text("عضو فوت شده است", modifier = Modifier.clickable { isDeceased = !isDeceased }, color = textColor)
                                    }
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
                                    item { InlineFarsiDatePicker(label = "تاریخ فوت:", initialDate = deathDateInput, onDateChanged = { deathDateInput = it }) }
                                }
                                item { AppTextField(value = deathPlace, onValueChange = { deathPlace = it }, label = "محل فوت") }
                            }
                            item { AppTextField(value = occupation, onValueChange = { occupation = it }, label = "شغل / پیشه") }
                            item { AppTextField(value = biography, onValueChange = { biography = it }, label = "شرح حال / بیوگرافی کوتاه", maxLines = 10) }
                            item {
                                Text("گروه فامیلی همسر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                var showGroupDropdown by remember { mutableStateOf(false) }
                                val selectedGroupName = groups.find { it.id == selectedGroupIdForPerson }?.name ?: "انتخاب گروه فامیلی"
                                Box(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).clickable { showGroupDropdown = true }.padding(12.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        Text(selectedGroupName, color = textColor, fontSize = 14.sp)
                                        Icon(TablerIcons.ChevronDown, contentDescription = null, tint = accentColor)
                                    }
                                    DropdownMenu(expanded = showGroupDropdown, onDismissRequest = { showGroupDropdown = false }, modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)) {
                                        groups.forEach { g ->
                                            DropdownMenuItem(
                                                text = { Text(g.name, fontSize = 13.sp, color = textColor) },
                                                onClick = { selectedGroupIdForPerson = g.id; showGroupDropdown = false }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("جستجو در اعضا...", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val filtered = allPersons.filter { it.id != spouseOf.id && (searchQuery.isBlank() || it.fullName.contains(searchQuery, ignoreCase = true)) }
                                items(filtered) { p ->
                                    val isSelected = selectedExistingPerson?.id == p.id
                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(if (isSelected) accentColor.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp)).border(1.dp, if (isSelected) accentColor else Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(8.dp)).clickable { selectedExistingPerson = p }.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(p.fullName, color = textColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                                        if (isSelected) Icon(TablerIcons.Check, contentDescription = null, tint = accentColor)
                                    }
                                }
                            }
                            androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)), border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isSecondSpouse, onCheckedChange = { isSecondSpouse = it })
                                        Text("همسر دوم است", modifier = Modifier.clickable { isSecondSpouse = !isSecondSpouse }, color = textColor, fontSize = 12.sp)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = isDivorced, onCheckedChange = { isDivorced = it })
                                        Text("مطلقه (جدا شده)", modifier = Modifier.clickable { isDivorced = !isDivorced }, color = textColor, fontSize = 12.sp)
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
                        if (selectedTab == 0) {
                            if (firstName.trim().isBlank() || lastName.trim().isBlank()) {
                        Toast.makeText(context, "تکمیل کادرهای نام و نام خانوادگی اجباری است", Toast.LENGTH_LONG).show()
                    } else if (isDeceased && hasDeathDate && hasBirthDate && !validateBirthAndDeathDates(birthDateInput, deathDateInput)) {
                        Toast.makeText(context, "تاریخ فوت نمی‌تواند کوچکتر از تاریخ تولد باشد", Toast.LENGTH_LONG).show()
                            } else {
                                val relType = if (isSecondSpouse) {
                                    if (isDivorced) "SecondSpouse_Divorced" else "SecondSpouse"
                                } else {
                                    if (isDivorced) "Divorced" else "Spouse"
                                }
                                onConfirm(firstName.trim(), lastName.trim(), gender, if (hasBirthDate) birthDateInput.ifBlank { null } else null, birthPlace.ifBlank { null }, if (isDeceased && hasDeathDate) deathDateInput.ifBlank { null } else null, if (isDeceased) deathPlace.ifBlank { null } else null, isDeceased, occupation.ifBlank { null }, biography.ifBlank { null }, selectedGroupIdForPerson, relType)
                            }
                        } else {
                            if (selectedExistingPerson == null) {
                                Toast.makeText(context, "لطفاً یک نفر را از لیست انتخاب کنید", Toast.LENGTH_LONG).show()
                            } else {
                                val relType = if (isSecondSpouse) {
                                    if (isDivorced) "SecondSpouse_Divorced" else "SecondSpouse"
                                } else {
                                    if (isDivorced) "Divorced" else "Spouse"
                                }
                                onConfirmExisting(selectedExistingPerson!!.id, relType)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("ثبت همسر")
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

