package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "persons")
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val gender: String, // "Male" or "Female"
    val birthDate: String? = null,
    val birthPlace: String? = null,
    val deathDate: String? = null,
    val deathPlace: String? = null,
    val isDeceased: Boolean = false,
    val occupation: String? = null,
    val biography: String? = null,
    val photoUri: String? = null,
    val generation: Int = 0,
    val groupId: Long? = null // New nullable field for family grouping
) : Serializable {
    val fullName: String
        get() = "$firstName $lastName".trim()
}

@Entity(tableName = "family_groups")
data class FamilyGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val displayOrder: Int = 0
) : Serializable

@Entity(tableName = "relationships")
data class Relationship(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId1: Long, // e.g. Parent or Spouse 1
    val personId2: Long, // e.g. Child or Spouse 2
    val type: String // "Spouse", "Parent-Child", "Divorced", "Adoptive-Parent-Child"
) : Serializable
