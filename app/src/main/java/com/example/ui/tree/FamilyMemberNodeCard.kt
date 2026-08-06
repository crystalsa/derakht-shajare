package com.example.ui.tree

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Boy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Girl
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.data.Person
import com.example.ui.common.photoUris
import com.example.ui.common.toFarsiNumbers
import com.example.ui.screens.formatLifeYearsOnlyLTR
import java.io.File

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FamilyMemberNodeCard(
    person: Person,
    isHighlighted: Boolean,
    accentColor: Color,
    cardBgColor: Color,
    textColor: Color,
    spouseHeartColor: Color? = null,
    isShadow: Boolean = false,
    onFocusClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    glowPersonId: Long? = null,
    onEyeClick: () -> Unit = {}
) {
    val isGlow = person.id == glowPersonId
    val borderStroke = if (isHighlighted) {
        BorderStroke(3.dp, Brush.linearGradient(listOf(Color(0xFFF57C00), Color.White, Color(0xFFF57C00))))
    } else if (isGlow) {
        BorderStroke(3.2.dp, accentColor)
    } else if (isShadow) {
        BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.5f))
    } else if (person.isDeceased) {
        BorderStroke(1.2.dp, Color.Gray.copy(alpha = 0.5f))
    } else {
        BorderStroke(1.5.dp, accentColor.copy(alpha = 0.5f))
    }

    val cardModifier = Modifier
        .width(160.dp)
        .let { modifier ->
            if (isGlow) {
                modifier.shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp))
            } else {
                modifier
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { onDoubleTap() },
                onTap = { onClick() }
            )
        }
        .testTag("member_node_${person.id}")

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isShadow) Color(0xFFF5F5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isShadow) 1.dp else 5.dp),
        border = borderStroke,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).let { 
            if (isShadow) it.alpha(0.65f) else it 
        }) {
            if (isShadow) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Shadow Link",
                    tint = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .size(16.dp)
                        .clickable { onEyeClick() }
                )
            }

            if (person.isDeceased) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Canvas(
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        drawLine(
                            color = Color(0xFF1E1E1E),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, 0f),
                            strokeWidth = 6.dp.toPx()
                        )
                    }
                }
            }

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
                            painter = rememberAsyncImagePainter(model = person.photoUris.firstOrNull()?.let { File(it) }),
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
                    color = Color(0xFF112E21),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = person.occupation ?: "-",
                    fontSize = 10.sp,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (person.birthDate != null || person.isDeceased) {
                    val ageDisplay = formatLifeYearsOnlyLTR(person.birthDate, person.deathDate, person.isDeceased)
                    
                    Text(
                        text = ageDisplay.toFarsiNumbers(),
                        fontSize = 10.sp,
                        color = Color(0xFF455A64),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
