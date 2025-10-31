package space.harbour.coffee.task.service.mapper

import space.harbour.coffee.task.controller.dto.TaskResponse
import space.harbour.coffee.task.domain.Task
import space.harbour.coffee.task.persistence.TaskEntity

// TaskEntity -> Task (Domain)
fun TaskEntity.toDomain(): Task {
    return Task(
        id = this.id ?: 0L,
        description = this.description,
        status = this.status,
    )
}

// Task (Domain) -> TaskEntity
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = if (this.id == 0L) null else this.id,
        description = this.description,
        status = this.status
    )
}

// Task (Domain) -> TaskResponse (DTO)
fun Task.toResponse(): TaskResponse {
    return TaskResponse(
        id = this.id,
        description = this.description,
        status = this.status
    )
}

// TaskResponse (DTO) -> Task (Domain)
fun TaskResponse.toDomain(): Task {
    return Task(
        id = this.id,
        description = this.description,
        status = this.status,
    )
}
