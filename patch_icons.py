import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """            // Shadow / reference indicator with Eye Icon
            if (isShadow) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "نمایش کارت اصلی",
                    tint = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(6.dp)
                        .size(20.dp)
                        .align(Alignment.TopStart)
                        .clickable { onEyeClick() }
                )
            }"""

replacement = """            // Shadow / reference indicator with Ghost Icon
            if (isShadow) {
                Text(
                    text = "👻",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart) // Bottom left in LTR, Bottom right in RTL
                        .padding(8.dp)
                        .clickable { onEyeClick() }
                )
            }"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched ghost icon")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
