fun isValidDates(birthDate: String?, deathDate: String?): Boolean {
    if (birthDate.isNullOrBlank() || deathDate.isNullOrBlank()) return true
    
    val bParts = birthDate.split("/","-").mapNotNull { it.toIntOrNull() }
    val dParts = deathDate.split("/","-").mapNotNull { it.toIntOrNull() }
    
    val bYear = bParts.getOrNull(0)
    val bMonth = bParts.getOrNull(1) ?: 1
    val bDay = bParts.getOrNull(2) ?: 1
    
    val dYear = dParts.getOrNull(0)
    val dMonth = dParts.getOrNull(1) ?: 1
    val dDay = dParts.getOrNull(2) ?: 1
    
    if (bYear != null && dYear != null) {
        if (dYear < bYear) return false
        if (dYear == bYear) {
            if (dMonth < bMonth) return false
            if (dMonth == bMonth) {
                if (dDay < bDay) return false
            }
        }
    }
    return true
}

println(isValidDates("1370-01-01", "1360-01-01"))
println(isValidDates("1370", "1370"))
println(isValidDates("1370-02", "1370-01"))
