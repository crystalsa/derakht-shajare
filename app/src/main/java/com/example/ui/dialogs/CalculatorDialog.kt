package com.example.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import coil.compose.rememberAsyncImagePainter
import com.example.data.FamilyGroup
import com.example.data.Person
import com.example.data.Relationship
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import com.example.ui.common.toFarsiNumbers
import com.example.utils.RelationshipCalculator
import java.io.File

@Composable
fun CalculatorDialog(
    availableGroups: List<FamilyGroup>,
    selectedGroupId: Long?,
    onGroupChanged: (Long) -> Unit,
    persons: List<Person>,
    relationships: List<Relationship>,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onCalculate: (Person, Person, Long) -> Unit
) {
    var p1 by remember(persons) { mutableStateOf<Person?>(persons.firstOrNull()) }
    var p2 by remember(persons) { mutableStateOf<Person?>(persons.getOrNull(1) ?: persons.firstOrNull()) }

    var p1Dropdown by remember { mutableStateOf(false) }
    var p2Dropdown by remember { mutableStateOf(false) }
    var groupDropdown by remember { mutableStateOf(false) }

    val currentGroup = remember(availableGroups, selectedGroupId) {
        availableGroups.find { it.id == selectedGroupId } ?: availableGroups.firstOrNull()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(2.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(28.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(accentColor.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TablerIcons.Calculator,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "محاسبه نسبت فامیلی",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = textColor
                        )
                        Text(
                            "محاسبه اختصاصی اعضای درخت جاری",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tree / Group selector or Indicator
                    if (availableGroups.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "انتخاب درخت فامیلی:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF5F7FA), RoundedCornerShape(14.dp))
                                    .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .clickable { groupDropdown = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            TablerIcons.Folder,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = currentGroup?.name ?: "انتخاب درخت...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = accentColor
                                    )
                                }

                                DropdownMenu(
                                    expanded = groupDropdown,
                                    onDismissRequest = { groupDropdown = false },
                                    modifier = Modifier.background(Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                ) {
                                    availableGroups.forEach { grp ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    grp.name,
                                                    fontWeight = if (grp.id == currentGroup?.id) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (grp.id == currentGroup?.id) accentColor else textColor
                                                )
                                            },
                                            onClick = {
                                                onGroupChanged(grp.id)
                                                groupDropdown = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    TablerIcons.Folder,
                                                    contentDescription = null,
                                                    tint = if (grp.id == currentGroup?.id) accentColor else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (currentGroup != null) {
                        Surface(
                            color = accentColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    TablerIcons.Folder,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "درخت فعال: ${currentGroup.name}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }

                    if (persons.size < 2) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    TablerIcons.AlertCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (persons.isEmpty()) "هیچ عضوی در این درخت وجود ندارد." else "برای محاسبه نسبت فامیلی، حداقل باید ۲ عضو در این درخت وجود داشته باشد.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100),
                                    textAlign = TextAlign.Center,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Selector 1
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("شخص مبدأ (اول):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F9FB), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                                    .clickable { p1Dropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PersonMiniAvatar(person = p1, accentColor = accentColor)
                                        Text(
                                            p1?.fullName ?: "انتخاب شخص...",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor.copy(alpha = 0.6f))
                                }

                                DropdownMenu(
                                    expanded = p1Dropdown,
                                    onDismissRequest = { p1Dropdown = false },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = Color.White,
                                    tonalElevation = 6.dp,
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                ) {
                                    persons.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    PersonMiniAvatar(person = p, accentColor = accentColor)
                                                    Text(p.fullName, color = textColor, fontSize = 13.sp)
                                                }
                                            },
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("شخص مقصد (دوم):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF9F9FB), RoundedCornerShape(14.dp))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(14.dp))
                                    .clickable { p2Dropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        PersonMiniAvatar(person = p2, accentColor = accentColor)
                                        Text(
                                            p2?.fullName ?: "انتخاب شخص...",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textColor.copy(alpha = 0.6f))
                                }

                                DropdownMenu(
                                    expanded = p2Dropdown,
                                    onDismissRequest = { p2Dropdown = false },
                                    shape = RoundedCornerShape(16.dp),
                                    containerColor = Color.White,
                                    tonalElevation = 6.dp,
                                    shadowElevation = 8.dp,
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                                ) {
                                    persons.forEach { p ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    PersonMiniAvatar(person = p, accentColor = accentColor)
                                                    Text(p.fullName, color = textColor, fontSize = 13.sp)
                                                }
                                            },
                                            onClick = {
                                                p2 = p
                                                p2Dropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Relationship Result Card
                        if (p1 != null && p2 != null) {
                            val computed = remember(p1, p2, persons, relationships) {
                                RelationshipCalculator.getRelationshipLabel(p1!!, p2!!, persons, relationships)
                            }

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (p1?.id == p2?.id) Color(0xFFFFF9C4) else accentColor.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "نسبت فامیلی:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = if (p1?.id == p2?.id) "یک شخص انتخاب شده است (خودش)" else computed.toFarsiNumbers(),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (persons.size >= 2 && p1 != null && p2 != null && p1?.id != p2?.id && currentGroup != null) {
                    Button(
                        onClick = { onCalculate(p1!!, p2!!, currentGroup.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(TablerIcons.GitFork, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ترسیم و روشن کردن خط رابطه", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("بستن", color = textColor, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun PersonMiniAvatar(person: Person?, accentColor: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        val photo = person?.photoUri
        if (!photo.isNullOrBlank() && File(photo).exists()) {
            Image(
                painter = rememberAsyncImagePainter(model = File(photo)),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = if (person?.gender == "Male") "👨" else "👩",
                fontSize = 14.sp
            )
        }
    }
}
