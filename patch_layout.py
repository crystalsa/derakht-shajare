import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """        val children = spouseGroup.flatMap { childrenMap[it] ?: emptyList() }
            .filter { visiblePersonSet.contains(it) && !visitedSubtrees.contains(it) }
            .distinct()
            .sorted()

        if (children.isEmpty()) {"""

replacement = """        val allChildren = spouseGroup.flatMap { childrenMap[it] ?: emptyList() }
            .filter { visiblePersonSet.contains(it) }
            .distinct()
            .sorted()
            
        val mainChildren = mutableListOf<Long>()
        val shadowChildren = mutableListOf<Long>()
        
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
            val fatherInSpouseGroup = fatherId != null && spouseGroup.contains(fatherId)
            val motherInSpouseGroup = motherId != null && spouseGroup.contains(motherId)
            
            if (fatherInSpouseGroup || (!fatherInVisible && motherInSpouseGroup)) {
                if (!visitedSubtrees.contains(child)) {
                    mainChildren.add(child)
                }
            } else if (motherInSpouseGroup && fatherInVisible && !fatherInSpouseGroup) {
                shadowChildren.add(child)
            }
        }
        
        val children = mainChildren

        if (children.isEmpty() && shadowChildren.isEmpty()) {"""

content = content.replace(target, replacement)

target2 = """        // Layout all children subtrees
        val childLayouts = children.map { childId ->
            layoutSubtree(childId, level + 1)
        }"""

replacement2 = """        // Layout all children subtrees
        val childLayouts = children.map { childId ->
            layoutSubtree(childId, level + 1)
        }.toMutableList()
        
        val anySpouseExpanded = spouseGroup.any { expandedGhostParents.contains(it) }
        val parentHasGhostChildren = shadowChildren.isNotEmpty()
        
        if (anySpouseExpanded) {
            for (childId in shadowChildren) {
                // Determine which parent rendered this shadow child
                val mother = spouseGroup.firstOrNull { it in expandedGhostParents } ?: personId
                val childShadowPositions = mapOf("shadow_child_${childId}_${mother}" to 0f)
                val childMinX = mapOf(level + 1 to 0f)
                val childMaxX = mapOf(level + 1 to 0f)
                childLayouts.add(SubtreeLayout(emptyMap(), childShadowPositions, childMinX, childMaxX))
            }
        }"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
print("done")
