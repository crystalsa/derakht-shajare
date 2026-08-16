package com.example.utils

import com.example.data.Person
import com.example.data.Relationship
import java.util.*

object RelationshipCalculator {

    data class PathNode(val person: Person, val incomingRelType: String? = null, val parentNode: PathNode? = null)

    /**
     * Determines the exact directed relationship from Person 'from' to Person 'to'.
     * Returns "FATHER", "MOTHER", "SON", "DAUGHTER", "SPOUSE", "EX_SPOUSE",
     * "ADOPTIVE_FATHER", "ADOPTIVE_MOTHER", "ADOPTIVE_SON", "ADOPTIVE_DAUGHTER", or null.
     */
    fun getDirectedRelation(from: Person, to: Person, allRelationships: List<Relationship>): String? {
        for (rel in allRelationships) {
            if (rel.personId1 == from.id && rel.personId2 == to.id) {
                return when (rel.type) {
                    "Spouse", "SecondSpouse" -> "SPOUSE"
                    "Divorced", "SecondSpouse_Divorced" -> "EX_SPOUSE"
                    "Parent-Child" -> if (to.gender == "Male") "SON" else "DAUGHTER"
                    "Adoptive-Parent-Child" -> if (to.gender == "Male") "ADOPTIVE_SON" else "ADOPTIVE_DAUGHTER"
                    else -> null
                }
            }
            if (rel.personId1 == to.id && rel.personId2 == from.id) {
                return when (rel.type) {
                    "Spouse", "SecondSpouse" -> "SPOUSE"
                    "Divorced", "SecondSpouse_Divorced" -> "EX_SPOUSE"
                    "Parent-Child" -> if (to.gender == "Male") "FATHER" else "MOTHER"
                    "Adoptive-Parent-Child" -> if (to.gender == "Male") "ADOPTIVE_FATHER" else "ADOPTIVE_MOTHER"
                    else -> null
                }
            }
        }
        return null
    }

    /**
     * Finds the shortest path between Person A and Person B in the family graph.
     */
    fun findShortestPath(
        personA: Person,
        personB: Person,
        allPersons: List<Person>,
        allRelationships: List<Relationship>
    ): List<Pair<Person, String>>? {
        val paths = findAllPaths(personA, personB, allPersons, allRelationships, maxDepth = 6, maxPaths = 50)
        return paths.minByOrNull { it.size }
    }

    /**
     * Finds all simple paths between Person A and Person B in the family graph up to maxDepth.
     */
    fun findAllPaths(
        personA: Person,
        personB: Person,
        allPersons: List<Person>,
        allRelationships: List<Relationship>,
        maxDepth: Int = 6,
        maxPaths: Int = 10
    ): List<List<Pair<Person, String>>> {
        if (personA.id == personB.id) return emptyList()

        val personMap = allPersons.associateBy { it.id }

        // Build adjacency map: PersonId -> List of Pair<NeighborPersonId, DirectedRelType>
        val adjList = mutableMapOf<Long, MutableList<Pair<Long, String>>>()
        for (rel in allRelationships) {
            val p1 = personMap[rel.personId1]
            val p2 = personMap[rel.personId2]
            if (p1 != null && p2 != null) {
                val dir1To2 = getDirectedRelation(p1, p2, allRelationships)
                if (dir1To2 != null) {
                    adjList.getOrPut(p1.id) { mutableListOf() }.add(Pair(p2.id, dir1To2))
                }
                val dir2To1 = getDirectedRelation(p2, p1, allRelationships)
                if (dir2To1 != null) {
                    adjList.getOrPut(p2.id) { mutableListOf() }.add(Pair(p1.id, dir2To1))
                }
            }
        }

        val resultPaths = mutableListOf<List<Pair<Person, String>>>()

        fun dfs(
            currentPersonId: Long,
            currentPath: MutableList<Pair<Person, String>>,
            visitedIds: MutableSet<Long>,
            depth: Int
        ) {
            if (resultPaths.size >= maxPaths || depth > maxDepth) return

            // Branch-and-bound optimization: prune branches that already exceed the shortest path found so far
            val shortestFound = resultPaths.minOfOrNull { it.size }
            if (shortestFound != null && currentPath.size > shortestFound) return

            if (currentPersonId == personB.id) {
                resultPaths.add(ArrayList(currentPath))
                return
            }

            val neighbors = adjList[currentPersonId] ?: return
            for ((neighborId, dirRel) in neighbors) {
                if (!visitedIds.contains(neighborId)) {
                    val neighborPerson = personMap[neighborId] ?: continue
                    visitedIds.add(neighborId)
                    currentPath.add(Pair(neighborPerson, dirRel))

                    dfs(neighborId, currentPath, visitedIds, depth + 1)

                    currentPath.removeAt(currentPath.size - 1)
                    visitedIds.remove(neighborId)
                }
            }
        }

        val startVisited = mutableSetOf(personA.id)
        val startPath = mutableListOf(Pair(personA, "Start"))
        dfs(personA.id, startPath, startVisited, 0)

        return resultPaths.sortedBy { it.size }
    }

    private val STANDARD_TERMS = setOf(
        "پدر", "مادر", "پسر", "دختر",
        "شوهر", "زن", "همسر", "همسر سابق", "هوو",
        "برادر", "خواهر", "برادر ناتنی", "خواهر ناتنی", "برادر ناتنی هم‌پدر", "برادر ناتنی هم‌مادر", "خواهر ناتنی هم‌پدر", "خواهر ناتنی هم‌مادر",
        "پدر بزرگ", "مادر بزرگ", "پدربزرگ", "مادربزرگ", "پدربزرگ پدری", "مادربزرگ پدری", "پدربزرگ مادری", "مادربزرگ مادری",
        "جد پدری", "جده پدری", "جد مادری", "جده مادری", "جد بزرگ", "جده بزرگ", "جد اعلا", "جده اعلا",
        "نوه", "نوه پسری", "نوه دختری", "نوه‌ی پسری", "نوه‌ی دختری", "نتیجه", "نبیره", "ندیده",
        "عمو", "عمه", "دایی", "خاله",
        "برادرزاده", "خواهرزاده",
        "پسرعمو", "دخترعمو", "پسرعمه", "دخترعمه",
        "پسردایی", "دختردایی", "پسرخاله", "دخترخاله",
        "پدر همسر", "مادر همسر", "پدرزن", "مادرزن", "پدرشوهر", "مادرشوهر",
        "برادر همسر", "خواهر همسر", "برادرزن", "خواهرزن", "برادرشوهر", "خواهرشوهر",
        "داماد", "عروس", "داماد نوه", "عروس نوه", "داماد نتیجه", "عروس نتیجه", "داماد نبیره", "عروس نبیره",
        "زن برادر", "شوهر خواهر", "باجناق", "جاری",
        "ناپدری", "نامادری", "پدرخوانده", "مادرخوانده", "فرزندخوانده", "پسرخوانده", "دخترخوانده",
        "پسر همسر", "دختر همسر", "ربیب", "ربیبه", "نوه‌ی همسر", "نوه همسر",
        "زن عمو", "شوهر عمه", "زن دایی", "شوهر خاله",
        "عموی همسر", "عمه همسر", "دایی همسر", "خاله همسر",
        "پدربزرگ همسر", "مادربزرگ همسر", "جد اعلای همسر", "جده اعلای همسر",
        "برادرزاده همسر", "خواهرزاده همسر",
        "زن برادرزاده", "شوهر برادرزاده", "زن خواهرزاده", "شوهر خواهرزاده",
        "زن برادر همسر", "شوهر خواهر همسر",
        "عموی پدر", "عمه پدر", "دایی پدر", "خاله پدر",
        "عموی مادر", "عمه مادر", "دایی مادر", "خاله مادر",
        "زن عموی پدر", "شوهر عمه پدر", "زن دایی پدر", "شوهر خاله پدر",
        "زن عموی مادر", "شوهر عمه مادر", "زن دایی مادر", "شوهر خاله مادر",
        "پسر عموی پدر", "دختر عموی پدر", "پسر عمه پدر", "دختر عمه پدر",
        "پسر دایی پدر", "دختر دایی پدر", "پسر خاله پدر", "دختر خاله پدر",
        "پسر عموی مادر", "دختر عموی مادر", "پسر عمه مادر", "دختر عمه مادر",
        "پسر دایی مادر", "دختر دایی مادر", "پسر خاله مادر", "دختر خاله مادر",
        "نوه‌ی برادر", "نوه‌ی خواهر", "نوه برادر", "نوه خواهر",
        "زن پسرعمو", "شوهر دخترعمو", "زن پسردایی", "شوهر دختردایی", "زن پسرخاله", "شوهر دخترخاله", "زن پسرعمه", "شوهر دخترعمه",
        "پسر پسرعمو", "دختر پسرعمو", "پسر پسردایی", "دختر پسردایی", "پسر پسرخاله", "دختر پسرخاله", "پسر پسرعمه", "دختر پسرعمه",
        "پسر عموزاده", "دختر عموزاده", "پسر دایی‌زاده", "دختر دایی‌زاده", "پسر خاله‌زاده", "دختر خاله‌زاده", "پسر عمه‌زاده", "دختر عمه‌زاده",
        "پسرعموی همسر", "دخترعموی همسر", "پسردایی همسر", "دختردایی همسر", "پسرخاله‌ی همسر", "دخترخاله‌ی همسر", "پسرعمه‌ی همسر", "دخترعمه‌ی همسر"
    )

    private fun isStandardTerm(term: String): Boolean {
        val clean = cleanTerm(term)
        if (clean.isBlank() || clean == "خویشاوند" || clean == "نامشخص") return false
        if (STANDARD_TERMS.contains(clean) || STANDARD_TERMS.contains(term)) return true
        if (clean.endsWith(" همسر") || clean.endsWith(" نوه") || clean.endsWith(" برادر") || clean.endsWith(" خواهر") || clean.endsWith(" پدر") || clean.endsWith(" مادر")) {
            return true
        }
        return true
    }

    /**
     * Decodes the list of directed steps from B to A into a clean, natural Persian relationship label.
     */
    fun getRelationLabelFromSteps(steps: List<String>, personA: Person, personB: Person): String {
        val size = steps.size
        if (size == 0) return "خودِ شخص"

        fun isParent(s: String) = s == "FATHER" || s == "MOTHER" || s == "ADOPTIVE_FATHER" || s == "ADOPTIVE_MOTHER"
        fun isFather(s: String) = s == "FATHER" || s == "ADOPTIVE_FATHER"
        fun isMother(s: String) = s == "MOTHER" || s == "ADOPTIVE_MOTHER"
        fun isChild(s: String) = s == "SON" || s == "DAUGHTER" || s == "ADOPTIVE_SON" || s == "ADOPTIVE_DAUGHTER"
        fun isSon(s: String) = s == "SON" || s == "ADOPTIVE_SON"
        fun isDaughter(s: String) = s == "DAUGHTER" || s == "ADOPTIVE_DAUGHTER"
        fun isSpouse(s: String) = s == "SPOUSE"

        // 1 STEP (Direct)
        if (size == 1) {
            return when (steps[0]) {
                "FATHER" -> "پدرِ"
                "MOTHER" -> "مادرِ"
                "SON" -> "پسرِ"
                "DAUGHTER" -> "دخترِ"
                "SPOUSE" -> if (personA.gender == "Male") "شوهرِ" else "زنِ"
                "EX_SPOUSE" -> "همسر سابقِ"
                "ADOPTIVE_FATHER" -> "پدرخوانده‌ی"
                "ADOPTIVE_MOTHER" -> "مادرخوانده‌ی"
                "ADOPTIVE_SON" -> "پسرخوانده‌ی"
                "ADOPTIVE_DAUGHTER" -> "دخترخوانده‌ی"
                else -> "خویشاوندِ"
            }
        }

        // 2 STEPS
        if (size == 2) {
            val (s1, s2) = steps

            // Sibling: Parent -> Child (B -> Parent -> Sibling A)
            if (isParent(s1) && isChild(s2)) {
                return if (personA.gender == "Male") "برادرِ" else "خواهرِ"
            }

            // Grandparent: Parent -> Parent (B -> Parent -> Grandparent A)
            if (isParent(s1) && isParent(s2)) {
                return if (isFather(s1)) {
                    if (isFather(s2)) "پدربزرگِ پدریِ" else "مادربزرگِ پدریِ"
                } else {
                    if (isFather(s2)) "پدربزرگِ مادریِ" else "مادربزرگِ مادریِ"
                }
            }

            // Grandchild: Child -> Child (B -> Child -> Grandchild A)
            if (isChild(s1) && isChild(s2)) {
                return if (isSon(s1)) "نوه‌ی پسریِ" else "نوه‌ی دختریِ"
            }

            // Spouse's Parent: Spouse -> Parent (B -> Spouse -> Parent A)
            if (isSpouse(s1) && isParent(s2)) {
                return if (personB.gender == "Male") {
                    if (isFather(s2)) "پدرزنِ" else "مادرزنِ"
                } else if (personB.gender == "Female") {
                    if (isFather(s2)) "پدرشوهرِ" else "مادرشوهرِ"
                } else {
                    if (isFather(s2)) "پدر همسرِ" else "مادر همسرِ"
                }
            }

            // Child's Spouse (عروس / داماد): Child -> Spouse (B -> Child -> Spouse A)
            if (isChild(s1) && isSpouse(s2)) {
                return if (personA.gender == "Male") "دامادِ" else "عروسِ"
            }

            // Spouse's Child (Stepchild / ربیب و ربیبه): Spouse -> Child (B -> Spouse -> Stepchild A)
            if (isSpouse(s1) && isChild(s2)) {
                return if (personA.gender == "Male") "پسر همسرِ (ربیبِ)" else "دختر همسرِ (ربیبه‌ی)"
            }

            // Stepparent: Parent -> Spouse
            if (isParent(s1) && isSpouse(s2)) {
                return if (personA.gender == "Male") "ناپدریِ" else "نامادریِ"
            }

            // Co-wife (هوو): Spouse -> Spouse (Two wives of the same husband)
            if (isSpouse(s1) && isSpouse(s2)) {
                if (personB.gender == "Female" && personA.gender == "Female") {
                    return "هوویِ"
                }
            }
        }

        // 3 STEPS
        if (size == 3) {
            val (s1, s2, s3) = steps

            // Great-grandparent (جد اعلا / جد بزرگ): Parent -> Parent -> Parent
            if (isParent(s1) && isParent(s2) && isParent(s3)) {
                return if (personA.gender == "Male") "جد اعلایِ" else "جدّه اعلایِ"
            }

            // Great-grandchild (نتیجه): Child -> Child -> Child (فرزند نوه)
            if (isChild(s1) && isChild(s2) && isChild(s3)) {
                return "نتیجه‌ی"
            }

            // Aunt / Uncle: Parent -> Parent -> Child (B -> Parent -> Grandparent -> Aunt/Uncle A)
            if (isParent(s1) && isParent(s2) && isChild(s3)) {
                return if (isFather(s1)) {
                    if (isSon(s3)) "عمویِ" else "عمه‌ی"
                } else {
                    if (isSon(s3)) "داییِ" else "خاله‌ی"
                }
            }

            // Niece / Nephew: Parent -> Child -> Child (B -> Parent -> Sibling -> Niece/Nephew A)
            if (isParent(s1) && isChild(s2) && isChild(s3)) {
                val isBrother = isSon(s2)
                return if (isBrother) "برادرزاده‌ی" else "خواهرزاده‌ی"
            }

            // Sibling's Spouse (زن برادر / شوهر خواهر): Parent -> Child -> Spouse
            if (isParent(s1) && isChild(s2) && isSpouse(s3)) {
                return if (isSon(s2)) "زن برادرِ" else "شوهر خواهرِ"
            }

            // Spouse's Sibling (برادرزن / خواهرزن / برادرشوهر / خواهرشوهر): Spouse -> Parent -> Child
            if (isSpouse(s1) && isParent(s2) && isChild(s3)) {
                return if (personB.gender == "Male") {
                    if (isSon(s3)) "برادرزنِ" else "خواهرزنِ"
                } else if (personB.gender == "Female") {
                    if (isSon(s3)) "برادرشوهرِ" else "خواهرشوهرِ"
                } else {
                    if (isSon(s3)) "برادر همسرِ" else "خواهر همسرِ"
                }
            }

            // Grandchild's Spouse (عروس نوه / داماد نوه): Child -> Child -> Spouse (B -> Child -> Grandchild -> Spouse A)
            if (isChild(s1) && isChild(s2) && isSpouse(s3)) {
                return if (personA.gender == "Male") "دامادِ نوه‌ی" else "عروسِ نوه‌ی"
            }

            // Spouse's Grandparent (پدربزرگ همسر / مادربزرگ همسر): Spouse -> Parent -> Parent (B -> Spouse -> Parent -> Grandparent A)
            if (isSpouse(s1) && isParent(s2) && isParent(s3)) {
                return if (isFather(s3)) "پدربزرگِ همسرِ" else "مادربزرگِ همسرِ"
            }

            // Spouse's Grandchild: Spouse -> Child -> Child
            if (isSpouse(s1) && isChild(s2) && isChild(s3)) {
                return "نوه‌ی همسرِ"
            }

            // Stepparent's Child (Stepsibling): Parent -> Spouse -> Child
            if (isParent(s1) && isSpouse(s2) && isChild(s3)) {
                val side = if (isFather(s1)) "هم‌پدر" else "هم‌مادر"
                return if (personA.gender == "Male") "برادر ناتنیِ ($sideِ)" else "خواهر ناتنیِ ($sideِ)"
            }
        }

        // 4 STEPS
        if (size == 4) {
            val (s1, s2, s3, s4) = steps

            // Great-great-grandchild (نبیره): Child -> Child -> Child -> Child (فرزند نتیجه)
            if (isChild(s1) && isChild(s2) && isChild(s3) && isChild(s4)) {
                return "نبیره‌ی"
            }

            // Great-great-grandparent (جد اعلا): Parent -> Parent -> Parent -> Parent
            if (isParent(s1) && isParent(s2) && isParent(s3) && isParent(s4)) {
                return if (personA.gender == "Male") "جد اعلایِ" else "جدّه اعلایِ"
            }

            // Great-Uncle / Great-Aunt (عموی پدر/مادر، عمه پدر/مادر، دایی پدر/مادر، خاله پدر/مادر): Parent -> Parent -> Parent -> Child
            if (isParent(s1) && isParent(s2) && isParent(s3) && isChild(s4)) {
                val parentWord = if (isFather(s1)) "پدر" else "مادر"
                val uncleWord = if (isFather(s2)) {
                    if (isSon(s4)) "عمویِ" else "عمه‌ی"
                } else {
                    if (isSon(s4)) "داییِ" else "خاله‌ی"
                }
                return "$uncleWord $parentWordِ"
            }

            // Cousins: Parent -> Parent -> Child -> Child (B -> Parent -> Grandparent -> Aunt/Uncle -> Cousin A)
            if (isParent(s1) && isParent(s2) && isChild(s3) && isChild(s4)) {
                val uncleAunt = if (isFather(s1)) {
                    if (isSon(s3)) "عمو" else "عمه"
                } else {
                    if (isSon(s3)) "دایی" else "خاله"
                }

                return when (uncleAunt) {
                    "عمو" -> if (isSon(s4)) "پسرعمویِ" else "دخترعمویِ"
                    "عمه" -> if (isSon(s4)) "پسرعمه‌ی" else "دخترعمه‌ی"
                    "دایی" -> if (isSon(s4)) "پسرداییِ" else "دخترداییِ"
                    "خاله" -> if (isSon(s4)) "پسرخاله‌ی" else "دخترخاله‌ی"
                    else -> "خویشاوندِ"
                }
            }

            // Aunt/Uncle spouse (زن عمو, شوهر عمه, زن دایی, شوهر خاله): Parent -> Parent -> Child -> Spouse
            if (isParent(s1) && isParent(s2) && isChild(s3) && isSpouse(s4)) {
                return if (isFather(s1)) {
                    if (isSon(s3)) "زن عمویِ" else "شوهر عمه‌ی"
                } else {
                    if (isSon(s3)) "زن داییِ" else "شوهر خاله‌ی"
                }
            }

            // Bajenagh / Jari / Sibling-in-law's spouse: Spouse -> Parent -> Child -> Spouse
            if (isSpouse(s1) && isParent(s2) && isChild(s3) && isSpouse(s4)) {
                if (personB.gender == "Male" && personA.gender == "Male") return "باجناقِ"
                if (personB.gender == "Female" && personA.gender == "Female") return "جاریِ"
                return if (personA.gender == "Male") "شوهر خواهر همسرِ" else "زن برادر همسرِ"
            }

            // Niece/Nephew's Spouse: Parent -> Child -> Child -> Spouse
            if (isParent(s1) && isChild(s2) && isChild(s3) && isSpouse(s4)) {
                return if (isSon(s2)) {
                    if (personA.gender == "Female") "زن برادرزاده‌ی" else "شوهر برادرزاده‌ی"
                } else {
                    if (personA.gender == "Female") "زن خواهرزاده‌ی" else "شوهر خواهرزاده‌ی"
                }
            }

            // Spouse's Niece/Nephew: Spouse -> Parent -> Child -> Child
            if (isSpouse(s1) && isParent(s2) && isChild(s3) && isChild(s4)) {
                return if (isSon(s3)) "برادرزاده‌ی همسرِ" else "خواهرزاده‌ی همسرِ"
            }

            // Spouse's Aunt/Uncle: Spouse -> Parent -> Parent -> Child
            if (isSpouse(s1) && isParent(s2) && isParent(s3) && isChild(s4)) {
                return if (isFather(s2)) {
                    if (isSon(s4)) "عمویِ همسرِ" else "عمه‌ی همسرِ"
                } else {
                    if (isSon(s4)) "داییِ همسرِ" else "خاله‌ی همسرِ"
                }
            }

            // Grand-niece / Grand-nephew (نوه برادر / نوه خواهر): Parent -> Child -> Child -> Child
            if (isParent(s1) && isChild(s2) && isChild(s3) && isChild(s4)) {
                return if (isSon(s2)) "نوه‌ی برادرِ" else "نوه‌ی خواهرِ"
            }

            // Great-grandchild's spouse (عروس نتیجه / داماد نتیجه): Child -> Child -> Child -> Spouse
            if (isChild(s1) && isChild(s2) && isChild(s3) && isSpouse(s4)) {
                return if (personA.gender == "Male") "دامادِ نتیجه‌ی" else "عروسِ نتیجه‌ی"
            }

            // Spouse's great-grandparent: Spouse -> Parent -> Parent -> Parent
            if (isSpouse(s1) && isParent(s2) && isParent(s3) && isParent(s4)) {
                return if (personA.gender == "Male") "جد اعلایِ همسرِ" else "جدّه اعلایِ همسرِ"
            }
        }

        // 5 STEPS
        if (size == 5) {
            val (s1, s2, s3, s4, s5) = steps

            // Great-great-great-grandchild (ندیده): Child -> Child -> Child -> Child -> Child (فرزند نبیره)
            if (isChild(s1) && isChild(s2) && isChild(s3) && isChild(s4) && isChild(s5)) {
                return "ندیده‌ی"
            }

            // Parents' Cousins' Children: Parent -> Parent -> Parent -> Child -> Child (پسرِ عموی پدر، دخترِ دایی مادر و ...)
            if (isParent(s1) && isParent(s2) && isParent(s3) && isChild(s4) && isChild(s5)) {
                val parentWord = if (isFather(s1)) "پدر" else "مادر"
                val uncleWord = if (isFather(s2)) {
                    if (isSon(s4)) "عمویِ" else "عمه‌ی"
                } else {
                    if (isSon(s4)) "داییِ" else "خاله‌ی"
                }
                val childPrefix = if (isSon(s5)) "پسرِ" else "دخترِ"
                return "$childPrefix $uncleWord $parentWordِ"
            }

            // Spouses of Parents' Aunts/Uncles: Parent -> Parent -> Parent -> Child -> Spouse (شوهرعمه پدر، زن عموی مادر و ...)
            if (isParent(s1) && isParent(s2) && isParent(s3) && isChild(s4) && isSpouse(s5)) {
                val parentWord = if (isFather(s1)) "پدر" else "مادر"
                val spouseRelation = if (isFather(s2)) {
                    if (isSon(s4)) "زن عمویِ" else "شوهر عمه‌ی"
                } else {
                    if (isSon(s4)) "زن داییِ" else "شوهر خاله‌ی"
                }
                return "$spouseRelation $parentWordِ"
            }

            // Cousin's child (فرزند پسرعمو/دختردایی/...): Parent -> Parent -> Child -> Child -> Child
            if (isParent(s1) && isParent(s2) && isChild(s3) && isChild(s4) && isChild(s5)) {
                val cousinBase = if (isFather(s1)) {
                    if (isSon(s3)) (if (isSon(s4)) "پسرعمویِ" else "دخترعمویِ")
                    else (if (isSon(s4)) "پسرعمه‌ی" else "دخترعمه‌ی")
                } else {
                    if (isSon(s3)) (if (isSon(s4)) "پسرداییِ" else "دخترداییِ")
                    else (if (isSon(s4)) "پسرخاله‌ی" else "دخترخاله‌ی")
                }
                return if (isSon(s5)) "پسرِ $cousinBase" else "دخترِ $cousinBase"
            }

            // Cousin's spouse (همسر پسرعمو/دخترخاله/...): Parent -> Parent -> Child -> Child -> Spouse
            if (isParent(s1) && isParent(s2) && isChild(s3) && isChild(s4) && isSpouse(s5)) {
                val cousinBase = if (isFather(s1)) {
                    if (isSon(s3)) (if (isSon(s4)) "پسرعمویِ" else "دخترعمویِ")
                    else (if (isSon(s4)) "پسرعمه‌ی" else "دخترعمه‌ی")
                } else {
                    if (isSon(s3)) (if (isSon(s4)) "پسرداییِ" else "دخترداییِ")
                    else (if (isSon(s4)) "پسرخاله‌ی" else "دخترخاله‌ی")
                }
                return if (personA.gender == "Female") "زن $cousinBase" else "شوهر $cousinBase"
            }

            // Spouse's cousin: Spouse -> Parent -> Parent -> Child -> Child
            if (isSpouse(s1) && isParent(s2) && isParent(s3) && isChild(s4) && isChild(s5)) {
                val cousinBase = if (isFather(s2)) {
                    if (isSon(s4)) (if (isSon(s5)) "پسرعمویِ" else "دخترعمویِ")
                    else (if (isSon(s5)) "پسرعمه‌ی" else "دخترعمه‌ی")
                } else {
                    if (isSon(s4)) (if (isSon(s5)) "پسرداییِ" else "دخترداییِ")
                    else (if (isSon(s5)) "پسرخاله‌ی" else "دخترخاله‌ی")
                }
                return "$cousinBase همسرِ"
            }

            // Great-great-grandchild's spouse (عروس نبیره / داماد نبیره): Child -> Child -> Child -> Child -> Spouse
            if (isChild(s1) && isChild(s2) && isChild(s3) && isChild(s4) && isSpouse(s5)) {
                return if (personA.gender == "Male") "دامادِ نبیره‌ی" else "عروسِ نبیره‌ی"
            }
        }

        return "خویشاوندِ"
    }

    fun cleanTerm(raw: String): String {
        var term = raw.trim()
        if (term.endsWith("ِ")) {
            term = term.substring(0, term.length - 1).trim()
        }
        if (term.endsWith("‌ی")) {
            term = term.substring(0, term.length - 2).trim()
            if (!term.endsWith("ه") && !term.endsWith("ة")) {
                term += "ه"
            }
        } else if (term.endsWith("ی") && !term.endsWith("دایی") && !term.endsWith("عمو") && !term.endsWith("عمه") && !term.endsWith("خاله") && !term.endsWith("جاری") && !term.endsWith("ناپدری") && !term.endsWith("نامادری") && !term.endsWith("پسردایی") && !term.endsWith("دختردایی") && !term.endsWith("ناتنی") && !term.endsWith("پدری") && !term.endsWith("مادری") && !term.endsWith("پسری") && !term.endsWith("دختری")) {
            term = term.substring(0, term.length - 1).trim()
        }
        if (term == "مادربزرگ") term = "مادر بزرگ"
        if (term == "پدربزرگ") term = "پدر بزرگ"
        return term
    }

    fun addEzafeToTerm(term: String): String {
        val clean = cleanTerm(term)
        return when (clean) {
            "پدر" -> "پدرِ"
            "مادر" -> "مادرِ"
            "پسر" -> "پسرِ"
            "دختر" -> "دخترِ"
            "شوهر" -> "شوهرِ"
            "زن" -> "زنِ"
            "همسر" -> "همسرِ"
            "همسر سابق" -> "همسر سابقِ"
            "برادر" -> "برادرِ"
            "خواهر" -> "خواهرِ"
            "برادر ناتنی" -> "برادر ناتنیِ"
            "خواهر ناتنی" -> "خواهر ناتنیِ"
            "پدربزرگ", "پدر بزرگ" -> "پدربزرگِ"
            "مادربزرگ", "مادر بزرگ" -> "مادربزرگِ"
            "نوه" -> "نوه‌ی"
            "عمو" -> "عمویِ"
            "عمه" -> "عمه‌ی"
            "دایی" -> "داییِ"
            "خاله" -> "خاله‌ی"
            "برادرزاده" -> "برادرزاده‌ی"
            "خواهرزاده" -> "خواهرزاده‌ی"
            "پسرعمو" -> "پسرعمویِ"
            "دخترعمو" -> "دخترعمویِ"
            "پسرعمه" -> "پسرعمه‌ی"
            "دخترعمه" -> "دخترعمه‌ی"
            "پسردایی" -> "پسرداییِ"
            "دختردایی" -> "دخترداییِ"
            "پسرخاله" -> "پسرخاله‌ی"
            "دخترخاله" -> "دخترخاله‌ی"
            "داماد" -> "دامادِ"
            "عروس" -> "عروسِ"
            "عروس نوه" -> "عروسِ نوه‌ی"
            "داماد نوه" -> "دامادِ نوه‌ی"
            "پدر همسر" -> "پدر همسرِ"
            "مادر همسر" -> "مادر همسرِ"
            "پدرزن" -> "پدرزنِ"
            "مادرزن" -> "مادرزنِ"
            "پدرشوهر" -> "پدرشوهرِ"
            "مادرشوهر" -> "مادرشوهرِ"
            "برادر همسر" -> "برادر همسرِ"
            "خواهر همسر" -> "خواهر همسرِ"
            "برادرزن" -> "برادرزنِ"
            "خواهرزن" -> "خواهرزنِ"
            "برادرشوهر" -> "برادرشوهرِ"
            "خواهرشوهر" -> "خواهرشوهرِ"
            "هوو" -> "هوویِ"
            "ربیب" -> "ربیبِ"
            "ربیبه" -> "ربیبه‌ی"
            "نتیجه" -> "نتیجه‌ی"
            "نوه پسری", "نوه‌ی پسری" -> "نوه‌ی پسریِ"
            "نوه دختری", "نوه‌ی دختری" -> "نوه‌ی دختریِ"
            "پدربزرگ پدری" -> "پدربزرگِ پدریِ"
            "مادربزرگ پدری" -> "مادربزرگِ پدریِ"
            "پدربزرگ مادری" -> "پدربزرگِ مادریِ"
            "مادربزرگ مادری" -> "مادربزرگِ مادریِ"
            "جد پدری" -> "جدّ پدریِ"
            "جده پدری" -> "جدّه پدریِ"
            "جد مادری" -> "جدّ مادریِ"
            "جده مادری" -> "جدّه مادریِ"
            "عروس نتیجه" -> "عروسِ نتیجه‌ی"
            "داماد نتیجه" -> "دامادِ نتیجه‌ی"
            "جد اعلای همسر" -> "جد اعلایِ همسرِ"
            "جده اعلای همسر" -> "جدّه اعلایِ همسرِ"
            "زن برادر" -> "زن برادرِ"
            "شوهر خواهر" -> "شوهر خواهرِ"
            "زن عمو" -> "زن عمویِ"
            "شوهر عمه" -> "شوهر عمه‌ی"
            "زن دایی" -> "زن داییِ"
            "شوهر خاله" -> "شوهر خاله‌ی"
            "باجناق" -> "باجناقِ"
            "جاری" -> "جاریِ"
            "جد بزرگ" -> "جد بزرگِ"
            "جده بزرگ" -> "جده بزرگِ"
            "جد اعلا" -> "جد اعلایِ"
            "جده اعلا" -> "جده اعلایِ"
            "نبیره" -> "نبیره‌ی"
            "ندیده" -> "ندیده‌ی"
            "ناپدری" -> "ناپدریِ"
            "نامادری" -> "نامادریِ"
            "پدرخوانده" -> "پدرخوانده‌ی"
            "مادرخوانده" -> "مادرخوانده‌ی"
            "فرزندخوانده" -> "فرزندخوانده‌ی"
            "پسرخوانده" -> "پسرخوانده‌ی"
            "دخترخوانده" -> "دخترخوانده‌ی"
            "پدربزرگ همسر", "پدر بزرگ همسر" -> "پدربزرگِ همسرِ"
            "مادربزرگ همسر", "مادر بزرگ همسر" -> "مادربزرگِ همسرِ"
            "عموی همسر" -> "عمویِ همسرِ"
            "عمه همسر" -> "عمه‌ی همسرِ"
            "دایی همسر" -> "داییِ همسرِ"
            "خاله همسر" -> "خاله‌ی همسرِ"
            "برادرزاده همسر" -> "برادرزاده‌ی همسرِ"
            "خواهرزاده همسر" -> "خواهرزاده‌ی همسرِ"
            "زن برادرزاده" -> "زن برادرزاده‌ی"
            "شوهر برادرزاده" -> "شوهر برادرزاده‌ی"
            "زن خواهرزاده" -> "زن خواهرزاده‌ی"
            "شوهر خواهرزاده" -> "شوهر خواهرزاده‌ی"
            "عموی پدر" -> "عمویِ پدرِ"
            "عمه پدر" -> "عمه‌ی پدرِ"
            "دایی پدر" -> "داییِ پدرِ"
            "خاله پدر" -> "خاله‌ی پدرِ"
            "عموی مادر" -> "عمویِ مادرِ"
            "عمه مادر" -> "عمه‌ی مادرِ"
            "دایی مادر" -> "داییِ مادرِ"
            "خاله مادر" -> "خاله‌ی مادرِ"
            "نوه‌ی برادر", "نوه برادر" -> "نوه‌ی برادرِ"
            "نوه‌ی خواهر", "نوه خواهر" -> "نوه‌ی خواهرِ"
            "عروس نبیره" -> "عروسِ نبیره‌ی"
            "داماد نبیره" -> "دامادِ نبیره‌ی"
            "پسر همسر" -> "پسرِ همسرِ"
            "دختر همسر" -> "دخترِ همسرِ"
            "نوه‌ی همسر", "نوه همسر" -> "نوه‌ی همسرِ"
            "خویشاوند" -> "خویشاوندِ"
            else -> {
                when {
                    clean.endsWith("ه") || clean.endsWith("ة") -> "${clean}‌ی"
                    clean.endsWith("ا") || clean.endsWith("و") -> "${clean}یِ"
                    clean.endsWith("ی") -> "${clean}ِ"
                    else -> "${clean}ِ"
                }
            }
        }
    }

    /**
     * Finds all unique standard relationship terms between Person A and Person B.
     */
    fun getAllRelationshipTerms(
        personA: Person,
        personB: Person,
        allPersons: List<Person>,
        allRelationships: List<Relationship>
    ): List<String> {
        if (personA.id == personB.id) return listOf("خودِ شخص")

        val pathsFromBToA = findAllPaths(personB, personA, allPersons, allRelationships, maxDepth = 5, maxPaths = 10)
        if (pathsFromBToA.isEmpty()) return emptyList()

        val standardTerms = LinkedHashSet<String>()
        for (path in pathsFromBToA) {
            val steps = path.drop(1).map { it.second }
            val rawLabel = getRelationLabelFromSteps(steps, personA, personB)
            val cleaned = cleanTerm(rawLabel)
            if (cleaned.isNotBlank() && isStandardTerm(cleaned)) {
                standardTerms.add(cleaned)
            }
        }

        if (standardTerms.isNotEmpty()) {
            return standardTerms.toList()
        }

        return listOf("خویشاوند")
    }

    /**
     * Returns individual sentences for each distinct relationship between Person A and Person B.
     */
    fun getAllRelationshipSentences(
        personA: Person,
        personB: Person,
        allPersons: List<Person>,
        allRelationships: List<Relationship>
    ): List<String> {
        val terms = getAllRelationshipTerms(personA, personB, allPersons, allRelationships)
        if (terms.isEmpty()) return listOf("${personA.firstName} هیچ نسبت فامیلی مستقیمی با ${personB.firstName} ندارد")
        return terms.map { term ->
            val termWithEzafe = addEzafeToTerm(term)
            "${personA.firstName} $termWithEzafe ${personB.firstName} هست"
        }
    }

    /**
     * Computes a friendly Persian relationship label from Person A to Person B.
     * If multiple relationships exist, combines them seamlessly (e.g., "علی همزمان شوهر و پسرعمویِ مریم هست").
     */
    fun getRelationshipLabel(
        personA: Person,
        personB: Person,
        allPersons: List<Person>,
        allRelationships: List<Relationship>
    ): String {
        if (personA.id == personB.id) return "خودِ شخص هست"

        val terms = getAllRelationshipTerms(personA, personB, allPersons, allRelationships)
        if (terms.isEmpty()) return "هیچ نسبت فامیلی مستقیمی تعریف نشده است"

        if (terms.size == 1) {
            val termWithEzafe = addEzafeToTerm(terms[0])
            return "${personA.firstName} $termWithEzafe ${personB.firstName} هست"
        }

        val firstTerms = terms.dropLast(1).joinToString("، ")
        val lastTermWithEzafe = addEzafeToTerm(terms.last())

        return "${personA.firstName} همزمان $firstTerms و $lastTermWithEzafe ${personB.firstName} هست"
    }

    /**
     * Determines the consanguineous relationship between two spouses.
     */
    fun getBloodRelationshipNameBetweenSpouses(
        spouseA: Person,
        spouseB: Person,
        allPersons: List<Person>,
        allRelationships: List<Relationship>
    ): String? {
        val terms = getAllRelationshipTerms(spouseA, spouseB, allPersons, allRelationships)
        val bloodTerm = terms.find { it != "شوهر" && it != "زن" && it != "همسر" && it != "همسر سابق" && it != "خودِ شخص" }
        return bloodTerm
    }
}

