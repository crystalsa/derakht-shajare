fun computeTreeLayoutPositions(
    persons: List<Person>,
    relationships: List<Relationship>,
    layoutType: String,
    focusPersonId: Long?
): Map<String, TreePos> {
