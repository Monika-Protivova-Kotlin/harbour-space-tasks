package space.harbour.tasks.task.controller.dto

import space.harbour.tasks.task.domain.TaskId
import space.harbour.tasks.task.domain.TaskStatus

data class TaskResponse(
    val id: TaskId,
    val description: String,
    val status: TaskStatus
)
