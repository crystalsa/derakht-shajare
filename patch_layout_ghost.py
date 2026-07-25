import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """        val anySpouseExpanded = spouseGroup.any { expandedGhostParents.contains(it) }
        val parentHasGhostChildren = shadowChildren.isNotEmpty()
        
        if (anySpouseExpanded) {"""

replacement = """        val anySpouseExpanded = spouseGroup.any { expandedGhostParents.contains(it) }
        val parentHasGhostChildren = shadowChildren.isNotEmpty()
        
        if (parentHasGhostChildren) {
            spouseGroup.forEach { spId ->
                shadowPositions["has_ghost_children_${spId}"] = 0f
            }
        }
        
        if (anySpouseExpanded) {"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched layout ghost marker")

target2 = """                    val hasGhostChildren = positions.keys.any { it.startsWith("shadow_child_") && it.endsWith("_${person.id}") }"""

replacement2 = """                    val hasGhostChildren = positions.containsKey("has_ghost_children_${person.id}")"""

if target2 in content:
    content = content.replace(target2, replacement2)
    print("Patched hasGhostChildren check")

target3 = """            positions.forEach { (key, pos) ->
                val isShadow = key.startsWith("shadow_")
                val isShadowChild = key.startsWith("shadow_child_")"""

replacement3 = """            positions.forEach { (key, pos) ->
                if (key.startsWith("has_ghost_children_")) return@forEach
                
                val isShadow = key.startsWith("shadow_")
                val isShadowChild = key.startsWith("shadow_child_")"""

if target3 in content:
    content = content.replace(target3, replacement3)
    print("Patched render loop")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
