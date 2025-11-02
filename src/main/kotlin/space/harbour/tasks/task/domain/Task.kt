package space.harbour.tasks.task.domain

typealias TaskId = Long

data class Task(
    val id: TaskId,
    val description: String,
    val status: TaskStatus
)
