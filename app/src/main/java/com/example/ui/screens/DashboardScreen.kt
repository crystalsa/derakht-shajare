package com.example.ui.screens

import android.widget.Toast
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.roundToInt
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Person
import com.example.data.Relationship
import com.example.utils.RelationshipCalculator
import com.example.viewmodel.FamilyEvent
import com.example.viewmodel.FamilyViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

fun isSpouseRelation(type: String): Boolean {
    return type == "Spouse" || type == "Divorced" || type == "SecondSpouse" || type == "SecondSpouse_Divorced"
}

fun isSecondSpouseRelation(type: String): Boolean {
    return type == "SecondSpouse" || type == "SecondSpouse_Divorced"
}

val Person.photoUris: List<String>
    get() = if (photoUri.isNullOrBlank()) emptyList() else photoUri.split('|').filter { it.isNotBlank() }

fun getFullOrOriginalPhotoPath(photoPath: String): String {
    try {
        val file = java.io.File(photoPath)
        if (file.name.startsWith("person_cropped_")) {
            val originalFile = java.io.File(file.parent, file.name.replace("person_cropped_", "person_original_"))
            if (originalFile.exists()) {
                return originalFile.absolutePath
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return photoPath
}

fun cropAndSaveBitmap(
    context: android.content.Context,
    originalBitmap: android.graphics.Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    boxSizePx: Float,
    cropSizePx: Float
): String? {
    try {
        val outputSize = 400
        val croppedBitmap = android.graphics.Bitmap.createBitmap(outputSize, outputSize, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(croppedBitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
        val matrix = android.graphics.Matrix()

        val srcWidth = originalBitmap.width.toFloat()
        val srcHeight = originalBitmap.height.toFloat()
        val fitScale = Math.min(boxSizePx / srcWidth, boxSizePx / srcHeight)
        val initialX = (boxSizePx - srcWidth * fitScale) / 2f
        val initialY = (boxSizePx - srcHeight * fitScale) / 2f

        matrix.postScale(fitScale, fitScale)
        matrix.postTranslate(initialX, initialY)

        val centerX = boxSizePx / 2f
        val centerY = boxSizePx / 2f
        matrix.postScale(scale, scale, centerX, centerY)
        matrix.postTranslate(offsetX, offsetY)

        val cropLeft = (boxSizePx - cropSizePx) / 2f
        val cropTop = (boxSizePx - cropSizePx) / 2f
        matrix.postTranslate(-cropLeft, -cropTop)

        val finalScale = outputSize.toFloat() / cropSizePx
        matrix.postScale(finalScale, finalScale, 0f, 0f)

        canvas.drawBitmap(originalBitmap, matrix, paint)

        val directory = java.io.File(context.filesDir, "photos")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val timestamp = System.currentTimeMillis()
        val croppedFile = java.io.File(directory, "person_cropped_$timestamp.jpg")
        val originalFile = java.io.File(directory, "person_original_$timestamp.jpg")

        java.io.FileOutputStream(croppedFile).use { out ->
            croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }

        java.io.FileOutputStream(originalFile).use { out ->
            originalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }

        return croppedFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

data class TreePos(val x: Float, val y: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FamilyViewModel) {
    val context = LocalContext.current
    val persons by viewModel.filteredPersons.collectAsStateWithLifecycle()
    val allPersonsRaw by viewModel.allPersons.collectAsStateWithLifecycle()
    val relationships by viewModel.allRelationships.collectAsStateWithLifecycle()
    
    val currentLayout by viewModel.treeLayout.collectAsStateWithLifecycle()
    val currentTheme = "Bento Grid"
    val focusPersonId by viewModel.focusPersonId.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()

    val highlightP1Id by viewModel.highlightPerson1Id.collectAsStateWithLifecycle()
    val highlightP2Id by viewModel.highlightPerson2Id.collectAsStateWithLifecycle()
    val glowPersonId by viewModel.glowPersonId.collectAsStateWithLifecycle()

    // Group & Spouse states
    val allGroups by viewModel.allGroups.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()

    // Group Drag-Reorder states
    var draggingGroupIndex by remember { mutableStateOf<Int?>(null) }
    var dragGroupOffset by remember { mutableStateOf(0f) }
    var orderedGroupsList by remember(allGroups) { mutableStateOf(allGroups) }

    // Modals & form state
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showNoGroupsWarningDialog by remember { mutableStateOf(false) }
    var showNoSelectionWarningDialog by remember { mutableStateOf(false) }
    var personToDelete by remember { mutableStateOf<Person?>(null) }
    var isRestoringSubtree by remember { mutableStateOf(false) }
    var showSubtreeBackupPerson by remember { mutableStateOf<Person?>(null) }
    var showSubtreeRestoreDialog by remember { mutableStateOf(false) }
    
    val onAddPersonTrigger = {
        if (allGroups.isEmpty()) {
            showNoGroupsWarningDialog = true
        } else if (selectedGroupId == null) {
            showNoSelectionWarningDialog = true
        } else {
            showAddPersonDialog = true
        }
    }
    
    var showAddRelationshipDialog by remember { mutableStateOf(false) }
    var selectedPersonForDetails by remember { mutableStateOf<Person?>(null) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showFamilyOverviewStatsDialog by remember { mutableStateOf(false) }
    var showRemindersDialog by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var tempExportGroupId by remember { mutableStateOf<Long?>(null) }
    var backupFileNameInput by remember { mutableStateOf("بکاپ_کامل_خاندان") }
    var backupJsonToSave by remember { mutableStateOf("") }

    var showSelectGroupRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonPending by remember { mutableStateOf<String?>(null) }
    var showImmersivePhoto by remember { mutableStateOf<String?>(null) }
    var immersivePhotoIndex by remember { mutableStateOf<Int?>(null) }
    var immersivePhotoUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val onRestoreBackupText = { jsonText: String ->
        viewModel.importBackupFromJson(jsonText, null) { success, msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            if (success) {
                showRestoreDialog = false
            }
        }
    }

    // Sub-member & relationship transfer states
    var personToSubMemberOf by remember { mutableStateOf<Person?>(null) }
    var personToMoveRelationOf by remember { mutableStateOf<Person?>(null) }
    
    var personToEdit by remember { mutableStateOf<Person?>(null) }
    var personToAddSpouseFor by remember { mutableStateOf<Person?>(null) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToEdit by remember { mutableStateOf<com.example.data.FamilyGroup?>(null) }
    
    // Parent addition states
    var personToAddParentsFor by remember { mutableStateOf<Person?>(null) }
    var selectedGroupIdForParents by remember { mutableStateOf<Long?>(null) }
    var showGroupPromptForParents by remember { mutableStateOf<Person?>(null) }

    // Navigation panel tab
    var activeTab by remember { mutableStateOf("Tree") } // "Tree", "Directory"
    var isTreeExpanded by remember { mutableStateOf(false) }
    var showFiltersExpanded by remember { mutableStateOf(false) }

    var tempPickedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showCropDialog by remember { mutableStateOf(false) }
    var personForPhotoEdit by remember { mutableStateOf<Person?>(null) }
    var showFullPhotoDialog by remember { mutableStateOf<Person?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            tempPickedUri = uri
            showCropDialog = true
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(backupJsonToSave.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "فایل پشتیبان با موفقیت در گوشی ذخیره شد.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در ذخیره فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonText = inputStream.bufferedReader().use { it.readText() }
                    if (isRestoringSubtree) {
                        if (selectedGroupId == null) {
                            Toast.makeText(context, "جهت بازیابی بکاپ عضو، ابتدا باید یک گروه فامیلی ساخته و انتخاب کرده باشید.", Toast.LENGTH_LONG).show()
                            isRestoringSubtree = false
                            return@rememberLauncherForActivityResult
                        }
                        viewModel.importSubtreeBackupFromJson(jsonText) { success, msg, newGroupId ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                if (newGroupId != null) {
                                    viewModel.setSelectedGroupId(newGroupId)
                                }
                                showSubtreeRestoreDialog = false
                            }
                            isRestoringSubtree = false
                        }
                    } else {
                        onRestoreBackupText(jsonText)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "خطا در خواندن فایل پشتیبان: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                isRestoringSubtree = false
            }
        } else {
            isRestoringSubtree = false
        }
    }
    var activeRoleFilter by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(allGroups, selectedGroupId) {
        if (selectedGroupId == null && allGroups.isNotEmpty()) {
            viewModel.setSelectedGroupId(allGroups.first().id)
        }
    }

    // Theme values (Optimized with gorgeous greens, high-contrast readability, and complementary accents)
    val bgColor = Color(0xFFF1F8F5) // Soft Mint Cream background
    val cardColor = Color(0xFFFFFFFF) // Pure White card surface
    val textColor = Color(0xFF112E21) // High-contrast Deep Forest Charcoal text
    val accentColor = Color(0xFF4CAF50) // Vibrant Light Green accent
    val lineEffectColor = Color(0xFFCBE3D8) // Mint Sage border/grid lines

    val relationshipsInGroup = remember(relationships, persons) {
        val personIds = persons.map { it.id }.toSet()
        relationships.filter { rel ->
            personIds.contains(rel.personId1) && personIds.contains(rel.personId2)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "شجره‌نامه خانوادگی", 
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    ) 
                },
                actions = {
                    var showSettingsMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { showSettingsMenu = true },
                            modifier = Modifier.testTag("settings_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "تنظیمات", tint = accentColor)
                        }
                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            Text("ابزارها", fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 6.dp), fontSize = 12.sp, color = accentColor)
                            DropdownMenuItem(
                                text = { Text("محاسبه نسبت فامیلی", color = textColor) },
                                onClick = { showCalculatorDialog = true; showSettingsMenu = false },
                                leadingIcon = { Icon(Icons.Default.CompareArrows, contentDescription = null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = { Text("آمار و آنالیز جمعیتی", color = textColor) },
                                onClick = { showStatsDialog = true; showSettingsMenu = false },
                                leadingIcon = { Icon(Icons.Default.Analytics, contentDescription = null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = { Text("یادآورها و رویدادها (${upcomingEvents.size})", color = textColor) },
                                onClick = { showRemindersDialog = true; showSettingsMenu = false },
                                leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = { Text("بارگذاری اطلاعات نمونه", color = textColor) },
                                onClick = { viewModel.seedSampleData(); showSettingsMenu = false },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = { Text("تهیه بکاپ کلی (کل برنامه)", color = textColor) },
                                onClick = { 
                                    tempExportGroupId = null
                                    backupFileNameInput = "بکاپ_کامل_خاندان"
                                    showBackupDialog = true
                                    showSettingsMenu = false 
                                },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = { Text("بازیابی بکاپ", color = textColor) },
                                onClick = { showRestoreDialog = true; showSettingsMenu = false },
                                leadingIcon = { Icon(Icons.Default.Publish, contentDescription = null, tint = accentColor) }
                            )
                            
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            Text("چیدمان درخت شجره‌نامه", fontWeight = FontWeight.Bold, modifier = Modifier.padding(12.dp, 6.dp), fontSize = 12.sp, color = accentColor)
                            DropdownMenuItem(
                                text = { Text("چیدمان عمودی", color = textColor) },
                                onClick = { viewModel.setTreeLayout("Vertical"); showSettingsMenu = false },
                                trailingIcon = { if (currentLayout == "Vertical") Icon(Icons.Default.Check, contentDescription = null, tint = accentColor) }
                            )
                            DropdownMenuItem(
                                text = { Text("چیدمان افقی", color = textColor) },
                                onClick = { viewModel.setTreeLayout("Horizontal"); showSettingsMenu = false },
                                trailingIcon = { if (currentLayout == "Horizontal") Icon(Icons.Default.Check, contentDescription = null, tint = accentColor) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = bgColor
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = bgColor
            ) {
                NavigationBarItem(
                    selected = activeTab == "Tree",
                    onClick = { activeTab = "Tree" },
                    icon = { Icon(Icons.Default.AccountTree, contentDescription = "درخت") },
                    label = { Text("درخت شجره‌نامه") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        unselectedIconColor = textColor.copy(alpha = 0.6f),
                        unselectedTextColor = textColor.copy(alpha = 0.6f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "Directory",
                    onClick = { activeTab = "Directory" },
                    icon = { Icon(Icons.Default.People, contentDescription = "لیست اعضا") },
                    label = { Text("لیست اعضا") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        unselectedIconColor = textColor.copy(alpha = 0.6f),
                        unselectedTextColor = textColor.copy(alpha = 0.6f)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPersonTrigger,
                containerColor = accentColor,
                contentColor = if (currentTheme == "Dark Gold") Color.Black else Color.White,
                modifier = Modifier.testTag("add_member_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن عضو")
            }
        },
        containerColor = bgColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group Filtering Chip Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Edit active group button - Fixed on the right (start of RTL)
                if (selectedGroupId != null) {
                    val currentGroup = allGroups.find { it.id == selectedGroupId }
                    if (currentGroup != null) {
                        IconButton(
                            onClick = { groupToEdit = currentGroup },
                            modifier = Modifier
                                .size(36.dp)
                                .background(accentColor.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "ویرایش مشخصات گروه فعلی",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Scrollable container for the groups
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Render the ordered groups
                    orderedGroupsList.forEachIndexed { index, group ->
                        val isSelected = selectedGroupId == group.id
                        val isDraggingThis = draggingGroupIndex == index
                        val translationX = if (isDraggingThis) dragGroupOffset else 0f
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setSelectedGroupId(group.id) },
                            label = { Text(group.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.9f),
                                selectedLabelColor = Color.White,
                                containerColor = Color.White,
                                labelColor = textColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = lineEffectColor,
                                selectedBorderColor = accentColor
                            ),
                            modifier = Modifier
                                .offset { IntOffset(translationX.roundToInt(), 0) }
                                .pointerInput(index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset: Offset ->
                                            draggingGroupIndex = index
                                            dragGroupOffset = 0f
                                        },
                                        onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                            change.consume()
                                            dragGroupOffset += dragAmount.x
                                            
                                            val dragIndex = draggingGroupIndex
                                            if (dragIndex != null) {
                                                val threshold = 150f
                                                if (dragGroupOffset < -threshold && dragIndex < orderedGroupsList.size - 1) {
                                                    val newList = orderedGroupsList.toMutableList()
                                                    val temp = newList[dragIndex]
                                                    newList[dragIndex] = newList[dragIndex + 1]
                                                    newList[dragIndex + 1] = temp
                                                    orderedGroupsList = newList
                                                    draggingGroupIndex = dragIndex + 1
                                                    dragGroupOffset += threshold
                                                } else if (dragGroupOffset > threshold && dragIndex > 0) {
                                                    val newList = orderedGroupsList.toMutableList()
                                                    val temp = newList[dragIndex]
                                                    newList[dragIndex] = newList[dragIndex - 1]
                                                    newList[dragIndex - 1] = temp
                                                    orderedGroupsList = newList
                                                    draggingGroupIndex = dragIndex - 1
                                                    dragGroupOffset -= threshold
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggingGroupIndex = null
                                            dragGroupOffset = 0f
                                            viewModel.updateGroupOrder(orderedGroupsList)
                                        },
                                        onDragCancel = {
                                            draggingGroupIndex = null
                                            dragGroupOffset = 0f
                                        }
                                    )
                                }
                        )
                    }

                    // Add Group Chip Button - always at the far left (end of scrollable row)
                    InputChip(
                        selected = false,
                        onClick = { showAddGroupDialog = true },
                        label = { Text("ایجاد گروه فامیلی جدید +", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = Color(0xFFFFF3E0),
                            labelColor = Color(0xFFE65100)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D))
                    )
                }
            }

            // Configuration bar (Layouts & Themes selector has been moved to Top Bar 3-dot menu)
            if (activeTab == "Tree") {
                // Focus Mode alert
                if (focusPersonId != null) {
                    val focusPerson = allPersonsRaw.find { it.id == focusPersonId }
                    if (focusPerson != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "حالت تمرکز فعال روی: ${focusPerson.fullName}",
                                fontSize = 13.sp,
                                color = textColor,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { viewModel.setFocusPersonId(null) }) {
                                Text("لغو تمرکز", color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Relationship highlights panel
                if (highlightP1Id != null && highlightP2Id != null) {
                    val p1 = allPersonsRaw.find { it.id == highlightP1Id }
                    val p2 = allPersonsRaw.find { it.id == highlightP2Id }
                    if (p1 != null && p2 != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE5C158).copy(alpha = 0.2f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val relLabel = RelationshipCalculator.getRelationshipLabel(p1, p2, allPersonsRaw, relationships)
                            Column {
                                Text(
                                    "مسیر هایلایت شده بین:",
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                                Text(
                                    "${p1.fullName} ➔ ${p2.fullName} (${relLabel})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                            IconButton(onClick = { viewModel.clearHighlighting() }) {
                                Icon(Icons.Default.Clear, contentDescription = "پاک کردن مسیر", tint = textColor)
                            }
                        }
                    }
                }
            }

            // Tabs implementation
            Box(modifier = Modifier.fillMaxSize()) {
                if (activeTab == "Tree") {
                    if (currentTheme == "Bento Grid" && !isTreeExpanded) {
                        // Bento Grid Dashboard view
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Beautiful welcoming and group status header
                            val selectedGroupName = remember(allGroups, selectedGroupId) {
                                allGroups.find { it.id == selectedGroupId }?.name ?: "خاندان عمومی"
                            }
                            val selectedGroupDesc = remember(allGroups, selectedGroupId, selectedGroupName) {
                                val desc = allGroups.find { it.id == selectedGroupId }?.description
                                if (desc.isNullOrBlank()) "نمای کلی خانواده $selectedGroupName" else desc
                            }
                            


                            // Beautiful combined Family Identity & Root Member Header Card
                            val featuredPerson = remember(persons, focusPersonId) {
                                if (focusPersonId != null) {
                                    persons.find { it.id == focusPersonId }
                                } else {
                                    val genZero = persons.filter { it.generation == 0 }
                                    val firstParent = genZero.find { it.gender == "Male" } ?: genZero.firstOrNull()
                                    firstParent ?: persons.minByOrNull { it.generation } ?: persons.minByOrNull { it.birthDate ?: "9999" }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, lineEffectColor, RoundedCornerShape(32.dp))
                                    .clip(RoundedCornerShape(32.dp))
                                    .clickable { if (featuredPerson != null) selectedPersonForDetails = featuredPerson },
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (currentTheme == "Bento Grid") {
                                                Brush.linearGradient(colors = listOf(Color(0xFFEAF5EF), Color(0xFFD0ECD8)))
                                            } else {
                                                Brush.linearGradient(colors = listOf(cardColor, cardColor))
                                            }
                                        )
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // Decorative corner circle or pattern
                                    Canvas(modifier = Modifier.size(64.dp).align(Alignment.TopStart)) {
                                        drawArc(
                                            color = accentColor.copy(alpha = 0.05f),
                                            startAngle = 180f,
                                            sweepAngle = 90f,
                                            useCenter = true,
                                            topLeft = Offset(-32.dp.toPx(), -32.dp.toPx()),
                                            size = size * 2f
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                         // Root Ancestor / First Member Details Row
                                         Row(
                                             modifier = Modifier.fillMaxWidth(),
                                             verticalAlignment = Alignment.CenterVertically,
                                             horizontalArrangement = Arrangement.spacedBy(12.dp)
                                         ) {
                                             // Avatar with a badge
                                             val featuredPhoto = featuredPerson?.photoUris?.firstOrNull()
                                             Box(
                                                 modifier = Modifier
                                                     .size(56.dp)
                                                     .clickable {
                                                         if (featuredPerson != null) {
                                                             if (featuredPerson.photoUris.isNotEmpty()) {
                                                                 showFullPhotoDialog = featuredPerson
                                                             } else {
                                                                 personForPhotoEdit = featuredPerson
                                                                 photoPickerLauncher.launch("image/*")
                                                             }
                                                         }
                                                     }
                                             ) {
                                                 Box(
                                                     modifier = Modifier
                                                         .fillMaxSize()
                                                         .clip(CircleShape)
                                                         .background(accentColor.copy(alpha = 0.1f))
                                                         .border(2.dp, Color.White, CircleShape),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     if (!featuredPhoto.isNullOrBlank()) {
                                                         Image(
                                                             painter = rememberAsyncImagePainter(model = java.io.File(featuredPhoto)),
                                                             contentDescription = null,
                                                             modifier = Modifier.fillMaxSize(),
                                                             contentScale = ContentScale.Crop
                                                         )
                                                     } else {
                                                         Text(
                                                             text = if (featuredPerson?.gender == "Male") "👴" else "👵",
                                                             fontSize = 24.sp
                                                         )
                                                     }
                                                 }
                                             }

                                             Column(modifier = Modifier.weight(1f)) {
                                                 Text(
                                                     text = featuredPerson?.fullName ?: selectedGroupName,
                                                     fontWeight = FontWeight.Bold,
                                                     fontSize = 16.sp,
                                                     color = textColor
                                                 )

                                                 Text(
                                                     text = if (featuredPerson != null) {
                                                         val birth = featuredPerson.birthDate?.split("-")?.firstOrNull() ?: ""
                                                         val death = if (featuredPerson.isDeceased) featuredPerson.deathDate?.split("-")?.firstOrNull() ?: "؟" else ""
                                                         val prefix = if (focusPersonId != null) "شخص برجسته" else "سرشاخه خاندان"
                                                         (if (featuredPerson.isDeceased) "$prefix ($birth - $death)" else "$prefix (متولد $birth)").toFarsiNumbers()
                                                     } else "آغازگر شجره‌نامه",
                                                     fontSize = 11.sp,
                                                     color = textColor.copy(alpha = 0.7f),
                                                     style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                                 )

                                                 if (featuredPerson != null) {
                                                     Spacer(modifier = Modifier.height(4.dp))
                                                     Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                         if (featuredPerson.occupation != null) {
                                                             Box(
                                                                 modifier = Modifier
                                                                     .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                                     .padding(horizontal = 8.dp, vertical = 4.dp)
                                                             ) {
                                                                 Text("شغل: ${featuredPerson.occupation}", fontSize = 10.sp, color = textColor)
                                                             }
                                                         }
                                                         if (featuredPerson.birthPlace != null) {
                                                             Box(
                                                                 modifier = Modifier
                                                                     .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                                                     .padding(horizontal = 8.dp, vertical = 4.dp)
                                                             ) {
                                                                 Text("دیار: ${featuredPerson.birthPlace}", fontSize = 10.sp, color = textColor)
                                                             }
                                                         }
                                                     }
                                                 }
                                             }

                                             // Interactive stats dialog button (Hub)
                                             Box(
                                                 modifier = Modifier
                                                     .size(44.dp)
                                                     .clip(RoundedCornerShape(12.dp))
                                                     .background(accentColor.copy(alpha = 0.12f))
                                                     .clickable { showFamilyOverviewStatsDialog = true },
                                                 contentAlignment = Alignment.Center
                                             ) {
                                                 Icon(
                                                     imageVector = Icons.Default.Hub,
                                                     contentDescription = "مشاهده گزارش و آمار خاندان",
                                                     tint = accentColor,
                                                     modifier = Modifier.size(24.dp)
                                                 )
                                             }
                                         }
                                     }
                                 }
                             }

                            // Bento Card 2: Two-column Stats Row (Total Members & Generations)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Total members card (Col 1)
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(24.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(
                                                if (currentTheme == "Bento Grid") {
                                                    Brush.linearGradient(colors = listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9)))
                                                } else {
                                                    Brush.linearGradient(colors = listOf(cardColor, cardColor))
                                                }
                                            )
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "تعداد اعضا",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor.copy(alpha = 0.8f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${stats.totalCount}",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Black,
                                                color = accentColor
                                            )
                                            Text(
                                                "نفر",
                                                fontSize = 11.sp,
                                                color = textColor.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )

                                        }
                                    }
                                }

                                // Generations card (Col 2)
                                val maxGeneration = remember(allPersonsRaw, selectedGroupId) {
                                    val groupPersons = if (selectedGroupId != null) allPersonsRaw.filter { it.groupId == selectedGroupId } else emptyList()
                                    if (groupPersons.isEmpty()) 0 else (groupPersons.maxOfOrNull { it.generation }?.let { it + 1 } ?: 0)
                                }
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(24.dp)),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(
                                                if (currentTheme == "Bento Grid") {
                                                    Brush.linearGradient(colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))) // Warm Gold gradient
                                                } else {
                                                    Brush.linearGradient(colors = listOf(cardColor, cardColor))
                                                }
                                            )
                                            .fillMaxSize()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "نسل‌ها",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textColor.copy(alpha = 0.8f)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.Bottom,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "$maxGeneration",
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFE65100) // Rich orange complementary
                                            )
                                            Text(
                                                "سطح",
                                                fontSize = 11.sp,
                                                color = textColor.copy(alpha = 0.6f),
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Bento Card 3: Interactive Tree panel inside a Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(380.dp)
                                    .border(1.dp, Color(0xFFEBE3D5), RoundedCornerShape(32.dp))
                                    .clip(RoundedCornerShape(32.dp)),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Focus Mode Button
                                        Button(
                                            onClick = {
                                                if (focusPersonId != null) {
                                                    viewModel.setFocusPersonId(null)
                                                } else {
                                                    Toast.makeText(context, "برای فعال‌سازی نمای متمرکز، روی کارت شخص مورد نظر دوبار ضربه بزنید", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (focusPersonId != null) accentColor else Color(0xFFF3E5F5),
                                                contentColor = if (focusPersonId != null) Color.White else Color(0xFF4A148C)
                                            ),
                                            border = if (focusPersonId == null) BorderStroke(1.dp, Color(0xFF4A148C).copy(alpha = 0.2f)) else null,
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp).testTag("focus_mode_toggle_button")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.Visibility,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (focusPersonId != null) "نمای متمرکز: فعال" else "نمای متمرکز",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (focusPersonId != null) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "لغو",
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Full View Button
                                        Button(
                                            onClick = { isTreeExpanded = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFFFF3E0),
                                                contentColor = Color(0xFFE65100)
                                            ),
                                            border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp).testTag("full_tree_expanded_button")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.AccountTree,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = "مشاهده کامل",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clipToBounds() // Enforce clear boundary preventing overlap on pan/zoom
                                    ) {
                                        InteractiveFamilyTree(
                                            persons = persons,
                                            relationships = relationshipsInGroup,
                                            layoutType = currentLayout,
                                            focusPersonId = focusPersonId,
                                            highlightP1Id = highlightP1Id,
                                            highlightP2Id = highlightP2Id,
                                            textColor = textColor,
                                            accentColor = accentColor,
                                            cardBgColor = cardColor,
                                            lineColor = Color(0xFF2E7D32),
                                            onPersonClick = { selectedPersonForDetails = it },
                                            onPersonDoubleTap = { person ->
                                                if (focusPersonId == person.id) {
                                                    viewModel.setFocusPersonId(null)
                                                } else {
                                                    viewModel.setFocusPersonId(person.id)
                                                }
                                            },
                                            onViewFamilyClick = { person ->
                                                person.groupId?.let { gid ->
                                                    viewModel.setSelectedGroupId(gid)
                                                }
                                                if (focusPersonId == person.id) {
                                                    viewModel.setFocusPersonId(null)
                                                } else {
                                                    viewModel.setFocusPersonId(person.id)
                                                }
                                            },
                                            onAddFirstPerson = onAddPersonTrigger,
                                            onPhotoClick = { person ->
                                                if (person.photoUris.isNotEmpty()) {
                                                    showFullPhotoDialog = person
                                                } else {
                                                    personForPhotoEdit = person
                                                    photoPickerLauncher.launch("image/*")
                                                }
                                            },
                                            glowPersonId = glowPersonId
                                        )
                                    }
                                }
                            }


                        }
                    } else {
                        // Standard view (expanded interactive canvas)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds()
                        ) {
                            InteractiveFamilyTree(
                                persons = persons,
                                relationships = relationshipsInGroup,
                                layoutType = currentLayout,
                                focusPersonId = focusPersonId,
                                highlightP1Id = highlightP1Id,
                                highlightP2Id = highlightP2Id,
                                textColor = textColor,
                                accentColor = accentColor,
                                cardBgColor = cardColor,
                                lineColor = Color(0xFF2E7D32),
                                onPersonClick = { selectedPersonForDetails = it },
                                onPersonDoubleTap = { person ->
                                    if (focusPersonId == person.id) {
                                        viewModel.setFocusPersonId(null)
                                    } else {
                                        viewModel.setFocusPersonId(person.id)
                                    }
                                },
                                onViewFamilyClick = { person ->
                                    person.groupId?.let { gid ->
                                        viewModel.setSelectedGroupId(gid)
                                    }
                                    if (focusPersonId == person.id) {
                                        viewModel.setFocusPersonId(null)
                                    } else {
                                        viewModel.setFocusPersonId(person.id)
                                    }
                                },
                                onAddFirstPerson = onAddPersonTrigger,
                                onPhotoClick = { person ->
                                    if (person.photoUris.isNotEmpty()) {
                                        showFullPhotoDialog = person
                                    } else {
                                        personForPhotoEdit = person
                                        photoPickerLauncher.launch("image/*")
                                    }
                                },
                                glowPersonId = glowPersonId
                            )

                            // Floating back button overlay if expanded under Bento Grid theme
                            if (currentTheme == "Bento Grid" && isTreeExpanded) {
                                FloatingActionButton(
                                    onClick = { isTreeExpanded = false },
                                    containerColor = accentColor,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "بستن تمام صفحه",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Directory List view
                    val filterDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                    val drawerScope = rememberCoroutineScope()
                    val activeGender = viewModel.filterGender.collectAsState().value
                    val activeDeceased = viewModel.filterIsDeceased.collectAsState().value

                    ModalNavigationDrawer(
                        drawerState = filterDrawerState,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = Color.White,
                                modifier = Modifier.width(300.dp).fillMaxHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.FilterList,
                                                contentDescription = null,
                                                tint = accentColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "فیلترهای پیشرفته",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = textColor
                                            )
                                        }
                                        IconButton(onClick = {
                                            drawerScope.launch { filterDrawerState.close() }
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "بستن",
                                                tint = textColor
                                            )
                                        }
                                    }

                                    HorizontalDivider(color = textColor.copy(alpha = 0.1f))

                                    // Content
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // 1. Gender Filter
                                        Column {
                                            Text(
                                                text = "جنسیت اعضا",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(
                                                    Pair(null, "همه"),
                                                    Pair("Male", "آقایان"),
                                                    Pair("Female", "بانوان")
                                                ).forEach { (genderKey, label) ->
                                                    val isSelected = activeGender == genderKey
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = { viewModel.setFilterGender(genderKey) },
                                                        label = { Text(label, fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = accentColor,
                                                            selectedLabelColor = Color.White,
                                                            containerColor = Color.White,
                                                            labelColor = textColor.copy(alpha = 0.8f)
                                                        ),
                                                        border = FilterChipDefaults.filterChipBorder(
                                                            enabled = true,
                                                            selected = isSelected,
                                                            borderColor = textColor.copy(alpha = 0.1f),
                                                            selectedBorderColor = Color.Transparent
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // 2. Living/Deceased Filter
                                        Column {
                                            Text(
                                                text = "وضعیت حیات",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(
                                                    Pair(null, "همه وضعیت‌ها"),
                                                    Pair(false, "در قید حیات"),
                                                    Pair(true, "مرحومین")
                                                ).forEach { (deceasedKey, label) ->
                                                    val isSelected = activeDeceased == deceasedKey
                                                    FilterChip(
                                                        selected = isSelected,
                                                        onClick = { viewModel.setFilterIsDeceased(deceasedKey) },
                                                        label = { Text(label, fontSize = 11.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = accentColor,
                                                            selectedLabelColor = Color.White,
                                                            containerColor = Color.White,
                                                            labelColor = textColor.copy(alpha = 0.8f)
                                                        ),
                                                        border = FilterChipDefaults.filterChipBorder(
                                                            enabled = true,
                                                            selected = isSelected,
                                                            borderColor = textColor.copy(alpha = 0.1f),
                                                            selectedBorderColor = Color.Transparent
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // 3. Generation & Role Filter (Organized nicely)
                                        Column {
                                            Text(
                                                text = "نقش در خاندان (رده‌ها و نسبت‌ها)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            val roles = listOf(
                                                Pair(null, "همه رده‌ها"),
                                                Pair("Root", "بزرگ خاندان"),
                                                Pair("Child", "فرزندان"),
                                                Pair("Grandchild", "نوه‌ها"),
                                                Pair("GreatGrandchild", "نتیجه‌ها"),
                                                Pair("GreatGreatGrandchild", "نبیره‌ها"),
                                                Pair("Bride", "عروس‌ها"),
                                                Pair("Groom", "دامادها")
                                            )
                                            
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    roles.take(3).forEach { (roleKey, label) ->
                                                        val isSelected = activeRoleFilter == roleKey
                                                        FilterChip(
                                                            selected = isSelected,
                                                            onClick = { activeRoleFilter = roleKey },
                                                            label = { Text(label, fontSize = 11.sp) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                selectedContainerColor = accentColor,
                                                                selectedLabelColor = Color.White,
                                                                containerColor = Color.White,
                                                                labelColor = textColor.copy(alpha = 0.8f)
                                                            ),
                                                            border = FilterChipDefaults.filterChipBorder(
                                                                enabled = true,
                                                                selected = isSelected,
                                                                borderColor = textColor.copy(alpha = 0.1f),
                                                                selectedBorderColor = Color.Transparent
                                                            )
                                                        )
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    roles.subList(3, 6).forEach { (roleKey, label) ->
                                                        val isSelected = activeRoleFilter == roleKey
                                                        FilterChip(
                                                            selected = isSelected,
                                                            onClick = { activeRoleFilter = roleKey },
                                                            label = { Text(label, fontSize = 11.sp) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                selectedContainerColor = accentColor,
                                                                selectedLabelColor = Color.White,
                                                                containerColor = Color.White,
                                                                labelColor = textColor.copy(alpha = 0.8f)
                                                            ),
                                                            border = FilterChipDefaults.filterChipBorder(
                                                                enabled = true,
                                                                selected = isSelected,
                                                                borderColor = textColor.copy(alpha = 0.1f),
                                                                selectedBorderColor = Color.Transparent
                                                            )
                                                        )
                                                    }
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    roles.drop(6).forEach { (roleKey, label) ->
                                                        val isSelected = activeRoleFilter == roleKey
                                                        FilterChip(
                                                            selected = isSelected,
                                                            onClick = { activeRoleFilter = roleKey },
                                                            label = { Text(label, fontSize = 11.sp) },
                                                            colors = FilterChipDefaults.filterChipColors(
                                                                selectedContainerColor = accentColor,
                                                                selectedLabelColor = Color.White,
                                                                containerColor = Color.White,
                                                                labelColor = textColor.copy(alpha = 0.8f)
                                                            ),
                                                            border = FilterChipDefaults.filterChipBorder(
                                                                enabled = true,
                                                                selected = isSelected,
                                                                borderColor = textColor.copy(alpha = 0.1f),
                                                                selectedBorderColor = Color.Transparent
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Reset filters button
                                    Button(
                                        onClick = {
                                            viewModel.setFilterGender(null)
                                            viewModel.setFilterIsDeceased(null)
                                            activeRoleFilter = null
                                            viewModel.setSearchQuery("")
                                            drawerScope.launch { filterDrawerState.close() }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.12f), contentColor = accentColor),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("پاک کردن همه فیلترها", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            // Search and filter button row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = viewModel.searchQuery.collectAsState().value,
                                    onValueChange = { newValue ->
                                        if (newValue.isEmpty() || (!newValue.startsWith(" ") && !newValue.startsWith("\n") && !newValue.startsWith("\r"))) {
                                            viewModel.setSearchQuery(newValue)
                                        }
                                    },
                                    placeholder = { Text("جستجو بر اساس نام، شغل و بیوگرافی...", fontSize = 12.sp, color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "جستجو", tint = accentColor) },
                                    modifier = Modifier.weight(1f).testTag("search_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black,
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White,
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = textColor.copy(alpha = 0.3f),
                                        cursorColor = accentColor
                                    )
                                )

                                // Drawer Toggle Button
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .background(Color.White, RoundedCornerShape(12.dp))
                                        .border(1.dp, textColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            drawerScope.launch {
                                                if (filterDrawerState.isClosed) filterDrawerState.open() else filterDrawerState.close()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val activeFilterCount = (if (activeGender != null) 1 else 0) +
                                            (if (activeDeceased != null) 1 else 0) +
                                            (if (activeRoleFilter != null) 1 else 0)
                                    Box {
                                        Icon(
                                            imageVector = Icons.Default.FilterList,
                                            contentDescription = "فیلترها",
                                            tint = if (activeFilterCount > 0) accentColor else textColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        if (activeFilterCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(accentColor, CircleShape)
                                                    .align(Alignment.TopEnd)
                                                    .offset(x = 6.dp, y = (-6).dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = activeFilterCount.toString(),
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Compute final filtered persons locally using role filter
                            val finalFilteredPersons = remember(persons, relationships, activeRoleFilter) {
                                if (activeRoleFilter == null) {
                                    persons
                                } else {
                                    val parentChildRels = relationships.filter { it.type == "Parent-Child" || it.type == "Adoptive-Parent-Child" }
                                    val childrenIds = parentChildRels.map { it.personId2 }.toSet()
                                    val spouseRels = relationships.filter { it.type == "Spouse" }
                                    
                                    val mainRoot = persons.filter { it.generation == 0 }.find { it.gender == "Male" }
                                        ?: persons.minByOrNull { it.generation }

                                    when (activeRoleFilter) {
                                        "Root" -> persons.filter { it.id == mainRoot?.id }
                                        "Bride" -> persons.filter { p ->
                                            p.id != mainRoot?.id &&
                                            p.gender == "Female" &&
                                            !childrenIds.contains(p.id) &&
                                            spouseRels.any { it.personId1 == p.id || it.personId2 == p.id }
                                        }
                                        "Groom" -> persons.filter { p ->
                                            p.id != mainRoot?.id &&
                                            p.gender == "Male" &&
                                            !childrenIds.contains(p.id) &&
                                            spouseRels.any { it.personId1 == p.id || it.personId2 == p.id }
                                        }
                                        "Child" -> persons.filter { childrenIds.contains(it.id) && it.generation == 1 }
                                        "Grandchild" -> persons.filter { childrenIds.contains(it.id) && it.generation == 2 }
                                        "GreatGrandchild" -> persons.filter { childrenIds.contains(it.id) && it.generation == 3 }
                                        "GreatGreatGrandchild" -> persons.filter { childrenIds.contains(it.id) && it.generation == 4 }
                                        else -> persons
                                    }
                                }
                            }

                            if (finalFilteredPersons.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PeopleOutline, contentDescription = "بدون عضو", modifier = Modifier.size(64.dp), tint = textColor.copy(alpha = 0.4f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("عضوی با این مشخصات یافت نشد", color = textColor.copy(alpha = 0.6f))
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(finalFilteredPersons) { person ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedPersonForDetails = person }
                                                .testTag("directory_member_${person.id}"),
                                            colors = CardDefaults.cardColors(containerColor = cardColor),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Avatar
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (person.gender == "Male") Color(0xFFE3F2FD) else Color(0xFFFCE4EC)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    val (avatarIcon, iconTint) = when {
                                                        person.gender == "Male" -> Pair(Icons.Default.Face, Color(0xFF1E88E5))
                                                        else -> Pair(Icons.Default.FaceRetouchingNatural, Color(0xFFD81B60))
                                                    }
                                                    Icon(
                                                        avatarIcon,
                                                        contentDescription = null,
                                                        tint = iconTint,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            person.fullName,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textColor,
                                                            fontSize = 15.sp
                                                        )
                                                        if (person.isDeceased) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(Color.DarkGray, RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("متوفی", color = Color.White, fontSize = 9.sp)
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        person.occupation ?: "بدون شغل مشخص",
                                                        fontSize = 12.sp,
                                                        color = textColor.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        if (isTreeExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            ) {
                InteractiveFamilyTree(
                    persons = persons,
                    relationships = relationshipsInGroup,
                    layoutType = currentLayout,
                    focusPersonId = focusPersonId,
                    highlightP1Id = highlightP1Id,
                    highlightP2Id = highlightP2Id,
                    textColor = textColor,
                    accentColor = accentColor,
                    cardBgColor = cardColor,
                    lineColor = Color(0xFF2E7D32),
                    onPersonClick = { selectedPersonForDetails = it },
                    onPersonDoubleTap = { person ->
                        if (focusPersonId == person.id) {
                            viewModel.setFocusPersonId(null)
                        } else {
                            viewModel.setFocusPersonId(person.id)
                        }
                    },
                    onViewFamilyClick = { person ->
                        person.groupId?.let { gid ->
                            viewModel.setSelectedGroupId(gid)
                        }
                        if (focusPersonId == person.id) {
                            viewModel.setFocusPersonId(null)
                        } else {
                            viewModel.setFocusPersonId(person.id)
                        }
                    },
                    onAddFirstPerson = onAddPersonTrigger,
                    onPhotoClick = { person ->
                        if (person.photoUris.isNotEmpty()) {
                            showFullPhotoDialog = person
                        } else {
                            personForPhotoEdit = person
                            photoPickerLauncher.launch("image/*")
                        }
                    },
                    glowPersonId = glowPersonId
                )

                // Beautiful floating close button (FAB) at bottom-end
                FloatingActionButton(
                    onClick = { isTreeExpanded = false },
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .testTag("close_full_tree_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "بازگشت به صفحه اصلی"
                        )
                        Text(
                            text = "بازگشت",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Modal dialogs
    if (showAddPersonDialog) {
        AddPersonDialog(
            theme = currentTheme,
            textColor = textColor,
            accentColor = accentColor,
            groups = allGroups,
            defaultGroupId = selectedGroupId,
            onDismiss = { showAddPersonDialog = false },
            onConfirm = { firstName, lastName, gender, birthDate, birthPlace, deathDate, deathPlace, isDeceased, occupation, bio, groupId, _ ->
                viewModel.addPerson(
                    Person(
                        firstName = firstName,
                        lastName = lastName,
                        gender = gender,
                        birthDate = birthDate,
                        birthPlace = birthPlace,
                        deathDate = deathDate,
                        deathPlace = deathPlace,
                        isDeceased = isDeceased,
                        occupation = occupation,
                        biography = bio,
                        groupId = groupId
                    )
                ) { newId ->
                    Toast.makeText(context, "عضو با موفقیت اضافه شد", Toast.LENGTH_SHORT).show()
                }
                showAddPersonDialog = false
            }
        )
    }

    if (showAddRelationshipDialog) {
        val selectedP1 = highlightP1Id ?: 0L
        AddRelationshipDialog(
            persons = allPersonsRaw,
            preselectedP1 = selectedP1,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { showAddRelationshipDialog = false },
            onConfirm = { p1Id, p2Id, type ->
                viewModel.addRelationship(p1Id, p2Id, type)
                Toast.makeText(context, "رابطه فامیلی جدید ثبت شد", Toast.LENGTH_SHORT).show()
                viewModel.clearHighlighting()
                showAddRelationshipDialog = false
            }
        )
    }

    if (selectedPersonForDetails != null) {
        val person = allPersonsRaw.find { it.id == selectedPersonForDetails!!.id } ?: selectedPersonForDetails!!
        
        MemberDetailsDialog(
            person = person,
            relationships = relationships,
            allPersons = allPersonsRaw,
            theme = currentTheme,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { selectedPersonForDetails = null },
            onDelete = {
                personToDelete = person
                selectedPersonForDetails = null
            },
            onHighlightFrom = {
                viewModel.setHighlightPerson1(person.id)
                selectedPersonForDetails = null
                Toast.makeText(context, "عضو اول انتخاب شد. اکنون عضو دوم را انتخاب کنید.", Toast.LENGTH_SHORT).show()
            },
            onHighlightTo = {
                viewModel.setHighlightPerson2(person.id)
                selectedPersonForDetails = null
            },
            onAddChild = { parent ->
                personToSubMemberOf = parent
                selectedPersonForDetails = null
            },
            onAddSpouse = { parent ->
                personToAddSpouseFor = parent
                selectedPersonForDetails = null
            },
            onEditPerson = { p ->
                personToEdit = p
                selectedPersonForDetails = null
            },
            onMoveRelation = { targetPerson ->
                personToMoveRelationOf = targetPerson
                selectedPersonForDetails = null
            },
            onAddParents = { p ->
                val parentRelations = relationships.filter { 
                    (it.type == "Parent-Child" || it.type == "Adoptive-Parent-Child") && it.personId2 == p.id 
                }
                if (parentRelations.isNotEmpty()) {
                    // One of the parents is already defined; don't create a new group.
                    val parentIds = parentRelations.map { it.personId1 }
                    val parents = allPersonsRaw.filter { it.id in parentIds }
                    val existingFather = parents.find { it.gender == "Male" }
                    val existingMother = parents.find { it.gender == "Female" }
                    selectedGroupIdForParents = existingFather?.groupId ?: existingMother?.groupId ?: p.groupId
                    personToAddParentsFor = p
                } else {
                    // Neither parent is registered yet; prompt for group creation
                    showGroupPromptForParents = p
                }
                selectedPersonForDetails = null
            },
            onFocusPerson = { p ->
                viewModel.setFocusPersonId(p.id)
                selectedPersonForDetails = null
            },
            onPhotoClick = { person ->
                if (person.photoUris.isNotEmpty()) {
                    showFullPhotoDialog = person
                } else {
                    personForPhotoEdit = person
                    photoPickerLauncher.launch("image/*")
                }
            },
            onBackupSubtree = { p -> showSubtreeBackupPerson = p },
            onRestoreSubtree = { 
                showSubtreeRestoreDialog = true
            },
            viewModel = viewModel
        )
    }

    if (showCropDialog && tempPickedUri != null) {
        val originalBitmap = remember(tempPickedUri) {
            try {
                val inputStream = context.contentResolver.openInputStream(tempPickedUri!!)
                android.graphics.BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {
                null
            }
        }

        if (originalBitmap != null) {
            var scale by remember { mutableStateOf(1f) }
            var offsetX by remember { mutableStateOf(0f) }
            var offsetY by remember { mutableStateOf(0f) }

            val density = LocalDensity.current
            val boxSizeDp = 250.dp
            val cropSizeDp = 200.dp
            val boxSizePx = with(density) { boxSizeDp.toPx() }
            val cropSizePx = with(density) { cropSizeDp.toPx() }

            Dialog(onDismissRequest = { showCropDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Color.LightGray, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "تنظیم و برش عکس",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textColor
                        )

                        Text(
                            text = "تصویر را بکشید یا بزرگنمایی کنید تا در مرکز کادر قرار گیرد",
                            fontSize = 11.sp,
                            color = textColor.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )

                        // Interactive Box with Gestures
                        Box(
                            modifier = Modifier
                                .size(boxSizeDp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFEEEEEE))
                                .clipToBounds()
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        offsetX += dragAmount.x
                                        offsetY += dragAmount.y
                                    }
                                }
                        ) {
                            Image(
                                bitmap = originalBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offsetX,
                                        translationY = offsetY
                                    ),
                                contentScale = ContentScale.Fit
                            )

                            // Highlighted Crop Area (Circle shape matching requested circular image styling!)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(cropSizeDp)
                                    .border(2.dp, Color.White, CircleShape)
                                    .border(3.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                            )
                        }

                        // Sliders for Fine-Tuning
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("بزرگنمایی", fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Bold)
                            Slider(
                                value = scale,
                                onValueChange = { scale = it },
                                valueRange = 1f..5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = accentColor,
                                    activeTrackColor = accentColor
                                )
                            )
                        }

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    val croppedPath = cropAndSaveBitmap(
                                        context = context,
                                        originalBitmap = originalBitmap,
                                        scale = scale,
                                        offsetX = offsetX,
                                        offsetY = offsetY,
                                        boxSizePx = boxSizePx,
                                        cropSizePx = cropSizePx
                                    )
                                    if (croppedPath != null && personForPhotoEdit != null) {
                                        val freshPerson = allPersonsRaw.find { it.id == personForPhotoEdit!!.id } ?: personForPhotoEdit!!
                                        val currentUris = freshPerson.photoUris.toMutableList()
                                        currentUris.add(croppedPath)
                                        val newPhotoUri = currentUris.joinToString("|")
                                        val updatedPerson = freshPerson.copy(photoUri = newPhotoUri)
                                        viewModel.updatePerson(updatedPerson)
                                        Toast.makeText(context, "عکس با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "خطا در ذخیره عکس", Toast.LENGTH_SHORT).show()
                                    }
                                    showCropDialog = false
                                    tempPickedUri = null
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                Text("تایید و ذخیره", color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    showCropDialog = false
                                    tempPickedUri = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("انصراف", color = textColor)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFullPhotoDialog != null) {
        val person = allPersonsRaw.find { it.id == showFullPhotoDialog!!.id } ?: showFullPhotoDialog!!
        val uris = person.photoUris
        var currentImageIndex by remember(person.id, uris.size) { mutableStateOf(0) }
        
        Dialog(onDismissRequest = { showFullPhotoDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, accentColor, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "گالری تصاویر ${person.fullName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )

                    // Big square/circle photo container
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEEEEEE))
                            .border(3.dp, accentColor, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uris.isEmpty()) {
                            // Empty state
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(80.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "هیچ عکسی ثبت نشده است",
                                    color = textColor.copy(alpha = 0.5f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            val activePhotoPath = uris.getOrNull(currentImageIndex)
                            if (activePhotoPath != null) {
                                val fullPhotoPath = getFullOrOriginalPhotoPath(activePhotoPath)
                                Image(
                                    painter = rememberAsyncImagePainter(model = java.io.File(fullPhotoPath)),
                                    contentDescription = "تصویر کامل",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { 
                                            immersivePhotoUris = uris
                                            immersivePhotoIndex = currentImageIndex
                                        }
                                        .pointerInput(Unit) {
                                            var totalDrag = 0f
                                            detectDragGestures(
                                                onDragStart = { totalDrag = 0f },
                                                onDragEnd = {
                                                    if (totalDrag > 100f) {
                                                        currentImageIndex = (currentImageIndex - 1 + uris.size) % uris.size
                                                    } else if (totalDrag < -100f) {
                                                        currentImageIndex = (currentImageIndex + 1) % uris.size
                                                    }
                                                },
                                                onDragCancel = { totalDrag = 0f },
                                                onDrag = { _, dragAmount ->
                                                    totalDrag += dragAmount.x
                                                }
                                            )
                                        },
                                    contentScale = ContentScale.Crop
                                )
                                
                                // Text indicator overlayed at top right: e.g. "۱ از ۳"
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${currentImageIndex + 1} از ${uris.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Swipe/Pagination Buttons (Next / Prev)
                    if (uris.size > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    currentImageIndex = (currentImageIndex - 1 + uris.size) % uris.size
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "قبلی",
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Dot page indicators
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(uris.size) { index ->
                                    val isActive = index == currentImageIndex
                                    Box(
                                        modifier = Modifier
                                            .size(if (isActive) 8.dp else 6.dp)
                                            .background(
                                                if (isActive) accentColor else Color.LightGray,
                                                CircleShape
                                            )
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    currentImageIndex = (currentImageIndex + 1) % uris.size
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(accentColor.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "بعدی",
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Options: Stacked vertically for ample space and beautiful appearance
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Add photo
                        Button(
                            onClick = {
                                personForPhotoEdit = person
                                photoPickerLauncher.launch("image/*")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "افزودن عکس جدید",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Delete photo
                        if (uris.isNotEmpty()) {
                            androidx.compose.material3.FilledTonalButton(
                                onClick = {
                                    val freshPerson = allPersonsRaw.find { it.id == person.id } ?: person
                                    val currentUris = freshPerson.photoUris.toMutableList()
                                    if (currentUris.isNotEmpty() && currentImageIndex in currentUris.indices) {
                                        currentUris.removeAt(currentImageIndex)
                                        val newPhotoUri = if (currentUris.isEmpty()) null else currentUris.joinToString("|")
                                        val updatedPerson = freshPerson.copy(photoUri = newPhotoUri)
                                        viewModel.updatePerson(updatedPerson)
                                        Toast.makeText(context, "عکس با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                                        if (currentImageIndex >= currentUris.size && currentImageIndex > 0) {
                                            currentImageIndex--
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color(0xFFFEEBEE),
                                    contentColor = Color(0xFFD32F2F)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFFD32F2F)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "حذف عکس فعلی",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Close Button full-width below
                        OutlinedButton(
                            onClick = { showFullPhotoDialog = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.LightGray.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = textColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بستن گالری",
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    if (immersivePhotoIndex != null && immersivePhotoUris.isNotEmpty()) {
        Dialog(onDismissRequest = { immersivePhotoIndex = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        var totalDrag = 0f
                        detectDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onDragEnd = {
                                if (totalDrag > 100f) {
                                    val nextIndex = (immersivePhotoIndex!! - 1 + immersivePhotoUris.size) % immersivePhotoUris.size
                                    immersivePhotoIndex = nextIndex
                                } else if (totalDrag < -100f) {
                                    val nextIndex = (immersivePhotoIndex!! + 1) % immersivePhotoUris.size
                                    immersivePhotoIndex = nextIndex
                                }
                            },
                            onDragCancel = { totalDrag = 0f },
                            onDrag = { _, dragAmount ->
                                totalDrag += dragAmount.x
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                val activeIndex = immersivePhotoIndex!!
                val activePath = immersivePhotoUris.getOrNull(activeIndex)
                val fullImmersivePath = if (activePath != null) getFullOrOriginalPhotoPath(activePath) else ""

                if (fullImmersivePath.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = java.io.File(fullImmersivePath)),
                        contentDescription = "تصویر تمام صفحه",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
                
                // Close button
                IconButton(
                    onClick = { immersivePhotoIndex = null },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن تمام صفحه",
                        tint = Color.White
                    )
                }

                if (immersivePhotoUris.size > 1) {
                    // Outward-pointing Arrows (KeyboardArrowLeft and KeyboardArrowRight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .align(Alignment.Center),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val nextIndex = (immersivePhotoIndex!! - 1 + immersivePhotoUris.size) % immersivePhotoUris.size
                                immersivePhotoIndex = nextIndex
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "قبلی",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val nextIndex = (immersivePhotoIndex!! + 1) % immersivePhotoUris.size
                                immersivePhotoIndex = nextIndex
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "بعدی",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Top indicator overlay: e.g. "۱ از ۳"
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${activeIndex + 1} از ${immersivePhotoUris.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (personToSubMemberOf != null) {
        val parent = personToSubMemberOf!!
        val parentSpouses = remember(parent, relationships, allPersonsRaw) {
            relationships.filter { rel ->
                isSpouseRelation(rel.type) && (rel.personId1 == parent.id || rel.personId2 == parent.id)
            }.mapNotNull { rel ->
                val spouseId = if (rel.personId1 == parent.id) rel.personId2 else rel.personId1
                allPersonsRaw.find { it.id == spouseId }
            }.distinctBy { it.id }
        }
        AddPersonDialog(
            theme = currentTheme,
            textColor = textColor,
            accentColor = accentColor,
            parentName = parent.fullName,
            groups = allGroups,
            defaultGroupId = parent.groupId,
            availableSpouses = parentSpouses,
            onDismiss = { personToSubMemberOf = null },
            onConfirm = { firstName, lastName, gender, birthDate, birthPlace, deathDate, deathPlace, isDeceased, occupation, bio, groupId, selectedSpouseId ->
                viewModel.addChildToParent(
                    parent = parent,
                    child = Person(
                        firstName = firstName,
                        lastName = lastName,
                        gender = gender,
                        birthDate = birthDate,
                        birthPlace = birthPlace,
                        deathDate = deathDate,
                        deathPlace = deathPlace,
                        isDeceased = isDeceased,
                        occupation = occupation,
                        biography = bio,
                        groupId = groupId
                    ),
                    selectedSpouseId = selectedSpouseId
                ) { newId: Long ->
                    Toast.makeText(context, "زیرمجموعه (فرزند) با موفقیت به ${parent.fullName} اضافه شد", Toast.LENGTH_SHORT).show()
                }
                personToSubMemberOf = null
            }
        )
    }

    if (personToEdit != null) {
        val person = personToEdit!!
        EditPersonDialog(
            person = person,
            groups = allGroups,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { personToEdit = null },
            onConfirm = { updatedPerson ->
                viewModel.updatePerson(updatedPerson)
                Toast.makeText(context, "اطلاعات عضو با موفقیت ویرایش شد", Toast.LENGTH_SHORT).show()
                personToEdit = null
            }
        )
    }

    if (personToAddSpouseFor != null) {
        val spouseOf = personToAddSpouseFor!!
        AddSpouseDialog(
            spouseOf = spouseOf,
            groups = allGroups,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { personToAddSpouseFor = null },
            onConfirm = { firstName, lastName, gender, birthDate, birthPlace, deathDate, deathPlace, isDeceased, occupation, bio, groupId, relationshipType ->
                viewModel.addSpouseToPerson(
                    spouseOf = spouseOf,
                    spouse = Person(
                        firstName = firstName,
                        lastName = lastName,
                        gender = gender,
                        birthDate = birthDate,
                        birthPlace = birthPlace,
                        deathDate = deathDate,
                        deathPlace = deathPlace,
                        isDeceased = isDeceased,
                        occupation = occupation,
                        biography = bio,
                        groupId = groupId
                    ),
                    relationshipType = relationshipType
                ) { newId: Long ->
                    Toast.makeText(context, "همسر با موفقیت برای ${spouseOf.fullName} اضافه شد", Toast.LENGTH_SHORT).show()
                }
                personToAddSpouseFor = null
            }
        )
    }

    if (showAddGroupDialog) {
        AddGroupDialog(
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { name, description ->
                viewModel.addGroup(com.example.data.FamilyGroup(name = name, description = description))
                Toast.makeText(context, "گروه جدید با موفقیت ایجاد شد", Toast.LENGTH_SHORT).show()
                showAddGroupDialog = false
            }
        )
    }

    if (showNoGroupsWarningDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showNoGroupsWarningDialog = false },
                title = {
                    Text(
                        "خطا در ایجاد شخص جدید",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        "ابتدا باید یک خانواده (گروه فامیلی) جدید تعریف شود تا بتوان شخص جدید را به آن منتسب کرد. لطفاً ابتدا خانواده جدید بسازید.",
                        color = textColor,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNoGroupsWarningDialog = false
                            showAddGroupDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("ساخت خانواده جدید", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoGroupsWarningDialog = false }) {
                        Text("برگشت", color = textColor)
                    }
                },
                modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }

    if (showNoSelectionWarningDialog) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showNoSelectionWarningDialog = false },
                title = {
                    Text(
                        "خانواده‌ای انتخاب نشده است",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        "خانواده‌های متعددی در برنامه تعریف شده‌اند اما در حال حاضر هیچ خانواده‌ای انتخاب نشده است. لطفاً یا یک خانواده جدید بسازید یا از لیست بالا، یکی از خانواده‌های موجود را انتخاب کنید.",
                        color = textColor,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showNoSelectionWarningDialog = false
                            showAddGroupDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("ساخت خانواده جدید", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNoSelectionWarningDialog = false }) {
                        Text("برگشت / انتخاب خانواده موجود", color = textColor)
                    }
                },
                modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            )
        }
    }

    if (personToDelete != null) {
        val p = personToDelete!!
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { personToDelete = null },
                modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text("تایید حذف عضو", fontWeight = FontWeight.Bold, color = textColor) },
                text = {
                    Text(
                        "آیا از حذف «${p.fullName}» از شجره‌نامه اطمینان دارید؟ این عمل غیرقابل بازگشت است و تمامی روابط مربوط به این شخص نیز حذف خواهند شد.",
                        color = textColor,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePerson(p)
                            Toast.makeText(context, "عضو با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                            personToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("حذف شود", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { personToDelete = null }) {
                        Text("انصراف", color = textColor)
                    }
                }
            )
        }
    }

    if (showGroupPromptForParents != null) {
        val child = showGroupPromptForParents!!
        val currentGroup = allGroups.find { it.id == child.groupId }
        val currentGroupName = currentGroup?.name ?: "خانواده عمومی"
        var newGroupName by remember { mutableStateOf("خاندان ${child.lastName}") }
        
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { showGroupPromptForParents = null },
                modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                title = { Text("ثبت گروه جدید برای خانواده والدین", fontWeight = FontWeight.Bold, color = textColor) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "شما در حال ثبت پدر و مادر برای ${child.fullName} هستید. " +
                            "از آنجایی که آنها متعلق به خاندان دیگری هستند، پیشنهاد می‌شود یک گروه فامیلی جدید برای آنها ثبت کنید تا اطلاعات با خانواده اصلی (${currentGroupName}) مخلوط نشوند.",
                            color = textColor.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        AppTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = "نام گروه فامیلی جدید"
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newGroupName.isNotBlank()) {
                                viewModel.addGroup(com.example.data.FamilyGroup(name = newGroupName)) { newGroupId ->
                                    selectedGroupIdForParents = newGroupId
                                    personToAddParentsFor = child
                                    showGroupPromptForParents = null
                                }
                            } else {
                                Toast.makeText(context, "لطفاً نام گروه را وارد کنید", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Text("ایجاد گروه جدید و ادامه", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                selectedGroupIdForParents = child.groupId
                                personToAddParentsFor = child
                                showGroupPromptForParents = null
                            }
                        ) {
                            Text("استفاده از گروه فعلی", color = accentColor)
                        }
                        TextButton(
                            onClick = { showGroupPromptForParents = null }
                        ) {
                            Text("انصراف", color = Color.Gray)
                        }
                    }
                }
            )
        }
    }

    if (personToAddParentsFor != null) {
        val child = personToAddParentsFor!!
        val parentRelations = relationships.filter { 
            (it.type == "Parent-Child" || it.type == "Adoptive-Parent-Child") && it.personId2 == child.id 
        }
        val parentIds = parentRelations.map { it.personId1 }
        val parents = allPersonsRaw.filter { it.id in parentIds }
        val existingFather = parents.find { it.gender == "Male" }
        val existingMother = parents.find { it.gender == "Female" }

        AddParentsDialog(
            child = child,
            groupId = selectedGroupIdForParents,
            textColor = textColor,
            accentColor = accentColor,
            existingFather = existingFather,
            existingMother = existingMother,
            onDismiss = { personToAddParentsFor = null },
            onConfirm = { father, mother ->
                viewModel.addParentsToPerson(
                    child = child,
                    father = father,
                    mother = mother
                ) {
                    Toast.makeText(context, "والدین با موفقیت برای ${child.fullName} ثبت شدند", Toast.LENGTH_SHORT).show()
                    personToAddParentsFor = null
                }
            }
        )
    }

    if (groupToEdit != null) {
        val group = groupToEdit!!
        EditGroupDialog(
            group = group,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { groupToEdit = null },
            onConfirm = { updatedGroup ->
                viewModel.updateGroup(updatedGroup)
                Toast.makeText(context, "اطلاعات گروه با موفقیت ذخیره شد", Toast.LENGTH_SHORT).show()
                groupToEdit = null
            },
            onDelete = { g ->
                viewModel.deleteGroup(g)
                Toast.makeText(context, "گروه با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                groupToEdit = null
            },
            onBackupGroup = { g ->
                tempExportGroupId = g.id
                backupFileNameInput = "بکاپ_گروه_${g.name}"
                showBackupDialog = true
                groupToEdit = null
            },
            onRestoreGroup = { g ->
                try {
                    importFileLauncher.launch("*/*")
                    groupToEdit = null
                } catch (e: Exception) {
                    Toast.makeText(context, "خطا در اجرای انتخاب‌گر فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (personToMoveRelationOf != null) {
        val person = personToMoveRelationOf!!
        AddRelationshipDialog(
            persons = allPersonsRaw,
            preselectedP1 = person.id,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { personToMoveRelationOf = null },
            onConfirm = { p1Id, p2Id, type ->
                viewModel.addRelationship(p1Id, p2Id, type)
                Toast.makeText(context, "رابطه فامیلی جدید با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                personToMoveRelationOf = null
            }
        )
    }

    if (showStatsDialog) {
        StatsDialog(
            stats = stats,
            allPersons = allPersonsRaw,
            textColor = textColor,
            accentColor = accentColor,
            cardBgColor = cardColor,
            onDismiss = { showStatsDialog = false }
        )
    }

    if (showFamilyOverviewStatsDialog) {
        val currentGroupName = remember(allGroups, selectedGroupId) {
            allGroups.find { it.id == selectedGroupId }?.name ?: "خاندان عمومی"
        }
        FamilyOverviewStatsDialog(
            groupName = currentGroupName,
            persons = persons,
            relationships = relationships,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { showFamilyOverviewStatsDialog = false },
            onPersonClick = { person ->
                selectedPersonForDetails = person
            }
        )
    }

    if (showRemindersDialog) {
        RemindersDialog(
            events = upcomingEvents,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { showRemindersDialog = false }
        )
    }

    if (showCalculatorDialog) {
        CalculatorDialog(
            persons = allPersonsRaw,
            relationships = relationships,
            textColor = textColor,
            accentColor = accentColor,
            onDismiss = { showCalculatorDialog = false },
            onCalculate = { p1, p2 ->
                viewModel.setHighlightPerson1(p1.id)
                viewModel.setHighlightPerson2(p2.id)
                showCalculatorDialog = false
                activeTab = "Tree"
            }
        )
    }

    if (showBackupDialog) {
        val backupJson = viewModel.exportBackupToJson(tempExportGroupId)
        var fileName by remember(backupFileNameInput) { mutableStateOf(backupFileNameInput) }
        
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(if (tempExportGroupId == null) "تهیه فایل پشتیبان کلی" else "تهیه فایل پشتیبان گروه", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "برای ذخیره بکاپ به عنوان فایل در گوشی خود، ابتدا نام دلخواه را در کادر زیر وارد کنید و دکمه ذخیره فایل را بزنید:",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("نام فایل پشتیبان (بدون پسوند)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        onClick = {
                            if (fileName.isNotBlank()) {
                                backupJsonToSave = backupJson
                                try {
                                    createDocumentLauncher.launch("${fileName}.json")
                                    showBackupDialog = false
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطا در فراخوانی ذخیره‌ساز سیستم: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "لطفا ابتدا نام فایل را وارد کنید.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ذخیره به عنوان فایل (.json) در گوشی", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        text = "روش جایگزین: کپی کردن کد متنی زیر و ذخیره آن:",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn {
                            item {
                                Text(
                                    text = backupJson,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    onClick = {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("FamilyTreeBackup", backupJson)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "کد پشتیبان با موفقیت کپی شد.", Toast.LENGTH_SHORT).show()
                        showBackupDialog = false
                    }
                ) {
                    Text("کپی کد متنی", color = textColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupDialog = false }) {
                    Text("بستن", color = accentColor)
                }
            },
            containerColor = Color.White
        )
    }

    if (showRestoreDialog) {
        var restoreText by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("بازگردانی فایل پشتیبان (بکاپ)", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ادغام شجره‌نامه: اطلاعات نسخه پشتیبان با اطلاعات فعلی شما ادغام شده و اطلاعات قبلی شما پاک نخواهند شد.",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        onClick = {
                            try {
                                importFileLauncher.launch("*/*")
                            } catch (e: Exception) {
                                Toast.makeText(context, "خطا در اجرای انتخاب‌گر فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("انتخاب فایل بکاپ (.json) از گوشی", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        text = "روش جایگزین: قرار دادن کد متنی پشتیبان در کادر زیر:",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    
                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = { 
                            restoreText = it
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("کد پشتیبان را اینجا جایگذاری کنید...", fontSize = 11.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    onClick = {
                        if (restoreText.trim().isEmpty()) {
                            errorMessage = "لطفا ابتدا کد پشتیبان را وارد کنید یا فایل انتخاب نمایید."
                            return@Button
                        }
                        onRestoreBackupText(restoreText)
                    }
                ) {
                    Text("بازگردانی متن پشتیبان", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("انصراف", color = accentColor)
                }
            },
            containerColor = Color.White
        )
    }

    if (showSelectGroupRestoreDialog && restoreJsonPending != null) {
        AlertDialog(
            onDismissRequest = { 
                showSelectGroupRestoreDialog = false 
                restoreJsonPending = null
            },
            title = { Text("انتخاب گروه فامیلی هدف", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "اطلاعات بازیابی شده به کدام گروه فامیلی اضافه شوند؟",
                        fontSize = 13.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 250.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allGroups) { group ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.importBackupFromJson(restoreJsonPending!!, group.id) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            if (success) {
                                                showRestoreDialog = false
                                            }
                                        }
                                        showSelectGroupRestoreDialog = false
                                        restoreJsonPending = null
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(group.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                                        if (!group.description.isNullOrBlank()) {
                                            Text(
                                                group.description,
                                                fontSize = 11.sp,
                                                color = textColor.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { 
                        showSelectGroupRestoreDialog = false 
                        restoreJsonPending = null
                    }
                ) {
                    Text("انصراف", color = accentColor)
                }
            },
            containerColor = Color.White
        )
    }

    if (showSubtreeBackupPerson != null) {
        val rootPerson = showSubtreeBackupPerson!!
        val backupJson = viewModel.exportSubtreeBackupToJson(rootPerson.id)
        var fileName by remember(rootPerson) { mutableStateOf("backup_${rootPerson.firstName}_${rootPerson.lastName}") }
        
        AlertDialog(
            onDismissRequest = { showSubtreeBackupPerson = null },
            title = { Text("تهیه فایل پشتیبان عضو و زیرمجموعه‌ها", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "فایل پشتیبان شامل این عضو (${rootPerson.fullName})، همسر و تمام فرزندان و نوادگان ایشان خواهد بود.",
                        fontSize = 12.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("نام فایل پشتیبان (بدون پسوند)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        onClick = {
                            if (fileName.isNotBlank()) {
                                backupJsonToSave = backupJson
                                try {
                                    createDocumentLauncher.launch("${fileName}.json")
                                    showSubtreeBackupPerson = null
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطا در فراخوانی ذخیره‌ساز سیستم: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "لطفا ابتدا نام فایل را وارد کنید.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ذخیره به عنوان فایل (.json) در گوشی", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        text = "روش جایگزین: کپی کردن کد متنی زیر و ذخیره آن:",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn {
                            item {
                                Text(
                                    text = backupJson,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    onClick = {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("FamilyTreeSubtreeBackup", backupJson)
                        clipboardManager.setPrimaryClip(clip)
                        Toast.makeText(context, "کد پشتیبان عضو با موفقیت کپی شد.", Toast.LENGTH_SHORT).show()
                        showSubtreeBackupPerson = null
                    }
                ) {
                    Text("کپی کد متنی", color = textColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubtreeBackupPerson = null }) {
                    Text("بستن", color = accentColor)
                }
            },
            containerColor = Color.White
        )
    }

    if (showSubtreeRestoreDialog) {
        var restoreText by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showSubtreeRestoreDialog = false },
            title = { Text("بازگردانی شجره‌نامه عضو", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "بازگردانی شجره‌نامه عضو: اطلاعات این بکاپ به عنوان یک گروه جدید در برنامه ذخیره خواهد شد و تاثیری روی اطلاعات سایر گروه‌ها نخواهد داشت.",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        onClick = {
                            if (selectedGroupId == null) {
                                Toast.makeText(context, "جهت بازیابی بکاپ عضو، ابتدا باید یک گروه فامیلی ساخته و انتخاب کرده باشید.", Toast.LENGTH_LONG).show()
                            } else {
                                try {
                                    isRestoringSubtree = true
                                    importFileLauncher.launch("*/*")
                                } catch (e: Exception) {
                                    Toast.makeText(context, "خطا در اجرای انتخاب‌گر فایل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("انتخاب فایل بکاپ عضو (.json) از گوشی", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    Text(
                        text = "روش جایگزین: قرار دادن کد متنی پشتیبان در کادر زیر:",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    
                    OutlinedTextField(
                        value = restoreText,
                        onValueChange = { 
                            restoreText = it
                            errorMessage = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        placeholder = { Text("کد پشتیبان عضو را اینجا جایگذاری کنید...", fontSize = 11.sp) },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    onClick = {
                        if (selectedGroupId == null) {
                            Toast.makeText(context, "جهت بازیابی بکاپ عضو، ابتدا باید یک گروه فامیلی ساخته و انتخاب کرده باشید.", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (restoreText.trim().isEmpty()) {
                            errorMessage = "لطفا ابتدا کد پشتیبان را وارد کنید یا فایل انتخاب نمایید."
                            return@Button
                        }
                        viewModel.importSubtreeBackupFromJson(restoreText) { success, msg, newGroupId ->
                            if (success) {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (newGroupId != null) {
                                    viewModel.setSelectedGroupId(newGroupId)
                                }
                                showSubtreeRestoreDialog = false
                            } else {
                                errorMessage = msg
                            }
                        }
                    }
                ) {
                    Text("بازگردانی متن پشتیبان", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubtreeRestoreDialog = false }) {
                    Text("انصراف", color = accentColor)
                }
            },
            containerColor = Color.White
        )
    }
}
}

@Composable
fun InteractiveFamilyTree(
    persons: List<Person>,
    relationships: List<Relationship>,
    layoutType: String,
    focusPersonId: Long?,
    highlightP1Id: Long?,
    highlightP2Id: Long?,
    textColor: Color,
    accentColor: Color,
    cardBgColor: Color,
    lineColor: Color,
    onPersonClick: (Person) -> Unit,
    onPersonDoubleTap: (Person) -> Unit,
    onViewFamilyClick: (Person) -> Unit,
    onAddFirstPerson: () -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    glowPersonId: Long? = null
) {
    if (persons.isEmpty()) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val isCompact = maxHeight < 350.dp
            
            Card(
                modifier = Modifier
                    .fillMaxWidth(if (isCompact) 0.95f else 0.9f)
                    .border(1.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Brush.verticalGradient(listOf(Color(0xFFF9FBF9), Color.White)))
                        .padding(if (isCompact) 16.dp else 28.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 56.dp else 80.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountTree,
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompact) 28.dp else 40.dp),
                            tint = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 16.dp))
                    Text(
                        "درخت شجره‌نامه خالی است",
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompact) 15.sp else 18.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(if (isCompact) 4.dp else 8.dp))
                    Text(
                        "هیچ عضوی در این گروه یافت نشد. اولین عضو خانواده را اضافه کنید تا ترسیم هوشمند و زیبای شجره‌نامه آغاز شود.",
                        textAlign = TextAlign.Center,
                        color = textColor.copy(alpha = 0.6f),
                        fontSize = if (isCompact) 11.sp else 13.sp,
                        lineHeight = if (isCompact) 17.sp else 20.sp
                    )
                    Spacer(modifier = Modifier.height(if (isCompact) 12.dp else 20.dp))
                    Button(
                        onClick = onAddFirstPerson,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32), // High-contrast deep green for perfect readability
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(
                            horizontal = if (isCompact) 16.dp else 24.dp,
                            vertical = if (isCompact) 8.dp else 12.dp
                        ),
                        modifier = Modifier
                            .testTag("add_first_person_button")
                            .heightIn(min = if (isCompact) 40.dp else 48.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(if (isCompact) 16.dp else 20.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(if (isCompact) 4.dp else 8.dp))
                        Text(
                            text = "افزودن اولین عضو فامیل",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isCompact) 13.sp else 15.sp,
                            color = Color.White,
                            lineHeight = if (isCompact) 18.sp else 22.sp
                        )
                    }
                }
            }
        }
        return
    }

    // Position state tracker
    var scale by remember { mutableStateOf(1f) }
    val animatableOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // Generational Layout positioning calculations
    val positions = remember(persons, relationships, layoutType, focusPersonId) {
        computeTreeLayoutPositions(persons, relationships, layoutType, focusPersonId)
    }

    LaunchedEffect(glowPersonId, positions) {
        if (glowPersonId != null) {
            val pos = positions[glowPersonId]
            if (pos != null) {
                val targetXPx = pos.x * density
                val targetYPx = pos.y * density
                val targetOffset = Offset(-targetXPx * scale, -targetYPx * scale)
                animatableOffset.animateTo(
                    targetValue = targetOffset,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = FastOutSlowInEasing
                    )
                )
            }
        }
    }

    // Identify paths to highlight
    val highlightedPathIds = remember(highlightP1Id, highlightP2Id, persons, relationships) {
        if (highlightP1Id != null && highlightP2Id != null) {
            val p1 = persons.find { it.id == highlightP1Id }
            val p2 = persons.find { it.id == highlightP2Id }
            if (p1 != null && p2 != null) {
                val path = RelationshipCalculator.findShortestPath(p1, p2, persons, relationships)
                path?.map { it.first.id }?.toSet() ?: emptySet()
            } else {
                emptySet()
            }
        } else {
            emptySet()
        }
    }

    val childParentsMap = remember(relationships) {
        val map = mutableMapOf<Long, MutableList<Long>>()
        for (rel in relationships) {
            if (rel.type == "Parent-Child") {
                map.getOrPut(rel.personId2) { mutableListOf() }.add(rel.personId1)
            }
        }
        map
    }

    val isSpouseMap = remember(relationships) {
        val set = mutableSetOf<String>()
        for (rel in relationships) {
            if (isSpouseRelation(rel.type)) {
                val minId = minOf(rel.personId1, rel.personId2)
                val maxId = maxOf(rel.personId1, rel.personId2)
                set.add("$minId-$maxId")
            }
        }
        set
    }

    val spouseMapForHeart = remember(persons, relationships) {
        val map = mutableMapOf<Long, Color>()
        val spousePairs = mutableListOf<Pair<Long, Long>>()
        
        for (rel in relationships) {
            if (isSpouseRelation(rel.type)) {
                val minId = minOf(rel.personId1, rel.personId2)
                val maxId = maxOf(rel.personId1, rel.personId2)
                if (spousePairs.none { it.first == minId && it.second == maxId }) {
                    spousePairs.add(minId to maxId)
                }
            }
        }
        
        val heartColors = listOf(
            Color(0xFFE91E63), // Pink
            Color(0xFFFF2D55), // Red-pink
            Color(0xFFFF3B30), // System Red
            Color(0xFF9C27B0), // Purple
            Color(0xFFFF9500), // Orange
            Color(0xFFE040FB), // Magenta
            Color(0xFF007AFF), // Blue
            Color(0xFF4CD964)  // Green
        )
        
        spousePairs.forEachIndexed { index, pair ->
            val color = heartColors[index % heartColors.size]
            map[pair.first] = color
            map[pair.second] = color
        }
        map
    }

    // Dynamic panning / zooming canvas container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.2f, 3f)
                    val newOffset = animatableOffset.value + pan
                    coroutineScope.launch {
                        animatableOffset.snapTo(newOffset)
                    }
                }
            }
            .background(Color.Transparent)
    ) {
        Image(
            painter = painterResource(id = com.example.R.drawable.ic_family_tree_icon),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.25f),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = animatableOffset.value.x,
                    translationY = animatableOffset.value.y
                )
        ) {
            // Draw connection lines on bottom layer
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Draw lines between spouses and child relations
                for (rel in relationships) {
                    val pos1 = positions[rel.personId1]
                    val pos2 = positions[rel.personId2]
                    if (pos1 != null && pos2 != null) {
                        val p1Offset = Offset(
                            x = pos1.x * density + size.width / 2,
                            y = pos1.y * density + size.height / 2
                        )
                        val p2Offset = Offset(
                            x = pos2.x * density + size.width / 2,
                            y = pos2.y * density + size.height / 2
                        )

                        val isHighlightedConnection = highlightedPathIds.contains(rel.personId1) &&
                                highlightedPathIds.contains(rel.personId2)

                        val strokeWidth = if (isHighlightedConnection) 5.dp.toPx() else 2.5.dp.toPx()
                        
                        // Deterministic random line color per family/relationship to distinguish families
                        val lineColors = listOf(
                            Color(0xFF2E7D32), // Green
                            Color(0xFF1565C0), // Blue
                            Color(0xFFC2185B), // Pink/Red
                            Color(0xFF8E24AA), // Purple
                            Color(0xFFE65100), // Orange/Amber
                            Color(0xFF00838F), // Cyan
                            Color(0xFF00695C), // Teal
                            Color(0xFFD84315), // Deep Orange
                            Color(0xFF6D4C41), // Brown
                            Color(0xFF455A64)  // Blue Grey
                        )
                        val colorSeed = if (isSpouseRelation(rel.type)) {
                            minOf(rel.personId1, rel.personId2)
                        } else {
                            rel.personId1 // Parent ID for child relationship connections
                        }
                        val randomLineColor = lineColors[(colorSeed % lineColors.size).toInt()]
                        val drawColor = if (isHighlightedConnection) Color(0xFFD84315) else randomLineColor.copy(alpha = 0.9f)

                        when (rel.type) {
                            "Spouse", "SecondSpouse" -> {
                                // Solid connection line between spouses
                                drawLine(
                                    color = drawColor,
                                    start = p1Offset,
                                    end = p2Offset,
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            "Divorced", "SecondSpouse_Divorced" -> {
                                // Dashed connection line between divorced
                                drawLine(
                                    color = drawColor,
                                    start = p1Offset,
                                    end = p2Offset,
                                    strokeWidth = strokeWidth,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                                    cap = StrokeCap.Round
                                )
                            }
                            "Parent-Child" -> {
                                val childId = rel.personId2
                                val parentId = rel.personId1
                                
                                // Skip drawing child line if parentId is a second spouse
                                val isParentSecondSpouse = relationships.any { r ->
                                    isSecondSpouseRelation(r.type) && r.personId2 == parentId
                                }
                                
                                if (!isParentSecondSpouse) {
                                    val parents = childParentsMap[childId] ?: emptyList()

                                    val hasSpouseParents = if (parents.size >= 2) {
                                        val p1 = parents[0]
                                        val p2 = parents[1]
                                        val minId = minOf(p1, p2)
                                        val maxId = maxOf(p1, p2)
                                        isSpouseMap.contains("$minId-$maxId") && positions.containsKey(p1) && positions.containsKey(p2)
                                    } else {
                                        false
                                    }

                                    if (hasSpouseParents) {
                                        val p1 = parents[0]
                                        val p2 = parents[1]
                                        if (parentId == minOf(p1, p2)) {
                                            val posParent1 = positions[p1]
                                            val posParent2 = positions[p2]
                                            if (posParent1 != null && posParent2 != null) {
                                                val parent1Offset = Offset(
                                                    x = posParent1.x * density + size.width / 2,
                                                    y = posParent1.y * density + size.height / 2
                                                )
                                                val parent2Offset = Offset(
                                                    x = posParent2.x * density + size.width / 2,
                                                    y = posParent2.y * density + size.height / 2
                                                )
                                                val midPoint = Offset(
                                                    x = (parent1Offset.x + parent2Offset.x) / 2,
                                                    y = (parent1Offset.y + parent2Offset.y) / 2
                                                )
                                                drawElbowLine(
                                                    start = midPoint,
                                                    end = p2Offset,
                                                    color = drawColor,
                                                    strokeWidth = strokeWidth,
                                                    layoutType = layoutType
                                                )
                                            }
                                        }
                                    } else {
                                        drawElbowLine(
                                            start = p1Offset,
                                            end = p2Offset,
                                            color = drawColor,
                                            strokeWidth = strokeWidth,
                                            layoutType = layoutType
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Render card nodes above lines
            persons.forEach { person ->
                val pos = positions[person.id]
                if (pos != null) {
                    val cardXPx = pos.x * density
                    val cardYPx = pos.y * density

                    Box(
                        modifier = Modifier
                            .absoluteOffset { IntOffset(cardXPx.roundToInt(), cardYPx.roundToInt()) }
                            .padding(8.dp)
                            .align(Alignment.Center)
                    ) {
                        val isPathHighlighted = highlightedPathIds.contains(person.id)
                        val isSecondSpouse = remember(person.id, relationships) {
                            relationships.any { rel ->
                                isSecondSpouseRelation(rel.type) && rel.personId2 == person.id
                            }
                        }
                        FamilyMemberNodeCard(
                            person = person,
                            isHighlighted = isPathHighlighted,
                            accentColor = accentColor,
                            cardBgColor = cardBgColor,
                            textColor = textColor,
                            spouseHeartColor = spouseMapForHeart[person.id],
                            isSecondSpouse = isSecondSpouse,
                            onFocusClick = { onViewFamilyClick(person) },
                            onClick = { onPersonClick(person) },
                            onDoubleTap = { onPersonDoubleTap(person) },
                            onPhotoClick = onPhotoClick,
                            glowPersonId = glowPersonId
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawElbowLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    layoutType: String
) {
    if (layoutType == "Circular") {
        // Direct line for circular layout to prevent distorted elbows
        drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth)
    } else if (layoutType == "Horizontal") {
        // Horizontal elbow (X-axis dominant)
        val midX = (start.x + end.x) / 2
        drawLine(color = color, start = start, end = Offset(midX, start.y), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(midX, start.y), end = Offset(midX, end.y), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(midX, end.y), end = end, strokeWidth = strokeWidth)
    } else {
        // Vertical elbow (Y-axis dominant)
        val midY = (start.y + end.y) / 2
        drawLine(color = color, start = start, end = Offset(start.x, midY), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(start.x, midY), end = Offset(end.x, midY), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(end.x, midY), end = end, strokeWidth = strokeWidth)
    }
}

@Composable
fun FamilyMemberNodeCard(
    person: Person,
    isHighlighted: Boolean,
    accentColor: Color,
    cardBgColor: Color,
    textColor: Color,
    spouseHeartColor: Color? = null,
    isSecondSpouse: Boolean = false,
    onFocusClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    glowPersonId: Long? = null
) {
    val isGlow = person.id == glowPersonId
    val borderStroke = if (isHighlighted) {
        // High-contrast Orange and White gradient border for selection/highlight
        BorderStroke(3.dp, Brush.linearGradient(listOf(Color(0xFFF57C00), Color.White, Color(0xFFF57C00))))
    } else if (isGlow) {
        BorderStroke(3.2.dp, accentColor)
    } else if (person.isDeceased) {
        BorderStroke(1.2.dp, Color.Gray.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
    }

    val cardModifier = Modifier
        .width(160.dp) // Slightly wider for better details fitting
        .let { modifier ->
            if (isGlow) {
                modifier.shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp))
            } else {
                modifier
            }
        }
        .clickable { onClick() }
        .testTag("member_node_${person.id}")

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White // Explicit pure white card background for maximum contrast
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        border = borderStroke,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))) {
            // Deceased overlay (Elegant Diagonal Black Ribbon in top-left corner, non-obtrusive)
            if (person.isDeceased) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Canvas(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        drawLine(
                            color = Color(0xFF1E1E1E), // Solid dark charcoal/black
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 6.dp.toPx()
                        )
                    }
                }
            }

            // Spouse heart icon (top-right, shifted left of the eye button)
            if (spouseHeartColor != null) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "همسر",
                    tint = spouseHeartColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 32.dp, top = 8.dp)
                        .size(16.dp)
                )
            }

            // View family eye button (top-right)
            IconButton(
                onClick = { onFocusClick?.invoke() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "مشاهده خاندان",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // Gender distinction icon background or Custom circular photo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            if (person.gender == "Male") Color(0xFFE3F2FD) else Color(0xFFFCE4EC)
                        )
                        .border(1.5.dp, if (person.photoUris.isNotEmpty()) accentColor else Color.Transparent, CircleShape)
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
                            if (person.gender == "Male") Icons.Default.Boy else Icons.Default.Girl,
                            contentDescription = null,
                            tint = if (person.gender == "Male") Color(0xFF1E88E5) else Color(0xFFD81B60),
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = person.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF112E21), // High-contrast Deep Forest Charcoal text
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                if (isSecondSpouse) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFF9C4), RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFFFBC02D), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFF57F17),
                                modifier = Modifier.size(10.dp)
                            )
                            Text("همسر دوم", fontSize = 8.sp, color = Color(0xFFF57F17), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = person.occupation ?: "-",
                    fontSize = 10.sp,
                    color = accentColor, // Highly visible rich light green for details
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (person.birthDate != null) {
                    val year = person.birthDate.split("-").firstOrNull() ?: ""
                    val deathYear = if (person.isDeceased) person.deathDate?.split("-")?.firstOrNull() ?: "؟" else ""
                    val ageDisplay = if (person.isDeceased) "($year - $deathYear)" else "متولد $year"
                    
                    Text(
                        text = ageDisplay.toFarsiNumbers(),
                        fontSize = 10.sp,
                        color = Color(0xFF455A64), // Extremely legible slate gray for year
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Convert DP to TB equivalent or small sizes
private fun Int.tb() = this.dp

fun computeTreeLayoutPositions(
    persons: List<Person>,
    relationships: List<Relationship>,
    layoutType: String,
    focusPersonId: Long?
): Map<Long, TreePos> {
    if (persons.isEmpty()) return emptyMap()

    val parentsMap = mutableMapOf<Long, MutableList<Long>>()
    val childrenMap = mutableMapOf<Long, MutableList<Long>>()
    val spousesMap = mutableMapOf<Long, MutableSet<Long>>()

    for (rel in relationships) {
        when (rel.type) {
            "Spouse", "Divorced", "SecondSpouse", "SecondSpouse_Divorced" -> {
                spousesMap.getOrPut(rel.personId1) { mutableSetOf() }.add(rel.personId2)
                spousesMap.getOrPut(rel.personId2) { mutableSetOf() }.add(rel.personId1)
            }
            "Parent-Child", "Adoptive-Parent-Child" -> {
                childrenMap.getOrPut(rel.personId1) { mutableListOf() }.add(rel.personId2)
                parentsMap.getOrPut(rel.personId2) { mutableListOf() }.add(rel.personId1)
            }
        }
    }

    val levels = mutableMapOf<Long, Int>()
    val roots = persons.filter { p ->
        val hasNoParents = parentsMap[p.id].isNullOrEmpty()
        val spouseHasParents = (spousesMap[p.id] ?: emptySet()).any { spouseId ->
            parentsMap[spouseId]?.isNotEmpty() == true
        }
        hasNoParents && !spouseHasParents
    }
    val baseRoots = if (roots.isEmpty()) listOf(persons.first()) else roots

    val queue: Queue<Long> = LinkedList()
    for (root in baseRoots) {
        levels[root.id] = 0
        queue.add(root.id)
    }

    while (queue.isNotEmpty()) {
        val currId = queue.poll() ?: continue
        val currLevel = levels[currId] ?: 0

        val spouses = spousesMap[currId] ?: emptySet()
        for (spouseId in spouses) {
            if (!levels.containsKey(spouseId)) {
                levels[spouseId] = currLevel
                queue.add(spouseId)
            }
        }

        val children = childrenMap[currId] ?: emptyList()
        for (childId in children) {
            if (!levels.containsKey(childId)) {
                levels[childId] = currLevel + 1
                queue.add(childId)
            }
        }
    }

    // Assign default 0 to any disconnected nodes
    for (p in persons) {
        if (!levels.containsKey(p.id)) {
            levels[p.id] = 0
        }
    }

    // Determine focus mode filters
    val visiblePersonIds = if (focusPersonId != null) {
        val visitedSet = mutableSetOf<Long>()
        
        // 1. Traverse upwards to get ancestors
        val ancestorQueue: Queue<Long> = LinkedList()
        ancestorQueue.add(focusPersonId)
        visitedSet.add(focusPersonId)
        while (ancestorQueue.isNotEmpty()) {
            val curr = ancestorQueue.poll() ?: continue
            parentsMap[curr]?.forEach { parentId ->
                if (visitedSet.add(parentId)) {
                    ancestorQueue.add(parentId)
                }
            }
        }

        // 2. Traverse downwards from focus person and ancestors to get descendants
        val descendantsQueue: Queue<Long> = LinkedList()
        descendantsQueue.add(focusPersonId)
        while (descendantsQueue.isNotEmpty()) {
            val curr = descendantsQueue.poll() ?: continue
            childrenMap[curr]?.forEach { childId ->
                if (visitedSet.add(childId)) {
                    descendantsQueue.add(childId)
                }
            }
        }

        // 3. Add spouses of everyone in the visited set
        val spousesToAdd = mutableSetOf<Long>()
        visitedSet.forEach { personId ->
            spousesMap[personId]?.forEach { spouseId ->
                spousesToAdd.add(spouseId)
            }
        }
        visitedSet.addAll(spousesToAdd)

        visitedSet
    } else {
        persons.map { it.id }.toSet()
    }

    class SubtreeLayout(
        val positions: Map<Long, Float>,
        val minXAtLevel: Map<Int, Float>,
        val maxXAtLevel: Map<Int, Float>
    )

    val visiblePersonSet = visiblePersonIds.toSet()
    val visitedSubtrees = mutableSetOf<Long>()

    fun layoutSubtree(personId: Long, level: Int): SubtreeLayout {
        // Find spouse group
        val spouses = (spousesMap[personId] ?: emptySet()).filter { visiblePersonSet.contains(it) }.sorted()
        val spouseGroup = (listOf(personId) + spouses).filter { !visitedSubtrees.contains(it) }
        
        spouseGroup.forEach { visitedSubtrees.add(it) }
        
        val spouseSpacing = 220f
        val siblingSpacing = 240f
        val S = spouseGroup.size
        
        // Initial positions of spouses centered at 0
        val localPositions = mutableMapOf<Long, Float>()
        for (i in 0 until S) {
            val memberId = spouseGroup[i]
            localPositions[memberId] = i * spouseSpacing - (S - 1) * spouseSpacing / 2f
        }
        
        val children = spouseGroup.flatMap { childrenMap[it] ?: emptyList() }
            .filter { visiblePersonSet.contains(it) && !visitedSubtrees.contains(it) }
            .distinct()
            .sorted()

        if (children.isEmpty()) {
            val levelMinX = mapOf(level to -(S - 1) * spouseSpacing / 2f)
            val levelMaxX = mapOf(level to (S - 1) * spouseSpacing / 2f)
            return SubtreeLayout(localPositions, levelMinX, levelMaxX)
        }

        // Layout all children subtrees
        val childLayouts = children.map { childId ->
            layoutSubtree(childId, level + 1)
        }

        // Merge children subtrees from left to right, avoiding overlap at any level
        val mergedPositions = mutableMapOf<Long, Float>()
        val mergedMinX = mutableMapOf<Int, Float>()
        val mergedMaxX = mutableMapOf<Int, Float>()

        for (i in childLayouts.indices) {
            val childLayout = childLayouts[i]
            if (i == 0) {
                mergedPositions.putAll(childLayout.positions)
                mergedMinX.putAll(childLayout.minXAtLevel)
                mergedMaxX.putAll(childLayout.maxXAtLevel)
            } else {
                // Find the minimum shift to avoid overlap with merged so far
                var minShift = 0f
                val overlapLevels = mergedMaxX.keys.intersect(childLayout.minXAtLevel.keys)
                for (lvl in overlapLevels) {
                    val currentMax = mergedMaxX[lvl] ?: 0f
                    val childMin = childLayout.minXAtLevel[lvl] ?: 0f
                    val neededShift = currentMax + siblingSpacing - childMin
                    if (neededShift > minShift) {
                        minShift = neededShift
                    }
                }

                // Apply shift and merge
                childLayout.positions.forEach { (id, x) ->
                    mergedPositions[id] = x + minShift
                }
                childLayout.minXAtLevel.forEach { (lvl, x) ->
                    val newMin = x + minShift
                    mergedMinX[lvl] = minOf(mergedMinX[lvl] ?: newMin, newMin)
                }
                childLayout.maxXAtLevel.forEach { (lvl, x) ->
                    val newMax = x + minShift
                    mergedMaxX[lvl] = maxOf(mergedMaxX[lvl] ?: newMax, newMax)
                }
            }
        }

        // Now, we want to center the parent spouse group over the children
        // The children's root nodes are the first level of the merged children (level + 1)
        val childrenMin = mergedMinX[level + 1] ?: 0f
        val childrenMax = mergedMaxX[level + 1] ?: 0f
        val childrenCenter = (childrenMin + childrenMax) / 2f

        // We want childrenCenter to align with parent center (which is 0)
        val shiftAmount = -childrenCenter
        val finalPositions = mutableMapOf<Long, Float>()
        finalPositions.putAll(localPositions) // parent spouses stay at their local positions

        mergedPositions.forEach { (id, x) ->
            finalPositions[id] = x + shiftAmount
        }

        // Compute final level bounds
        val finalMinX = mutableMapOf<Int, Float>()
        val finalMaxX = mutableMapOf<Int, Float>()

        finalMinX[level] = -(S - 1) * spouseSpacing / 2f
        finalMaxX[level] = (S - 1) * spouseSpacing / 2f

        mergedMinX.forEach { (lvl, x) ->
            finalMinX[lvl] = x + shiftAmount
        }
        mergedMaxX.forEach { (lvl, x) ->
            finalMaxX[lvl] = x + shiftAmount
        }

        return SubtreeLayout(finalPositions, finalMinX, finalMaxX)
    }

    val allSubtreeLayouts = mutableListOf<SubtreeLayout>()
    
    // Process roots first
    val rootIds = persons.filter { parentsMap[it.id].isNullOrEmpty() }.map { it.id }.sorted()
    for (rootId in rootIds) {
        if (visiblePersonSet.contains(rootId) && !visitedSubtrees.contains(rootId)) {
            allSubtreeLayouts.add(layoutSubtree(rootId, 0))
        }
    }

    // Process any remaining unvisited visible nodes (disconnected subtrees)
    for (p in persons) {
        if (visiblePersonSet.contains(p.id) && !visitedSubtrees.contains(p.id)) {
            allSubtreeLayouts.add(layoutSubtree(p.id, 0))
        }
    }

    // Merge all independent subtrees side-by-side
    val finalPositions = mutableMapOf<Long, Float>()
    val globalMaxX = mutableMapOf<Int, Float>()
    val siblingSpacing = 240f

    for (i in allSubtreeLayouts.indices) {
        val layout = allSubtreeLayouts[i]
        if (i == 0) {
            finalPositions.putAll(layout.positions)
            layout.maxXAtLevel.forEach { (lvl, x) ->
                globalMaxX[lvl] = x
            }
        } else {
            // Find minimum shift to avoid overlap with global max X at any level
            var minShift = 0f
            val overlapLevels = globalMaxX.keys.intersect(layout.minXAtLevel.keys)
            for (lvl in overlapLevels) {
                val currentMax = globalMaxX[lvl] ?: 0f
                val childMin = layout.minXAtLevel[lvl] ?: 0f
                val neededShift = currentMax + siblingSpacing - childMin
                if (neededShift > minShift) {
                    minShift = neededShift
                }
            }

            // Apply shift and merge
            layout.positions.forEach { (id, x) ->
                finalPositions[id] = x + minShift
            }
            layout.maxXAtLevel.forEach { (lvl, x) ->
                val newMax = x + minShift
                globalMaxX[lvl] = maxOf(globalMaxX[lvl] ?: newMax, newMax)
            }
        }
    }

    val positions = mutableMapOf<Long, TreePos>()
    val vSpacing = 220f

    val radialAngles = mutableMapOf<Long, Float>()
    if (layoutType == "Circular") {
        val visited = mutableSetOf<Long>()
        val rootCouples = mutableListOf<List<Long>>()
        val rootSet = baseRoots.map { it.id }.toMutableSet()
        while (rootSet.isNotEmpty()) {
            val rId = rootSet.first()
            rootSet.remove(rId)
            val spouses = (spousesMap[rId] ?: emptySet()).filter { baseRoots.any { br -> br.id == it } }
            val couple = listOf(rId) + spouses
            rootCouples.add(couple)
            rootSet.removeAll(spouses)
        }

        fun assignAngles(
            currentId: Long,
            minAngle: Float,
            maxAngle: Float
        ) {
            if (visited.contains(currentId)) return
            visited.add(currentId)

            val midAngle = (minAngle + maxAngle) / 2f
            radialAngles[currentId] = midAngle

            val spouses = (spousesMap[currentId] ?: emptySet()).filter { visiblePersonIds.contains(it) && !visited.contains(it) }.sorted()
            val numSpouses = spouses.size
            if (numSpouses > 0) {
                val spouseAngleSpan = minOf(15f * (Math.PI.toFloat() / 180f), (maxAngle - minAngle) * 0.2f)
                spouses.forEachIndexed { index, spouseId ->
                    visited.add(spouseId)
                    val offsetFraction = (index + 1) / (numSpouses.toFloat() + 1f) - 0.5f
                    radialAngles[spouseId] = midAngle + offsetFraction * spouseAngleSpan
                }
            }

            val allParentsInGroup = listOf(currentId) + spouses
            val children = allParentsInGroup.flatMap { childrenMap[it] ?: emptyList() }
                .filter { visiblePersonIds.contains(it) && !visited.contains(it) }
                .distinct()
                .sorted()

            if (children.isNotEmpty()) {
                val childSectorSpan = (maxAngle - minAngle) / children.size
                children.forEachIndexed { index, childId ->
                    val childMinAngle = minAngle + index * childSectorSpan
                    val childMaxAngle = childMinAngle + childSectorSpan
                    assignAngles(childId, childMinAngle, childMaxAngle)
                }
            }
        }

        val numCouples = rootCouples.size
        if (numCouples > 0) {
            val sectorSpan = (2f * Math.PI.toFloat()) / numCouples
            rootCouples.forEachIndexed { coupleIndex, couple ->
                val minAngle = coupleIndex * sectorSpan
                val maxAngle = minAngle + sectorSpan
                val mainId = couple.first()
                assignAngles(mainId, minAngle, maxAngle)
            }
        }

        // Handle any unvisited nodes that are in visiblePersonIds
        for (pId in visiblePersonIds) {
            if (!visited.contains(pId)) {
                radialAngles[pId] = 0f
            }
        }
    }

    for (p in persons) {
        val id = p.id
        if (visiblePersonIds.contains(id)) {
            val level = levels[id] ?: 0
            val posX = finalPositions[id] ?: 0f

            when (layoutType) {
                "Horizontal" -> {
                    positions[id] = TreePos(
                        x = level * vSpacing,
                        y = posX
                    )
                }
                "Circular" -> {
                    val angle = radialAngles[id] ?: 0f
                    val radius = if (level == 0) {
                        val spouses = spousesMap[id] ?: emptySet()
                        if (spouses.isNotEmpty()) 70f else 0f
                    } else {
                        level * 300f + 100f
                    }
                    positions[id] = TreePos(
                        x = radius * kotlin.math.cos(angle),
                        y = radius * kotlin.math.sin(angle)
                    )
                }
                else -> { // "Vertical"
                    positions[id] = TreePos(
                        x = posX,
                        y = level * vSpacing
                    )
                }
            }
        }
    }

    // Center the entire tree around (0, 0)
    if (positions.isNotEmpty() && layoutType != "Circular") {
        val minX = positions.values.map { it.x }.minOrNull() ?: 0f
        val maxX = positions.values.map { it.x }.maxOrNull() ?: 0f
        val minY = positions.values.map { it.y }.minOrNull() ?: 0f
        val maxY = positions.values.map { it.y }.maxOrNull() ?: 0f
        
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        
        for (id in positions.keys) {
            val pos = positions[id]!!
            positions[id] = TreePos(pos.x - centerX, pos.y - centerY)
        }
    }

    return positions
}

@Composable
fun <T> WheelColumn(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String
) {
    val listState = rememberLazyListState()
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    
    // Pad items with 2 null items at start and end for 5-slot wheel centering
    val paddedItems = remember(items) {
        listOf<T?>(null, null) + items.map { it } + listOf<T?>(null, null)
    }

    // Scroll to the selected item on start or selection change
    LaunchedEffect(selectedIndex) {
        listState.scrollToItem(selectedIndex)
    }

    // Observe scroll position to find the center item
    val centerIndex = remember {
        derivedStateOf {
            val firstIndex = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            // Average item height is around 30.dp, which is 80-100 pixels depending on density.
            // If offset is more than half the item height, round up.
            if (offset > 45) {
                (firstIndex + 1).coerceIn(0, items.size - 1)
            } else {
                firstIndex.coerceIn(0, items.size - 1)
            }
        }
    }.value

    LaunchedEffect(centerIndex) {
        if (centerIndex in items.indices) {
            onItemSelected(items[centerIndex])
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        // Center selection bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                .border(1.dp, Color(0xFF81C784), RoundedCornerShape(4.dp))
        )

        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            itemsIndexed(paddedItems) { idx, item ->
                val realIdx = idx - 2
                val isSelected = realIdx == centerIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .clickable {
                            if (item != null) {
                                onItemSelected(item)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (item != null) {
                        Text(
                            text = labelProvider(item),
                            fontSize = if (isSelected) 15.sp else 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF1B5E20) else Color.Gray,
                            maxLines = 1
                        )
                    } else {
                        Spacer(modifier = Modifier.size(1.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun InlineFarsiDatePicker(
    label: String,
    initialDate: String,
    onDateChanged: (String) -> Unit
) {
    // Parse existing date (YYYY-MM-DD or YYYY-MM or YYYY)
    val parts = initialDate.split("-")
    val initialYear = parts.getOrNull(0)?.toIntOrNull() ?: 1380
    val initialMonth = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val initialDay = parts.getOrNull(2)?.toIntOrNull() ?: 0

    val years = (1300..1420).toList()
    val months = listOf(
        "نامشخص",
        "فروردین (۰۱)",
        "اردیبهشت (۰۲)",
        "خرداد (۰۳)",
        "تیر (۰۴)",
        "مرداد (۰۵)",
        "شهریور (۰۶)",
        "مهر (۰۷)",
        "آبان (۰۸)",
        "آذر (۰۹)",
        "دی (۱۰)",
        "بهمن (۱۱)",
        "اسفند (۱۲)"
    )

    var selectedYear by remember { mutableStateOf(if (initialYear in 1300..1420) initialYear else 1380) }
    var selectedMonthIndex by remember { mutableStateOf(if (initialMonth in 0..12) initialMonth else 0) }

    val maxDays = when (selectedMonthIndex) {
        0 -> 31
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> 29
        else -> 31
    }

    var selectedDayIndex by remember(selectedMonthIndex) {
        val coercedDay = if (initialDay in 0..maxDays) initialDay else 0
        mutableStateOf(coercedDay)
    }

    var yearMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    // Whenever selections change, report back the formatted date string
    LaunchedEffect(selectedYear, selectedMonthIndex, selectedDayIndex) {
        val formattedMonth = if (selectedMonthIndex > 0) String.format("%02d", selectedMonthIndex) else null
        val formattedDay = if (selectedDayIndex > 0) String.format("%02d", selectedDayIndex) else null

        val finalDate = when {
            formattedMonth != null && formattedDay != null -> "$selectedYear-$formattedMonth-$formattedDay"
            formattedMonth != null -> "$selectedYear-$formattedMonth"
            else -> "$selectedYear"
        }
        onDateChanged(finalDate)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F8F5), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFCBE3D8), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32),
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Day Card Selector
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .clickable { dayMenuExpanded = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Column {
                    Text("روز", fontSize = 9.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val dayDisplay = if (selectedDayIndex == 0) "نامشخص" else selectedDayIndex.toString()
                        Text(dayDisplay, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112E21))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                    }
                }
                DropdownMenu(
                    expanded = dayMenuExpanded,
                    onDismissRequest = { dayMenuExpanded = false },
                    modifier = Modifier
                        .width(110.dp)
                        .heightIn(max = 240.dp)
                        .background(Color.White)
                ) {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LaunchedEffect(dayMenuExpanded) {
                        if (dayMenuExpanded) {
                            listState.scrollToItem(selectedDayIndex)
                        }
                    }
                    Box(modifier = Modifier.size(width = 110.dp, height = 240.dp)) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(maxDays + 1) { index ->
                                val dayVal = if (index == 0) "نامشخص" else index.toString()
                                DropdownMenuItem(
                                    text = { Text(dayVal, fontSize = 12.sp, fontWeight = if (index == selectedDayIndex) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF112E21)) },
                                    onClick = {
                                        selectedDayIndex = index
                                        dayMenuExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            // 2. Month Card Selector
            Box(
                modifier = Modifier
                    .weight(1.3f)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .clickable { monthMenuExpanded = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Column {
                    Text("ماه", fontSize = 9.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val monthDisplay = months[selectedMonthIndex]
                        Text(monthDisplay, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112E21), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                    }
                }
                DropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false },
                    modifier = Modifier
                        .width(130.dp)
                        .heightIn(max = 240.dp)
                        .background(Color.White)
                ) {
                    months.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            text = { Text(name, fontSize = 12.sp, fontWeight = if (index == selectedMonthIndex) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF112E21)) },
                            onClick = {
                                selectedMonthIndex = index
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // 3. Year Card Selector
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .clickable { yearMenuExpanded = true }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Column {
                    Text("سال", fontSize = 9.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedYear.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112E21))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                    }
                }
                DropdownMenu(
                    expanded = yearMenuExpanded,
                    onDismissRequest = { yearMenuExpanded = false },
                    modifier = Modifier
                        .width(110.dp)
                        .heightIn(max = 240.dp)
                        .background(Color.White)
                ) {
                    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                    LaunchedEffect(yearMenuExpanded) {
                        if (yearMenuExpanded) {
                            val idx = years.indexOf(selectedYear).coerceAtLeast(0)
                            listState.scrollToItem(idx)
                        }
                    }
                    Box(modifier = Modifier.size(width = 110.dp, height = 240.dp)) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(years.size) { index ->
                                val yr = years[index]
                                DropdownMenuItem(
                                    text = { Text(yr.toString(), fontSize = 12.sp, fontWeight = if (yr == selectedYear) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF112E21)) },
                                    onClick = {
                                        selectedYear = yr
                                        yearMenuExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FarsiWheelDatePickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Parse existing date (YYYY-MM-DD or YYYY-MM or YYYY)
    val parts = initialDate.split("-")
    val initialYear = parts.getOrNull(0)?.toIntOrNull() ?: 1380
    val initialMonth = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val initialDay = parts.getOrNull(2)?.toIntOrNull() ?: 0

    val years = (1300..1420).toList()
    val months = listOf(
        "نامشخص",
        "فروردین (۰۱)",
        "اردیبهشت (۰۲)",
        "خرداد (۰۳)",
        "تیر (۰۴)",
        "مرداد (۰۵)",
        "شهریور (۰۶)",
        "مهر (۰۷)",
        "آبان (۰۸)",
        "آذر (۰۹)",
        "دی (۱۰)",
        "بهمن (۱۱)",
        "اسفند (۱۲)"
    )

    var selectedYear by remember { mutableStateOf(if (initialYear in 1300..1420) initialYear else 1380) }
    var selectedMonthIndex by remember { mutableStateOf(if (initialMonth in 0..12) initialMonth else 0) }
    
    val maxDays = when (selectedMonthIndex) {
        0 -> 31
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> 29
        else -> 31
    }
    
    var selectedDayIndex by remember(selectedMonthIndex) { 
        val coercedDay = if (initialDay in 0..maxDays) initialDay else 0
        mutableStateOf(coercedDay) 
    }

    var yearMenuExpanded by remember { mutableStateOf(false) }
    var monthMenuExpanded by remember { mutableStateOf(false) }
    var dayMenuExpanded by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                Button(
                    onClick = {
                        // Year is mandatory. Month and Day are optional.
                        val formattedMonth = if (selectedMonthIndex > 0) String.format("%02d", selectedMonthIndex) else null
                        val formattedDay = if (selectedDayIndex > 0) String.format("%02d", selectedDayIndex) else null
                        
                        val finalDate = when {
                            formattedMonth != null && formattedDay != null -> "$selectedYear-$formattedMonth-$formattedDay"
                            formattedMonth != null -> "$selectedYear-$formattedMonth"
                            else -> "$selectedYear"
                        }
                        onConfirm(finalDate)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("تایید", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("انصراف", color = Color.Gray)
                }
            },
            title = {
                Text(
                    "ثبت سن و تاریخ تولد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "سال تولد الزامی است. ماه و روز اختیاری می‌باشد.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Three side-by-side drop-down fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Year Card Selector
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .background(Color(0xFFF9FBF9), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFCBE3D8), RoundedCornerShape(12.dp))
                                .clickable { yearMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text("سال", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(selectedYear.toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112E21))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                                }
                            }
                            DropdownMenu(
                                expanded = yearMenuExpanded,
                                onDismissRequest = { yearMenuExpanded = false },
                                modifier = Modifier
                                    .width(100.dp)
                                    .heightIn(max = 240.dp)
                                    .background(Color.White)
                            ) {
                                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                // Scroll to selected year
                                LaunchedEffect(yearMenuExpanded) {
                                    if (yearMenuExpanded) {
                                        val idx = years.indexOf(selectedYear).coerceAtLeast(0)
                                        listState.scrollToItem(idx)
                                    }
                                }
                                Box(modifier = Modifier.size(width = 100.dp, height = 240.dp)) {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(years.size) { index ->
                                            val yr = years[index]
                                            DropdownMenuItem(
                                                text = { Text(yr.toString(), fontSize = 13.sp, fontWeight = if (yr == selectedYear) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    selectedYear = yr
                                                    yearMenuExpanded = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Month Card Selector
                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .background(Color(0xFFF9FBF9), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFCBE3D8), RoundedCornerShape(12.dp))
                                .clickable { monthMenuExpanded = true }
                                .padding(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text("ماه", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val monthDisplay = months[selectedMonthIndex]
                                    Text(monthDisplay, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112E21), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                                }
                            }
                            DropdownMenu(
                                expanded = monthMenuExpanded,
                                onDismissRequest = { monthMenuExpanded = false },
                                modifier = Modifier
                                    .width(130.dp)
                                    .heightIn(max = 240.dp)
                                    .background(Color.White)
                            ) {
                                months.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name, fontSize = 12.sp, fontWeight = if (index == selectedMonthIndex) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedMonthIndex = index
                                            monthMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Day Card Selector
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .background(Color(0xFFF9FBF9), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFCBE3D8), RoundedCornerShape(12.dp))
                                .clickable { dayMenuExpanded = true }
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text("روز", fontSize = 10.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val dayDisplay = if (selectedDayIndex == 0) "نامشخص" else selectedDayIndex.toString()
                                    Text(dayDisplay, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112E21))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                                }
                            }
                            DropdownMenu(
                                expanded = dayMenuExpanded,
                                onDismissRequest = { dayMenuExpanded = false },
                                modifier = Modifier
                                    .width(90.dp)
                                    .heightIn(max = 240.dp)
                                    .background(Color.White)
                            ) {
                                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                                LaunchedEffect(dayMenuExpanded) {
                                    if (dayMenuExpanded) {
                                        listState.scrollToItem(selectedDayIndex)
                                    }
                                }
                                Box(modifier = Modifier.size(width = 90.dp, height = 240.dp)) {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(maxDays + 1) { index ->
                                            val dayVal = if (index == 0) "نامشخص" else index.toString()
                                            DropdownMenuItem(
                                                text = { Text(dayVal, fontSize = 13.sp, fontWeight = if (index == selectedDayIndex) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    selectedDayIndex = index
                                                    dayMenuExpanded = false
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.5.dp, Color(0xFF2E7D32), RoundedCornerShape(20.dp))
        )
    }
}

// Dialog Component for adding new people
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
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = accentColor)
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
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = accentColor)
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

@Composable
fun AddGroupDialog(
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("ایجاد گروه فامیلی جدید", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("شما می‌توانید چند خانواده یا طایفه متفاوت را در گروه‌های جداگانه دسته‌بندی کنید تا درخت شجره‌نامه آنها خلوت‌تر و منظم‌تر نمایش داده شود.", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "نام گروه (مثلا: خاندان علوی)"
                )
                
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "توضیحات کوتاه گروه",
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, description.ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("ایجاد گروه")
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

@Composable
fun EditGroupDialog(
    group: com.example.data.FamilyGroup,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (com.example.data.FamilyGroup) -> Unit,
    onDelete: (com.example.data.FamilyGroup) -> Unit,
    onBackupGroup: (com.example.data.FamilyGroup) -> Unit = {},
    onRestoreGroup: (com.example.data.FamilyGroup) -> Unit = {}
) {
    var name by remember { mutableStateOf(group.name) }
    var description by remember { mutableStateOf(group.description ?: "") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text("حذف گروه فامیلی", color = Color(0xFFC62828)) },
            text = { Text("آیا مطمئن هستید که می‌خواهید گروه '${group.name}' را حذف کنید؟ با این کار تمامی اعضای شجره‌نامه و ارتباطات ثبت شده در این گروه برای همیشه پاک خواهند شد.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(group)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                ) {
                    Text("بله، حذف شود", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("انصراف", color = textColor)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        title = { Text("ویرایش اطلاعات گروه", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "نام گروه"
                )
                
                AppTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "توضیحات گروه",
                    maxLines = 2
                )
                
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                Text(
                    text = "پشتیبان‌گیری اختصاصی گروه (بکاپ جزئی)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onBackupGroup(group) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تهیه بکاپ گروه", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { onRestoreGroup(group) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9), contentColor = Color(0xFF2E7D32)),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("بازگردانی بکاپ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFC62828))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف این گروه و اعضای آن", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(group.copy(name = name, description = description.ifBlank { null }))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("ذخیره تغییرات")
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

@Composable
fun EditPersonDialog(
    person: Person,
    groups: List<com.example.data.FamilyGroup>,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (Person) -> Unit
) {
    var firstName by remember { mutableStateOf(person.firstName) }
    var lastName by remember { mutableStateOf(person.lastName) }
    var gender by remember { mutableStateOf(person.gender) }
    var hasBirthDate by remember { mutableStateOf(!person.birthDate.isNullOrBlank()) }
    var birthDateInput by remember { mutableStateOf(person.birthDate ?: "") }
    var birthPlace by remember { mutableStateOf(person.birthPlace ?: "") }
    var hasDeathDate by remember { mutableStateOf(!person.deathDate.isNullOrBlank()) }
    var deathDateInput by remember { mutableStateOf(person.deathDate ?: "") }
    var deathPlace by remember { mutableStateOf(person.deathPlace ?: "") }
    var isDeceased by remember { mutableStateOf(person.isDeceased) }
    var occupation by remember { mutableStateOf(person.occupation ?: "") }
    var biography by remember { mutableStateOf(person.biography ?: "") }
    var selectedGroupIdForPerson by remember { mutableStateOf<Long?>(person.groupId) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("ویرایش اطلاعات ${person.fullName}", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AppTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "نام"
                    )
                }
                item {
                    AppTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "نام خانوادگی"
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
                
                // Group selector
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
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = accentColor)
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
            }
        },
        confirmButton = {
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    if (firstName.trim().isBlank() || lastName.trim().isBlank()) {
                        Toast.makeText(context, "تکمیل کادرهای نام و نام خانوادگی اجباری است", Toast.LENGTH_LONG).show()
                    } else {
                        onConfirm(
                            person.copy(
                                firstName = firstName.trim(),
                                lastName = lastName.trim(),
                                gender = gender,
                                birthDate = if (hasBirthDate) birthDateInput.ifBlank { null } else null,
                                birthPlace = birthPlace.ifBlank { null },
                                deathDate = if (isDeceased && hasDeathDate) deathDateInput.ifBlank { null } else null,
                                deathPlace = if (isDeceased) deathPlace.ifBlank { null } else null,
                                isDeceased = isDeceased,
                                occupation = occupation.ifBlank { null },
                                biography = biography.ifBlank { null },
                                groupId = selectedGroupIdForPerson
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("ذخیره تغییرات")
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

@Composable
fun AddSpouseDialog(
    spouseOf: Person,
    groups: List<com.example.data.FamilyGroup>,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, String?, String?, String?, Boolean, String?, String?, Long?, String) -> Unit
) {
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("ثبت و افزودن همسر برای ${spouseOf.fullName}", fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    AppTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = "نام"
                    )
                }
                item {
                    AppTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "نام خانوادگی"
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
                        label = "محل زندگی / تولد"
                    )
                }
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
                
                // Group selector
                item {
                    Text("گروه فامیلی همسر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
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
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = accentColor)
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
            }
        },
        confirmButton = {
            val context = androidx.compose.ui.platform.LocalContext.current
            Button(
                onClick = {
                    if (firstName.trim().isBlank() || lastName.trim().isBlank()) {
                        Toast.makeText(context, "تکمیل کادرهای نام و نام خانوادگی اجباری است", Toast.LENGTH_LONG).show()
                    } else {
                        val relType = if (isSecondSpouse) {
                            if (isDivorced) "SecondSpouse_Divorced" else "SecondSpouse"
                        } else {
                            if (isDivorced) "Divorced" else "Spouse"
                        }
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
                            relType
                        )
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
    viewModel: com.example.viewmodel.FamilyViewModel? = null
) {
    val dialogOrange = Color(0xFFF57C00)
    val dialogAccentOrange = Color(0xFFE65100)
    
    val directRelationships = remember(person, relationships) {
        relationships.filter { it.personId1 == person.id || it.personId2 == person.id }
    }

    val parentIds = remember(person, relationships) {
        relationships.filter { it.type == "Parent-Child" && it.personId2 == person.id }.map { it.personId1 }
    }

    val siblings = remember(person, relationships, allPersons, parentIds) {
        if (parentIds.isEmpty()) emptyList<Person>() else {
            val siblingIds = relationships.filter { 
                it.type == "Parent-Child" && 
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
            rel.type == "Parent-Child" && rel.personId2 == person.id
        }
    }

    val childrenList = remember(person, directRelationships) {
        directRelationships.filter { rel ->
            (rel.type == "Parent-Child" && rel.personId1 == person.id) || rel.type == "Adoptive-Parent-Child"
        }
    }

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
                                if (person.gender == "Male") Icons.Default.Boy else Icons.Default.Girl,
                                contentDescription = null,
                                tint = if (person.gender == "Male") Color(0xFF1976D2) else Color(0xFFC2185B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(person.fullName, fontWeight = FontWeight.Bold, color = textColor)
                }

                // 3-dot action menu for all operations
                var showActionMenu by remember { mutableStateOf(false) }
                val context = LocalContext.current
                Box {
                    IconButton(
                        onClick = { showActionMenu = true },
                        modifier = Modifier.testTag("member_action_menu")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "عملیات", tint = textColor)
                    }
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("ویرایش مشخصات عضو", color = textColor) },
                            onClick = {
                                showActionMenu = false
                                onEditPerson(person)
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = dialogAccentOrange) }
                        )
                        DropdownMenuItem(
                            text = { Text("افزودن همسر", color = textColor) },
                            onClick = {
                                showActionMenu = false
                                onAddSpouse(person)
                            },
                            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = dialogAccentOrange) }
                        )
                        DropdownMenuItem(
                            text = { Text("افزودن فرزند (زیرمجموعه)", color = textColor) },
                            onClick = {
                                showActionMenu = false
                                onAddChild(person)
                            },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = dialogAccentOrange) }
                        )
                        DropdownMenuItem(
                            text = { Text("کپی اطلاعات عضو", color = textColor) },
                            onClick = {
                                showActionMenu = false
                                val info = """
                                    نام: ${person.fullName}
                                    جنسیت: ${if (person.gender == "Male") "آقا" else "خانم"}
                                    تاریخ تولد: ${person.birthDate ?: "ثبت نشده"}
                                    محل زندگی: ${person.birthPlace ?: "ثبت نشده"}
                                    شغل: ${person.occupation ?: "ثبت نشده"}
                                    توضیحات: ${person.biography ?: "ثبت نشده"}
                                """.trimIndent()
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("مشخصات عضو", info)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "اطلاعات عضو در حافظه کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = dialogAccentOrange) }
                        )
                        DropdownMenuItem(
                            text = { Text("مسیر یابی از این شخص (مبداء)", color = textColor) },
                            onClick = {
                                showActionMenu = false
                                onHighlightFrom()
                            },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = dialogAccentOrange) }
                        )
                        DropdownMenuItem(
                            text = { Text("مسیر یابی به این شخص (مقصد)", color = textColor) },
                            onClick = {
                                showActionMenu = false
                                onHighlightTo()
                            },
                            leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = dialogAccentOrange) }
                        )
                        DropdownMenuItem(
                            text = { Text("تغییر یا انتقال ارتباط", color = textColor) },
                            onClick = {
                                showActionMenu = false
                                onMoveRelation(person)
                            },
                            leadingIcon = { Icon(Icons.Default.SyncAlt, contentDescription = null, tint = dialogAccentOrange) }
                        )
                        if (parentsList.size < 2) {
                            DropdownMenuItem(
                                text = { Text("افزودن پدر و مادر", color = textColor) },
                                onClick = {
                                    showActionMenu = false
                                    onAddParents(person)
                                },
                                leadingIcon = { Icon(Icons.Default.People, contentDescription = null, tint = dialogAccentOrange) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("تهیه پشتیبان عضو و زیرمجموعه‌ها", color = textColor) },
                            onClick = {
                                showActionMenu = false
                                onBackupSubtree(person)
                            },
                            leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null, tint = dialogAccentOrange) }
                        )
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = { Text("حذف این عضو فامیل", color = Color.Red) },
                            onClick = {
                                showActionMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                        )
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "مشخصات فردی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = dialogAccentOrange
                    )
                    Divider(color = dialogAccentOrange.copy(alpha = 0.4f))
                }

                item {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("جنسیت:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                        Text(if (person.gender == "Male") "مرد (آقا)" else "زن (خانم)", color = textColor, fontSize = 12.sp)
                    }
                }

                if (person.birthDate != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("تاریخ تولد:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                            Text(person.birthDate, color = textColor, fontSize = 12.sp)
                        }
                    }
                }

                if (person.birthPlace != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("محل زندگی / تولد:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                            Text(person.birthPlace, color = textColor, fontSize = 12.sp)
                        }
                    }
                }

                if (person.isDeceased) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.DarkGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("وضعیت: متوفی (مرحوم)", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (person.deathDate != null) {
                                    Text("تاریخ فوت: ${person.deathDate}", color = textColor, fontSize = 11.sp)
                                }
                                if (person.deathPlace != null) {
                                    Text("محل فوت: ${person.deathPlace}", color = textColor, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                if (person.occupation != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("شغل / پیشه:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                            Text(person.occupation, color = textColor, fontSize = 12.sp)
                        }
                    }
                }

                if (person.biography != null) {
                    item {
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
                }

                item {
                    Text(
                        "روابط ثبت شده",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = dialogAccentOrange,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Divider(color = dialogAccentOrange.copy(alpha = 0.4f))
                }

                if (spouseList.isNotEmpty()) {
                    item {
                        Text(
                            "همسر",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFC2185B),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Divider(color = Color(0xFFC2185B).copy(alpha = 0.3f))
                    }
                    items(spouseList) { rel ->
                        val relativeId = if (rel.personId1 == person.id) rel.personId2 else rel.personId1
                        val relative = allPersons.find { it.id == relativeId }
                        if (relative != null) {
                            val relTypeName = if (spouseList.size > 1) {
                                val isSecond = isSecondSpouseRelation(rel.type)
                                val labelText = if (isSecond) "همسر دوم" else "همسر اول"
                                val isEx = rel.type == "Divorced" || rel.type == "SecondSpouse_Divorced"
                                if (isEx) "$labelText (سابق)" else labelText
                            } else {
                                val isEx = rel.type == "Divorced" || rel.type == "SecondSpouse_Divorced"
                                if (isEx) "همسر سابق" else "همسر"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFCE4EC), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFFC2185B).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel?.setGlowPersonId(relativeId)
                                        onDismiss()
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(relative.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                                Text(relTypeName, fontSize = 11.sp, color = Color(0xFFC2185B), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (parentsList.isNotEmpty()) {
                    item {
                        Text(
                            "والدین (پدر و مادر)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF4A148C),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Divider(color = Color(0xFF4A148C).copy(alpha = 0.3f))
                    }
                    items(parentsList) { rel ->
                        val relativeId = if (rel.personId1 == person.id) rel.personId2 else rel.personId1
                        val relative = allPersons.find { it.id == relativeId }
                        if (relative != null) {
                            val relTypeName = if (relative.gender == "Male") "پدر" else "مادر"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF3E5F5), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF4A148C).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel?.setGlowPersonId(relativeId)
                                        onDismiss()
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(relative.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                                Text(relTypeName, fontSize = 11.sp, color = Color(0xFF4A148C), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (childrenList.isNotEmpty()) {
                    item {
                        Text(
                            "فرزندان",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0288D1),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Divider(color = Color(0xFF0288D1).copy(alpha = 0.3f))
                    }
                    items(childrenList) { rel ->
                        val relativeId = if (rel.personId1 == person.id) rel.personId2 else rel.personId1
                        val relative = allPersons.find { it.id == relativeId }
                        if (relative != null) {
                            val relTypeName = when (rel.type) {
                                "Parent-Child" -> "فرزند"
                                else -> "فرزندخوانده"
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE1F5FE), RoundedCornerShape(6.dp))
                                    .border(1.dp, Color(0xFF0288D1).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        viewModel?.setGlowPersonId(relativeId)
                                        onDismiss()
                                    }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(relative.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                                Text(relTypeName, fontSize = 11.sp, color = Color(0xFF0288D1), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (directRelationships.isEmpty()) {
                    item {
                        Text("رابطه‌ای برای این شخص ثبت نشده است.", fontSize = 11.sp, color = textColor.copy(alpha = 0.6f))
                    }
                }

                if (siblings.isNotEmpty()) {
                    item {
                        Text(
                            "خواهران و برادران",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = dialogAccentOrange,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Divider(color = dialogAccentOrange.copy(alpha = 0.4f))
                    }
                    items(siblings) { sib ->
                        val relTypeName = if (sib.gender == "Male") "برادر" else "خواهر"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF4CAF50).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel?.setGlowPersonId(sib.id)
                                    onDismiss()
                                }
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(sib.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                            Text(relTypeName, fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
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
@Composable
fun StatsDialog(
    stats: com.example.viewmodel.FamilyStats,
    allPersons: List<Person>,
    textColor: Color,
    accentColor: Color,
    cardBgColor: Color,
    onDismiss: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, accentColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("آمار و آنالیز جمعیتی فامیل", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "بستن", tint = textColor) }
                }

                Divider()

                // Numerical statistics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("کل جمعیت", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(stats.totalCount.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("در قید حیات", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(stats.livingCount.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("درگذشتگان", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(stats.deceasedCount.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                }

                // Gender Ratio chart
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("نسبت جنسیتی اعضا", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val malePercent = if (stats.totalCount > 0) (stats.malesCount.toFloat() / stats.totalCount) else 0.5f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8BBD0)) // Pink background
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(malePercent)
                                .background(Color(0xFFBBDEFB)) // Blue overlay
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("آقایان: ${stats.malesCount} نفر (${(malePercent * 100).toInt()}٪)", fontSize = 11.sp, color = Color(0xFF1565C0))
                        Text("بانوان: ${stats.femalesCount} نفر (${((1 - malePercent) * 100).toInt()}٪)", fontSize = 11.sp, color = Color(0xFFC2185B))
                    }
                }

                Divider()

                // Demographic summaries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("میانگین سن افراد زنده:", fontSize = 12.sp, color = textColor)
                        Text("${stats.avgLivingAge} سال", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("میانگین سن فوت شدگان:", fontSize = 12.sp, color = textColor)
                        Text("${stats.avgDeceasedAge} سال", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("پرتکرارترین نام پسر:", fontSize = 12.sp, color = textColor)
                        Text(stats.mostCommonBoyName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1E88E5))
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("پرتکرارترین نام دختر:", fontSize = 12.sp, color = textColor)
                        Text(stats.mostCommonGirlName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD81B60))
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("پرتکرارترین نام کوچک:", fontSize = 12.sp, color = textColor)
                        Text(stats.mostCommonFirstName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accentColor)
                    }
                }
            }
        }
    }
    }
}

// Reminders event notification list
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

    var calculatedRelation by remember { mutableStateOf<String?>(null) }

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
                                        calculatedRelation = null
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
                                        calculatedRelation = null
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
                    calculatedRelation = computed

                    Card(
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("نسبت فامیلی:", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(
                                computed,
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

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    placeholder: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    testTag: String? = null
) {
    val finalModifier = if (testTag != null) modifier.testTag(testTag) else modifier
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = Color(0xFF112E21).copy(alpha = 0.7f)) },
        placeholder = placeholder?.let { { Text(it, fontSize = 11.sp, color = Color(0xFF112E21).copy(alpha = 0.5f)) } },
        leadingIcon = leadingIcon,
        maxLines = maxLines,
        singleLine = maxLines == 1,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color(0xFF112E21),
            unfocusedTextColor = Color(0xFF112E21),
            focusedLabelColor = Color(0xFF4CAF50),
            unfocusedLabelColor = Color(0xFF112E21).copy(alpha = 0.6f),
            focusedBorderColor = Color(0xFF4CAF50),
            unfocusedBorderColor = Color(0xFFCBE3D8),
            focusedContainerColor = Color(0xFFF9FBF9),
            unfocusedContainerColor = Color(0xFFF9FBF9),
            cursorColor = Color(0xFF4CAF50)
        ),
        modifier = finalModifier.fillMaxWidth()
    )
}

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
        val currentYear = 1405 // solar hijri equivalent
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
                                imageVector = Icons.Default.Analytics,
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
                                imageVector = Icons.Default.Close,
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
                                    value = "$totalCount نفر",
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
                                    value = "$maleCount نفر",
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
                                    value = "$femaleCount نفر",
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
                                    value = "$totalGenerations نسل",
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
                                    value = "${brides.size} نفر",
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
                                    value = "${grooms.size} نفر",
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
                                    value = "${children.size} نفر",
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
                                    value = "${grandchildren.size} نفر",
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
                                    value = "${greatGrandchildren.size} نفر",
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
                                    value = "${greatGreatGrandchildren.size} نفر",
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
                                    value = "$livingCount نفر",
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
                                    value = "$deceasedCount نفر",
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
                                StatRowItem(title = "میانگین سن اعضای خانواده", value = "$averageAge سال", icon = "📅", accentColor = accentColor, textColor = textColor)
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

@Composable
fun StatRowItem(
    title: String,
    value: String,
    icon: String,
    accentColor: Color,
    textColor: Color,
    members: List<com.example.data.Person>? = null,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onPersonClick: ((com.example.data.Person) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onToggleExpand != null) {
                    Modifier.clickable { onToggleExpand() }
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, fontSize = 13.sp, color = textColor)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
                    if (onToggleExpand != null) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "جزییات",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            if (isExpanded && !members.isNullOrEmpty() && onPersonClick != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = textColor.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { person ->
                        val avatar = if (person.gender == "Male") "👨" else "👩"
                        val stateColor = if (person.isDeceased) Color.Gray else accentColor
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(stateColor.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .border(1.dp, stateColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(avatar, fontSize = 14.sp)
                                Text(
                                    text = person.fullName, 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = textColor
                                )
                                if (person.isDeceased) {
                                    Text(
                                        text = "(مرحوم)", 
                                        fontSize = 10.sp, 
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = { onPersonClick(person) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "مشاهده جزئیات",
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun String.toFarsiNumbers(): String {
    return this.map { char ->
        if (char in '0'..'9') {
            (char.code + 1584).toChar() // '0' is 48, '۰' is 1632, so 1632 - 48 = 1584
        } else {
            char
        }
    }.joinToString("")
}

fun Int.toFarsiNumbers(): String {
    return this.toString().toFarsiNumbers()
}

fun Long.toFarsiNumbers(): String {
    return this.toString().toFarsiNumbers()
}

