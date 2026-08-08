package com.example.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FamilyFolder
import com.example.data.FamilyGroup
import com.example.ui.common.toFarsiNumbers
import com.example.viewmodel.FamilyViewModel

@Composable
fun FolderExplorerDialog(
    viewModel: FamilyViewModel,
    allFolders: List<FamilyFolder>,
    allGroups: List<FamilyGroup>,
    currentFolderId: Long?,
    selectedGroupId: Long?,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelectGroup: (Long) -> Unit,
    onAddFolder: (parentId: Long?) -> Unit,
    onAddGroup: (folderId: Long?) -> Unit,
    onEditFolder: (FamilyFolder) -> Unit,
    onEditGroup: (FamilyGroup) -> Unit,
    onDeleteFolder: (FamilyFolder) -> Unit,
    onDeleteGroup: (FamilyGroup) -> Unit,
    onMoveCopyItem: (item: Any, isFolder: Boolean, isCopy: Boolean) -> Unit
) {
    var activeFolderId by remember { mutableStateOf(currentFolderId) }
    val breadcrumbs = remember(allFolders, activeFolderId) {
        viewModel.getFolderBreadcrumbs(activeFolderId)
    }

    val currentSubfolders = remember(allFolders, activeFolderId) {
        allFolders.filter { it.parentId == activeFolderId }
    }

    val currentGroups = remember(allGroups, activeFolderId) {
        allGroups.filter { it.folderId == activeFolderId }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.85f)
                    .border(2.dp, accentColor, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color(0xFFFFF3E0),
                                shape = CircleShape,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = TablerIcons.Folder,
                                        contentDescription = null,
                                        tint = Color(0xFFF57C00),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "مدیریت پوشه‌ها و گروه‌های فامیلی",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = textColor
                                )
                                Text(
                                    "دسته‌بندی تو در تو مناطق، روستاها و خاندان‌ها",
                                    fontSize = 11.sp,
                                    color = textColor.copy(alpha = 0.6f)
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(TablerIcons.X, contentDescription = "بستن", tint = textColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Breadcrumb Bar
                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "مسیر فعلی: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.7f)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    onClick = {
                                        activeFolderId = null
                                        viewModel.setCurrentFolderId(null)
                                    },
                                    color = if (activeFolderId == null) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "🏠 ریشه",
                                        fontSize = 11.sp,
                                        fontWeight = if (activeFolderId == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (activeFolderId == null) accentColor else textColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    )
                                }

                                breadcrumbs.forEach { folder ->
                                    Text(" > ", fontSize = 11.sp, color = Color.Gray)
                                    Surface(
                                        onClick = {
                                            activeFolderId = folder.id
                                            viewModel.setCurrentFolderId(folder.id)
                                        },
                                        color = if (activeFolderId == folder.id) accentColor.copy(alpha = 0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "📁 ${folder.name}",
                                            fontSize = 11.sp,
                                            fontWeight = if (activeFolderId == folder.id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (activeFolderId == folder.id) accentColor else textColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons bar for current directory
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAddFolder(activeFolderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(TablerIcons.FolderPlus, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ایجاد پوشه جدید", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { onAddGroup(activeFolderId) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(TablerIcons.UserPlus, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ایجاد گروه فامیلی", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main Explorer List
                    Surface(
                        color = Color(0xFFFAFAFA),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (currentSubfolders.isEmpty() && currentGroups.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = TablerIcons.Folder,
                                        contentDescription = null,
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        "این پوشه خالی است.",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "میتوانید پوشه جدید یا گروه فامیلی درون آن اضافه کنید.",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Subfolders Section
                                if (currentSubfolders.isNotEmpty()) {
                                    item {
                                        Text(
                                            "پوشه‌ها (${currentSubfolders.size.toString().toFarsiNumbers()}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    items(currentSubfolders) { folder ->
                                        var menuExpanded by remember { mutableStateOf(false) }
                                        val (subCount, groupCount) = viewModel.getFolderContentCounts(folder.id)

                                        Surface(
                                            onClick = {
                                                activeFolderId = folder.id
                                                viewModel.setCurrentFolderId(folder.id)
                                            },
                                            color = Color.White,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = TablerIcons.Folder,
                                                        contentDescription = null,
                                                        tint = Color(0xFFF57C00),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            folder.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textColor
                                                        )
                                                        Text(
                                                            "شامل ${subCount.toString().toFarsiNumbers()} زیرپوشه و ${groupCount.toString().toFarsiNumbers()} گروه",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }

                                                Box {
                                                    IconButton(onClick = { menuExpanded = true }) {
                                                        Icon(
                                                            TablerIcons.DotsVertical,
                                                            contentDescription = "منو",
                                                            tint = textColor
                                                        )
                                                    }

                                                    DropdownMenu(
                                                        expanded = menuExpanded,
                                                        onDismissRequest = { menuExpanded = false },
                                                        shape = RoundedCornerShape(18.dp),
                                                        containerColor = Color.White,
                                                        tonalElevation = 8.dp,
                                                        shadowElevation = 10.dp,
                                                        border = BorderStroke(1.5.dp, Color(0xFFFFB74D))
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("ورود به پوشه") },
                                                            leadingIcon = { Icon(TablerIcons.Folder, contentDescription = null) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                activeFolderId = folder.id
                                                                viewModel.setCurrentFolderId(folder.id)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("تغییر نام / ویرایش") },
                                                            leadingIcon = { Icon(TablerIcons.Pencil, contentDescription = null) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                onEditFolder(folder)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("انتقال پوشه") },
                                                            leadingIcon = { Icon(TablerIcons.FolderMinus, contentDescription = null) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                onMoveCopyItem(folder, true, false)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("کپی پوشه") },
                                                            leadingIcon = { Icon(TablerIcons.Copy, contentDescription = null) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                onMoveCopyItem(folder, true, true)
                                                            }
                                                        )
                                                        HorizontalDivider()
                                                        DropdownMenuItem(
                                                            text = { Text("حذف پوشه", color = Color.Red) },
                                                            leadingIcon = { Icon(TablerIcons.Trash, contentDescription = null, tint = Color.Red) },
                                                            onClick = {
                                                                menuExpanded = false
                                                                onDeleteFolder(folder)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Groups Section
                                if (currentGroups.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            "گروه‌های فامیلی (${currentGroups.size.toString().toFarsiNumbers()}):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    items(currentGroups) { group ->
                                        var menuExpanded by remember { mutableStateOf(false) }
                                        val isSelected = selectedGroupId == group.id

                                        Surface(
                                            onClick = {
                                                onSelectGroup(group.id)
                                            },
                                            color = if (isSelected) accentColor.copy(alpha = 0.1f) else Color.White,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) accentColor else Color(0xFFC8E6C9)
                                            ),
                                            shadowElevation = 1.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(
                                                        imageVector = TablerIcons.Users,
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            group.name,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = textColor
                                                        )
                                                        if (!group.description.isNullOrBlank()) {
                                                            Text(
                                                                group.description,
                                                                fontSize = 10.sp,
                                                                color = Color.Gray,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (isSelected) {
                                                        Surface(
                                                            color = accentColor,
                                                            shape = RoundedCornerShape(6.dp)
                                                        ) {
                                                            Text(
                                                                "انتخاب شده",
                                                                fontSize = 10.sp,
                                                                color = Color.White,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }

                                                    Box {
                                                        IconButton(onClick = { menuExpanded = true }) {
                                                            Icon(
                                                                TablerIcons.DotsVertical,
                                                                contentDescription = "منو",
                                                                tint = textColor
                                                            )
                                                        }

                                                        DropdownMenu(
                                                            expanded = menuExpanded,
                                                            onDismissRequest = { menuExpanded = false },
                                                            shape = RoundedCornerShape(18.dp),
                                                            containerColor = Color.White,
                                                            tonalElevation = 8.dp,
                                                            shadowElevation = 10.dp,
                                                            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = { Text("نمایش شجره‌نامه این گروه") },
                                                                leadingIcon = { Icon(TablerIcons.Sitemap, contentDescription = null) },
                                                                onClick = {
                                                                    menuExpanded = false
                                                                    onSelectGroup(group.id)
                                                                    onDismiss()
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("ویرایش مشخصات") },
                                                                leadingIcon = { Icon(TablerIcons.Pencil, contentDescription = null) },
                                                                onClick = {
                                                                    menuExpanded = false
                                                                    onEditGroup(group)
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("انتقال به پوشه دیگر") },
                                                                leadingIcon = { Icon(TablerIcons.FolderMinus, contentDescription = null) },
                                                                onClick = {
                                                                    menuExpanded = false
                                                                    onMoveCopyItem(group, false, false)
                                                                }
                                                            )
                                                            DropdownMenuItem(
                                                                text = { Text("کپی گروه") },
                                                                leadingIcon = { Icon(TablerIcons.Copy, contentDescription = null) },
                                                                onClick = {
                                                                    menuExpanded = false
                                                                    onMoveCopyItem(group, false, true)
                                                                }
                                                            )
                                                            HorizontalDivider()
                                                            DropdownMenuItem(
                                                                text = { Text("حذف گروه", color = Color.Red) },
                                                                leadingIcon = { Icon(TablerIcons.Trash, contentDescription = null, tint = Color.Red) },
                                                                onClick = {
                                                                    menuExpanded = false
                                                                    onDeleteGroup(group)
                                                                }
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
        }
    }
}
