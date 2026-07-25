import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """fun FamilyMemberNodeCard(
    person: Person,
    isHighlighted: Boolean,
    accentColor: Color,
    cardBgColor: Color,
    textColor: Color,
    spouseHeartColor: Color? = null,
    isShadow: Boolean = false,
    onFocusClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    glowPersonId: Long? = null
) {"""

replacement = """fun FamilyMemberNodeCard(
    person: Person,
    isHighlighted: Boolean,
    accentColor: Color,
    cardBgColor: Color,
    textColor: Color,
    spouseHeartColor: Color? = null,
    isShadow: Boolean = false,
    onFocusClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit,
    onPhotoClick: (Person) -> Unit = {},
    glowPersonId: Long? = null,
    hasGhostChildren: Boolean = false,
    isGhostChildrenExpanded: Boolean = false,
    onToggleGhostChildren: () -> Unit = {},
    onEyeClick: () -> Unit = {}
) {"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced signature")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)

