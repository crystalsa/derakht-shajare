package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {
    @Query("SELECT * FROM persons ORDER BY lastName, firstName ASC")
    fun getAllPersons(): Flow<List<Person>>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getPersonById(id: Long): Person?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPerson(person: Person): Long

    @Update
    suspend fun updatePerson(person: Person)

    @Delete
    suspend fun deletePerson(person: Person)

    @Query("SELECT * FROM relationships")
    fun getAllRelationships(): Flow<List<Relationship>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelationship(relationship: Relationship): Long

    @Delete
    suspend fun deleteRelationship(relationship: Relationship)

    @Query("DELETE FROM relationships WHERE personId1 = :personId OR personId2 = :personId")
    suspend fun deleteRelationshipsForPerson(personId: Long)

    // Group methods
    @Query("SELECT * FROM family_groups ORDER BY displayOrder ASC, id ASC")
    fun getAllGroups(): Flow<List<FamilyGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: FamilyGroup): Long

    @Update
    suspend fun updateGroup(group: FamilyGroup)

    @Delete
    suspend fun deleteGroup(group: FamilyGroup)

    @Query("UPDATE persons SET groupId = NULL WHERE groupId = :groupId")
    suspend fun removeGroupAssociationFromPersons(groupId: Long)

    @Query("DELETE FROM relationships WHERE personId1 IN (SELECT id FROM persons WHERE groupId = :groupId) OR personId2 IN (SELECT id FROM persons WHERE groupId = :groupId)")
    suspend fun deleteRelationshipsForGroup(groupId: Long)

    @Query("DELETE FROM persons WHERE groupId = :groupId")
    suspend fun deletePersonsForGroup(groupId: Long)

    @Query("DELETE FROM persons")
    suspend fun deleteAllPersons()

    @Query("DELETE FROM relationships")
    suspend fun deleteAllRelationships()

    @Query("DELETE FROM family_groups")
    suspend fun deleteAllGroups()
}
