package com.example.ui.screens

import com.example.ui.common.*
import com.example.ui.dialogs.*
import com.example.ui.tree.*

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.PI

import android.widget.Toast
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.draw.drawWithContent
import com.example.utils.TreePdfExporter
import com.example.utils.AppLogger
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.utils.detectTransformGestures
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
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.compose.AsyncImagePainter
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
import androidx.compose.ui.unit.Density
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: FamilyViewModel) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val persons by viewModel.filteredPersons.collectAsStateWithLifecycle()
    val allPersonsRaw by viewModel.allPersons.collectAsStateWithLifecycle()
    val relationships by viewModel.allRelationships.collectAsStateWithLifecycle()
    
    val currentLayout by viewModel.treeLayout.collectAsStateWithLifecycle()
    val currentTheme = "Bento Grid"
    val focusPersonId by viewModel.focusPersonId.collectAsStateWithLifecycle()
    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    
    var expandedGhostParents by remember { mutableStateOf(setOf<Long>()) }

    val highlightP1Id by viewModel.highlightPerson1Id.collectAsStateWithLifecycle()
    val highlightP2Id by viewModel.highlightPerson2Id.collectAsStateWithLifecycle()
    val glowPersonId by viewModel.glowPersonId.collectAsStateWithLifecycle()

    // Group & Spouse states
    val allGroups by viewModel.allGroups.collectAsStateWithLifecycle()
    val selectedGroupId by viewModel.selectedGroupId.collectAsStateWithLifecycle()
    var isExportingPdf by remember { mutableStateOf(false) }

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
    var showLogsDialog by remember { mutableStateOf(false) }
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

    val isDatabaseReady by viewModel.isDatabaseReady.collectAsStateWithLifecycle()
    val databaseError by viewModel.databaseError.collectAsStateWithLifecycle()

    if (databaseError != null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { viewModel.clearDatabaseError() },
                title = { Text("خطای پایگاه داده", fontWeight = FontWeight.Bold, color = Color.Red) },
                text = { Text("""متاسفانه در بارگذاری اطلاعات مشکلی رخ داد:

$databaseError

لطفا برنامه را دوباره اجرا کنید.""", color = Color.Black) },
                confirmButton = {
                    Button(onClick = { 
                        viewModel.clearDatabaseError()
                        viewModel.retryDatabaseInit() 
                    }) {
                        Text("تلاش مجدد")
                    }
                }
            )
        }
    }

    if (!isDatabaseReady) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFF57C00))
        }
        return
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
                                text = { Text("خروجی PDF", color = textColor) },
                                onClick = { showSettingsMenu = false; isExportingPdf = true },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = accentColor) }
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
                            if (viewModel.hasDatabaseBackup()) {
                                DropdownMenuItem(
                                    text = { Text("بازگردانی پایگاه داده آسیب‌دیده", color = Color.Red) },
                                    onClick = {
                                        showSettingsMenu = false
                                        viewModel.restoreDatabaseBackup { success ->
                                            if (success) {
                                                Toast.makeText(context, "پایگاه داده با موفقیت بازگردانی شد.", Toast.LENGTH_LONG).show()
                                            } else {
                                                Toast.makeText(context, "بازگردانی با شکست مواجه شد.", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red) }
                                )
                            }
                            
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
                                                         val prefix = if (focusPersonId != null) "شخص برجسته" else "سرشاخه خاندان"
                                                         val dateStr = formatLifeDates(featuredPerson.birthDate, featuredPerson.deathDate, featuredPerson.isDeceased, getCurrentJalaliYear())
                                                         "$prefix - $dateStr".toFarsiNumbers()
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
                                                    Toast.makeText(context, "برای فعال‌سازی نمای متمرکز، روی کارت شخص مورد نظر دوبار ضربه بزنید (یا آیکون چشم را لمس کنید)", Toast.LENGTH_LONG).show()
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
                                            onPanToPerson = { person ->
                                                viewModel.setGlowPersonId(person.id)
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
                                onPanToPerson = { person ->
                                    viewModel.setGlowPersonId(person.id)
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
                    onPanToPerson = { person ->
                        viewModel.setGlowPersonId(person.id)
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
                
                // Exit Focus Mode Button (Floating at bottom-start)
                if (focusPersonId != null) {
                    FloatingActionButton(
                        onClick = { viewModel.setFocusPersonId(null) },
                        containerColor = Color(0xFFFF9800),
                        contentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VisibilityOff,
                                contentDescription = "خروج از نمای متمرکز"
                            )
                            Text(
                                text = "خروج از نمای متمرکز",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
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
                        var isSavingPhoto by remember { mutableStateOf(false) }
                        val cropScope = rememberCoroutineScope()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                enabled = !isSavingPhoto,
                                onClick = {
                                    isSavingPhoto = true
                                    cropScope.launch {
                                        val croppedPath = cropAndSaveBitmap(
                                            context = context,
                                            originalBitmap = originalBitmap,
                                            scale = scale,
                                            offsetX = offsetX,
                                            offsetY = offsetY,
                                            boxSizePx = boxSizePx,
                                            cropSizePx = cropSizePx
                                        )
                                        isSavingPhoto = false
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
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                            ) {
                                if (isSavingPhoto) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                                } else {
                                    Text("تایید و ذخیره", color = Color.White)
                                }
                            }

                            OutlinedButton(
                                enabled = !isSavingPhoto,
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
        var showPhotoDeleteConfirm by remember { mutableStateOf(false) }
        
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
                                onClick = { showPhotoDeleteConfirm = true },
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
            
            if (showPhotoDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showPhotoDeleteConfirm = false },
                    title = { Text("حذف عکس", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
                    text = { Text("آیا مطمئن هستید که می‌خواهید این عکس را حذف کنید؟", fontSize = 14.sp) },
                    confirmButton = {
                        Button(
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
                                showPhotoDeleteConfirm = false
                                if (currentUris.isEmpty() || currentUris.size == 1) { // If it was 1, now it's 0.
                                    showFullPhotoDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("حذف")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPhotoDeleteConfirm = false }) {
                            Text("انصراف", color = textColor)
                        }
                    }
                )
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
            allPersons = persons,
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
            },
            onConfirmExisting = { existingSpouseId, relationshipType ->
                viewModel.linkExistingSpouse(spouseOf.id, existingSpouseId, relationshipType)
                Toast.makeText(context, "ازدواج فامیلی با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
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
        var backupJson by remember(tempExportGroupId) { mutableStateOf<String?>(null) }
        LaunchedEffect(tempExportGroupId) {
            backupJson = viewModel.exportBackupToJson(tempExportGroupId)
        }
        var fileName by remember(backupFileNameInput) { mutableStateOf(backupFileNameInput) }
        
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text(if (tempExportGroupId == null) "تهیه فایل پشتیبان کلی" else "تهیه فایل پشتیبان گروه", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                if (backupJson == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else {
                    val currentJson = backupJson!!
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
                                    backupJsonToSave = currentJson
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
                                        text = currentJson,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = backupJson != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    onClick = {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("FamilyTreeBackup", backupJson ?: "")
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
        var backupJson by remember(rootPerson) { mutableStateOf<String?>(null) }
        LaunchedEffect(rootPerson) {
            backupJson = viewModel.exportSubtreeBackupToJson(rootPerson.id)
        }
        var fileName by remember(rootPerson) { mutableStateOf("backup_${rootPerson.firstName}_${rootPerson.lastName}") }
        
        AlertDialog(
            onDismissRequest = { showSubtreeBackupPerson = null },
            title = { Text("تهیه فایل پشتیبان عضو و زیرمجموعه‌ها", fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                if (backupJson == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentColor)
                    }
                } else {
                    val currentJson = backupJson!!
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
                                    backupJsonToSave = currentJson
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
                                        text = currentJson,
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = backupJson != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    onClick = {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("FamilyTreeSubtreeBackup", backupJson ?: "")
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

    if (isExportingPdf) {
        val graphicsLayer = rememberGraphicsLayer()

        val groupName = remember(allGroups, selectedGroupId) {
            allGroups.find { it.id == selectedGroupId }?.name ?: "خاندان عمومی"
        }

        val exportPersons = remember(allPersonsRaw, selectedGroupId) {
            if (selectedGroupId != null) allPersonsRaw.filter { it.groupId == selectedGroupId } else allPersonsRaw
        }

        val pdfPositions = remember(exportPersons, relationships, currentLayout, focusPersonId, expandedGhostParents) {
            computeTreeLayoutPositions(exportPersons, relationships, currentLayout, focusPersonId, expandedGhostParents)
        }

        val exportDensity = remember(pdfPositions, density) {
            if (pdfPositions.isEmpty()) density
            else {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE

                for (pos in pdfPositions.values) {
                    if (pos.x < minX) minX = pos.x
                    if (pos.y < minY) minY = pos.y
                    if (pos.x > maxX) maxX = pos.x
                    if (pos.y > maxY) maxY = pos.y
                }

                val maxAbsX = maxOf(kotlin.math.abs(minX), kotlin.math.abs(maxX))
                val maxAbsY = maxOf(kotlin.math.abs(minY), kotlin.math.abs(maxY))
                val widthDp = 2f * maxAbsX + 340f
                val heightDp = 2f * maxAbsY + 380f
                val maxSpanDp = maxOf(widthDp, heightDp)

                val targetMaxPx = 3600f
                val calculatedDensity = targetMaxPx / maxSpanDp
                minOf(density, calculatedDensity).coerceAtLeast(0.8f)
            }
        }

        val (pdfContentWidthDp, pdfContentHeightDp) = remember(pdfPositions) {
            if (pdfPositions.isEmpty()) {
                300.dp to 300.dp
            } else {
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE

                for (pos in pdfPositions.values) {
                    if (pos.x < minX) minX = pos.x
                    if (pos.y < minY) minY = pos.y
                    if (pos.x > maxX) maxX = pos.x
                    if (pos.y > maxY) maxY = pos.y
                }

                val maxAbsX = maxOf(kotlin.math.abs(minX), kotlin.math.abs(maxX))
                val maxAbsY = maxOf(kotlin.math.abs(minY), kotlin.math.abs(maxY))

                val widthDp = (2f * maxAbsX + 340f).dp
                val heightDp = (2f * maxAbsY + 380f).dp
                widthDp to heightDp
            }
        }

        AlertDialog(
            onDismissRequest = { /* Non-dismissable while exporting */ },
            confirmButton = {},
            title = { Text("در حال آماده‌سازی فایل PDF...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = accentColor) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    CircularProgressIndicator(color = accentColor)
                    Text("لطفاً شکیبا باشید. شجره‌نامه $groupName با کیفیت بالا در حال رندر است...", fontSize = 13.sp, color = textColor)
                }
            },
            containerColor = Color.White
        )

        Box(
            modifier = Modifier.size(1.dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(pdfContentWidthDp, pdfContentHeightDp)
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
            ) {
                CompositionLocalProvider(
                    LocalDensity provides Density(density = exportDensity, fontScale = 1.0f)
                ) {
                    FamilyTreeContent(
                        persons = exportPersons,
                        relationships = relationships,
                        positions = pdfPositions,
                        layoutType = currentLayout,
                        accentColor = accentColor,
                        cardBgColor = cardColor,
                        textColor = textColor,
                        density = exportDensity,
                        glowPersonId = null,
                        highlightedPathIds = emptySet(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        LaunchedEffect(pdfPositions) {
            try {
                AppLogger.i("PDF_EXPORT", "شروع فرایند تولید PDF برای خاندان: $groupName")
                withFrameNanos { }
                withFrameNanos { }

                var imageBitmap = graphicsLayer.toImageBitmap()
                var bitmap = imageBitmap.asAndroidBitmap()

                if (isBitmapVisuallyBlank(bitmap)) {
                    AppLogger.i("PDF_EXPORT", "تصویر رندر شده در نوبت اول خالی به نظر می‌رسد، انتظار ۲ فریم دیگر...")
                    withFrameNanos { }
                    withFrameNanos { }
                    imageBitmap = graphicsLayer.toImageBitmap()
                    bitmap = imageBitmap.asAndroidBitmap()
                }

                AppLogger.i("PDF_EXPORT", "تصویر شجره‌نامه رندر شد. ابعاد: ${bitmap.width}x${bitmap.height}, کانفیگ: ${bitmap.config}")

                val file = TreePdfExporter.saveBitmapToPdf(context, bitmap, groupName)
                isExportingPdf = false
                TreePdfExporter.shareTreePdf(context, file)
            } catch (e: Throwable) {
                AppLogger.e("PDF_EXPORT", "خطا در فرایند تولید یا اشتراک‌گذاری PDF", e)
                e.printStackTrace()
                isExportingPdf = false
                val msg = e.localizedMessage ?: "خطا در تولید فایل PDF"
                Toast.makeText(context, "خطا در تولید PDF: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showLogsDialog) {
        AppLogsDialog(
            accentColor = accentColor,
            textColor = textColor,
            onDismiss = { showLogsDialog = false }
        )
    }
}
}

