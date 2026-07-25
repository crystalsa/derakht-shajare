import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """            // Shadow / reference indicator
            if (isShadow) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "پیوند فامیلی",
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.padding(6.dp).size(16.dp).align(Alignment.TopStart)
                )
            }"""

replacement = """            // Shadow / reference indicator with Eye Icon
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
            }
            
            // Expand ghost children toggle
            if (hasGhostChildren) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(24.dp)
                        .background(Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                        .clickable { onToggleGhostChildren() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGhostChildrenExpanded) Icons.Default.Remove else Icons.Default.Add,
                        contentDescription = "نمایش/مخفی فرزندان گوست",
                        tint = Color.Gray.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }"""

if target in content:
    content = content.replace(target, replacement)
    print("Replaced inside card")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)

