import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """fun computeTreeLayoutPositions(
    persons: List<Person>,
    relationships: List<Relationship>,
    layoutType: String,
    focusPersonId: Long?"""

replacement = """fun computeTreeLayoutPositions(
    persons: List<Person>,
    relationships: List<Relationship>,
    layoutType: String,
    focusPersonId: Long?,
    expandedGhostParents: Set<Long> = emptySet()"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced signature")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)

