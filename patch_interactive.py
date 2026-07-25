import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target_sig = """fun InteractiveFamilyTree(
    persons: List<Person>,
    relationships: List<Relationship>,
    layoutType: String,
    onPersonClick: (Person) -> Unit,
    onPersonDoubleTap: (Person) -> Unit,
    onViewFamilyClick: (Person) -> Unit,
    onPhotoClick: (Person) -> Unit,
    glowPersonId: Long?
) {"""

replacement_sig = """fun InteractiveFamilyTree(
    persons: List<Person>,
    relationships: List<Relationship>,
    layoutType: String,
    onPersonClick: (Person) -> Unit,
    onPersonDoubleTap: (Person) -> Unit,
    onViewFamilyClick: (Person) -> Unit,
    onPhotoClick: (Person) -> Unit,
    glowPersonId: Long?,
    expandedGhostParents: Set<Long>,
    onToggleGhostChildren: (Long) -> Unit
) {"""

content = content.replace(target_sig, replacement_sig)

target_call = """        computeTreeLayoutPositions(persons, relationships, layoutType, focusPersonId)
    }"""

replacement_call = """        computeTreeLayoutPositions(persons, relationships, layoutType, glowPersonId, expandedGhostParents)
    }"""

content = content.replace(target_call, replacement_call)

target_main_call = """                            InteractiveFamilyTree(
                                persons = filteredPersonsList,
                                relationships = filteredRelationships,
                                layoutType = currentLayout,
                                onPersonClick = {
                                    val member = allPersonsRaw.find { p -> p.id == it.id }
                                    if (member != null) {
                                        selectedMemberForDetails = member
                                        showMemberDetailsDialog = true
                                    }
                                },
                                onPersonDoubleTap = {
                                    viewModel.setFocusPerson(it.id)
                                },
                                onViewFamilyClick = {
                                    viewModel.setFocusPerson(it.id)
                                },
                                onPhotoClick = { person ->
                                    val path = person.photoUri ?: person.photoPath
                                    if (!path.isNullOrEmpty()) {
                                        fullScreenPhotoPath = getFullOrOriginalPhotoPath(path)
                                    }
                                },
                                glowPersonId = focusPersonId
                            )"""

replacement_main_call = """                            InteractiveFamilyTree(
                                persons = filteredPersonsList,
                                relationships = filteredRelationships,
                                layoutType = currentLayout,
                                onPersonClick = {
                                    val member = allPersonsRaw.find { p -> p.id == it.id }
                                    if (member != null) {
                                        selectedMemberForDetails = member
                                        showMemberDetailsDialog = true
                                    }
                                },
                                onPersonDoubleTap = {
                                    viewModel.setFocusPerson(it.id)
                                },
                                onViewFamilyClick = {
                                    viewModel.setFocusPerson(it.id)
                                },
                                onPhotoClick = { person ->
                                    val path = person.photoUri ?: person.photoPath
                                    if (!path.isNullOrEmpty()) {
                                        fullScreenPhotoPath = getFullOrOriginalPhotoPath(path)
                                    }
                                },
                                glowPersonId = focusPersonId,
                                expandedGhostParents = expandedGhostParents,
                                onToggleGhostChildren = { parentId ->
                                    expandedGhostParents = if (expandedGhostParents.contains(parentId)) {
                                        expandedGhostParents - parentId
                                    } else {
                                        expandedGhostParents + parentId
                                    }
                                }
                            )"""

content = content.replace(target_main_call, replacement_main_call)

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)

print("done")
