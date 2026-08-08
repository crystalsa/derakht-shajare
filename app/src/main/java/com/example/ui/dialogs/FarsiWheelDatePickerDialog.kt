package com.example.ui.dialogs

import com.example.ui.common.toFarsiNumbers
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

        LazyColumn(
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
                            text = labelProvider(item).toFarsiNumbers(),
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
                        val dayDisplay = if (selectedDayIndex == 0) "نامشخص" else selectedDayIndex.toFarsiNumbers()
                        Text(dayDisplay, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112E21))
                        Icon(TablerIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
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
                    val listState = rememberLazyListState()
                    LaunchedEffect(dayMenuExpanded) {
                        if (dayMenuExpanded) {
                            listState.scrollToItem(selectedDayIndex)
                        }
                    }
                    Box(modifier = Modifier.size(width = 110.dp, height = 240.dp)) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(maxDays + 1) { index ->
                                val dayVal = if (index == 0) "نامشخص" else index.toFarsiNumbers()
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
                        Icon(TablerIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
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
                        Text(selectedYear.toFarsiNumbers(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF112E21))
                        Icon(TablerIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
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
                    val listState = rememberLazyListState()
                    LaunchedEffect(yearMenuExpanded) {
                        if (yearMenuExpanded) {
                            val idx = years.indexOf(selectedYear).coerceAtLeast(0)
                            listState.scrollToItem(idx)
                        }
                    }
                    Box(modifier = Modifier.size(width = 110.dp, height = 240.dp)) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(years.size) { index ->
                                val yr = years[index]
                                DropdownMenuItem(
                                    text = { Text(yr.toFarsiNumbers(), fontSize = 12.sp, fontWeight = if (yr == selectedYear) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF112E21)) },
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
                                    Icon(TablerIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
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
                                val listState = rememberLazyListState()
                                LaunchedEffect(yearMenuExpanded) {
                                    if (yearMenuExpanded) {
                                        val idx = years.indexOf(selectedYear).coerceAtLeast(0)
                                        listState.scrollToItem(idx)
                                    }
                                }
                                Box(modifier = Modifier.size(width = 100.dp, height = 240.dp)) {
                                    LazyColumn(
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
                                    Icon(TablerIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
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
                                    Icon(TablerIcons.ChevronDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
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
                                val listState = rememberLazyListState()
                                LaunchedEffect(dayMenuExpanded) {
                                    if (dayMenuExpanded) {
                                        listState.scrollToItem(selectedDayIndex)
                                    }
                                }
                                Box(modifier = Modifier.size(width = 90.dp, height = 240.dp)) {
                                    LazyColumn(
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
