package app.rippl.profile

data class TaskMix(
    val writing: Double = 0.0,
    val coding: Double = 0.0,
    val research: Double = 0.0,
    val planning: Double = 0.0,
    val communication: Double = 0.0,
    val other: Double = 0.0
) {
    fun sum() = writing + coding + research + planning + communication + other

    companion object {
        val GLOBAL_DEFAULT = TaskMix(
            writing = 0.15,
            coding = 0.15,
            research = 0.2,
            planning = 0.15,
            communication = 0.2,
            other = 0.15
        )
    }
}
