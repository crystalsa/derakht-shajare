import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

# For lines 1048, 1101, 1613 calls, we can just replace:
# glowPersonId = glowPersonId (if it exists, wait, it doesn't pass glowPersonId in those calls)
# Let's see. The last line of the calls is `onPhotoClick = { ... }` or `onViewFamilyClick = { ... }`

target_call = """                                onViewFamilyClick = { person ->
                                    person.groupId?.let { gid ->
                                        viewModel.selectGroup(gid)
                                    }
                                }
                            )"""

replacement_call = """                                onViewFamilyClick = { person ->
                                    person.groupId?.let { gid ->
                                        viewModel.selectGroup(gid)
                                    }
                                },
                                expandedGhostParents = expandedGhostParents,
                                onToggleGhostChildren = { parentId ->
                                    expandedGhostParents = if (expandedGhostParents.contains(parentId)) {
                                        expandedGhostParents - parentId
                                    } else {
                                        expandedGhostParents + parentId
                                    }
                                }
                            )"""

content = content.replace(target_call, replacement_call)

target_call2 = """                        onViewFamilyClick = { person ->
                            person.groupId?.let { gid ->
                                viewModel.selectGroup(gid)
                            }
                        }
                    )"""

replacement_call2 = """                        onViewFamilyClick = { person ->
                            person.groupId?.let { gid ->
                                viewModel.selectGroup(gid)
                            }
                        },
                        expandedGhostParents = expandedGhostParents,
                        onToggleGhostChildren = { parentId ->
                            expandedGhostParents = if (expandedGhostParents.contains(parentId)) {
                                expandedGhostParents - parentId
                            } else {
                                expandedGhostParents + parentId
                            }
                        }
                    )"""

content = content.replace(target_call2, replacement_call2)

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
