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
                    "Spouse" -> "SPOUSE"
                    "Divorced" -> "EX_SPOUSE"
                    "Parent-Child" -> if (to.gender == "Male") "SON" else "DAUGHTER"
                    "Adoptive-Parent-Child" -> if (to.gender == "Male") "ADOPTIVE_SON" else "ADOPTIVE_DAUGHTER"
                    else -> null
                }
            }
            if (rel.personId1 == to.id && rel.personId2 == from.id) {
                return when (rel.type) {
                    "Spouse" -> "SPOUSE"
                    "Divorced" -> "EX_SPOUSE"
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
     * Returns a list of Pairs of Person and the incoming directed relationship from the previous node.
     */
    fun findShortestPath(
        personA: Person,
        personB: Person,
        allPersons: List<Person>,
        allRelationships: List<Relationship>
    ): List<Pair<Person, String>>? {
        if (personA.id == personB.id) return emptyList()

        val personMap = allPersons.associateBy { it.id }
        
        // Build adjacency list: PersonId -> List of NeighborIds
        val adjList = mutableMapOf<Long, MutableList<Long>>()
        for (rel in allRelationships) {
            adjList.getOrPut(rel.personId1) { mutableListOf() }.add(rel.personId2)
            adjList.getOrPut(rel.personId2) { mutableListOf() }.add(rel.personId1)
        }

        val queue: Queue<PathNode> = LinkedList()
        val visited = mutableSetOf<Long>()

        queue.add(PathNode(personA))
        visited.add(personA.id)

        var targetNode: PathNode? = null

        while (queue.isNotEmpty()) {
            val curr = queue.poll() ?: continue
            if (curr.person.id == personB.id) {
                targetNode = curr
                break
            }

            val neighbors = adjList[curr.person.id] ?: emptyList()
            for (neighborId in neighbors) {
                if (!visited.contains(neighborId)) {
                    val neighborPerson = personMap[neighborId]
                    if (neighborPerson != null) {
                        val dirRel = getDirectedRelation(curr.person, neighborPerson, allRelationships)
                        if (dirRel != null) {
                            visited.add(neighborId)
                            queue.add(PathNode(neighborPerson, dirRel, curr))
                        }
                    }
                }
            }
        }

        if (targetNode == null) return null

        // Reconstruct path
        val path = mutableListOf<Pair<Person, String>>()
        var node: PathNode? = targetNode
        while (node != null) {
            val relName = node.incomingRelType ?: "Start"
            path.add(0, Pair(node.person, relName))
            node = node.parentNode
        }
        return path
    }

    /**
     * Decodes the list of directed steps from B to A into a clean, natural Persian relationship label.
     */
    private fun getRelationLabelFromSteps(steps: List<String>, personA: Person, personB: Person): String {
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
        }

        // Fallback for complex/long paths: Chain backwards step by step
        val stepDescs = steps.map { step ->
            when (step) {
                "FATHER" -> "پدرِ"
                "MOTHER" -> "مادرِ"
                "SON" -> "پسرِ"
                "DAUGHTER" -> "دخترِ"
                "SPOUSE" -> "همسرِ"
                "EX_SPOUSE" -> "همسر سابقِ"
                "ADOPTIVE_FATHER" -> "پدرخوانده‌ی"
                "ADOPTIVE_MOTHER" -> "مادرخوانده‌ی"
                "ADOPTIVE_SON" -> "فرزندخوانده‌ی"
                "ADOPTIVE_DAUGHTER" -> "فرزندخوانده‌ی"
                else -> "خویشاوندِ"
            }
        }
        return stepDescs.reversed().joinToString(" ")
    }

    /**
     * Computes a friendly Persian relationship label from Person A to Person B.
     * Formats output as: "زهرا مادربزرگ مریم هست" or "علی پسرِ زهرا هست".
     */
    fun getRelationshipLabel(
        personA: Person,
        personB: Person,
        allPersons: List<Person>,
        allRelationships: List<Relationship>
    ): String {
        if (personA.id == personB.id) return "خودِ شخص هست"

        // Find the shortest path from B to A.
        // The steps along the path from B to A directly represent how to reach A from B,
        // which tells us exactly what relation A is to B.
        val pathFromBToA = findShortestPath(personB, personA, allPersons, allRelationships)
            ?: return "هیچ نسبت فامیلی مستقیمی تعریف نشده است"

        if (pathFromBToA.isEmpty()) return "خودِ شخص هست"

        val steps = pathFromBToA.drop(1).map { it.second }
        val relationLabel = getRelationLabelFromSteps(steps, personA, personB)

        // Make sure we space things nicely and don't duplicate ezafe (ِ) or append it awkwardly.
        var relationshipTerm = relationLabel.trim()
        
        // Remove trailing Persian ezafe and relative markers for a natural spoken sentence format
        if (relationshipTerm.endsWith("ِ")) {
            relationshipTerm = relationshipTerm.substring(0, relationshipTerm.length - 1)
        }
        if (relationshipTerm.endsWith("‌ی")) {
            val prefix = relationshipTerm.substring(0, relationshipTerm.length - 2)
            relationshipTerm = if (prefix.endsWith("ه") || prefix.endsWith("ة")) prefix else prefix + "ه"
        } else if (relationshipTerm.endsWith("ی") && !relationshipTerm.endsWith("دایی")) {
            relationshipTerm = relationshipTerm.substring(0, relationshipTerm.length - 1)
        }
        
        // Handle specific replacements as requested
        if (relationshipTerm == "مادربزرگ") {
            relationshipTerm = "مادر بزرگ"
        } else if (relationshipTerm == "پدربزرگ") {
            relationshipTerm = "پدر بزرگ"
        }
        
        // Return a beautiful, descriptive full sentence
        return "${personA.firstName} $relationshipTerm ${personB.firstName} هست"
    }
}
