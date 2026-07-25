import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

if "import androidx.compose.material.icons.filled.Visibility" not in content:
    content = content.replace("import androidx.compose.material.icons.filled.Add",
                              "import androidx.compose.material.icons.filled.Add\nimport androidx.compose.material.icons.filled.Visibility\nimport androidx.compose.material.icons.filled.Remove")
    with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
        f.write(content)
    print("Added imports")

