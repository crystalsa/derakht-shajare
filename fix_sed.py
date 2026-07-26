import re

with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "r") as f:
    content = f.read()

bad_pattern = r"onClick = \{\n\s*if \(fIsDeceased \&\& fHasDeathDate \&\& fHasBirthDate \&\& \!validateBirthAndDeathDates\(fBirthDateInput, fDeathDateInput\)\) \{\n\s*Toast\.makeText\(context, \"تاریخ فوت پدر نمی‌تواند کوچکتر از تاریخ تولد او باشد\", Toast\.LENGTH_LONG\)\.show\(\)\n\s*return@Button\n\s*\}\n\s*if \(mIsDeceased \&\& mHasDeathDate \&\& mHasBirthDate \&\& \!validateBirthAndDeathDates\(mBirthDateInput, mDeathDateInput\)\) \{\n\s*Toast\.makeText\(context, \"تاریخ فوت مادر نمی‌تواند کوچکتر از تاریخ تولد او باشد\", Toast\.LENGTH_LONG\)\.show\(\)\n\s*return@Button\n\s*\}"

parts = re.split(bad_pattern, content)
new_content = ""
for i, part in enumerate(parts):
    if i > 0:
        if "val father = if (existingFather == null" in part[0:100]:
            # This is the one we want to keep!
            new_content += """onClick = {
                        if (fIsDeceased && fHasDeathDate && fHasBirthDate && !validateBirthAndDeathDates(fBirthDateInput, fDeathDateInput)) {
                            Toast.makeText(context, "تاریخ فوت پدر نمی‌تواند کوچکتر از تاریخ تولد او باشد", Toast.LENGTH_LONG).show()
                            return@Button
                        }
                        if (mIsDeceased && mHasDeathDate && mHasBirthDate && !validateBirthAndDeathDates(mBirthDateInput, mDeathDateInput)) {
                            Toast.makeText(context, "تاریخ فوت مادر نمی‌تواند کوچکتر از تاریخ تولد او باشد", Toast.LENGTH_LONG).show()
                            return@Button
                        }"""
        else:
            new_content += "onClick = {"
    new_content += part

with open("app/src/main/java/com/example/ui/screens/DashboardScreen.kt", "w") as f:
    f.write(new_content)
