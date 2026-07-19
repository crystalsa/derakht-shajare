package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FamilyDatabase
import com.example.data.FamilyRepository
import com.example.data.Person
import com.example.data.Relationship
import com.example.utils.RelationshipCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FamilyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FamilyRepository
    
    val allPersons: StateFlow<List<Person>>
    val allRelationships: StateFlow<List<Relationship>>
    val allGroups: StateFlow<List<com.example.data.FamilyGroup>>

    // UI Search & Filter states
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterGender = MutableStateFlow<String?>(null) // "Male", "Female", or null
    val filterGender = _filterGender.asStateFlow()

    private val _filterResidence = MutableStateFlow("")
    val filterResidence = _filterResidence.asStateFlow()

    private val _filterIsDeceased = MutableStateFlow<Boolean?>(null)
    val filterIsDeceased = _filterIsDeceased.asStateFlow()

    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId = _selectedGroupId.asStateFlow()

    private val prefs = application.getSharedPreferences("family_tree_prefs", android.content.Context.MODE_PRIVATE)

    // Tree configurations
    private val _treeLayout = MutableStateFlow(prefs.getString("tree_layout", "Vertical") ?: "Vertical") // "Vertical", "Horizontal"
    val treeLayout = _treeLayout.asStateFlow()

    private val _treeTheme = MutableStateFlow(prefs.getString("tree_theme", "Bento Grid") ?: "Bento Grid") // "Bento Grid", "Classic", "Vintage Paper", "Dark Gold"
    val treeTheme = _treeTheme.asStateFlow()

    private val _focusPersonId = MutableStateFlow<Long?>(null) // For "Focus Mode" on a specific family line
    val focusPersonId = _focusPersonId.asStateFlow()

    // Relationship path highlighting
    private val _highlightPerson1Id = MutableStateFlow<Long?>(null)
    private val _highlightPerson2Id = MutableStateFlow<Long?>(null)
    val highlightPerson1Id = _highlightPerson1Id.asStateFlow()
    val highlightPerson2Id = _highlightPerson2Id.asStateFlow()

    private val _glowPersonId = MutableStateFlow<Long?>(null)
    val glowPersonId = _glowPersonId.asStateFlow()
    private var glowJob: kotlinx.coroutines.Job? = null

    init {
        val database = FamilyDatabase.getDatabase(application)
        repository = FamilyRepository(database.familyDao())
        
        allPersons = repository.allPersons
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        allRelationships = repository.allRelationships
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allGroups = repository.allGroups
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    // Search and filtered persons
    val filteredPersons: StateFlow<List<Person>> = combine(
        allPersons, searchQuery, filterGender, filterResidence, filterIsDeceased, selectedGroupId, allRelationships
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val persons = flows[0] as List<Person>
        val query = flows[1] as String
        val gender = flows[2] as String?
        val residence = flows[3] as String
        val isDeceased = flows[4] as Boolean?
        val groupId = flows[5] as Long?
        @Suppress("UNCHECKED_CAST")
        val relationships = flows[6] as List<Relationship>

        val visibleIds = mutableSetOf<Long>()
        android.util.Log.d("FamilyViewModel", "filteredPersons: persons.size=${persons.size}, groupId=$groupId, relationships.size=${relationships.size}")
        if (groupId != null) {
            val baseGroupPersons = persons.filter { it.groupId == groupId }
            android.util.Log.d("FamilyViewModel", "filteredPersons: baseGroupPersons.size=${baseGroupPersons.size}")
            val queue: Queue<Long> = LinkedList()
            baseGroupPersons.forEach { 
                visibleIds.add(it.id)
                queue.add(it.id)
            }

            // Create spouses and children maps
            val spousesMap = mutableMapOf<Long, MutableSet<Long>>()
            val childrenMap = mutableMapOf<Long, MutableSet<Long>>()

            relationships.forEach { rel ->
                when (rel.type) {
                    "Spouse", "Divorced", "SecondSpouse", "SecondSpouse_Divorced" -> {
                        spousesMap.getOrPut(rel.personId1) { mutableSetOf() }.add(rel.personId2)
                        spousesMap.getOrPut(rel.personId2) { mutableSetOf() }.add(rel.personId1)
                    }
                    "Parent-Child", "Adoptive-Parent-Child" -> {
                        // personId1 is parent, personId2 is child
                        childrenMap.getOrPut(rel.personId1) { mutableSetOf() }.add(rel.personId2)
                    }
                }
            }

            while (queue.isNotEmpty()) {
                val curr = queue.poll() ?: continue
                
                // Add spouses
                spousesMap[curr]?.forEach { spouseId ->
                    if (visibleIds.add(spouseId)) {
                        queue.add(spouseId)
                    }
                }
                
                // Add children (downward search only)
                childrenMap[curr]?.forEach { childId ->
                    if (visibleIds.add(childId)) {
                        queue.add(childId)
                    }
                }
            }
        } else {
            persons.forEach { visibleIds.add(it.id) }
        }

        val result = persons.filter { person ->
            val matchesGroup = visibleIds.contains(person.id)

            val matchesQuery = query.isEmpty() || 
                person.fullName.contains(query, ignoreCase = true) ||
                (person.occupation?.contains(query, ignoreCase = true) ?: false) ||
                (person.biography?.contains(query, ignoreCase = true) ?: false)
            
            val matchesGender = gender == null || person.gender == gender
            
            val matchesResidence = residence.isEmpty() ||
                (person.birthPlace?.contains(residence, ignoreCase = true) ?: false) ||
                (person.deathPlace?.contains(residence, ignoreCase = true) ?: false)
                
            val matchesDeceased = isDeceased == null || person.isDeceased == isDeceased

            matchesGroup && matchesQuery && matchesGender && matchesResidence && matchesDeceased
        }
        android.util.Log.d("FamilyViewModel", "filteredPersons: result.size=${result.size}")
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Actions
    fun seedSampleData() {
        viewModelScope.launch {
            if (allPersons.value.isNotEmpty()) return@launch

            // Create a default group
            val defaultGroupId = repository.insertGroup(
                com.example.data.FamilyGroup(name = "خانواده بزرگ علوی", description = "شجره‌نامه تاریخی خاندان علوی و بستگان نزدیک")
            )

            // Generation 0 (بزرگ خاندان اول)
            val g0f = repository.insertPerson(Person(firstName = "حاج میرزا", lastName = "علوی", gender = "Male", birthDate = "1220-01-01", birthPlace = "کاشان", deathDate = "1300-05-12", isDeceased = true, occupation = "تاجر بزرگ فرش", generation = 0, groupId = defaultGroupId))
            val g0m = repository.insertPerson(Person(firstName = "بی‌بی خاتون", lastName = "حسینی", gender = "Female", birthDate = "1225-04-15", birthPlace = "یزد", deathDate = "1305-10-20", isDeceased = true, occupation = "خانه‌دار", generation = 0, groupId = defaultGroupId))
            repository.insertRelationship(Relationship(personId1 = g0f, personId2 = g0m, type = "Spouse"))

            // Generation 1 (پدربزرگ و مادربزرگ بزرگ)
            val g1f = repository.insertPerson(Person(firstName = "حاج رضا", lastName = "علوی", gender = "Male", birthDate = "1250-07-22", birthPlace = "کاشان", deathDate = "1320-02-10", isDeceased = true, occupation = "حکیم‌باشی طب سنتی", generation = 1, groupId = defaultGroupId))
            val g1m = repository.insertPerson(Person(firstName = "سکینه", lastName = "عباسی", gender = "Female", birthDate = "1255-03-10", birthPlace = "قم", deathDate = "1325-06-18", isDeceased = true, occupation = "خانه‌دار", generation = 1, groupId = defaultGroupId))
            repository.insertRelationship(Relationship(personId1 = g1f, personId2 = g1m, type = "Spouse"))
            repository.insertRelationship(Relationship(personId1 = g0f, personId2 = g1f, type = "Parent-Child"))
            repository.insertRelationship(Relationship(personId1 = g0m, personId2 = g1f, type = "Parent-Child"))

            // Generation 2 (پدربزرگ و مادربزرگ اصلی)
            val g2f = repository.insertPerson(Person(firstName = "محمد", lastName = "علوی", gender = "Male", birthDate = "1280-12-05", birthPlace = "اصفهان", deathDate = "1350-09-18", isDeceased = true, occupation = "معمار بناهای سنتی", generation = 2, groupId = defaultGroupId))
            val g2m = repository.insertPerson(Person(firstName = "زهرا", lastName = "حسینی", gender = "Female", birthDate = "1285-09-18", birthPlace = "شیراز", deathDate = "1360-11-22", isDeceased = true, occupation = "شاعر مکتب‌خانه", generation = 2, groupId = defaultGroupId))
            repository.insertRelationship(Relationship(personId1 = g2f, personId2 = g2m, type = "Spouse"))
            repository.insertRelationship(Relationship(personId1 = g1f, personId2 = g2f, type = "Parent-Child"))
            repository.insertRelationship(Relationship(personId1 = g1m, personId2 = g2f, type = "Parent-Child"))

            // Generation 3 (والدین بزرگ)
            val g3f = repository.insertPerson(Person(firstName = "علی", lastName = "علوی", gender = "Male", birthDate = "1310-11-25", birthPlace = "تهران", deathDate = "1390-04-12", isDeceased = true, occupation = "استاد ادبیات فارسی دانشگاه", generation = 3, groupId = defaultGroupId))
            val g3m = repository.insertPerson(Person(firstName = "مژگان", lastName = "رضایی", gender = "Female", birthDate = "1315-02-14", birthPlace = "تهران", deathDate = "1395-07-30", isDeceased = true, occupation = "نویسنده کتاب کودک", generation = 3, groupId = defaultGroupId))
            repository.insertRelationship(Relationship(personId1 = g3f, personId2 = g3m, type = "Spouse"))
            repository.insertRelationship(Relationship(personId1 = g2f, personId2 = g3f, type = "Parent-Child"))
            repository.insertRelationship(Relationship(personId1 = g2m, personId2 = g3f, type = "Parent-Child"))

            // Generation 4 (والدین)
            val g4f = repository.insertPerson(Person(firstName = "رضا", lastName = "علوی", gender = "Male", birthDate = "1340-05-15", birthPlace = "تهران", isDeceased = false, occupation = "پزشک متخصص قلب", generation = 4, groupId = defaultGroupId))
            val g4m = repository.insertPerson(Person(firstName = "فاطمه", lastName = "محسنی", gender = "Female", birthDate = "1345-08-20", birthPlace = "تبریز", isDeceased = false, occupation = "داروساز داروخانه ملی", generation = 4, groupId = defaultGroupId))
            repository.insertRelationship(Relationship(personId1 = g4f, personId2 = g4m, type = "Spouse"))
            repository.insertRelationship(Relationship(personId1 = g3f, personId2 = g4f, type = "Parent-Child"))
            repository.insertRelationship(Relationship(personId1 = g3m, personId2 = g4f, type = "Parent-Child"))

            // Generation 5 (فرزندان)
            val g5f = repository.insertPerson(Person(firstName = "حمید", lastName = "علوی", gender = "Male", birthDate = "1370-03-10", birthPlace = "تهران", isDeceased = false, occupation = "مهندس هوش مصنوعی", generation = 5, groupId = defaultGroupId))
            val g5m = repository.insertPerson(Person(firstName = "الهام", lastName = "سهرابی", gender = "Female", birthDate = "1375-01-25", birthPlace = "شیراز", isDeceased = false, occupation = "طراح ارشد محصولات وب", generation = 5, groupId = defaultGroupId))
            repository.insertRelationship(Relationship(personId1 = g5f, personId2 = g5m, type = "Spouse"))
            repository.insertRelationship(Relationship(personId1 = g4f, personId2 = g5f, type = "Parent-Child"))
            repository.insertRelationship(Relationship(personId1 = g4m, personId2 = g5f, type = "Parent-Child"))

            val g5d = repository.insertPerson(Person(firstName = "سارا", lastName = "علوی", gender = "Female", birthDate = "1373-11-12", birthPlace = "تهران", isDeceased = false, occupation = "مترجم و باستان‌شناس", generation = 5, groupId = defaultGroupId))
            val g5dh = repository.insertPerson(Person(firstName = "کیوان", lastName = "راد", gender = "Male", birthDate = "1368-06-05", birthPlace = "رشت", isDeceased = false, occupation = "عکاس مستند حیات وحش", generation = 5, groupId = defaultGroupId))
            repository.insertRelationship(Relationship(personId1 = g5dh, personId2 = g5d, type = "Spouse"))
            repository.insertRelationship(Relationship(personId1 = g4f, personId2 = g5d, type = "Parent-Child"))
            repository.insertRelationship(Relationship(personId1 = g4m, personId2 = g5d, type = "Parent-Child"))
        }
    }

    fun addPerson(person: Person, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.insertPerson(person)
            onComplete(id)
        }
    }

    fun addGroup(group: com.example.data.FamilyGroup, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val maxOrder = allGroups.value.map { it.displayOrder }.maxOrNull() ?: 0
            val groupWithOrder = group.copy(displayOrder = maxOrder + 1)
            val id = repository.insertGroup(groupWithOrder)
            onComplete(id)
        }
    }

    fun updateGroupOrder(orderedGroups: List<com.example.data.FamilyGroup>) {
        viewModelScope.launch {
            orderedGroups.forEachIndexed { index, group ->
                repository.updateGroup(group.copy(displayOrder = index))
            }
        }
    }

    fun updateGroup(group: com.example.data.FamilyGroup) {
        viewModelScope.launch {
            repository.updateGroup(group)
        }
    }

    fun deleteGroup(group: com.example.data.FamilyGroup) {
        viewModelScope.launch {
            if (_selectedGroupId.value == group.id) {
                _selectedGroupId.value = null
            }
            repository.deleteGroup(group)
        }
    }

    fun setSelectedGroupId(id: Long?) {
        _selectedGroupId.value = id
    }

    fun setGlowPersonId(id: Long?) {
        _glowPersonId.value = id
        glowJob?.cancel()
        if (id != null) {
            glowJob = viewModelScope.launch {
                kotlinx.coroutines.delay(5000)
                if (_glowPersonId.value == id) {
                    _glowPersonId.value = null
                }
            }
        }
    }

    fun addParentsToPerson(
        child: Person,
        father: Person?,
        mother: Person?,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            var fatherId: Long? = null
            var motherId: Long? = null
            val parentGen = child.generation - 1

            // Find existing parents in the relationships
            val existingParentRels = allRelationships.value.filter {
                (it.type == "Parent-Child" || it.type == "Adoptive-Parent-Child") && it.personId2 == child.id
            }
            
            var existingFatherId: Long? = null
            var existingMotherId: Long? = null
            
            existingParentRels.forEach { rel ->
                val parent = allPersons.value.find { it.id == rel.personId1 }
                if (parent != null) {
                    if (parent.gender == "Male") {
                        existingFatherId = parent.id
                    } else if (parent.gender == "Female") {
                        existingMotherId = parent.id
                    }
                }
            }

            if (father != null) {
                val f = father.copy(generation = parentGen)
                fatherId = repository.insertPerson(f)
                repository.insertRelationship(Relationship(personId1 = fatherId, personId2 = child.id, type = "Parent-Child"))
            } else {
                fatherId = existingFatherId
            }

            if (mother != null) {
                val m = mother.copy(generation = parentGen)
                motherId = repository.insertPerson(m)
                repository.insertRelationship(Relationship(personId1 = motherId, personId2 = child.id, type = "Parent-Child"))
            } else {
                motherId = existingMotherId
            }

            if (fatherId != null && motherId != null) {
                // Link father and mother as spouses if not already linked
                val alreadySpouses = allRelationships.value.any { rel ->
                    (rel.type == "Spouse" || rel.type == "Divorced" || rel.type == "SecondSpouse" || rel.type == "SecondSpouse_Divorced") &&
                    ((rel.personId1 == fatherId && rel.personId2 == motherId) ||
                     (rel.personId1 == motherId && rel.personId2 == fatherId))
                }
                if (!alreadySpouses) {
                    repository.insertRelationship(Relationship(personId1 = fatherId, personId2 = motherId, type = "Spouse"))
                }
            }

            onComplete()
        }
    }

    fun addChildToParent(parent: Person, child: Person, selectedSpouseId: Long? = null, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val childWithGen = child.copy(generation = parent.generation + 1)
            val childId = repository.insertPerson(childWithGen)
            repository.insertRelationship(Relationship(personId1 = parent.id, personId2 = childId, type = "Parent-Child"))
            
            if (selectedSpouseId != null) {
                repository.insertRelationship(Relationship(personId1 = selectedSpouseId, personId2 = childId, type = "Parent-Child"))
            } else {
                // If no specific spouse is chosen, find all spouses.
                // If there's exactly one spouse, automatically link to that one spouse.
                val spouses = allRelationships.value.filter { rel ->
                    (rel.type == "Spouse" || rel.type == "Divorced" || rel.type == "SecondSpouse" || rel.type == "SecondSpouse_Divorced") && (rel.personId1 == parent.id || rel.personId2 == parent.id)
                }.map { rel ->
                    if (rel.personId1 == parent.id) rel.personId2 else rel.personId1
                }.distinct()
                
                if (spouses.size == 1) {
                    repository.insertRelationship(Relationship(personId1 = spouses[0], personId2 = childId, type = "Parent-Child"))
                }
            }

            onComplete(childId)
        }
    }

    fun addSpouseToPerson(spouseOf: Person, spouse: Person, relationshipType: String = "Spouse", onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val spouseWithGen = spouse.copy(generation = spouseOf.generation)
            val spouseId = repository.insertPerson(spouseWithGen)
            repository.insertRelationship(Relationship(personId1 = spouseOf.id, personId2 = spouseId, type = relationshipType))
            
            // Automatically find children of spouseOf and link them to the new spouse too
            val children = allRelationships.value.filter { rel ->
                (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && rel.personId1 == spouseOf.id
            }
            for (childRel in children) {
                val exists = allRelationships.value.any { rel ->
                    (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && 
                    rel.personId1 == spouseId && rel.personId2 == childRel.personId2
                }
                if (!exists) {
                    repository.insertRelationship(Relationship(personId1 = spouseId, personId2 = childRel.personId2, type = childRel.type))
                }
            }

            onComplete(spouseId)
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            repository.updatePerson(person)
        }
    }

    fun deletePerson(person: Person) {
        viewModelScope.launch {
            repository.deletePerson(person)
            if (_focusPersonId.value == person.id) {
                _focusPersonId.value = null
            }
            if (_highlightPerson1Id.value == person.id) {
                _highlightPerson1Id.value = null
            }
            if (_highlightPerson2Id.value == person.id) {
                _highlightPerson2Id.value = null
            }
        }
    }

    fun addRelationship(personId1: Long, personId2: Long, type: String) {
        viewModelScope.launch {
            // Prevent self-relationships
            if (personId1 == personId2) return@launch
            
            // Avoid duplicate relationships
            val existing = allRelationships.value.any {
                (it.personId1 == personId1 && it.personId2 == personId2 && it.type == type) ||
                (it.personId1 == personId2 && it.personId2 == personId1 && it.type == type)
            }
            if (!existing) {
                repository.insertRelationship(Relationship(personId1 = personId1, personId2 = personId2, type = type))
            }

            // If the added relationship is Spouse/Divorced, automatically sync children between spouses!
            if (type == "Spouse" || type == "Divorced" || type == "SecondSpouse" || type == "SecondSpouse_Divorced") {
                // Children of personId1 -> link to personId2
                val children1 = allRelationships.value.filter { rel ->
                    (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && rel.personId1 == personId1
                }
                for (childRel in children1) {
                    val childExists = allRelationships.value.any { rel ->
                        (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && 
                        rel.personId1 == personId2 && rel.personId2 == childRel.personId2
                    }
                    if (!childExists) {
                        repository.insertRelationship(Relationship(personId1 = personId2, personId2 = childRel.personId2, type = childRel.type))
                    }
                }
                
                // Children of personId2 -> link to personId1
                val children2 = allRelationships.value.filter { rel ->
                    (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && rel.personId1 == personId2
                }
                for (childRel in children2) {
                    val childExists = allRelationships.value.any { rel ->
                        (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && 
                        rel.personId1 == personId1 && rel.personId2 == childRel.personId2
                    }
                    if (!childExists) {
                        repository.insertRelationship(Relationship(personId1 = personId1, personId2 = childRel.personId2, type = childRel.type))
                    }
                }
            }

            // If the added relationship is Parent-Child or Adoptive, automatically link child to all spouses of parent
            if (type == "Parent-Child" || type == "Adoptive-Parent-Child") {
                val spouses = allRelationships.value.filter { rel ->
                    (rel.type == "Spouse" || rel.type == "Divorced" || rel.type == "SecondSpouse" || rel.type == "SecondSpouse_Divorced") && (rel.personId1 == personId1 || rel.personId2 == personId1)
                }.map { rel ->
                    if (rel.personId1 == personId1) rel.personId2 else rel.personId1
                }.distinct()
                
                for (spouseId in spouses) {
                    val exists = allRelationships.value.any { rel ->
                        (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && 
                        rel.personId1 == spouseId && rel.personId2 == personId2
                    }
                    if (!exists) {
                        repository.insertRelationship(Relationship(personId1 = spouseId, personId2 = personId2, type = type))
                    }
                }
            }
        }
    }

    fun deleteRelationship(relationship: Relationship) {
        viewModelScope.launch {
            repository.deleteRelationship(relationship)
        }
    }

    // UI setters
    fun setSearchQuery(query: String) {
        if (query.isNotEmpty() && (query.startsWith(" ") || query.startsWith("\n") || query.startsWith("\r") || query.startsWith("\t"))) {
            return
        }
        _searchQuery.value = query
    }
    fun setFilterGender(gender: String?) { _filterGender.value = gender }
    fun setFilterResidence(residence: String) {
        if (residence.isNotEmpty() && (residence.startsWith(" ") || residence.startsWith("\n") || residence.startsWith("\r") || residence.startsWith("\t"))) {
            return
        }
        _filterResidence.value = residence
    }
    fun setFilterIsDeceased(deceased: Boolean?) { _filterIsDeceased.value = deceased }
    fun setTreeLayout(layout: String) { 
        _treeLayout.value = layout 
        prefs.edit().putString("tree_layout", layout).apply()
    }
    fun setTreeTheme(theme: String) { 
        _treeTheme.value = theme 
        prefs.edit().putString("tree_theme", theme).apply()
    }
    fun setFocusPersonId(id: Long?) { _focusPersonId.value = id }
    
    fun setHighlightPerson1(id: Long?) { _highlightPerson1Id.value = id }
    fun setHighlightPerson2(id: Long?) { _highlightPerson2Id.value = id }
    
    fun clearHighlighting() {
        _highlightPerson1Id.value = null
        _highlightPerson2Id.value = null
    }

    // Dynamic stats computation
    val statsState: StateFlow<FamilyStats> = combine(allPersons, allRelationships, selectedGroupId) { persons, relationships, groupId ->
        if (groupId == null) return@combine FamilyStats()
        val groupPersons = persons.filter { it.groupId == groupId }
        if (groupPersons.isEmpty()) return@combine FamilyStats()

        val secondSpouseDeceasedOrDivorcedIds = relationships.filter { rel ->
            rel.type == "SecondSpouse_Divorced" || 
            (rel.type == "SecondSpouse" && groupPersons.any { it.id == rel.personId2 && it.isDeceased })
        }.map { it.personId2 }.toSet()

        val filteredPersons = groupPersons.filter { it.id !in secondSpouseDeceasedOrDivorcedIds }

        val total = filteredPersons.size
        val livingCount = filteredPersons.count { !it.isDeceased }
        val deceasedCount = filteredPersons.count { it.isDeceased }
        
        val males = filteredPersons.count { it.gender == "Male" }
        val females = filteredPersons.count { it.gender == "Female" }

        // Average Age Calculation
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        var livingAgeSum = 0
        var livingAgeCount = 0
        var deceasedAgeSum = 0
        var deceasedAgeCount = 0

        for (p in filteredPersons) {
            val birthYear = extractYear(p.birthDate)
            if (birthYear != null) {
                if (p.isDeceased) {
                    val deathYear = extractYear(p.deathDate)
                    if (deathYear != null) {
                        deceasedAgeSum += (deathYear - birthYear).coerceAtLeast(0)
                        deceasedAgeCount++
                    }
                } else {
                    val correctCurrentYear = getCurrentYearFor(birthYear)
                    livingAgeSum += (correctCurrentYear - birthYear).coerceAtLeast(0)
                    livingAgeCount++
                }
            }
        }

        val avgLivingAge = if (livingAgeCount > 0) livingAgeSum / livingAgeCount else 0
        val avgDeceasedAge = if (deceasedAgeCount > 0) deceasedAgeSum / deceasedAgeCount else 0

        // Common names
        val firstNamesMap = filteredPersons.map { it.firstName }.groupBy { it }.mapValues { it.value.size }
        val commonFirstName = firstNamesMap.maxByOrNull { it.value }?.key ?: "-"

        val lastNamesMap = filteredPersons.map { it.lastName }.groupBy { it }.mapValues { it.value.size }
        val commonLastName = lastNamesMap.maxByOrNull { it.value }?.key ?: "-"

        // Most frequent boy name
        val boyNamesMap = filteredPersons.filter { it.gender == "Male" && !it.firstName.isNullOrBlank() }.map { it.firstName }.groupBy { it }.mapValues { it.value.size }
        val commonBoyName = boyNamesMap.maxByOrNull { it.value }?.key ?: "-"

        // Most frequent girl name
        val girlNamesMap = filteredPersons.filter { it.gender == "Female" && !it.firstName.isNullOrBlank() }.map { it.firstName }.groupBy { it }.mapValues { it.value.size }
        val commonGirlName = girlNamesMap.maxByOrNull { it.value }?.key ?: "-"

        FamilyStats(
            totalCount = total,
            livingCount = livingCount,
            deceasedCount = deceasedCount,
            malesCount = males,
            femalesCount = females,
            avgLivingAge = avgLivingAge,
            avgDeceasedAge = avgDeceasedAge,
            mostCommonFirstName = commonFirstName,
            mostCommonLastName = commonLastName,
            mostCommonBoyName = commonBoyName,
            mostCommonGirlName = commonGirlName
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FamilyStats())

    // Reminder notification scheduling helper
    val upcomingEvents: StateFlow<List<FamilyEvent>> = combine(
        allPersons, allRelationships
    ) { persons, relationships ->
        val events = mutableListOf<FamilyEvent>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)

        val personMap = persons.associateBy { it.id }

        for (p in persons) {
            val birthCal = parseDateCal(p.birthDate)
            if (birthCal != null) {
                // Birthday event
                birthCal.set(Calendar.YEAR, currentYear)
                if (birthCal.before(today)) {
                    birthCal.add(Calendar.YEAR, 1)
                }
                val daysRemaining = daysBetween(today, birthCal)
                val age = currentYear - (extractYear(p.birthDate) ?: currentYear)
                
                if (daysRemaining in 0..30) {
                    events.add(
                        FamilyEvent(
                            personId = p.id,
                            title = "تولد ${p.fullName}",
                            description = "تولد ${age} سالگی ${p.fullName} در ${daysRemaining} روز آینده",
                            dateString = p.birthDate ?: "",
                            daysRemaining = daysRemaining,
                            type = "Birthday"
                        )
                    )
                }
            }

            if (p.isDeceased) {
                val deathCal = parseDateCal(p.deathDate)
                if (deathCal != null) {
                    // Death anniversary
                    deathCal.set(Calendar.YEAR, currentYear)
                    if (deathCal.before(today)) {
                        deathCal.add(Calendar.YEAR, 1)
                    }
                    val daysRemaining = daysBetween(today, deathCal)
                    val yearsPassed = currentYear - (extractYear(p.deathDate) ?: currentYear)
                    if (daysRemaining in 0..30) {
                        events.add(
                            FamilyEvent(
                                personId = p.id,
                                title = "سالگرد فوت مرحوم ${p.fullName}",
                                description = "سالگرد ${yearsPassed} سالگی فوت مرحوم ${p.fullName} در ${daysRemaining} روز آینده",
                                dateString = p.deathDate ?: "",
                                daysRemaining = daysRemaining,
                                type = "DeathAnniversary"
                            )
                        )
                    }
                }
            }
        }

        // Marriages (Spouses)
        for (rel in relationships) {
            if (rel.type == "Spouse" || rel.type == "SecondSpouse") {
                val p1 = personMap[rel.personId1]
                val p2 = personMap[rel.personId2]
                if (p1 != null && p2 != null) {
                    // Let's assume a dummy or calculated wedding date or anniversary based on first child's birth minus 1 year
                    // Or since we don't have explicit wedding date, we can generate a wedding anniversary dynamically from first child birth
                    // Or we can assume marriages are celebrated. Since we don't have explicit date in relationships, we can search if there's any custom wedding date
                    // Let's add wedding reminders if the user sets them, or simulate. Let's look for first child's birthday as a source or just default to relationship anniversary
                    // Let's check: can we just add custom anniversaries? Yes! To make it functional, let's create a placeholder or base it on p1/p2.
                }
            }
        }

        events.sortedBy { it.daysRemaining }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun extractYear(dateString: String?): Int? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            val parts = dateString.split("-")
            if (parts.isNotEmpty()) parts[0].toIntOrNull() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun getCurrentYearFor(birthYear: Int): Int {
        val gregorianYear = Calendar.getInstance().get(Calendar.YEAR)
        return if (birthYear < 1600) {
            // It is a Jalali year.
            val month = Calendar.getInstance().get(Calendar.MONTH) // 0-indexed (0 is Jan, 2 is Mar)
            val day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            if (month < 2 || (month == 2 && day < 21)) {
                gregorianYear - 622
            } else {
                gregorianYear - 621
            }
        } else {
            gregorianYear
        }
    }

    private fun parseDateCal(dateString: String?): Calendar? {
        if (dateString.isNullOrEmpty()) return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString) ?: return null
            val cal = Calendar.getInstance()
            cal.time = date
            cal
        } catch (e: Exception) {
            null
        }
    }

    private fun daysBetween(cal1: Calendar, cal2: Calendar): Int {
        val diff = cal2.timeInMillis - cal1.timeInMillis
        return (diff / (24 * 60 * 60 * 1000)).toInt()
    }

    private fun getBase64Image(filePath: String): String? {
        return try {
            val file = java.io.File(filePath)
            if (file.exists()) {
                val bytes = file.readBytes()
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBase64Image(base64Str: String): String? {
        return try {
            val bytes = android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP)
            val directory = java.io.File(getApplication<Application>().filesDir, "photos")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = java.io.File(directory, "person_${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg")
            java.io.FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportBackupToJson(groupId: Long? = null): String {
        val backupObj = org.json.JSONObject()
        
        // Settings
        if (groupId == null) {
            val settingsObj = org.json.JSONObject().apply {
                put("treeLayout", treeLayout.value)
                put("treeTheme", treeTheme.value)
            }
            backupObj.put("settings", settingsObj)
        }
        
        // Groups
        val groupsArr = org.json.JSONArray()
        val groupsToBackup = if (groupId != null) {
            allGroups.value.filter { it.id == groupId }
        } else {
            allGroups.value
        }
        for (g in groupsToBackup) {
            val gObj = org.json.JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("description", g.description ?: org.json.JSONObject.NULL)
            }
            groupsArr.put(gObj)
        }
        backupObj.put("groups", groupsArr)
        
        // Persons
        val personsArr = org.json.JSONArray()
        val personsToBackup = if (groupId != null) {
            allPersons.value.filter { it.groupId == groupId }
        } else {
            allPersons.value
        }
        val personIdsToBackup = personsToBackup.map { it.id }.toSet()
        
        for (p in personsToBackup) {
            val pObj = org.json.JSONObject().apply {
                put("id", p.id)
                put("firstName", p.firstName)
                put("lastName", p.lastName)
                put("gender", p.gender)
                put("birthDate", p.birthDate ?: org.json.JSONObject.NULL)
                put("birthPlace", p.birthPlace ?: org.json.JSONObject.NULL)
                put("deathDate", p.deathDate ?: org.json.JSONObject.NULL)
                put("deathPlace", p.deathPlace ?: org.json.JSONObject.NULL)
                put("isDeceased", p.isDeceased)
                put("occupation", p.occupation ?: org.json.JSONObject.NULL)
                put("biography", p.biography ?: org.json.JSONObject.NULL)
                put("photoUri", p.photoUri ?: org.json.JSONObject.NULL)
                put("generation", p.generation)
                put("groupId", p.groupId ?: org.json.JSONObject.NULL)
                
                // Backup photos
                val photosBase64Arr = org.json.JSONArray()
                val urisList = p.photoUri?.split('|')?.filter { it.isNotBlank() } ?: emptyList()
                for (uri in urisList) {
                    val b64 = getBase64Image(uri)
                    if (b64 != null) {
                        photosBase64Arr.put(b64)
                    }
                }
                put("photosBase64", photosBase64Arr)
            }
            personsArr.put(pObj)
        }
        backupObj.put("persons", personsArr)
        
        // Relationships
        val relsArr = org.json.JSONArray()
        val relsToBackup = if (groupId != null) {
            allRelationships.value.filter { r -> personIdsToBackup.contains(r.personId1) && personIdsToBackup.contains(r.personId2) }
        } else {
            allRelationships.value
        }
        for (r in relsToBackup) {
            val rObj = org.json.JSONObject().apply {
                put("id", r.id)
                put("personId1", r.personId1)
                put("personId2", r.personId2)
                put("type", r.type)
            }
            relsArr.put(rObj)
        }
        backupObj.put("relationships", relsArr)
        
        return backupObj.toString(4)
    }

    fun importBackupFromJson(jsonString: String, targetGroupId: Long? = null, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupObj = org.json.JSONObject(jsonString)
                
                if (!backupObj.has("persons") || !backupObj.has("relationships")) {
                    onComplete(false, "ساختار فایل پشتیبان نامعتبر است.")
                    return@launch
                }
                
                val personsArr = backupObj.getJSONArray("persons")
                val relsArr = backupObj.getJSONArray("relationships")
                val groupsArr = if (backupObj.has("groups")) backupObj.getJSONArray("groups") else org.json.JSONArray()
                
                // Restore Settings
                if (backupObj.has("settings")) {
                    val settingsObj = backupObj.getJSONObject("settings")
                    if (settingsObj.has("treeLayout")) {
                        val layout = settingsObj.getString("treeLayout")
                        setTreeLayout(layout)
                    }
                    if (settingsObj.has("treeTheme")) {
                        val theme = settingsObj.getString("treeTheme")
                        setTreeTheme(theme)
                    }
                }
                
                // We do NOT clear the data, so we can merge existing and backup information safely!
                val oldToNewGroupIdMap = mutableMapOf<Long, Long>()
                val oldToNewPersonIdMap = mutableMapOf<Long, Long>()
                
                for (i in 0 until groupsArr.length()) {
                    val gObj = groupsArr.getJSONObject(i)
                    val oldId = gObj.getLong("id")
                    val name = gObj.getString("name")
                    val description = if (gObj.isNull("description")) null else gObj.getString("description")
                    
                    val newId = repository.insertGroup(com.example.data.FamilyGroup(id = 0, name = name, description = description))
                    oldToNewGroupIdMap[oldId] = newId
                }
                
                for (i in 0 until personsArr.length()) {
                    val pObj = personsArr.getJSONObject(i)
                    val oldId = pObj.getLong("id")
                    val firstName = pObj.getString("firstName")
                    val lastName = pObj.getString("lastName")
                    val gender = pObj.getString("gender")
                    val birthDate = if (pObj.isNull("birthDate")) null else pObj.getString("birthDate")
                    val birthPlace = if (pObj.isNull("birthPlace")) null else pObj.getString("birthPlace")
                    val deathDate = if (pObj.isNull("deathDate")) null else pObj.getString("deathDate")
                    val deathPlace = if (pObj.isNull("deathPlace")) null else pObj.getString("deathPlace")
                    val isDeceased = pObj.getBoolean("isDeceased")
                    val occupation = if (pObj.isNull("occupation")) null else pObj.getString("occupation")
                    val biography = if (pObj.isNull("biography")) null else pObj.getString("biography")
                    val originalPhotoUri = if (pObj.isNull("photoUri")) null else pObj.getString("photoUri")
                    val generation = pObj.getInt("generation")
                    
                    val oldGroupId = if (pObj.isNull("groupId")) null else pObj.getLong("groupId")
                    val newGroupId = if (targetGroupId != null) {
                        targetGroupId
                    } else if (oldGroupId != null) {
                        oldToNewGroupIdMap[oldGroupId]
                    } else {
                        null
                    }
                    
                    // Restore photos from Base64
                    var restoredPhotoUri: String? = null
                    if (pObj.has("photosBase64")) {
                        val photosBase64Arr = pObj.getJSONArray("photosBase64")
                        val restoredPaths = mutableListOf<String>()
                        for (j in 0 until photosBase64Arr.length()) {
                            val b64 = photosBase64Arr.getString(j)
                            val savedPath = saveBase64Image(b64)
                            if (savedPath != null) {
                                restoredPaths.add(savedPath)
                            }
                        }
                        if (restoredPaths.isNotEmpty()) {
                            restoredPhotoUri = restoredPaths.joinToString("|")
                        }
                    }
                    if (restoredPhotoUri == null) {
                        restoredPhotoUri = originalPhotoUri
                    }
                    
                    val newPersonId = repository.insertPerson(
                        com.example.data.Person(
                            id = 0,
                            firstName = firstName,
                            lastName = lastName,
                            gender = gender,
                            birthDate = birthDate,
                            birthPlace = birthPlace,
                            deathDate = deathDate,
                            deathPlace = deathPlace,
                            isDeceased = isDeceased,
                            occupation = occupation,
                            biography = biography,
                            photoUri = restoredPhotoUri,
                            generation = generation,
                            groupId = newGroupId
                        )
                    )
                    oldToNewPersonIdMap[oldId] = newPersonId
                }
                
                for (i in 0 until relsArr.length()) {
                    val rObj = relsArr.getJSONObject(i)
                    val oldP1 = rObj.getLong("personId1")
                    val oldP2 = rObj.getLong("personId2")
                    val type = rObj.getString("type")
                    
                    val newP1 = oldToNewPersonIdMap[oldP1]
                    val newP2 = oldToNewPersonIdMap[oldP2]
                    
                    if (newP1 != null && newP2 != null) {
                        repository.insertRelationship(
                            com.example.data.Relationship(
                                id = 0,
                                personId1 = newP1,
                                personId2 = newP2,
                                type = type
                            )
                        )
                    }
                }
                
                if (targetGroupId != null) {
                    setSelectedGroupId(targetGroupId)
                } else if (oldToNewGroupIdMap.isNotEmpty()) {
                    val firstNewGroup = oldToNewGroupIdMap.values.first()
                    setSelectedGroupId(firstNewGroup)
                }
                
                onComplete(true, "بازگردانی و ادغام فایل پشتیبان با موفقیت انجام شد.")
            } catch (e: Exception) {
                onComplete(false, "خطا در پردازش فایل پشتیبان: ${e.localizedMessage}")
            }
        }
    }

    fun getSubtreePersonsAndRelationships(rootId: Long): Pair<List<com.example.data.Person>, List<com.example.data.Relationship>> {
        val persons = allPersons.value
        val relationships = allRelationships.value
        
        val collectedPersonIds = mutableSetOf<Long>()
        
        fun isSpouseRelation(type: String): Boolean {
            return type == "Spouse" || type == "Divorced" || type == "SecondSpouse" || type == "SecondSpouse_Divorced"
        }

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
            
            // Find children of this person
            val childIds = relationships.filter { rel ->
                (rel.type == "Parent-Child" || rel.type == "Adoptive-Parent-Child") && rel.personId1 == personId
            }.map { it.personId2 }
            
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

    fun exportSubtreeBackupToJson(rootPersonId: Long): String {
        val backupObj = org.json.JSONObject()
        backupObj.put("isSubtreeBackup", true)
        
        val rootPerson = allPersons.value.find { it.id == rootPersonId }
        backupObj.put("rootPersonName", rootPerson?.fullName ?: "نامشخص")
        
        val (subtreePersons, subtreeRelationships) = getSubtreePersonsAndRelationships(rootPersonId)
        
        // Persons
        val personsArr = org.json.JSONArray()
        for (p in subtreePersons) {
            val pObj = org.json.JSONObject().apply {
                put("id", p.id)
                put("firstName", p.firstName)
                put("lastName", p.lastName)
                put("gender", p.gender)
                put("birthDate", p.birthDate ?: org.json.JSONObject.NULL)
                put("birthPlace", p.birthPlace ?: org.json.JSONObject.NULL)
                put("deathDate", p.deathDate ?: org.json.JSONObject.NULL)
                put("deathPlace", p.deathPlace ?: org.json.JSONObject.NULL)
                put("isDeceased", p.isDeceased)
                put("occupation", p.occupation ?: org.json.JSONObject.NULL)
                put("biography", p.biography ?: org.json.JSONObject.NULL)
                put("photoUri", p.photoUri ?: org.json.JSONObject.NULL)
                put("generation", p.generation)
                put("groupId", p.groupId ?: org.json.JSONObject.NULL)
                
                // Backup photos Base64
                val photosBase64Arr = org.json.JSONArray()
                val urisList = p.photoUri?.split('|')?.filter { it.isNotBlank() } ?: emptyList()
                for (uri in urisList) {
                    val b64 = getBase64Image(uri)
                    if (b64 != null) {
                        photosBase64Arr.put(b64)
                    }
                }
                put("photosBase64", photosBase64Arr)
            }
            personsArr.put(pObj)
        }
        backupObj.put("persons", personsArr)
        
        // Relationships
        val relsArr = org.json.JSONArray()
        for (r in subtreeRelationships) {
            val rObj = org.json.JSONObject().apply {
                put("id", r.id)
                put("personId1", r.personId1)
                put("personId2", r.personId2)
                put("type", r.type)
            }
            relsArr.put(rObj)
        }
        backupObj.put("relationships", relsArr)
        
        return backupObj.toString(4)
    }

    fun importSubtreeBackupFromJson(jsonString: String, onComplete: (Boolean, String, Long?) -> Unit) {
        viewModelScope.launch {
            try {
                val backupObj = org.json.JSONObject(jsonString)
                
                if (!backupObj.has("persons") || !backupObj.has("relationships")) {
                    onComplete(false, "ساختار فایل پشتیبان نامعتبر است.", null)
                    return@launch
                }
                
                val personsArr = backupObj.getJSONArray("persons")
                val relsArr = backupObj.getJSONArray("relationships")
                val rootPersonName = backupObj.optString("rootPersonName", "عضو")
                
                // Create a new group
                val newGroupName = "شجره‌نامه بازگردانی شده ($rootPersonName)"
                val newGroupId = repository.insertGroup(com.example.data.FamilyGroup(name = newGroupName, description = "بازگردانی شده از بکاپ عضو"))
                
                // Map to store old person ID to new person ID mappings
                val oldToNewIdMap = mutableMapOf<Long, Long>()
                
                // Find min generation to normalize them starting from 0
                var minGen = Int.MAX_VALUE
                for (i in 0 until personsArr.length()) {
                    val pObj = personsArr.getJSONObject(i)
                    val gen = pObj.getInt("generation")
                    if (gen < minGen) {
                        minGen = gen
                    }
                }
                if (minGen == Int.MAX_VALUE) minGen = 0
 
                for (i in 0 until personsArr.length()) {
                    val pObj = personsArr.getJSONObject(i)
                    val oldId = pObj.getLong("id")
                    val firstName = pObj.getString("firstName")
                    val lastName = pObj.getString("lastName")
                    val gender = pObj.getString("gender")
                    val birthDate = if (pObj.isNull("birthDate")) null else pObj.getString("birthDate")
                    val birthPlace = if (pObj.isNull("birthPlace")) null else pObj.getString("birthPlace")
                    val deathDate = if (pObj.isNull("deathDate")) null else pObj.getString("deathDate")
                    val deathPlace = if (pObj.isNull("deathPlace")) null else pObj.getString("deathPlace")
                    val isDeceased = pObj.getBoolean("isDeceased")
                    val occupation = if (pObj.isNull("occupation")) null else pObj.getString("occupation")
                    val biography = if (pObj.isNull("biography")) null else pObj.getString("biography")
                    val originalPhotoUri = if (pObj.isNull("photoUri")) null else pObj.getString("photoUri")
                    val generation = pObj.getInt("generation")
                    
                    // Normalize generation
                    val normalizedGen = (generation - minGen).coerceAtLeast(0)
                    
                    // Restore photos from Base64
                    var restoredPhotoUri: String? = null
                    if (pObj.has("photosBase64")) {
                        val photosBase64Arr = pObj.getJSONArray("photosBase64")
                        val restoredPaths = mutableListOf<String>()
                        for (j in 0 until photosBase64Arr.length()) {
                            val b64 = photosBase64Arr.getString(j)
                            val savedPath = saveBase64Image(b64)
                            if (savedPath != null) {
                                restoredPaths.add(savedPath)
                            }
                        }
                        if (restoredPaths.isNotEmpty()) {
                            restoredPhotoUri = restoredPaths.joinToString("|")
                        }
                    }
                    if (restoredPhotoUri == null) {
                        restoredPhotoUri = originalPhotoUri
                    }
 
                    val newPersonId = repository.insertPerson(
                        com.example.data.Person(
                            id = 0, // Let Room auto-generate a new ID
                            firstName = firstName,
                            lastName = lastName,
                            gender = gender,
                            birthDate = birthDate,
                            birthPlace = birthPlace,
                            deathDate = deathDate,
                            deathPlace = deathPlace,
                            isDeceased = isDeceased,
                            occupation = occupation,
                            biography = biography,
                            photoUri = restoredPhotoUri,
                            generation = normalizedGen,
                            groupId = newGroupId
                        )
                    )
                    oldToNewIdMap[oldId] = newPersonId
                }
                
                for (i in 0 until relsArr.length()) {
                    val rObj = relsArr.getJSONObject(i)
                    val oldP1 = rObj.getLong("personId1")
                    val oldP2 = rObj.getLong("personId2")
                    val type = rObj.getString("type")
                    
                    val newP1 = oldToNewIdMap[oldP1]
                    val newP2 = oldToNewIdMap[oldP2]
                    
                    if (newP1 != null && newP2 != null) {
                        repository.insertRelationship(
                            com.example.data.Relationship(
                                id = 0, // Auto-generate
                                personId1 = newP1,
                                personId2 = newP2,
                                type = type
                            )
                        )
                    }
                }
                
                // Set the active selected group to the new group so the user is immediately taken to it
                setSelectedGroupId(newGroupId)
                
                onComplete(true, "بکاپ با موفقیت در گروه جدید «$newGroupName» بازگردانی شد.", newGroupId)
            } catch (e: Exception) {
                onComplete(false, "خطا در بازگردانی بکاپ: ${e.localizedMessage}", null)
            }
        }
    }
}

data class FamilyStats(
    val totalCount: Int = 0,
    val livingCount: Int = 0,
    val deceasedCount: Int = 0,
    val malesCount: Int = 0,
    val femalesCount: Int = 0,
    val avgLivingAge: Int = 0,
    val avgDeceasedAge: Int = 0,
    val mostCommonFirstName: String = "-",
    val mostCommonLastName: String = "-",
    val mostCommonBoyName: String = "-",
    val mostCommonGirlName: String = "-"
)

data class FamilyEvent(
    val personId: Long,
    val title: String,
    val description: String,
    val dateString: String,
    val daysRemaining: Int,
    val type: String // "Birthday", "DeathAnniversary", "Wedding"
)
