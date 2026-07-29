import sys

with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

target1 = "var currentImageIndex by remember(person.id, uris.size) { mutableStateOf(0) }"
replacement1 = """var currentImageIndex by remember(person.id, uris.size) { mutableStateOf(0) }
        var showPhotoDeleteConfirm by remember { mutableStateOf(false) }"""

if target1 in content:
    content = content.replace(target1, replacement1)
else:
    print("target1 not found")
    sys.exit(1)

target2 = """                        // Delete photo
                        if (uris.isNotEmpty()) {
                            androidx.compose.material3.FilledTonalButton(
                                onClick = {
                                    val freshPerson = allPersonsRaw.find { it.id == person.id } ?: person
                                    val currentUris = freshPerson.photoUris.toMutableList()
                                    if (currentUris.isNotEmpty() && currentImageIndex in currentUris.indices) {
                                        currentUris.removeAt(currentImageIndex)
                                        val newPhotoUri = if (currentUris.isEmpty()) null else currentUris.joinToString("|")
                                        val updatedPerson = freshPerson.copy(photoUri = newPhotoUri)
                                        viewModel.updatePerson(updatedPerson)
                                        Toast.makeText(context, "عکس با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                                        if (currentImageIndex >= currentUris.size && currentImageIndex > 0) {
                                            currentImageIndex--
                                        }
                                    }
                                },"""

replacement2 = """                        // Delete photo
                        if (uris.isNotEmpty()) {
                            androidx.compose.material3.FilledTonalButton(
                                onClick = { showPhotoDeleteConfirm = true },"""

if target2 in content:
    content = content.replace(target2, replacement2)
else:
    print("target2 not found")
    sys.exit(1)

target3 = """                        }
                    }
                }
            }
        }
    }

    if (immersivePhotoIndex != null"""

replacement3 = """                        }
                    }
                }
            }
            
            if (showPhotoDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showPhotoDeleteConfirm = false },
                    title = { Text("حذف عکس", fontWeight = FontWeight.Bold, color = Color(0xFFC62828)) },
                    text = { Text("آیا مطمئن هستید که می‌خواهید این عکس را حذف کنید؟", fontSize = 14.sp) },
                    confirmButton = {
                        Button(
                            onClick = {
                                val freshPerson = allPersonsRaw.find { it.id == person.id } ?: person
                                val currentUris = freshPerson.photoUris.toMutableList()
                                if (currentUris.isNotEmpty() && currentImageIndex in currentUris.indices) {
                                    currentUris.removeAt(currentImageIndex)
                                    val newPhotoUri = if (currentUris.isEmpty()) null else currentUris.joinToString("|")
                                    val updatedPerson = freshPerson.copy(photoUri = newPhotoUri)
                                    viewModel.updatePerson(updatedPerson)
                                    Toast.makeText(context, "عکس با موفقیت حذف شد", Toast.LENGTH_SHORT).show()
                                    if (currentImageIndex >= currentUris.size && currentImageIndex > 0) {
                                        currentImageIndex--
                                    }
                                }
                                showPhotoDeleteConfirm = false
                                if (currentUris.isEmpty() || currentUris.size == 1) { // If it was 1, now it's 0.
                                    showFullPhotoDialog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) {
                            Text("حذف")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPhotoDeleteConfirm = false }) {
                            Text("انصراف", color = textColor)
                        }
                    }
                )
            }
        }
    }

    if (immersivePhotoIndex != null"""

if target3 in content:
    content = content.replace(target3, replacement3)
else:
    print("target3 not found")
    sys.exit(1)

with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
    f.write(content)

print("Applied successfully")
