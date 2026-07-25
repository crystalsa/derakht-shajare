import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("val positions = remember(persons, relationships, layoutType, focusPersonId) {", "val positions = remember(persons, relationships, layoutType, glowPersonId, expandedGhostParents) {")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
