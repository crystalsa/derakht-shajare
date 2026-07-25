with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace("    }\n    }\n    fun addSpouseToPerson", "    }\n\n    fun addSpouseToPerson")

with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'w') as f:
    f.write(content)
