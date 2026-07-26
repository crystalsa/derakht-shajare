fun String.toFarsiNumbers(): String {
    var result = this
    val englishNumbers = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
    val persianNumbers = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")
    for (i in englishNumbers.indices) {
        result = result.replace(englishNumbers[i], persianNumbers[i])
    }
    return result
}
val str = "\u202D1370 - 1390\u202C (20 ساله)".toFarsiNumbers()
println(str)
