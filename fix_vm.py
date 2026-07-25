import re

with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'r') as f:
    content = f.read()

new_func = """    fun linkExistingSpouse(personId1: Long, personId2: Long, relationshipType: String = "Spouse") {
        viewModelScope.launch {
            repository.insertRelationship(Relationship(personId1 = personId1, personId2 = personId2, type = relationshipType))
        }
    }"""

# Replace the bad function
content = re.sub(r'    fun linkExistingSpouse.*?    }', new_func, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'w') as f:
    f.write(content)
