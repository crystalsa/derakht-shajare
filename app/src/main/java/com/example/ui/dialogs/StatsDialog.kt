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
                    IconButton(onClick = onDismiss) { Icon(TablerIcons.X, contentDescription = "بستن", tint = textColor) }
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
                            Text(stats.totalCount.toFarsiNumbers(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("در قید حیات", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(stats.livingCount.toFarsiNumbers(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accentColor)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("درگذشتگان", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                            Text(stats.deceasedCount.toFarsiNumbers(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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
                        Text("آقایان: ${stats.malesCount.toFarsiNumbers()} نفر (${(malePercent * 100).toInt().toFarsiNumbers()}٪)", fontSize = 11.sp, color = Color(0xFF1565C0))
                        Text("بانوان: ${stats.femalesCount.toFarsiNumbers()} نفر (${((1 - malePercent) * 100).toInt().toFarsiNumbers()}٪)", fontSize = 11.sp, color = Color(0xFFC2185B))
                    }
                }

                Divider()

                // Demographic summaries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("میانگین سن افراد زنده:", fontSize = 12.sp, color = textColor)
                        Text("${stats.avgLivingAge.toFarsiNumbers()} سال", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                    }
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("میانگین سن فوت شدگان:", fontSize = 12.sp, color = textColor)
                        Text("${stats.avgDeceasedAge.toFarsiNumbers()} سال", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
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
