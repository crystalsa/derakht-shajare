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
fun PersonProfileContent(
    person: Person,
    relationships: List<Relationship>,
    allPersons: List<Person>,
    spouseList: List<Relationship>,
    parentsList: List<Relationship>,
    childrenList: List<Relationship>,
    siblings: List<Person>,
    textColor: Color,
    dialogOrange: Color,
    dialogAccentOrange: Color,
    onRelativeClick: ((Long) -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "مشخصات فردی",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = dialogAccentOrange
        )
        HorizontalDivider(color = dialogAccentOrange.copy(alpha = 0.4f))

        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("جنسیت:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
            Text(if (person.gender == "Male") "مرد (آقا)" else "زن (خانم)", color = textColor, fontSize = 12.sp)
        }

        if (person.birthDate != null || person.isDeceased) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(if (person.isDeceased) "تاریخ حیات:" else "تاریخ تولد:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                Text(formatLifeDates(person.birthDate, person.deathDate, person.isDeceased, getCurrentJalaliYear()).toFarsiNumbers(), color = textColor, fontSize = 12.sp)
            }
        }

        if (person.birthPlace != null) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("محل زندگی / تولد:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                Text(person.birthPlace, color = textColor, fontSize = 12.sp)
            }
        }

        if (person.isDeceased) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text("وضعیت: متوفی (مرحوم)", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    if (person.deathPlace != null) {
                        Text("محل فوت: ${person.deathPlace}", color = textColor, fontSize = 11.sp)
                    }
                }
            }
        }

        if (person.occupation != null) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("شغل / پیشه:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                Text(person.occupation, color = textColor, fontSize = 12.sp)
            }
        }

        if (person.biography != null) {
            Column {
                Text("بیوگرافی / یادداشت:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                Text(
                    person.biography,
                    color = textColor,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF9F2), RoundedCornerShape(8.dp))
                        .border(1.dp, dialogOrange.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
        }

        Text(
            "روابط ثبت شده",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = dialogAccentOrange,
            modifier = Modifier.padding(top = 8.dp)
        )
        HorizontalDivider(color = dialogAccentOrange.copy(alpha = 0.4f))

        if (spouseList.isNotEmpty()) {
            Text(
                "همسر",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFFC2185B),
                modifier = Modifier.padding(top = 8.dp)
            )
            HorizontalDivider(color = Color(0xFFC2185B).copy(alpha = 0.3f))

            spouseList.forEach { rel ->
                val relativeId = if (rel.personId1 == person.id) rel.personId2 else rel.personId1
                val relative = allPersons.find { it.id == relativeId }
                if (relative != null) {
                    var relTypeName = if (spouseList.size > 1) {
                        val isSecond = isSecondSpouseRelation(rel.type)
                        val labelText = if (isSecond) "همسر دوم" else "همسر اول"
                        val isEx = rel.type == "Divorced" || rel.type == "SecondSpouse_Divorced"
                        if (isEx) "$labelText (سابق)" else labelText
                    } else {
                        val isEx = rel.type == "Divorced" || rel.type == "SecondSpouse_Divorced"
                        if (isEx) "همسر سابق" else "همسر"
                    }

                    val bloodRel = RelationshipCalculator.getBloodRelationshipNameBetweenSpouses(relative, person, allPersons, relationships)
                    if (bloodRel != null) {
                        relTypeName += " ($bloodRel)"
                    }

                    val modifier = if (onRelativeClick != null) {
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFCE4EC), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFC2185B).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .clickable { onRelativeClick(relativeId) }
                            .padding(8.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFCE4EC), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFC2185B).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    }

                    Row(
                        modifier = modifier,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(relative.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                        Text(relTypeName, fontSize = 11.sp, color = Color(0xFFC2185B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (parentsList.isNotEmpty()) {
            Text(
                "والدین (پدر و مادر)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF4A148C),
                modifier = Modifier.padding(top = 8.dp)
            )
            HorizontalDivider(color = Color(0xFF4A148C).copy(alpha = 0.3f))

            parentsList.forEach { rel ->
                val relativeId = if (rel.personId1 == person.id) rel.personId2 else rel.personId1
                val relative = allPersons.find { it.id == relativeId }
                if (relative != null) {
                    val relTypeName = when (rel.type) {
                        "Adoptive-Parent-Child" -> if (relative.gender == "Male") "پدرخوانده" else "مادرخوانده"
                        else -> if (relative.gender == "Male") "پدر" else "مادر"
                    }
                    val modifier = if (onRelativeClick != null) {
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3E5F5), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF4A148C).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .clickable { onRelativeClick(relativeId) }
                            .padding(8.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF3E5F5), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF4A148C).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    }

                    Row(
                        modifier = modifier,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(relative.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                        Text(relTypeName, fontSize = 11.sp, color = Color(0xFF4A148C), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (childrenList.isNotEmpty()) {
            Text(
                "فرزندان",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color(0xFF0288D1),
                modifier = Modifier.padding(top = 8.dp)
            )
            HorizontalDivider(color = Color(0xFF0288D1).copy(alpha = 0.3f))

            childrenList.forEach { rel ->
                val relativeId = if (rel.personId1 == person.id) rel.personId2 else rel.personId1
                val relative = allPersons.find { it.id == relativeId }
                if (relative != null) {
                    val relTypeName = when (rel.type) {
                        "Parent-Child" -> "فرزند"
                        else -> "فرزندخوانده"
                    }
                    val modifier = if (onRelativeClick != null) {
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE1F5FE), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF0288D1).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .clickable { onRelativeClick(relativeId) }
                            .padding(8.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE1F5FE), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF0288D1).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    }

                    Row(
                        modifier = modifier,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(relative.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                        Text(relTypeName, fontSize = 11.sp, color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (spouseList.isEmpty() && parentsList.isEmpty() && childrenList.isEmpty()) {
            Text("رابطه‌ای برای این شخص ثبت نشده است.", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
        }

        if (siblings.isNotEmpty()) {
            Text(
                "خواهران و برادران",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = dialogAccentOrange,
                modifier = Modifier.padding(top = 8.dp)
            )
            HorizontalDivider(color = dialogAccentOrange.copy(alpha = 0.4f))

            siblings.forEach { sib ->
                val relTypeName = if (sib.gender == "Male") "برادر" else "خواهر"
                val modifier = if (onRelativeClick != null) {
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .clickable { onRelativeClick(sib.id) }
                        .padding(8.dp)
                } else {
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                }

                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(sib.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    Text(relTypeName, fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProfileExportCard(
    person: Person,
    relationships: List<Relationship>,
    allPersons: List<Person>,
    spouseList: List<Relationship>,
    parentsList: List<Relationship>,
    childrenList: List<Relationship>,
    siblings: List<Person>,
    textColor: Color,
    dialogOrange: Color,
    dialogAccentOrange: Color,
    onPhotoStateChange: ((AsyncImagePainter.State) -> Unit)? = null
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(
            modifier = Modifier
                .requiredWidth(360.dp)
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .border(3.dp, dialogOrange, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (person.gender == "Male") Color(0xFFBBDEFB) else Color(0xFFF8BBD0))
                            .border(1.5.dp, if (person.photoUris.isNotEmpty()) dialogAccentOrange else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (person.photoUris.isNotEmpty()) {
                            SubcomposeAsyncImage(
                                model = person.photoUris.firstOrNull()?.let { java.io.File(it) },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                onState = { state ->
                                    onPhotoStateChange?.invoke(state)
                                }
                            ) {
                                SubcomposeAsyncImageContent()
                            }
                        } else {
                            Icon(
                                if (person.gender == "Male") TablerIcons.User else TablerIcons.User,
                                contentDescription = null,
                                tint = if (person.gender == "Male") Color(0xFF1976D2) else Color(0xFFC2185B),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = person.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textColor
                        )
                        Text(
                            text = "شناسنامه و مشخصات عضو خانواده",
                            fontSize = 11.sp,
                            color = dialogAccentOrange
                        )
                    }
                }

                HorizontalDivider(color = dialogOrange.copy(alpha = 0.3f), thickness = 1.dp)

                PersonProfileContent(
                    person = person,
                    relationships = relationships,
                    allPersons = allPersons,
                    spouseList = spouseList,
                    parentsList = parentsList,
                    childrenList = childrenList,
                    siblings = siblings,
                    textColor = textColor,
                    dialogOrange = dialogOrange,
                    dialogAccentOrange = dialogAccentOrange,
                    onRelativeClick = null
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سامانه شجره‌نامه فامیل",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "derakht-shajare",
                        fontSize = 9.sp,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun MemberDetailsDialog(
    person: Person,
    relationships: List<Relationship>,
    allPersons: List<Person>,
    theme: String,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onHighlightFrom: () -> Unit,
    onHighlightTo: () -> Unit,
    onAddChild: (Person) -> Unit,
    onAddSpouse: (Person) -> Unit,
    onEditPerson: (Person) -> Unit,
    onMoveRelation: (Person) -> Unit,
    onAddParents: (Person) -> Unit,
    onFocusPerson: (Person) -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    onBackupSubtree: (Person) -> Unit = {},
    onRestoreSubtree: () -> Unit = {},
    onCopySubtreeToNewGroup: (Person) -> Unit = {},
    viewModel: com.example.viewmodel.FamilyViewModel? = null
) {
    val dialogOrange = Color(0xFFF57C00)
    val dialogAccentOrange = Color(0xFFE65100)
    
    val directRelationships = remember(person, relationships) {
        relationships.filter { it.personId1 == person.id || it.personId2 == person.id }
    }

    val parentIds = remember(person, relationships) {
        relationships.filter { (it.type == "Parent-Child" || it.type == "Adoptive-Parent-Child") && it.personId2 == person.id }.map { it.personId1 }
    }

    val siblings = remember(person, relationships, allPersons, parentIds) {
        if (parentIds.isEmpty()) emptyList<Person>() else {
            val siblingIds = relationships.filter { 
                (it.type == "Parent-Child" || it.type == "Adoptive-Parent-Child") && 
                parentIds.contains(it.personId1) && 
                it.personId2 != person.id 
            }.map { it.personId2 }.distinct()
            allPersons.filter { siblingIds.contains(it.id) }
        }
    }

    val spouseList = remember(person, directRelationships) {
        directRelationships.filter { rel ->
            isSpouseRelation(rel.type)
        }
    }

    val parentsList = remember(person, directRelationships) {
        directRelationships.filter { rel ->
            (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && rel.personId2 == person.id
        }
    }

    val childrenList = remember(person, directRelationships) {
        directRelationships.filter { rel ->
            (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && rel.personId1 == person.id
        }
    }

    var isExportingProfile by remember { mutableStateOf(false) }
    var profileExportIntent by remember { mutableStateOf<ProfileExportIntent?>(null) }
    var photoLoadState by remember(person.id, isExportingProfile) { mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty) }
    val profileGraphicsLayer = rememberGraphicsLayer()
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
        modifier = Modifier.border(3.dp, dialogOrange, RoundedCornerShape(24.dp)),
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (person.gender == "Male") Color(0xFFBBDEFB) else Color(0xFFF8BBD0))
                            .border(1.dp, if (person.photoUris.isNotEmpty()) accentColor else Color.Transparent, CircleShape)
                            .clickable { onPhotoClick(person) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (person.photoUris.isNotEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(model = person.photoUris.firstOrNull()?.let { java.io.File(it) }),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                if (person.gender == "Male") TablerIcons.User else TablerIcons.User,
                                contentDescription = null,
                                tint = if (person.gender == "Male") Color(0xFF1976D2) else Color(0xFFC2185B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(person.fullName, fontWeight = FontWeight.Bold, color = textColor)
                }

                // Action buttons and 3-dot menu
                var showActionMenu by remember { mutableStateOf(false) }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isExportingProfile) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(2.dp),
                            strokeWidth = 2.dp,
                            color = dialogAccentOrange
                        )
                    } else {
                        IconButton(
                            onClick = {
                                profileExportIntent = ProfileExportIntent.DOWNLOAD
                                isExportingProfile = true
                            }
                        ) {
                            Icon(
                                TablerIcons.Download,
                                contentDescription = "دانلود عکس پروفایل کامل",
                                tint = dialogAccentOrange
                            )
                        }
                        IconButton(
                            onClick = {
                                profileExportIntent = ProfileExportIntent.SHARE
                                isExportingProfile = true
                            }
                        ) {
                            Icon(
                                TablerIcons.Share,
                                contentDescription = "اشتراک‌گذاری سریع",
                                tint = dialogAccentOrange
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { showActionMenu = true },
                            modifier = Modifier.testTag("member_action_menu")
                        ) {
                            Icon(TablerIcons.DotsVertical, contentDescription = "عملیات", tint = textColor)
                        }
                        DropdownMenu(
                            expanded = showActionMenu,
                            onDismissRequest = { showActionMenu = false },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = Color.White,
                            tonalElevation = 8.dp,
                            shadowElevation = 10.dp,
                            border = BorderStroke(1.5.dp, dialogAccentOrange.copy(alpha = 0.5f))
                        ) {
                            DropdownMenuItem(
                                text = { Text("ویرایش مشخصات عضو", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onEditPerson(person)
                                },
                                leadingIcon = { Icon(TablerIcons.Pencil, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            DropdownMenuItem(
                                text = { Text("افزودن همسر", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onAddSpouse(person)
                                },
                                leadingIcon = { Icon(TablerIcons.Heart, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            DropdownMenuItem(
                                text = { Text("افزودن فرزند (زیرمجموعه)", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onAddChild(person)
                                },
                                leadingIcon = { Icon(TablerIcons.Plus, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            DropdownMenuItem(
                                text = { Text("کپی اطلاعات عضو", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    val dateLabel = if (person.isDeceased) "تاریخ حیات" else "تاریخ تولد"
                                    val dateValue = if (person.birthDate != null || person.isDeceased) formatLifeDates(person.birthDate, person.deathDate, person.isDeceased, getCurrentJalaliYear()).toFarsiNumbers() else "ثبت نشده"
                                    val info = """
                                        نام: ${person.fullName}
                                        جنسیت: ${if (person.gender == "Male") "آقا" else "خانم"}
                                        $dateLabel: $dateValue
                                        محل زندگی: ${person.birthPlace ?: "ثبت نشده"}
                                        شغل: ${person.occupation ?: "ثبت نشده"}
                                        توضیحات: ${person.biography ?: "ثبت نشده"}
                                    """.trimIndent()
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("مشخصات عضو", info)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "اطلاعات عضو در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = { Icon(TablerIcons.Copy, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            DropdownMenuItem(
                                text = { Text("مسیر یابی از این شخص (مبداء)", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onHighlightFrom()
                                },
                                leadingIcon = { Icon(TablerIcons.MapPin, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            DropdownMenuItem(
                                text = { Text("مسیر یابی به این شخص (مقصد)", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onHighlightTo()
                                },
                                leadingIcon = { Icon(TablerIcons.Flag, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            DropdownMenuItem(
                                text = { Text("تغییر یا انتقال ارتباط", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onMoveRelation(person)
                                },
                                leadingIcon = { Icon(TablerIcons.Refresh, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            if (parentsList.size < 2) {
                                DropdownMenuItem(
                                    text = { Text("افزودن پدر و مادر", color = textColor) },
                                    onClick = {
                                        showActionMenu = false
                                        onAddParents(person)
                                    },
                                    leadingIcon = { Icon(TablerIcons.Users, contentDescription = null, tint = dialogAccentOrange) }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("کپی شخص و زیرمجموعه در گروه جدید", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onCopySubtreeToNewGroup(person)
                                },
                                leadingIcon = { Icon(TablerIcons.GitFork, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            DropdownMenuItem(
                                text = { Text("تهیه پشتیبان عضو و زیرمجموعه‌ها", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onBackupSubtree(person)
                                },
                                leadingIcon = { Icon(TablerIcons.CloudUpload, contentDescription = null, tint = dialogAccentOrange) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("حذف این عضو فامیل", color = Color.Red) },
                                onClick = {
                                    showActionMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(TablerIcons.Trash, contentDescription = null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }
        },
        text = {
            Box {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PersonProfileContent(
                        person = person,
                        relationships = relationships,
                        allPersons = allPersons,
                        spouseList = spouseList,
                        parentsList = parentsList,
                        childrenList = childrenList,
                        siblings = siblings,
                        textColor = textColor,
                        dialogOrange = dialogOrange,
                        dialogAccentOrange = dialogAccentOrange,
                        onRelativeClick = { relativeId ->
                            viewModel?.setGlowPersonId(relativeId)
                            onDismiss()
                        }
                    )
                }

                if (isExportingProfile) {
                    Box(
                        modifier = Modifier.size(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = 10000.dp)
                                .requiredWidth(360.dp)
                                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                                .drawWithContent {
                                    profileGraphicsLayer.record { this@drawWithContent.drawContent() }
                                    drawLayer(profileGraphicsLayer)
                                }
                        ) {
                            ProfileExportCard(
                                person = person,
                                relationships = relationships,
                                allPersons = allPersons,
                                spouseList = spouseList,
                                parentsList = parentsList,
                                childrenList = childrenList,
                                siblings = siblings,
                                textColor = textColor,
                                dialogOrange = dialogOrange,
                                dialogAccentOrange = dialogAccentOrange,
                                onPhotoStateChange = { photoLoadState = it }
                            )
                        }
                    }

                    LaunchedEffect(isExportingProfile) {
                        try {
                            if (person.photoUris.isNotEmpty()) {
                                val startTime = System.currentTimeMillis()
                                while (photoLoadState !is AsyncImagePainter.State.Success &&
                                       photoLoadState !is AsyncImagePainter.State.Error &&
                                       (System.currentTimeMillis() - startTime) < 3000
                                ) {
                                    kotlinx.coroutines.delay(30)
                                }
                            }
                            withFrameNanos { }
                            withFrameNanos { }

                            val bitmap = profileGraphicsLayer.toImageBitmap().asAndroidBitmap()
                            if (profileExportIntent == ProfileExportIntent.DOWNLOAD) {
                                com.example.utils.PersonProfileExporter.savePersonProfileImage(context, bitmap, person)
                            } else if (profileExportIntent == ProfileExportIntent.SHARE) {
                                com.example.utils.PersonProfileExporter.sharePersonProfileImage(context, bitmap, person)
                            }
                        } catch (e: Exception) {
                            com.example.utils.AppLogger.e("PROFILE_EXPORT", "خطا در استخراج تصویر پروفایل", e)
                            Toast.makeText(context, "خطا در ایجاد تصویر پروفایل", Toast.LENGTH_SHORT).show()
                        } finally {
                            isExportingProfile = false
                            profileExportIntent = null
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = dialogAccentOrange)
            ) {
                Text("بستن", color = Color.White)
            }
        }
    )
    }
}

// Stats and analytics panel
