import re

with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'r') as f:
    content = f.read()

with open('AddSpouseDialog.kt', 'r') as f:
    new_dialog = f.read()

# Find the start of AddSpouseDialog
pattern = r'@Composable\s*fun AddSpouseDialog\(.*?^}\n\n@Composable\s*fun AddParentsDialog\('
match = re.search(pattern, content, re.DOTALL | re.MULTILINE)

if match:
    new_content = content[:match.start()] + new_dialog + "\n@Composable\nfun AddParentsDialog(" + content[match.end():]
    with open('app/src/main/java/com/example/ui/screens/DashboardScreen.kt', 'w') as f:
        f.write(new_content)
    print("Patched successfully")
else:
    print("Could not find match")
