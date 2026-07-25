import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """                        var drawn = false
                        if (positions.containsKey(p1) && positions.containsKey(s2)) {
                            pairsToDraw.add(p1 to s2)
                            drawn = true
                        }
                        if (positions.containsKey(p2) && positions.containsKey(s1)) {
                            pairsToDraw.add(p2 to s1)
                            drawn = true
                        }
                        if (!drawn && positions.containsKey(p1) && positions.containsKey(p2)) {
                            pairsToDraw.add(p1 to p2)
                        }"""

replacement = """                        var drawn = false
                        if (positions.containsKey(p1) && positions.containsKey(s2)) {
                            pairsToDraw.add(p1 to s2)
                            drawn = true
                        }
                        if (positions.containsKey(p2) && positions.containsKey(s1)) {
                            pairsToDraw.add(p2 to s1)
                            drawn = true
                        }
                        if (!drawn && positions.containsKey(p1) && positions.containsKey(p2)) {
                            pairsToDraw.add(p1 to p2)
                        }"""

replacement = target.replace(
    """                        if (positions.containsKey(p2) && positions.containsKey(s1)) {
                            pairsToDraw.add(p2 to s1)
                            drawn = true
                        }""",
    """                        if (positions.containsKey(p2) && positions.containsKey(s1)) {
                            pairsToDraw.add(p2 to s1)
                            drawn = true
                        }"""
)

# Wait, what I meant is:
# We should ALWAYS check both, and if EITHER is drawn, set drawn = true. Which the current code does!
# Oh. If it checks p1-s2 and then p2-s1, and BOTH are true, it adds BOTH to pairsToDraw!
# So both lines SHOULD be drawn!
