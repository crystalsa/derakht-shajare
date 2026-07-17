package com.example.data

import kotlinx.coroutines.flow.Flow

class FamilyRepository(private val familyDao: FamilyDao) {
    val allPersons: Flow<List<Person>> = familyDao.getAllPersons()
    val allRelationships: Flow<List<Relationship>> = familyDao.getAllRelationships()
    val allGroups: Flow<List<FamilyGroup>> = familyDao.getAllGroups()

    suspend fun getPersonById(id: Long): Person? = familyDao.getPersonById(id)

    suspend fun insertPerson(person: Person): Long = familyDao.insertPerson(person)

    suspend fun updatePerson(person: Person) = familyDao.updatePerson(person)

    suspend fun deletePerson(person: Person) {
        // First delete relationships associated with this person to prevent dangling relations
        familyDao.deleteRelationshipsForPerson(person.id)
        // Then delete the person
        familyDao.deletePerson(person)
    }

    suspend fun insertRelationship(relationship: Relationship): Long =
        familyDao.insertRelationship(relationship)

    suspend fun deleteRelationship(relationship: Relationship) =
        familyDao.deleteRelationship(relationship)

    suspend fun insertGroup(group: FamilyGroup): Long = familyDao.insertGroup(group)

    suspend fun updateGroup(group: FamilyGroup) = familyDao.updateGroup(group)

    suspend fun deleteGroup(group: FamilyGroup) {
        familyDao.deleteRelationshipsForGroup(group.id)
        familyDao.deletePersonsForGroup(group.id)
        familyDao.deleteGroup(group)
    }

    suspend fun clearAllData() {
        familyDao.deleteAllPersons()
        familyDao.deleteAllRelationships()
        familyDao.deleteAllGroups()
    }
}
