with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

target = """                if (person.birthDate != null || person.isDeceased) {
                    val ageDisplay = formatLifeDates(person.birthDate, person.deathDate, person.isDeceased, 1405)
                    
                    Text(
                        text = ageDisplay.toFarsiNumbers(),
                        fontSize = 10.sp,
                        color = Color(0xFF455A64), // Extremely legible slate gray for year
                        fontWeight = FontWeight.Bold
                    )
                }"""

replacement = """                if (person.birthDate != null || person.isDeceased) {
                    val ageDisplay = formatLifeYearsOnlyLTR(person.birthDate, person.deathDate, person.isDeceased)
                    
                    Text(
                        text = ageDisplay.toFarsiNumbers(),
                        fontSize = 10.sp,
                        color = Color(0xFF455A64), // Extremely legible slate gray for year
                        fontWeight = FontWeight.Bold
                    )
                }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced successfully")
else:
    print("Target not found")
