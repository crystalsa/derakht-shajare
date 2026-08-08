package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.FamilyFolder
import com.example.data.FamilyGroup
import com.example.ui.common.AppTextField
import com.example.ui.common.toFarsiNumbers

@Composable
fun AddFolderDialog(
    textColor: Color,
    accentColor: Color,
    currentFolderName: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, Color(0xFFF57C00), RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(TablerIcons.FolderPlus, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ایجاد پوشه جدید", fontWeight = FontWeight.Bold, color = textColor)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (currentFolderName != null) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(TablerIcons.Folder, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("مکان ایجاد: پوشه «$currentFolderName»", fontSize = 11.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Text("با ایجاد پوشه می‌توانید گروه منطقه‌ای، روستایی یا منطقه‌های مختلف را به صورت تو در تو دسته‌بندی کنید.", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                    }

                    AppTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "نام پوشه (مثلا: روستای علی‌آباد / محله بالا)"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57C00))
                ) {
                    Text("ایجاد پوشه", fontWeight = FontWeight.Bold, color = Color.White)
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
fun EditFolderDialog(
    folder: FamilyFolder,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(folder.name) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(TablerIcons.Folder, contentDescription = null, tint = Color(0xFFF57C00), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ویرایش مشخصات پوشه", fontWeight = FontWeight.Bold, color = textColor)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "نام پوشه"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("ذخیره تغییرات", color = Color.White)
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
fun DeleteFolderWarningDialog(
    folderName: String,
    subfolderCount: Int,
    groupCount: Int,
    textColor: Color,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.border(2.dp, Color(0xFFD32F2F), RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(TablerIcons.AlertTriangle, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("هشدار حذف پوشه غیرخالی", fontWeight = FontWeight.Bold, color = Color(0xFFD32F2F), fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("پوشه «$folderName» خالی نیست!", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                    
                    Surface(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("محتویات درون این پوشه:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFC62828))
                            if (subfolderCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(TablerIcons.Folder, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تعداد زیرپوشه‌ها: ${subfolderCount.toString().toFarsiNumbers()}", fontSize = 11.sp, color = Color(0xFFC62828))
                                }
                            }
                            if (groupCount > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(TablerIcons.Users, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تعداد گروه‌های فامیلی: ${groupCount.toString().toFarsiNumbers()}", fontSize = 11.sp, color = Color(0xFFC62828))
                                }
                            }
                        }
                    }

                    Text(
                        "توجّه: با حذف این پوشه، تمامی زیرپوشه‌ها، گروه‌های فامیلی و اعضای شجره‌نامه داخل آن به‌صورت کامل و جبران‌ناپذیر حذف خواهند شد.",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("حذف کامل پوشه و محتویات", color = Color.White, fontWeight = FontWeight.Bold)
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
fun MoveCopyDestinationDialog(
    itemName: String,
    isFolder: Boolean,
    isCopy: Boolean, // true = Copy, false = Move
    allFolders: List<FamilyFolder>,
    sourceFolderId: Long?, // To prevent selecting self or descendants when moving folder
    itemId: Long,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelectDestination: (targetFolderId: Long?) -> Unit
) {
    var selectedTargetFolderId by remember { mutableStateOf<Long?>(null) }
    var selectedFolderName by remember { mutableStateOf("صفحه اصلی (پوشه ریشه)") }

    val invalidFolderIds = remember(allFolders, sourceFolderId, isFolder, itemId) {
        if (!isFolder) emptySet()
        else {
            val set = mutableSetOf<Long>()
            set.add(itemId)
            fun addDescendants(parentFId: Long) {
                allFolders.filter { it.parentId == parentFId }.forEach { child ->
                    set.add(child.id)
                    addDescendants(child.id)
                }
            }
            addDescendants(itemId)
            set
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.border(2.dp, accentColor, RoundedCornerShape(24.dp)),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isCopy) TablerIcons.Copy else TablerIcons.FolderMinus,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isCopy) "کپی کردن ${if (isFolder) "پوشه" else "گروه"}" else "انتقال ${if (isFolder) "پوشه" else "گروه"}",
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "مقصد ${if (isCopy) "کپی" else "انتقال"} برای «$itemName» را انتخاب کنید:",
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.8f)
                    )

                    Surface(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        LazyColumn(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item {
                                val isRootSelected = selectedTargetFolderId == null
                                Surface(
                                    onClick = {
                                        selectedTargetFolderId = null
                                        selectedFolderName = "صفحه اصلی (پوشه ریشه)"
                                    },
                                    color = if (isRootSelected) accentColor.copy(alpha = 0.15f) else Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isRootSelected) accentColor else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(TablerIcons.Home, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("صفحه اصلی (پوشه ریشه)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                    }
                                }
                            }

                            items(allFolders) { folder ->
                                val isInvalid = invalidFolderIds.contains(folder.id)
                                val isSelected = selectedTargetFolderId == folder.id

                                Surface(
                                    onClick = {
                                        if (!isInvalid) {
                                            selectedTargetFolderId = folder.id
                                            selectedFolderName = folder.name
                                        }
                                    },
                                    color = when {
                                        isInvalid -> Color(0xFFEEEEEE)
                                        isSelected -> accentColor.copy(alpha = 0.15f)
                                        else -> Color.White
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, if (isSelected) accentColor else Color.Transparent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            TablerIcons.Folder,
                                            contentDescription = null,
                                            tint = if (isInvalid) Color.Gray else Color(0xFFF57C00),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                folder.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isInvalid) Color.Gray else textColor
                                            )
                                            if (isInvalid) {
                                                Text("غیرقابل انتخاب (پوشه خود یا زیرپوشه)", fontSize = 9.sp, color = Color.Red)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text("مقصد انتخاب شده: $selectedFolderName", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = accentColor)
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSelectDestination(selectedTargetFolderId) },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(if (isCopy) "کپی در این مقصد" else "انتقال به این مقصد", color = Color.White)
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
