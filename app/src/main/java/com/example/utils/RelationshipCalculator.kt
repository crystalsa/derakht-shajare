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
        val paths = findAllPaths(personA, personB, allPersons, allRelationships, maxDepth = 6, maxPaths = 1)
        return paths.firstOrNull()
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
        "شوهر", "زن", "همسر", "همسر سابق",
        "برادر", "خواهر",
        "پدر بزرگ", "مادر بزرگ", "پدربزرگ", "مادربزرگ", "نوه",
        "عمو", "عمه", "دایی", "خاله",
        "برادرزاده", "خواهرزاده",
        "پسرعمو", "دخترعمو", "پسرعمه", "دخترعمه",
        "پسردایی", "دختردایی", "پسرخاله", "دخترخاله",
        "پدر همسر", "مادر همسر", "داماد", "عروس",
        "برادر همسر", "خواهر همسر", "زن برادر", "شوهر خواهر", "باجناق", "جاری",
        "ناپدری", "نامادری", "پدرخوانده", "مادرخوانده", "فرزندخوانده",
        "جد بزرگ", "جده بزرگ", "نبیره", "ندیده",
        "عموی همسر", "عمه همسر", "دایی همسر", "خاله همسر",
        "پدربزرگ همسر", "مادربزرگ همسر"
    )

    private fun isStandardTerm(term: String): Boolean {
        if (STANDARD_TERMS.contains(term)) return true
        if (term.endsWith(" همسر")) {
            val base = term.removeSuffix(" همسر")
            if (STANDARD_TERMS.contains(base)) return true
        }
        return false
    }

    /**
     * Decodes the list of directed steps from B to A into a clean, natural Persian relationship label.
     */
    fun getRelationLabelFromSteps(steps: List<String>, personA: Person, personB: Person): String {
        val size = steps.size

        // Direct relationships
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
                "ADOPTIVE_SON" -> "فرزندخوانده‌ی"
                "ADOPTIVE_DAUGHTER" -> "فرزندخوانده‌ی"
                else -> "خویشاوندِ"
            }
        }

        // Two-step relationships
        if (size == 2) {
            val s1 = steps[0]
            val s2 = steps[1]

            // Sibling check: Parent -> Child (B -> Parent -> A)
            if ((s1 == "FATHER" || s1 == "MOTHER") && (s2 == "SON" || s2 == "DAUGHTER")) {
                return if (personA.gender == "Male") "برادرِ" else "خواهرِ"
            }

            // Grandparent check: Parent -> Parent (B -> Parent -> Grandparent)
            if ((s1 == "FATHER" || s1 == "MOTHER") && (s2 == "FATHER" || s2 == "MOTHER")) {
                return if (personA.gender == "Male") "پدربزرگِ" else "مادربزرگِ"
            }

            // Grandchild check: Child -> Child (B -> Child -> Grandchild)
            if ((s1 == "SON" || s1 == "DAUGHTER") && (s2 == "SON" || s2 == "DAUGHTER")) {
                return "نوه‌ی"
            }

            // Spouse parent
            if (s1 == "SPOUSE" && (s2 == "FATHER" || s2 == "MOTHER")) {
                return if (s2 == "FATHER") "پدر همسرِ" else "مادر همسرِ"
            }

            // Child's spouse (Daughter-in-law / Son-in-law)
            if ((s1 == "SON" || s1 == "DAUGHTER") && s2 == "SPOUSE") {
                return if (personA.gender == "Male") "دامادِ" else "عروسِ"
            }

            // Spouse child
            if (s1 == "SPOUSE" && (s2 == "SON" || s2 == "DAUGHTER")) {
                return if (s2 == "SON") "پسرِ همسرِ" else "دخترِ همسرِ"
            }
        }

        // Three-step relationships
        if (size == 3) {
            val s1 = steps[0]
            val s2 = steps[1]
            val s3 = steps[2]

            // Great-grandparent: Parent -> Parent -> Parent
            val allParents = steps.all { it == "FATHER" || it == "MOTHER" }
            if (allParents) {
                return if (personA.gender == "Male") "جد بزرگِ" else "جده بزرگِ"
            }

            // Great-grandchild (نبیره): Child -> Child -> Child
            val allChildren = steps.all { it == "SON" || it == "DAUGHTER" }
            if (allChildren) {
                return "نبیره‌ی"
            }

            // Aunt/Uncle: Parent -> Parent -> Child (B -> Parent -> Grandparent -> Aunt/Uncle)
            if ((s1 == "FATHER" || s1 == "MOTHER") && (s2 == "FATHER" || s2 == "MOTHER") && (s3 == "SON" || s3 == "DAUGHTER")) {
                return if (s1 == "FATHER") {
                    if (s3 == "SON") "عمویِ" else "عمه‌ی"
                } else {
                    if (s3 == "SON") "داییِ" else "خاله‌ی"
                }
            }

            // Niece/Nephew: Parent -> Child -> Child (B -> Sibling -> Niece/Nephew)
            if ((s1 == "FATHER" || s1 == "MOTHER") && (s2 == "SON" || s2 == "DAUGHTER") && (s3 == "SON" || s3 == "DAUGHTER")) {
                val isBrother = s2 == "SON"
                return if (isBrother) {
                    "برادرزاده‌ی"
                } else {
                    "خواهرزاده‌ی"
                }
            }

            // Sibling's spouse (زن داداش / شوهر خواهر)
            if ((s1 == "FATHER" || s1 == "MOTHER") && (s2 == "SON" || s2 == "DAUGHTER") && s3 == "SPOUSE") {
                return if (s2 == "SON") "زن برادرِ" else "شوهر خواهرِ"
            }

            // Spouse Sibling
            if (s1 == "SPOUSE" && (s2 == "FATHER" || s2 == "MOTHER") && (s3 == "SON" || s3 == "DAUGHTER")) {
                return if (s3 == "SON") "برادر همسرِ" else "خواهر همسرِ"
            }
        }

        // Four-step relationships
        if (size == 4) {
            val s1 = steps[0]
            val s2 = steps[1]
            val s3 = steps[2]
            val s4 = steps[3]

            // Great-great-grandchild (ندیده): Child -> Child -> Child -> Child
            val allChildren = steps.all { it == "SON" || it == "DAUGHTER" }
            if (allChildren) {
                return "ندیده‌ی"
            }

            // Cousin: Parent -> Parent -> Child -> Child (B -> Parent -> Grandparent -> Aunt/Uncle -> Cousin)
            if ((s1 == "FATHER" || s1 == "MOTHER") && (s2 == "FATHER" || s2 == "MOTHER") && (s3 == "SON" || s3 == "DAUGHTER") && (s4 == "SON" || s4 == "DAUGHTER")) {
                val uncleAunt = if (s1 == "FATHER") {
                    if (s3 == "SON") "عمو" else "عمه"
                } else {
                    if (s3 == "SON") "دایی" else "خاله"
                }

                return when (uncleAunt) {
                    "عمو" -> if (s4 == "SON") "پسرعمویِ" else "دخترعمویِ"
                    "عمه" -> if (s4 == "SON") "پسرعمه‌ی" else "دخترعمه‌ی"
                    "دایی" -> if (s4 == "SON") "پسرداییِ" else "دخترداییِ"
                    "خاله" -> if (s4 == "SON") "پسرخاله‌ی" else "دخترخاله‌ی"
                    else -> "خویشاوندِ"
                }
            }

            // Aunt/Uncle spouse (زن عمو, شوهر عمه, زن دایی, شوهر خاله)
            if ((s1 == "FATHER" || s1 == "MOTHER") && (s2 == "FATHER" || s2 == "MOTHER") && (s3 == "SON" || s3 == "DAUGHTER") && s4 == "SPOUSE") {
                return if (s1 == "FATHER") {
                    if (s3 == "SON") "زن عمویِ" else "شوهر عمه‌ی"
                } else {
                    if (s3 == "SON") "زن داییِ" else "شوهر خاله‌ی"
                }
            }

            // Bajenagh / Jari (B -> Spouse -> Parent -> Sibling -> Spouse)
            if (s1 == "SPOUSE" && (s2 == "FATHER" || s2 == "MOTHER") && (s3 == "SON" || s3 == "DAUGHTER") && s4 == "SPOUSE") {
                if (personB.gender == "Male" && personA.gender == "Male") return "باجناقِ"
                if (personB.gender == "Female" && personA.gender == "Female") return "جاریِ"
            }
        }

        return "خویشاوندِ"
    }

    private fun cleanTerm(raw: String): String {
        var term = raw.trim()
        if (term.endsWith("ِ")) {
            term = term.substring(0, term.length - 1).trim()
        }
        if (term.endsWith("‌ی")) {
            term = term.substring(0, term.length - 2).trim()
            if (!term.endsWith("ه") && !term.endsWith("ة")) {
                term += "ه"
            }
        } else if (term.endsWith("ی") && !term.endsWith("دایی") && !term.endsWith("عمو") && !term.endsWith("عمه") && !term.endsWith("خاله")) {
            term = term.substring(0, term.length - 1).trim()
        }
        if (term == "مادربزرگ") term = "مادر بزرگ"
        if (term == "پدربزرگ") term = "پدر بزرگ"
        return term
    }

    private fun addEzafeToTerm(term: String): String {
        if (term == "خویشاوند") return "خویشاوندِ"
        if (term.endsWith("ه") || term.endsWith("ة")) return "${term}‌ی"
        if (term.endsWith("ا") || term.endsWith("و") || term.endsWith("ی")) return "${term}یِ"
        return "${term}ِ"
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

