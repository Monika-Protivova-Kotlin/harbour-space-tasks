package space.harbour.coffee.task.domain

import java.time.ZonedDateTime

typealias TaskId = Long

data class Task(
    val id: TaskId,
    val description: String,
    val status: TaskStatus
)
