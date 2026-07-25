with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'r') as f:
    content = f.read()

import re
content = re.sub(r'    }\n    }\n\n    fun addSpouseToPerson', '    }\n\n    fun addSpouseToPerson', content)

with open('app/src/main/java/com/example/viewmodel/FamilyViewModel.kt', 'w') as f:
    f.write(content)
