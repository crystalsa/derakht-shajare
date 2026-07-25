import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """                    } else {
                        pairsToDraw.add(rel.personId1.toString() to rel.personId2.toString())
                    }"""

replacement = """                    } else {
                        pairsToDraw.add(rel.personId1.toString() to rel.personId2.toString())
                        
                        // Add shadow children lines
                        // rel.personId1 is parent, rel.personId2 is child
                        val childId = rel.personId2
                        val parentId = rel.personId1
                        
                        val shadowChildKeys = positions.keys.filter { it.startsWith("shadow_child_${childId}_") }
                        for (shadowKey in shadowChildKeys) {
                            val motherId = shadowKey.split("_")[3].toLong()
                            // If this parent is the mother who rendered it, or the shadow father rendered next to the mother
                            // The easiest way is to connect the shadow child to both parents in that subtree!
                            // The mother is `motherId` (main card). The father is `shadow_${father}_${mother}`.
                            
                            val motherStr = motherId.toString()
                            val shadowFatherStr = "shadow_${parentId}_${motherId}"
                            val shadowMotherStr = "shadow_${parentId}_${motherId}" // in case parent is mother and father is main? No, mother is always main.
                            
                            if (parentId == motherId && positions.containsKey(motherStr)) {
                                pairsToDraw.add(motherStr to shadowKey)
                            } else if (positions.containsKey(shadowFatherStr)) {
                                pairsToDraw.add(shadowFatherStr to shadowKey)
                            }
                        }
                    }"""

if target in content:
    content = content.replace(target, replacement)
    print("Added shadow lines")
    
with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
