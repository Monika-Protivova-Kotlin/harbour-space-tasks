package space.harbour.coffee.task.controller.dto

import space.harbour.coffee.task.domain.TaskId
import space.harbour.coffee.task.domain.TaskStatus

data class TaskResponse(
    val id: TaskId,
    val description: String,
    val status: TaskStatus
)