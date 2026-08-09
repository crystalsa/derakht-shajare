package com.example.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Matrix
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import compose.icons.TablerIcons
import compose.icons.tablericons.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.example.data.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

fun isBitmapVisuallyBlank(bitmap: Bitmap): Boolean {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= 0 || height <= 0) return true
    
    val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
        try {
            bitmap.copy(Bitmap.Config.ARGB_8888, false)
        } catch (e: Exception) {
            null
        }
    } else {
        bitmap
    } ?: return false

    return try {
        val firstPixel = safeBitmap.getPixel(0, 0)
        val numSamples = 10
        val dx = width / numSamples
        val dy = height / numSamples
        if (dx <= 0 || dy <= 0) false
        else {
            var isBlank = true
            for (i in 0 until numSamples) {
                for (j in 0 until numSamples) {
                    val x = (i * dx).coerceIn(0, width - 1)
                    val y = (j * dy).coerceIn(0, height - 1)
                    if (safeBitmap.getPixel(x, y) != firstPixel) {
                        isBlank = false
                        break
                    }
                }
                if (!isBlank) break
            }
            isBlank
        }
    } catch (e: Exception) {
        false
    } finally {
        if (safeBitmap != bitmap) {
            safeBitmap.recycle()
        }
    }
}

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
        val file = File(photoPath)
        if (file.name.startsWith("person_cropped_")) {
            val originalFile = File(file.parent, file.name.replace("person_cropped_", "person_original_"))
            if (originalFile.exists()) {
                return originalFile.absolutePath
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return photoPath
}

suspend fun cropAndSaveBitmap(
    context: Context,
    originalBitmap: Bitmap,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    boxSizePx: Float,
    cropSizePx: Float
): String? = withContext(Dispatchers.IO) {
    try {
        val outputSize = 400
        val croppedBitmap = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(croppedBitmap)
        canvas.drawColor(AndroidColor.WHITE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val matrix = Matrix()

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

        val directory = File(context.filesDir, "photos")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val timestamp = System.currentTimeMillis()
        val croppedFile = File(directory, "person_cropped_$timestamp.jpg")
        val originalFile = File(directory, "person_original_$timestamp.jpg")

        FileOutputStream(croppedFile).use { out ->
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        croppedBitmap.recycle()

        // Downscale the original bitmap before saving to cap longest side at 1600px
        val maxDim = 1600
        val origW = originalBitmap.width
        val origH = originalBitmap.height
        val scaledOriginal = if (origW > maxDim || origH > maxDim) {
            val ratio = maxDim.toFloat() / Math.max(origW, origH)
            val newW = (origW * ratio).toInt().coerceAtLeast(1)
            val newH = (origH * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(originalBitmap, newW, newH, true)
        } else {
            originalBitmap
        }

        FileOutputStream(originalFile).use { out ->
            scaledOriginal.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        if (scaledOriginal != originalBitmap) {
            scaledOriginal.recycle()
        }

        croppedFile.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun String.toFarsiNumbers(): String {
    return this.map { char ->
        if (char in '0'..'9') {
            (char.code + 1584).toChar()
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
fun StatRowItem(
    title: String,
    value: String,
    icon: String,
    accentColor: Color,
    textColor: Color,
    members: List<Person>? = null,
    isExpanded: Boolean = false,
    onToggleExpand: (() -> Unit)? = null,
    onPersonClick: ((Person) -> Unit)? = null
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAF8)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = icon, fontSize = 16.sp)
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    if (onToggleExpand != null && !members.isNullOrEmpty()) {
                        Icon(
                            imageVector = if (isExpanded) TablerIcons.ChevronUp else TablerIcons.ChevronDown,
                            contentDescription = if (isExpanded) "بستن" else "باز کردن",
                            tint = textColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isExpanded && !members.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEach { person ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable { onPersonClick?.invoke(person) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val firstPhoto = person.photoUris.firstOrNull()
                                if (firstPhoto != null) {
                                    SubcomposeAsyncImage(
                                        model = firstPhoto,
                                        contentDescription = person.fullName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                    ) {
                                        val state = painter.state
                                        if (state is AsyncImagePainter.State.Loading || state is AsyncImagePainter.State.Error) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(if (person.gender == "مرد") Color(0xFFE3F2FD) else Color(0xFFFCE4EC), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = if (person.gender == "مرد") "👨" else "👩",
                                                    fontSize = 12.sp
                                                )
                                            }
                                        } else {
                                            SubcomposeAsyncImageContent()
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(if (person.gender == "مرد") Color(0xFFE3F2FD) else Color(0xFFFCE4EC), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (person.gender == "مرد") "👨" else "👩",
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Text(
                                    text = person.fullName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                            }

                            IconButton(
                                onClick = { onPersonClick?.invoke(person) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            ) {
                                Icon(
                                    imageVector = TablerIcons.Eye,
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
