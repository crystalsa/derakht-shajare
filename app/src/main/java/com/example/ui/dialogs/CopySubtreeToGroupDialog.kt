package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import com.example.data.Person
import com.example.ui.common.photoUris
import com.example.ui.common.toFarsiNumbers
import com.example.viewmodel.FamilyViewModel
import java.io.File

@Composable
fun CopySubtreeToGroupDialog(
    person: Person,
    viewModel: FamilyViewModel,
    textColor: Color,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSuccess: (newGroupId: Long, groupName: String, count: Int) -> Unit
) {
    val context = LocalContext.current
    val allFolders by viewModel.allFolders.collectAsState()
    val currentFolderId by viewModel.currentFolderId.collectAsState()

    var groupName by remember(person) { 
        mutableStateOf("خاندان ${person.fullName}".trim()) 
    }
    var selectedFolderId by remember { 
        mutableStateOf(currentFolderId) 
    }
    var switchToNewGroup by remember { 
        mutableStateOf(true) 
    }
    var isLoading by remember { 
        mutableStateOf(false) 
    }
    var showFolderDropdown by remember { 
        mutableStateOf(false) 
    }

    // Subtree count
    val subtreeInfo = remember(person) {
        viewModel.getSubtreePersonsAndRelationships(person.id)
    }
    val subtreePersons = subtreeInfo.first
    val subtreeRels = subtreeInfo.second

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) onDismiss() },
            modifier = Modifier
                .border(2.dp, accentColor, RoundedCornerShape(24.dp))
                .testTag("copy_subtree_dialog"),
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = TablerIcons.GitFork,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "کپی شخص و زیرمجموعه در گروه جدید",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Person preview & subtree statistics
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(if (person.gender == "Male") Color(0xFFE3F2FD) else Color(0xFFFCE4EC))
                                    .border(1.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (person.photoUris.isNotEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = person.photoUris.firstOrNull()?.let { File(it) }),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = TablerIcons.User,
                                        contentDescription = null,
                                        tint = if (person.gender == "Male") Color(0xFF1E88E5) else Color(0xFFD81B60),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = person.fullName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "اعضای منتقل‌شونده: ${subtreePersons.size.toString().toFarsiNumbers()} نفر",
                                        fontSize = 11.sp,
                                        color = accentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "پیوندها: ${subtreeRels.size.toString().toFarsiNumbers()}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "با انجام این کار، «${person.fullName}» به عنوان سرسلسله و ریشه گروه جدید ثبت شده و تمامی همسران، فرزندان، نوه‌ها و نسل‌های بعدی ایشان به گروه جدید منتقل و شجره‌نامه‌ای مستقل تشکیل خواهند داد.",
                        fontSize = 11.sp,
                        lineHeight = 18.sp,
                        color = textColor.copy(alpha = 0.75f)
                    )

                    // Group Name Input
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("نام گروه فامیلی جدید") },
                        placeholder = { Text("مثال: خاندان ${person.fullName}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_group_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            focusedLabelColor = accentColor
                        )
                    )

                    // Folder Selector (if folders exist)
                    if (allFolders.isNotEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { showFolderDropdown = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = Color(0xFFFFA000),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        val folderName = allFolders.find { it.id == selectedFolderId }?.name ?: "ریشه اصلی (بدون پوشه)"
                                        Text(
                                            text = "پوشه مقصد: $folderName",
                                            fontSize = 12.sp,
                                            color = textColor
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.Gray
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showFolderDropdown,
                                onDismissRequest = { showFolderDropdown = false },
                                modifier = Modifier
                                    .background(Color.White)
                                    .fillMaxWidth(0.75f)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("ریشه اصلی (بدون پوشه)") },
                                    leadingIcon = { Icon(TablerIcons.Home, contentDescription = null, tint = accentColor) },
                                    onClick = {
                                        selectedFolderId = null
                                        showFolderDropdown = false
                                    }
                                )
                                HorizontalDivider()
                                allFolders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name) },
                                        leadingIcon = { Icon(TablerIcons.Folder, contentDescription = null, tint = Color(0xFFFFA000)) },
                                        onClick = {
                                            selectedFolderId = folder.id
                                            showFolderDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Switch to new group checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { switchToNewGroup = !switchToNewGroup }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = switchToNewGroup,
                            onCheckedChange = { switchToNewGroup = it },
                            colors = CheckboxDefaults.colors(checkedColor = accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "انتقال فوری به این گروه پس از ایجاد",
                            fontSize = 12.sp,
                            color = textColor
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalName = groupName.trim()
                        if (finalName.isBlank()) {
                            Toast.makeText(context, "لطفاً یک نام برای گروه جدید وارد کنید", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        viewModel.copyPersonSubtreeToNewGroup(
                            rootPersonId = person.id,
                            newGroupName = finalName,
                            targetFolderId = selectedFolderId
                        ) { newGroupId, count ->
                            isLoading = false
                            if (switchToNewGroup) {
                                viewModel.setSelectedGroupId(newGroupId)
                            }
                            Toast.makeText(
                                context,
                                "گروه فامیلی «$finalName» با $count عضو با موفقیت ایجاد شد.",
                                Toast.LENGTH_LONG
                            ).show()
                            onSuccess(newGroupId, finalName, count)
                        }
                    },
                    enabled = !isLoading && groupName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_copy_subtree_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(TablerIcons.Copy, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ایجاد گروه و کپی اعضا", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text("انصراف", color = Color.Gray)
                }
            }
        )
    }
}
