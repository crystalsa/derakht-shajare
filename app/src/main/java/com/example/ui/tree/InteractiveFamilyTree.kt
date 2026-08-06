package com.example.ui.tree

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Person
import com.example.data.Relationship
import com.example.utils.RelationshipCalculator
import kotlinx.coroutines.launch

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
    onPanToPerson: (Person) -> Unit = {},
    onAddFirstPerson: () -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    glowPersonId: Long? = null,
    expandedGhostParents: Set<Long> = emptySet(),
    onToggleGhostChildren: (Long) -> Unit = {}
) {
    if (persons.isEmpty()) {
        BoxWithConstraints(
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
                            containerColor = Color(0xFF2E7D32),
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

    var scale by remember { mutableStateOf(1f) }
    val animatableOffset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    val positions = remember(persons, relationships, layoutType, focusPersonId, expandedGhostParents) {
        computeTreeLayoutPositions(persons, relationships, layoutType, focusPersonId, expandedGhostParents)
    }

    LaunchedEffect(glowPersonId, positions) {
        if (glowPersonId != null) {
            val pos = positions[glowPersonId.toString()]
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
            painter = painterResource(id = R.drawable.ic_family_tree_icon),
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
            FamilyTreeContent(
                persons = persons,
                relationships = relationships,
                positions = positions,
                layoutType = layoutType,
                accentColor = accentColor,
                cardBgColor = cardBgColor,
                textColor = textColor,
                density = density,
                glowPersonId = glowPersonId,
                highlightedPathIds = highlightedPathIds,
                onViewFamilyClick = onViewFamilyClick,
                onPersonClick = onPersonClick,
                onPersonDoubleTap = onPersonDoubleTap,
                onPhotoClick = onPhotoClick,
                onPanToPerson = onPanToPerson,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
