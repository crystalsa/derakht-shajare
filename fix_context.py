with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

content = content.replace("val context = androidx.compose.ui.platform.LocalContext.current\nfun AddParentsDialog(", "fun AddParentsDialog(")

target = "    var addFather by remember { mutableStateOf(existingFather == null) }"
replacement = "    val context = androidx.compose.ui.platform.LocalContext.current\n    var addFather by remember { mutableStateOf(existingFather == null) }"

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
        f.write(content)
    print("Fixed context successfully")
else:
    print("Target not found")
