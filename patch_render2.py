import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """            positions.forEach { (key, pos) ->
                val isShadow = key.startsWith("shadow_")
                val personId = if (isShadow) key.split("_")[1].toLong() else key.toLong()
                val person = persons.find { it.id == personId } ?: return@forEach"""

replacement = """            positions.forEach { (key, pos) ->
                if (key.startsWith("has_ghost_children_")) return@forEach
                val isShadowChild = key.startsWith("shadow_child_")
                val isShadow = key.startsWith("shadow_")
                
                val personId = if (isShadowChild) {
                    key.split("_")[2].toLong()
                } else if (isShadow) {
                    key.split("_")[1].toLong()
                } else {
                    key.toLong()
                }
                val person = persons.find { it.id == personId } ?: return@forEach"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched render loop")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
