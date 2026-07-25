import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """            // Render card nodes above lines
            positions.forEach { (key, pos) ->
                val isShadow = key.startsWith("shadow_")
                val personId = if (isShadow) key.split("_")[1].toLong() else key.toLong()
                val person = persons.find { it.id == personId }
                if (person != null) {"""

replacement = """            // Render card nodes above lines
            positions.forEach { (key, pos) ->
                val isShadow = key.startsWith("shadow_")
                val isShadowChild = key.startsWith("shadow_child_")
                val personId = if (isShadowChild) {
                    key.split("_")[2].toLong()
                } else if (isShadow) {
                    key.split("_")[1].toLong()
                } else {
                    key.toLong()
                }
                
                // Which parent is this shadow attached to?
                val parentRefId = if (isShadowChild) {
                    key.split("_")[3].toLong()
                } else if (isShadow) {
                    key.split("_")[2].toLong()
                } else {
                    personId
                }
                
                val person = persons.find { it.id == personId }
                if (person != null) {"""

content = content.replace(target, replacement)

target2 = """                    FamilyMemberNodeCard(
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
                        glowPersonId = if (isShadow) null else glowPersonId
                    )"""

replacement2 = """                    
                    // Determine if this card should have a + button
                    // The + button is placed on the main card of the mother, or shadow card of the father
                    // Wait, we can just say if this card's personId has ghost children
                    // Let's check if there are ghost children for this personId in the layout
                    val hasGhostChildren = positions.keys.any { it.startsWith("shadow_child_") && it.endsWith("_${person.id}") }
                    
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
                        hasGhostChildren = hasGhostChildren,
                        isGhostChildrenExpanded = expandedGhostParents.contains(person.id),
                        onToggleGhostChildren = { onToggleGhostChildren(person.id) },
                        onEyeClick = {
                            // On eye click, we center the view on the main card of this person
                            // For that, we can just call onViewFamilyClick or similar, 
                            // or change focusPersonId to this person.
                            onViewFamilyClick(person)
                        }
                    )"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
print("done")
