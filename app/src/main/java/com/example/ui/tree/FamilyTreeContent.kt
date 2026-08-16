package com.example.ui.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.Person
import com.example.data.Relationship
import com.example.ui.common.isSecondSpouseRelation
import com.example.ui.common.isSpouseRelation
import java.util.LinkedList
import java.util.Queue
import kotlin.math.roundToInt

data class TreePos(val x: Float, val y: Float)

@Composable
fun FamilyTreeContent(
    persons: List<Person>,
    relationships: List<Relationship>,
    positions: Map<String, TreePos>,
    layoutType: String,
    accentColor: Color,
    cardBgColor: Color,
    textColor: Color,
    density: Float,
    glowPersonId: Long? = null,
    highlightedPathIds: Set<Long> = emptySet(),
    scale: Float = 1f,
    panOffset: Offset = Offset.Zero,
    onViewFamilyClick: (Person) -> Unit = {},
    onPersonClick: (Person) -> Unit = {},
    onPersonDoubleTap: (Person) -> Unit = {},
    onPhotoClick: (Person) -> Unit = {},
    onPanToPerson: (Person) -> Unit = {},
    modifier: Modifier = Modifier
) {
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
            0xFFE91E63, 0xFF3F51B5, 0xFF4CAF50, 0xFFFF9800, 0xFF9C27B0, 0xFF00BCD4, 0xFFFFEB3B, 0xFF795548,
            0xFFF44336, 0xFF2196F3, 0xFF8BC34A, 0xFFFF5722, 0xFF673AB7, 0xFF009688, 0xFFFFC107, 0xFF607D8B,
            0xFFE040FB, 0xFF03A9F4, 0xFFCDDC39, 0xFFFF7043, 0xFF512DA8, 0xFF00796B, 0xFFFBC02D, 0xFF5D4037,
            0xFFC2185B, 0xFF1976D2, 0xFF689F38, 0xFFE64A19, 0xFF7B1FA2, 0xFF0097A7, 0xFFF57C00, 0xFF455A64,
            0xFFD81B60, 0xFF0288D1, 0xFF9CCC65, 0xFFF4511E, 0xFF303F9F, 0xFF26A69A, 0xFFFFCA28, 0xFF8D6E63,
            0xFFAD1457, 0xFF1565C0, 0xFF558B2F, 0xFFD84315, 0xFF4527A0, 0xFF00838F, 0xFFF39C12, 0xFF37474F,
            0xFFEC407A, 0xFF29B6F6, 0xFF7CB342, 0xFFFF8A65, 0xFF5E35B1, 0xFF26C6DA, 0xFFFFD54F, 0xFF6D4C41,
            0xFF880E4F, 0xFF0D47A1, 0xFF33691E, 0xFFBF360C, 0xFF311B92, 0xFF006064, 0xFFE67E22, 0xFF263238
        ).map { Color(it) }
        
        val random = kotlin.random.Random(42)
        val shuffledColors = heartColors.shuffled(random)
        
        spousePairs.forEachIndexed { index, pair ->
            val color = shuffledColors[index % shuffledColors.size]
            map[pair.first] = color
            map[pair.second] = color
        }
        map
    }

    val personsById = remember(persons) {
        persons.associateBy { it.id }
    }

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val visibleBounds = remember(scale, panOffset, widthPx, heightPx, density) {
            if (widthPx <= 0f || heightPx <= 0f) null
            else {
                val marginPx = 300f * density
                val minX = (-panOffset.x - widthPx / 2f) / scale - marginPx
                val maxX = (widthPx / 2f - panOffset.x) / scale + marginPx
                val minY = (-panOffset.y - heightPx / 2f) / scale - marginPx
                val maxY = (heightPx / 2f - panOffset.y) / scale + marginPx
                Rect(minX, minY, maxX, maxY)
            }
        }
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            for (rel in relationships) {
                val isSpouse = isSpouseRelation(rel.type)
                
                val pairsToDraw = mutableListOf<Pair<String, String>>()
                if (isSpouse) {
                    val p1 = rel.personId1.toString()
                    val p2 = rel.personId2.toString()
                    val s1 = "shadow_${rel.personId1}_${rel.personId2}"
                    val s2 = "shadow_${rel.personId2}_${rel.personId1}"
                    
                    var drawn = false
                    if (positions.containsKey(p1) && positions.containsKey(s2)) {
                        pairsToDraw.add(p1 to s2)
                        drawn = true
                    }
                    if (positions.containsKey(p2) && positions.containsKey(s1)) {
                        pairsToDraw.add(p2 to s1)
                        drawn = true
                    }
                    if (!drawn && positions.containsKey(p1) && positions.containsKey(p2)) {
                        pairsToDraw.add(p1 to p2)
                    }
                } else {
                    pairsToDraw.add(rel.personId1.toString() to rel.personId2.toString())
                }

                for ((pos1Str, pos2Str) in pairsToDraw) {
                    val pos1 = positions[pos1Str]
                    val pos2 = positions[pos2Str]
                    if (pos1 != null && pos2 != null) {
                        if (visibleBounds != null) {
                            val minLineX = minOf(pos1.x, pos2.x) * density
                            val maxLineX = maxOf(pos1.x, pos2.x) * density
                            val minLineY = minOf(pos1.y, pos2.y) * density
                            val maxLineY = maxOf(pos1.y, pos2.y) * density

                            if (maxLineX < visibleBounds.left || minLineX > visibleBounds.right ||
                                maxLineY < visibleBounds.top || minLineY > visibleBounds.bottom) {
                                continue
                            }
                        }
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
                        
                        val lineColors = listOf(
                            Color(0xFF2E7D32),
                            Color(0xFF1565C0),
                            Color(0xFFC2185B),
                            Color(0xFF8E24AA),
                            Color(0xFFE65100),
                            Color(0xFF00838F),
                            Color(0xFF00695C),
                            Color(0xFFD84315),
                            Color(0xFF6D4C41),
                            Color(0xFF455A64)
                        )
                        val colorSeed = if (isSpouseRelation(rel.type)) {
                            minOf(rel.personId1, rel.personId2)
                        } else {
                            rel.personId1
                        }
                        val randomLineColor = lineColors[(colorSeed % lineColors.size).toInt()]
                        val drawColor = if (isHighlightedConnection) Color(0xFFD84315) else randomLineColor.copy(alpha = 0.9f)

                        when (rel.type) {
                            "Spouse", "SecondSpouse" -> {
                                drawLine(
                                    color = drawColor,
                                    start = p1Offset,
                                    end = p2Offset,
                                    strokeWidth = strokeWidth,
                                    cap = StrokeCap.Round
                                )
                            }
                            "Divorced", "SecondSpouse_Divorced" -> {
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
                                        val isConsanguineous = positions.containsKey("shadow_${p1}_${p2}") || positions.containsKey("shadow_${p2}_${p1}")
                                        
                                        if (isConsanguineous) {
                                            true
                                        } else {
                                            val p1Str = p1.toString()
                                            val p2Str = p2.toString()
                                            isSpouseMap.contains("$minId-$maxId") && positions.containsKey(p1Str) && positions.containsKey(p2Str)
                                        }
                                    } else {
                                        false
                                    }

                                    if (hasSpouseParents) {
                                        val p1 = parents[0]
                                        val p2 = parents[1]
                                        if (parentId == minOf(p1, p2)) {
                                            var p1Key = p1.toString()
                                            var p2Key = p2.toString()
                                            
                                            val p1Person = personsById[p1]
                                            val p2Person = personsById[p2]
                                            
                                            if (p1Person?.gender == "Female" && p2Person?.gender == "Male" && positions.containsKey("shadow_${p1}_${p2}")) {
                                                p1Key = "shadow_${p1}_${p2}"
                                            } else if (p1Person?.gender == "Male" && p2Person?.gender == "Female" && positions.containsKey("shadow_${p2}_${p1}")) {
                                                p2Key = "shadow_${p2}_${p1}"
                                            } else {
                                                if (positions.containsKey("shadow_${p1}_${p2}")) {
                                                    p1Key = "shadow_${p1}_${p2}"
                                                } else if (positions.containsKey("shadow_${p2}_${p1}")) {
                                                    p2Key = "shadow_${p2}_${p1}"
                                                }
                                            }
                                            
                                            val posParent1 = positions[p1Key]
                                            val posParent2 = positions[p2Key]
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
        }

        positions.forEach { (key, pos) ->
            val isShadow = key.startsWith("shadow_")
            
            val personId = if (isShadow) {
                key.split("_")[1].toLong()
            } else {
                key.toLong()
            }
            val person = personsById[personId] ?: return@forEach

            val cardXPx = pos.x * density
            val cardYPx = pos.y * density

            if (visibleBounds != null) {
                if (cardXPx < visibleBounds.left || cardXPx > visibleBounds.right ||
                    cardYPx < visibleBounds.top || cardYPx > visibleBounds.bottom) {
                    return@forEach
                }
            }

            Box(
                modifier = Modifier
                    .absoluteOffset { IntOffset(cardXPx.roundToInt(), cardYPx.roundToInt()) }
                    .padding(8.dp)
                    .align(Alignment.Center)
            ) {
                val isPathHighlighted = highlightedPathIds.contains(person.id)
                
                FamilyMemberNodeCard(
                    person = person,
                    isHighlighted = isPathHighlighted,
                    accentColor = accentColor,
                    cardBgColor = cardBgColor,
                    textColor = textColor,
                    spouseHeartColor = spouseMapForHeart[person.id],
                    isShadow = isShadow,
                    onFocusClick = { onViewFamilyClick(person) },
                    onClick = { onPersonClick(person) },
                    onDoubleTap = { onPersonDoubleTap(person) },
                    onPhotoClick = onPhotoClick,
                    glowPersonId = if (isShadow) null else glowPersonId,
                    onEyeClick = {
                        onPanToPerson(person)
                    }
                )
            }
        }
    }
}

private fun DrawScope.drawElbowLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    layoutType: String
) {
    if (layoutType == "Circular") {
        drawLine(color = color, start = start, end = end, strokeWidth = strokeWidth)
    } else if (layoutType == "Horizontal") {
        val midX = (start.x + end.x) / 2
        drawLine(color = color, start = start, end = Offset(midX, start.y), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(midX, start.y), end = Offset(midX, end.y), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(midX, end.y), end = end, strokeWidth = strokeWidth)
    } else {
        val midY = (start.y + end.y) / 2
        drawLine(color = color, start = start, end = Offset(start.x, midY), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(start.x, midY), end = Offset(end.x, midY), strokeWidth = strokeWidth)
        drawLine(color = color, start = Offset(end.x, midY), end = end, strokeWidth = strokeWidth)
    }
}

fun computeTreeLayoutPositions(
    persons: List<Person>,
    relationships: List<Relationship>,
    layoutType: String,
    focusPersonId: Long?,
    expandedGhostParents: Set<Long> = emptySet()
): Map<String, TreePos> {
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

    for (p in persons) {
        if (!levels.containsKey(p.id)) {
            levels[p.id] = 0
        }
    }

    val visiblePersonIds = if (focusPersonId != null) {
        val visitedSet = mutableSetOf<Long>()
        
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

        parentsMap[focusPersonId]?.forEach { parentId ->
            childrenMap[parentId]?.forEach { siblingId ->
                visitedSet.add(siblingId)
            }
        }

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
        val shadowPositions: Map<String, Float>,
        val minXAtLevel: Map<Int, Float>,
        val maxXAtLevel: Map<Int, Float>
    )

    val visiblePersonSet = visiblePersonIds.toSet()
    val visitedSubtrees = mutableSetOf<Long>()
    val isHorizontal = layoutType == "Horizontal"
    val spouseSpacing = if (isHorizontal) 240f else 180f
    val siblingSpacing = if (isHorizontal) 260f else 200f

    fun layoutSubtree(personId: Long, level: Int): SubtreeLayout {
        val spouses = (spousesMap[personId] ?: emptySet()).filter { visiblePersonSet.contains(it) }.sorted()
        
        val shadowSpouses = spouses.filter { spouseId ->
            visitedSubtrees.contains(spouseId) || 
            (parentsMap[spouseId]?.any { visiblePersonSet.contains(it) && !visitedSubtrees.contains(it) } == true)
        }
        val newSpouses = spouses.filter { !shadowSpouses.contains(it) }
        
        val spouseGroup = listOf(personId) + newSpouses
        val fullGroupForLayout = listOf(personId) + spouses
        
        spouseGroup.forEach { visitedSubtrees.add(it) }
        
        val S = fullGroupForLayout.size
        
        val localPositions = mutableMapOf<Long, Float>()
        val shadowPositions = mutableMapOf<String, Float>()
        for (i in 0 until S) {
            val memberId = fullGroupForLayout[i]
            val x = i * spouseSpacing - (S - 1) * spouseSpacing / 2f
            if (memberId in shadowSpouses) {
                shadowPositions["shadow_${memberId}_$personId"] = x
            } else {
                localPositions[memberId] = x
            }
        }
        
        val allChildren = fullGroupForLayout.flatMap { childrenMap[it] ?: emptyList() }
            .filter { visiblePersonSet.contains(it) }
            .distinct()
            .sorted()
            
        val mainChildren = mutableListOf<Long>()
        
        for (child in allChildren) {
            val pParents = parentsMap[child] ?: emptyList()
            var fatherId: Long? = null
            var motherId: Long? = null
            for (pId in pParents) {
                val p = persons.find { it.id == pId }
                if (p?.gender == "Male") fatherId = pId
                if (p?.gender == "Female") motherId = pId
            }
            
            val fatherInVisible = fatherId != null && visiblePersonSet.contains(fatherId)
            val motherInVisible = motherId != null && visiblePersonSet.contains(motherId)
            
            val fatherInFullGroup = fatherId != null && fullGroupForLayout.contains(fatherId)
            val motherInFullGroup = motherId != null && fullGroupForLayout.contains(motherId)
            
            val shouldDrawMain = if (fatherInVisible && motherInVisible) {
                fatherInFullGroup
            } else {
                fatherInFullGroup || motherInFullGroup
            }
            
            if (shouldDrawMain) {
                if (!visitedSubtrees.contains(child)) {
                    mainChildren.add(child)
                }
            }
        }
        
        val children = mainChildren

        if (children.isEmpty()) {
            val levelMinX = mapOf(level to -(S - 1) * spouseSpacing / 2f)
            val levelMaxX = mapOf(level to (S - 1) * spouseSpacing / 2f)
            return SubtreeLayout(localPositions, shadowPositions, levelMinX, levelMaxX)
        }

        val childLayouts = children.map { childId ->
            layoutSubtree(childId, level + 1)
        }.toMutableList()

        val mergedPositions = mutableMapOf<Long, Float>()
        val mergedShadowPositions = mutableMapOf<String, Float>()
        val mergedMinX = mutableMapOf<Int, Float>()
        val mergedMaxX = mutableMapOf<Int, Float>()

        for (i in childLayouts.indices) {
            val childLayout = childLayouts[i]
            if (i == 0) {
                mergedPositions.putAll(childLayout.positions)
                mergedShadowPositions.putAll(childLayout.shadowPositions)
                mergedMinX.putAll(childLayout.minXAtLevel)
                mergedMaxX.putAll(childLayout.maxXAtLevel)
            } else {
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

                childLayout.positions.forEach { (id, x) ->
                    mergedPositions[id] = x + minShift
                }
                childLayout.shadowPositions.forEach { (key, x) ->
                    mergedShadowPositions[key] = x + minShift
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

        val childrenMin = mergedMinX[level + 1] ?: 0f
        val childrenMax = mergedMaxX[level + 1] ?: 0f
        val childrenCenter = (childrenMin + childrenMax) / 2f

        val shiftAmount = -childrenCenter
        val finalPositions = mutableMapOf<Long, Float>()
        val finalShadowPositions = mutableMapOf<String, Float>()
        finalPositions.putAll(localPositions)
        finalShadowPositions.putAll(shadowPositions)

        mergedPositions.forEach { (id, x) ->
            finalPositions[id] = x + shiftAmount
        }
        mergedShadowPositions.forEach { (key, x) ->
            finalShadowPositions[key] = x + shiftAmount
        }

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

        return SubtreeLayout(finalPositions, finalShadowPositions, finalMinX, finalMaxX)
    }

    val allSubtreeLayouts = mutableListOf<SubtreeLayout>()
    
    val rootIds = persons.filter { parentsMap[it.id].isNullOrEmpty() }.map { it.id }.sorted()
    for (rootId in rootIds) {
        if (visiblePersonSet.contains(rootId) && !visitedSubtrees.contains(rootId)) {
            allSubtreeLayouts.add(layoutSubtree(rootId, 0))
        }
    }

    for (p in persons) {
        if (visiblePersonSet.contains(p.id) && !visitedSubtrees.contains(p.id)) {
            allSubtreeLayouts.add(layoutSubtree(p.id, 0))
        }
    }

    val finalPositions = mutableMapOf<Long, Float>()
    val finalShadowPositions = mutableMapOf<String, Float>()
    val globalMaxX = mutableMapOf<Int, Float>()

    for (i in allSubtreeLayouts.indices) {
        val layout = allSubtreeLayouts[i]
        if (i == 0) {
            finalPositions.putAll(layout.positions)
            finalShadowPositions.putAll(layout.shadowPositions)
            layout.maxXAtLevel.forEach { (lvl, x) ->
                globalMaxX[lvl] = x
            }
        } else {
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

            layout.positions.forEach { (id, x) ->
                finalPositions[id] = x + minShift
            }
            layout.shadowPositions.forEach { (key, x) ->
                finalShadowPositions[key] = x + minShift
            }
            layout.maxXAtLevel.forEach { (lvl, x) ->
                val newMax = x + minShift
                globalMaxX[lvl] = maxOf(globalMaxX[lvl] ?: newMax, newMax)
            }
        }
    }

    val positions = mutableMapOf<String, TreePos>()
    val vSpacing = 280f

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
                    positions[id.toString()] = TreePos(
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
                    positions[id.toString()] = TreePos(
                        x = radius * kotlin.math.cos(angle),
                        y = radius * kotlin.math.sin(angle)
                    )
                }
                else -> {
                    positions[id.toString()] = TreePos(
                        x = posX,
                        y = level * vSpacing
                    )
                }
            }
        }
    }
    
    finalShadowPositions.forEach { (key, posX) ->
        val parts = key.split("_")
        val personLevelId = parts[1].toLong()
        val level = levels[personLevelId] ?: 0
        when (layoutType) {
            "Horizontal" -> {
                positions[key] = TreePos(
                    x = level * vSpacing,
                    y = posX
                )
            }
            "Circular" -> {
                positions[key] = TreePos(0f, 0f)
            }
            else -> {
                positions[key] = TreePos(
                    x = posX,
                    y = level * vSpacing
                )
            }
        }
    }

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
