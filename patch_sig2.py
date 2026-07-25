import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """    onViewFamilyClick: (Person) -> Unit,
    onAddFirstPerson: () -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    glowPersonId: Long? = null
) {"""

replacement = """    onViewFamilyClick: (Person) -> Unit,
    onAddFirstPerson: () -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    glowPersonId: Long? = null,
    expandedGhostParents: Set<Long> = emptySet(),
    onToggleGhostChildren: (Long) -> Unit = {}
) {"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched signature")
    
with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
