import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """        val heartColors = listOf(
            Color(0xFFE91E63), // Pink
            Color(0xFFFF2D55), // Red-pink
            Color(0xFFFF3B30), // System Red
            Color(0xFF9C27B0), // Purple
            Color(0xFFFF9500), // Orange
            Color(0xFFE040FB), // Magenta
            Color(0xFF007AFF), // Blue
            Color(0xFF4CD964)  // Green
        )"""

replacement = """        val heartColors = listOf(
            Color(0xFFE91E63), // Pink
            Color(0xFFFF2D55), // Red-pink
            Color(0xFFFF3B30), // System Red
            Color(0xFF9C27B0), // Purple
            Color(0xFFFF9500), // Orange
            Color(0xFFE040FB), // Magenta
            Color(0xFF007AFF), // Blue
            Color(0xFF4CD964), // Green
            Color(0xFF00BCD4), // Cyan
            Color(0xFF009688), // Teal
            Color(0xFFFFC107), // Amber
            Color(0xFF3F51B5), // Indigo
            Color(0xFF795548), // Brown
            Color(0xFF607D8B), // Blue Grey
            Color(0xFF673AB7), // Deep Purple
            Color(0xFF8BC34A), // Light Green
            Color(0xFFFF5722), // Deep Orange
            Color(0xFFCDDC39), // Lime
            Color(0xFFE91E63), // Pink (Repeated, but more colors are good)
            Color(0xFFF44336), // Red
            Color(0xFF2196F3), // Blue
            Color(0xFF4CAF50), // Green
            Color(0xFFFFEB3B), // Yellow
            Color(0xFF9E9E9E)  // Grey
        )"""

if target in content:
    content = content.replace(target, replacement)
    print("Added more colors")
else:
    print("Could not find target")
    
with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
