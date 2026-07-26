import java.util.*

val parentsMap = mapOf<Long, List<Long>>(
    2L to listOf(1L), // 2's parent is 1
    3L to listOf(1L), // 3's parent is 1
    4L to listOf(2L), // 4's parent is 2
    5L to listOf(3L)  // 5's parent is 3
)
val childrenMap = mapOf<Long, List<Long>>(
    1L to listOf(2L, 3L),
    2L to listOf(4L),
    3L to listOf(5L)
)
val spousesMap = mapOf<Long, List<Long>>()

val focusPersonId = 2L // We focus on 2. 3 is a sibling. 5 is sibling's child.

val visitedSet = mutableSetOf<Long>()
val ancestorQueue: Queue<Long> = LinkedList()
ancestorQueue.add(focusPersonId)
visitedSet.add(focusPersonId)
while (ancestorQueue.isNotEmpty()) {
    val curr = ancestorQueue.poll() ?: continue
    parentsMap[curr]?.forEach { parentId ->
        if (visitedSet.add(parentId)) {
            ancestorQueue.add(parentId)
        }
    }
}

val descendantsQueue: Queue<Long> = LinkedList()
descendantsQueue.add(focusPersonId)
while (descendantsQueue.isNotEmpty()) {
    val curr = descendantsQueue.poll() ?: continue
    childrenMap[curr]?.forEach { childId ->
        if (visitedSet.add(childId)) {
            descendantsQueue.add(childId)
        }
    }
}

parentsMap[focusPersonId]?.forEach { parentId ->
    childrenMap[parentId]?.forEach { siblingId ->
        visitedSet.add(siblingId)
    }
}

println("Visited: $visitedSet") // Should be 1 (parent), 2 (focus), 3 (sibling), 4 (child). NOT 5 (sibling's child)
