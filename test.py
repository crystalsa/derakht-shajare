with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'r') as f:
    content = f.read()
import re
match = re.search(r'fun linkExistingSpouse.*?fun addSpouseToPerson', content, re.DOTALL)
if match:
    print(repr(match.group(0)))
