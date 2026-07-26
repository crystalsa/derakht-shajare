with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

target = """                                val info = \"\"\"
                                    نام: ${person.fullName}
                                    جنسیت: ${if (person.gender == "Male") "آقا" else "خانم"}
                                    تاریخ تولد: ${person.birthDate ?: "ثبت نشده"}
                                    محل زندگی: ${person.birthPlace ?: "ثبت نشده"}
                                    شغل: ${person.occupation ?: "ثبت نشده"}
                                    توضیحات: ${person.biography ?: "ثبت نشده"}
                                \"\"\".trimIndent()"""

replacement = """                                val dateLabel = if (person.isDeceased) "تاریخ حیات" else "تاریخ تولد"
                                val dateValue = if (person.birthDate != null || person.isDeceased) formatLifeDates(person.birthDate, person.deathDate, person.isDeceased, 1405).toFarsiNumbers() else "ثبت نشده"
                                val info = \"\"\"
                                    نام: ${person.fullName}
                                    جنسیت: ${if (person.gender == "Male") "آقا" else "خانم"}
                                    $dateLabel: $dateValue
                                    محل زندگی: ${person.birthPlace ?: "ثبت نشده"}
                                    شغل: ${person.occupation ?: "ثبت نشده"}
                                    توضیحات: ${person.biography ?: "ثبت نشده"}
                                \"\"\".trimIndent()"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
        f.write(content)
    print("Replaced clipboard age successfully")
else:
    print("Target not found")
