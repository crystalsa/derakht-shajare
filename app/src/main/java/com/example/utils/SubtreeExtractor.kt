package com.example.utils

import com.example.data.Person
import com.example.data.Relationship

object SubtreeExtractor {

    fun isSpouseRelation(type: String): Boolean {
        return type == "Spouse" || type == "Divorced" || type == "SecondSpouse" || type == "SecondSpouse_Divorced"
    }

    /**
     * Traverses downward starting from rootId to collect the person, all spouses,
     * children, and all subsequent descendant generations and their internal relationships.
     */
    fun getSubtreePersonsAndRelationships(
        rootId: Long,
        persons: List<Person>,
        relationships: List<Relationship>
    ): Pair<List<Person>, List<Relationship>> {
        val collectedPersonIds = mutableSetOf<Long>()

        fun collectDescendants(personId: Long) {
            if (!collectedPersonIds.add(personId)) return

            // Find spouses of this person
            val spouseIds = relationships.filter { rel ->
                isSpouseRelation(rel.type) && (rel.personId1 == personId || rel.personId2 == personId)
            }.flatMap { listOf(it.personId1, it.personId2) }
             .filter { it != personId }

            for (spouseId in spouseIds) {
                collectedPersonIds.add(spouseId)
            }

            // Find children of this person or any spouse
            val parentIds = mutableSetOf(personId).apply { addAll(spouseIds) }
            val childIds = relationships.filter { rel ->
                (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && parentIds.contains(rel.personId1)
            }.map { it.personId2 }.distinct()

            for (childId in childIds) {
                collectDescendants(childId)
            }
        }

        collectDescendants(rootId)

        val subtreePersons = persons.filter { collectedPersonIds.contains(it.id) }
        val subtreeRelationships = relationships.filter { rel ->
            collectedPersonIds.contains(rel.personId1) && collectedPersonIds.contains(rel.personId2)
        }

        return Pair(subtreePersons, subtreeRelationships)
    }

    /**
     * Remaps person generations and relationship personIds for a newly created group.
     * Bases generation numbers from root person (root becomes 0) and strips any incoming parent links to root.
     */
    fun remapSubtreeForNewGroup(
        subtreePersons: List<Person>,
        subtreeRelationships: List<Relationship>,
        rootId: Long,
        newGroupId: Long,
        idOffset: Long = 1000L
    ): Pair<List<Person>, List<Relationship>> {
        val rootPerson = subtreePersons.find { it.id == rootId }
        val rootGen = rootPerson?.generation ?: 0
        val oldToNewPersonIdMap = mutableMapOf<Long, Long>()

        var currentNewId = idOffset
        val remappedPersons = subtreePersons.map { p ->
            val adjustedGen = (p.generation - rootGen).coerceAtLeast(0)
            val newId = currentNewId++
            oldToNewPersonIdMap[p.id] = newId
            p.copy(
                id = newId,
                groupId = newGroupId,
                generation = adjustedGen
            )
        }

        val remappedRelationships = mutableListOf<Relationship>()
        for (r in subtreeRelationships) {
            // Strip any parent-child relationship where rootPerson is the child
            if ((r.type == "Parent-Child" || r.type == "Adoptive-Parent-Child") && r.personId2 == rootId) {
                continue
            }
            val newP1 = oldToNewPersonIdMap[r.personId1]
            val newP2 = oldToNewPersonIdMap[r.personId2]
            if (newP1 != null && newP2 != null) {
                remappedRelationships.add(
                    r.copy(
                        id = 0,
                        personId1 = newP1,
                        personId2 = newP2
                    )
                )
            }
        }

        return Pair(remappedPersons, remappedRelationships)
    }
}
