with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

gesture_func = """

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.PI

suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTreeTransformGestures(
    panZoomLock: Boolean = false,
    onGesture: (centroid: androidx.compose.ui.geometry.Offset, pan: androidx.compose.ui.geometry.Offset, zoom: Float, rotation: Float) -> Unit,
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = androidx.compose.ui.geometry.Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            // We ignore canceled state so we can zoom even if children consume the down event!
            val canceled = false 
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = 0f // We don't need rotation
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val panMotion = pan.getDistance()

                    if (
                        zoomMotion > touchSlop ||
                            panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    if (zoomChange != 1f || panChange != androidx.compose.ui.geometry.Offset.Zero) {
                        onGesture(centroid, panChange, zoomChange, 0f)
                    }
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}
"""

content = content.replace("import androidx.compose.foundation.gestures.detectTransformGestures", "import androidx.compose.foundation.gestures.detectTransformGestures")

if "fun String.toFarsiNumbers" in content and "suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTreeTransformGestures" not in content:
    content += gesture_func
    
    # Now replace detectTransformGestures with detectTreeTransformGestures
    content = content.replace("detectTransformGestures { _, pan, zoom, _ ->", "detectTreeTransformGestures { _, pan, zoom, _ ->")
    
    with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
        f.write(content)
    print("Fixed gestures successfully")
else:
    print("Already added or missing marker")

