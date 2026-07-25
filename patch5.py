import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

target = """                // Draw lines between spouses and child relations
                for (rel in relationships) {
                    val isSpouse = isSpouseRelation(rel.type)
                    var pos1Str = rel.personId1.toString()
                    var pos2Str = rel.personId2.toString()
                    
                    if (isSpouse) {
                        if (positions.containsKey("shadow_${rel.personId1}_${rel.personId2}")) {
                            pos1Str = "shadow_${rel.personId1}_${rel.personId2}"
                        } else if (positions.containsKey("shadow_${rel.personId2}_${rel.personId1}")) {
                            pos2Str = "shadow_${rel.personId2}_${rel.personId1}"
                        }
                    }

                    val pos1 = positions[pos1Str]
                    val pos2 = positions[pos2Str]
                    if (pos1 != null && pos2 != null) {"""

replacement = """                // Draw lines between spouses and child relations
                for (rel in relationships) {
                    val isSpouse = isSpouseRelation(rel.type)
                    
                    val pairsToDraw = mutableListOf<Pair<String, String>>()
                    if (isSpouse) {
                        val p1 = rel.personId1.toString()
                        val p2 = rel.personId2.toString()
                        val s1 = "shadow_${rel.personId1}_${rel.personId2}"
                        val s2 = "shadow_${rel.personId2}_${rel.personId1}"
                        
                        var drawn = false
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
                        }
                    } else {
                        pairsToDraw.add(rel.personId1.toString() to rel.personId2.toString())
                    }

                    for ((pos1Str, pos2Str) in pairsToDraw) {
                        val pos1 = positions[pos1Str]
                        val pos2 = positions[pos2Str]
                        if (pos1 != null && pos2 != null) {"""

content = content.replace(target, replacement)

target_end = """                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Render card nodes above lines"""

replacement_end = """                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }

            // Render card nodes above lines"""

content = content.replace(target_end, replacement_end)

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
    f.write(content)
print("done")
