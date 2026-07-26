with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

target = """                                                     text = if (featuredPerson != null) {
                                                         val birth = featuredPerson.birthDate?.split("-")?.firstOrNull() ?: ""
                                                         val death = if (featuredPerson.isDeceased) featuredPerson.deathDate?.split("-")?.firstOrNull() ?: "؟" else ""
                                                         val prefix = if (focusPersonId != null) "شخص برجسته" else "سرشاخه خاندان"
                                                         (if (featuredPerson.isDeceased) "$prefix ($birth - $death)" else "$prefix (متولد $birth)").toFarsiNumbers()
                                                     } else "آغازگر شجره‌نامه","""

replacement = """                                                     text = if (featuredPerson != null) {
                                                         val prefix = if (focusPersonId != null) "شخص برجسته" else "سرشاخه خاندان"
                                                         val dateStr = formatLifeDates(featuredPerson.birthDate, featuredPerson.deathDate, featuredPerson.isDeceased, 1405)
                                                         "$prefix - $dateStr".toFarsiNumbers()
                                                     } else "آغازگر شجره‌نامه","""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced featured person age successfully")
else:
    print("Target not found")
