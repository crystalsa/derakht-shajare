with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

bad_imports = """import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.PI"""

if bad_imports in content:
    content = content.replace(bad_imports, "")
    
    # insert them after package com.example.ui.screens
    content = content.replace("package com.example.ui.screens", "package com.example.ui.screens\n\n" + bad_imports)
    
    with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
        f.write(content)
    print("Fixed imports successfully")
else:
    print("Not found")

