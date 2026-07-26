with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

target1 = """                if (person.birthDate != null) {
                    item {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("تاریخ تولد:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                            Text(person.birthDate, color = textColor, fontSize = 12.sp)
                        }
                    }
                }"""

replacement1 = """                if (person.birthDate != null || person.isDeceased) {
                    item {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(if (person.isDeceased) "تاریخ حیات:" else "تاریخ تولد:", fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.sp)
                            Text(formatLifeDates(person.birthDate, person.deathDate, person.isDeceased, 1405).toFarsiNumbers(), color = textColor, fontSize = 12.sp)
                        }
                    }
                }"""

target2 = """                                if (person.deathDate != null) {
                                    Text("تاریخ فوت: ${person.deathDate}", color = textColor, fontSize = 11.sp)
                                }"""

replacement2 = ""

if target1 in content:
    content = content.replace(target1, replacement1)
    content = content.replace(target2, replacement2)
    with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced details sheet age successfully")
else:
    print("Target 1 not found")
