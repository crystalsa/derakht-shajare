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
fun FamilyOverviewStatsDialog(
    groupName: String,
    persons: List<com.example.data.Person>,
    relationships: List<com.example.data.Relationship>,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onPersonClick: (com.example.data.Person) -> Unit
) {
    // Dynamic calculations
    val totalCount = persons.size
    
    val maleCount = remember(persons) {
        persons.count { it.gender == "Male" }
    }
    val femaleCount = remember(persons) {
        persons.count { it.gender == "Female" }
    }
    
    val parentChildRels = remember(relationships) {
        relationships.filter { it.type == "Parent-Child" || it.type == "Adoptive-Parent-Child" }
    }
    val childrenIds = remember(parentChildRels) {
        parentChildRels.map { it.personId2 }.toSet()
    }
    val spouseRels = remember(relationships) {
        relationships.filter { it.type == "Spouse" }
    }
    
    val maxGen = remember(persons) {
        persons.maxOfOrNull { it.generation } ?: 0
    }
    val totalGenerations = if (totalCount > 0) maxGen + 1 else 0
    
    val mainRoot = remember(persons) {
        persons.filter { it.generation == 0 }.find { it.gender == "Male" }
            ?: persons.minByOrNull { it.generation }
    }
    
    val brides = remember(persons, spouseRels, childrenIds, mainRoot) {
        persons.filter { p ->
            p.id != mainRoot?.id &&
            p.gender == "Female" &&
            !childrenIds.contains(p.id) &&
            spouseRels.any { it.personId1 == p.id || it.personId2 == p.id }
        }
    }
    
    val grooms = remember(persons, spouseRels, childrenIds, mainRoot) {
        persons.filter { p ->
            p.id != mainRoot?.id &&
            p.gender == "Male" &&
            !childrenIds.contains(p.id) &&
            spouseRels.any { it.personId1 == p.id || it.personId2 == p.id }
        }
    }
    
    val children = remember(persons, childrenIds) {
        persons.filter { childrenIds.contains(it.id) && it.generation == 1 }
    }
    
    val grandchildren = remember(persons, childrenIds) {
        persons.filter { childrenIds.contains(it.id) && it.generation == 2 }
    }
    
    val greatGrandchildren = remember(persons, childrenIds) {
        persons.filter { childrenIds.contains(it.id) && it.generation == 3 }
    }
    
    val greatGreatGrandchildren = remember(persons, childrenIds) {
        persons.filter { childrenIds.contains(it.id) && it.generation == 4 }
    }
    
    val livingCount = remember(persons) {
        persons.count { !it.isDeceased }
    }
    
    val deceasedCount = remember(persons) {
        persons.count { it.isDeceased }
    }
    
    // Additional statistics
    val birthPlaces = remember(persons) {
        persons.mapNotNull { it.birthPlace?.trim() }.filter { it.isNotEmpty() }
    }
    val topBirthPlaces = remember(birthPlaces) {
        birthPlaces.groupBy { it }
            .entries.sortedByDescending { it.value.size }
            .take(2)
            .map { "${it.key} (${it.value.size} نفر)" }
    }
    
    val occupations = remember(persons) {
        persons.mapNotNull { it.occupation?.trim() }.filter { it.isNotEmpty() }
    }
    val topOccupations = remember(occupations) {
        occupations.groupBy { it }
            .entries.sortedByDescending { it.value.size }
            .take(2)
            .map { "${it.key} (${it.value.size} نفر)" }
    }
    
    val averageAge = remember(persons) {
        val currentYear = getCurrentJalaliYear() // solar hijri equivalent
        val ageList = persons.mapNotNull { p ->
            val birthYearStr = p.birthDate?.split("-")?.firstOrNull()?.filter { it.isDigit() }
            val birthYear = birthYearStr?.toIntOrNull()
            if (birthYear != null) {
                if (p.isDeceased) {
                    val deathYearStr = p.deathDate?.split("-")?.firstOrNull()?.filter { it.isDigit() }
                    val deathYear = deathYearStr?.toIntOrNull()
                    if (deathYear != null) {
                        (deathYear - birthYear).coerceAtLeast(0)
                    } else null
                } else {
                    (currentYear - birthYear).coerceAtLeast(0)
                }
            } else null
        }
        if (ageList.isNotEmpty()) ageList.average().toInt() else null
    }

    var expandedSection by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, accentColor, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = TablerIcons.ChartBar,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "گزارش جامع $groupName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = textColor
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = TablerIcons.X,
                                contentDescription = "بستن",
                                tint = textColor
                            )
                        }
                    }

                    HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)

                    // Scrollable report items
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Total members
                        if (totalCount > 0) {
                            item {
                                StatRowItem(
                                    title = "تعداد کل اعضای خانواده",
                                    value = "${totalCount.toFarsiNumbers()} نفر",
                                    icon = "👥",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = persons,
                                    isExpanded = expandedSection == "total",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "total") null else "total"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // Male members count
                        if (maleCount > 0) {
                            item {
                                StatRowItem(
                                    title = "تعداد اعضای مذکر (آقایان)",
                                    value = "${maleCount.toFarsiNumbers()} نفر",
                                    icon = "👨",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = persons.filter { it.gender == "Male" },
                                    isExpanded = expandedSection == "males",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "males") null else "males"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // Female members count
                        if (femaleCount > 0) {
                            item {
                                StatRowItem(
                                    title = "تعداد اعضای مونث (بانوان)",
                                    value = "${femaleCount.toFarsiNumbers()} نفر",
                                    icon = "👩",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = persons.filter { it.gender == "Female" },
                                    isExpanded = expandedSection == "females",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "females") null else "females"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 2. Generations
                        if (totalGenerations > 0) {
                            item {
                                StatRowItem(
                                    title = "تعداد نسل‌ها در شجره‌نامه",
                                    value = "${totalGenerations.toFarsiNumbers()} نسل",
                                    icon = "🧬",
                                    accentColor = accentColor,
                                    textColor = textColor
                                )
                            }
                        }

                        // 3. Brides
                        if (brides.isNotEmpty()) {
                            item {
                                StatRowItem(
                                    title = "تعداد عروس‌های خانواده",
                                    value = "${brides.size.toFarsiNumbers()} نفر",
                                    icon = "👰",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = brides,
                                    isExpanded = expandedSection == "brides",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "brides") null else "brides"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 4. Grooms
                        if (grooms.isNotEmpty()) {
                            item {
                                StatRowItem(
                                    title = "تعداد دامادهای خانواده",
                                    value = "${grooms.size.toFarsiNumbers()} نفر",
                                    icon = "🤵",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = grooms,
                                    isExpanded = expandedSection == "grooms",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "grooms") null else "grooms"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 5. Children
                        if (children.isNotEmpty()) {
                            item {
                                StatRowItem(
                                    title = "تعداد فرزندان (نسل اول)",
                                    value = "${children.size.toFarsiNumbers()} نفر",
                                    icon = "👶",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = children,
                                    isExpanded = expandedSection == "children",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "children") null else "children"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 6. Grandchildren
                        if (grandchildren.isNotEmpty()) {
                            item {
                                StatRowItem(
                                    title = "تعداد نوه‌ها (نسل دوم)",
                                    value = "${grandchildren.size.toFarsiNumbers()} نفر",
                                    icon = "🪁",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = grandchildren,
                                    isExpanded = expandedSection == "grandchildren",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "grandchildren") null else "grandchildren"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 7. Great-grandchildren
                        if (greatGrandchildren.isNotEmpty()) {
                            item {
                                StatRowItem(
                                    title = "تعداد نبیره‌ها (نسل سوم)",
                                    value = "${greatGrandchildren.size.toFarsiNumbers()} نفر",
                                    icon = "🧸",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = greatGrandchildren,
                                    isExpanded = expandedSection == "greatGrandchildren",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "greatGrandchildren") null else "greatGrandchildren"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 8. Great-great-grandchildren
                        if (greatGreatGrandchildren.isNotEmpty()) {
                            item {
                                StatRowItem(
                                    title = "تعداد ندیده‌ها (نسل چهارم)",
                                    value = "${greatGreatGrandchildren.size.toFarsiNumbers()} نفر",
                                    icon = "🐣",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = greatGreatGrandchildren,
                                    isExpanded = expandedSection == "greatGreatGrandchildren",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "greatGreatGrandchildren") null else "greatGreatGrandchildren"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 9. Living
                        if (livingCount > 0) {
                            item {
                                StatRowItem(
                                    title = "اعضای در قید حیات",
                                    value = "${livingCount.toFarsiNumbers()} نفر",
                                    icon = "🌱",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = persons.filter { !it.isDeceased },
                                    isExpanded = expandedSection == "living",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "living") null else "living"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 10. Deceased
                        if (deceasedCount > 0) {
                            item {
                                StatRowItem(
                                    title = "تعداد درگذشتگان (آسمانی شده)",
                                    value = "${deceasedCount.toFarsiNumbers()} نفر",
                                    icon = "🕯️",
                                    accentColor = accentColor,
                                    textColor = textColor,
                                    members = persons.filter { it.isDeceased },
                                    isExpanded = expandedSection == "deceased",
                                    onToggleExpand = {
                                        expandedSection = if (expandedSection == "deceased") null else "deceased"
                                    },
                                    onPersonClick = onPersonClick
                                )
                            }
                        }

                        // 11. Average age
                        if (averageAge != null) {
                            item {
                                StatRowItem(title = "میانگین سن اعضای خانواده", value = "${averageAge.toFarsiNumbers()} سال", icon = "📅", accentColor = accentColor, textColor = textColor)
                            }
                        }

                        // 12. Top Birthplaces
                        if (topBirthPlaces.isNotEmpty()) {
                            item {
                                StatRowItem(title = "زادگاه اصلی و غالب خاندان", value = topBirthPlaces.joinToString("، "), icon = "🏡", accentColor = accentColor, textColor = textColor)
                            }
                        }

                        // 13. Top Occupations
                        if (topOccupations.isNotEmpty()) {
                            item {
                                StatRowItem(title = "پیشه و مشاغل غالب خاندان", value = topOccupations.joinToString("، "), icon = "💼", accentColor = accentColor, textColor = textColor)
                            }
                        }
                    }

                    HorizontalDivider(color = textColor.copy(alpha = 0.1f), thickness = 1.dp)

                    // Footer button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("متوجه شدم", color = Color.White)
                    }
                }
            }
        }
    }
}

