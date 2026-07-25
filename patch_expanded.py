import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()"""

replacement = """    val stats by viewModel.statsState.collectAsStateWithLifecycle()
    val upcomingEvents by viewModel.upcomingEvents.collectAsStateWithLifecycle()
    
    var expandedGhostParents by remember { mutableStateOf(setOf<Long>()) }"""

if target in content:
    content = content.replace(target, replacement)
    print("Added expandedGhostParents state")

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)

