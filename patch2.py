import re

with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'r') as f:
    content = f.read()

new_func = """
    fun linkExistingSpouse(personId1: Long, personId2: Long, relationshipType: String = "Spouse") {
        androidx.lifecycle.viewModelScope.launch {
            repository.insertRelationship(com.example.data.Relationship(personId1 = personId1, personId2 = personId2, type = relationshipType))
            loadFamilyData()
        }
    }
"""

if "fun linkExistingSpouse" not in content:
    content = content.replace("fun addSpouseToPerson", new_func + "\n    fun addSpouseToPerson")
    with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'w') as f:
        f.write(content)
    print("ViewModel Patched")
