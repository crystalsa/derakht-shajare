import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """    // Add shadow positions
    finalShadowPositions.forEach { (key, posX) ->
        val parts = key.split("_")
        val mainPersonId = parts[2].toLong()
        val level = levels[mainPersonId] ?: 0"""

replacement = """    // Add shadow positions
    finalShadowPositions.forEach { (key, posX) ->
        val parts = key.split("_")
        val isShadowChild = key.startsWith("shadow_child_")
        // For shadow spouse: shadow_12_34 -> parts[1]=12, parts[2]=34 (main spouse)
        // For shadow child: shadow_child_56_78 -> parts[2]=56 (child), parts[3]=78 (parent rendering it)
        val personLevelId = if (isShadowChild) parts[2].toLong() else parts[1].toLong()
        val level = levels[personLevelId] ?: 0"""

if target in content:
    content = content.replace(target, replacement)
    print("Fixed shadow positions")
else:
    print("Could not find shadow positions block")
    
with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
