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
        )
        
        spousePairs.forEachIndexed { index, pair ->
            val color = heartColors[index % heartColors.size]
            map[pair.first] = color
            map[pair.second] = color
        }"""

replacement = """        val heartColors = listOf(
            0xFFE91E63, 0xFF3F51B5, 0xFF4CAF50, 0xFFFF9800, 0xFF9C27B0, 0xFF00BCD4, 0xFFFFEB3B, 0xFF795548,
            0xFFF44336, 0xFF2196F3, 0xFF8BC34A, 0xFFFF5722, 0xFF673AB7, 0xFF009688, 0xFFFFC107, 0xFF607D8B,
            0xFFE040FB, 0xFF03A9F4, 0xFFCDDC39, 0xFFFF7043, 0xFF512DA8, 0xFF00796B, 0xFFFBC02D, 0xFF5D4037,
            0xFFC2185B, 0xFF1976D2, 0xFF689F38, 0xFFE64A19, 0xFF7B1FA2, 0xFF0097A7, 0xFFF57C00, 0xFF455A64,
            0xFFD81B60, 0xFF0288D1, 0xFF9CCC65, 0xFFF4511E, 0xFF303F9F, 0xFF26A69A, 0xFFFFCA28, 0xFF8D6E63,
            0xFFAD1457, 0xFF1565C0, 0xFF558B2F, 0xFFD84315, 0xFF4527A0, 0xFF00838F, 0xFFF39C12, 0xFF37474F,
            0xFFEC407A, 0xFF29B6F6, 0xFF7CB342, 0xFFFF8A65, 0xFF5E35B1, 0xFF26C6DA, 0xFFFFD54F, 0xFF6D4C41,
            0xFF880E4F, 0xFF0D47A1, 0xFF33691E, 0xFFBF360C, 0xFF311B92, 0xFF006064, 0xFFE67E22, 0xFF263238
        ).map { Color(it) }
        
        // Shuffle based on a fixed seed so it's consistent across recompositions but random-looking
        val random = java.util.Random(42)
        val shuffledColors = heartColors.shuffled(random)
        
        spousePairs.forEachIndexed { index, pair ->
            val color = shuffledColors[index % shuffledColors.size]
            map[pair.first] = color
            map[pair.second] = color
        }"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched 64 colors")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
