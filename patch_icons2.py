import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """            // Expand ghost children toggle
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
                        imageVector = if (isGhostChildrenExpanded) Icons.Default.Remove else Icons.Default.Add,"""

replacement = """            // Expand ghost children toggle
            if (hasGhostChildren) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 8.dp, start = if (isShadow) 36.dp else 8.dp)
                        .size(24.dp)
                        .background(Color.Gray.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp))
                        .clickable { onToggleGhostChildren() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGhostChildrenExpanded) Icons.Default.Remove else Icons.Default.Add,"""

if target in content:
    content = content.replace(target, replacement)
    print("Patched plus icon")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
