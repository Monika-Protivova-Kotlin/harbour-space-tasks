package space.harbour.tasks.task.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import space.harbour.tasks.task.controller.dto.NewTaskRequest
import space.harbour.tasks.task.controller.dto.TaskResponse
import space.harbour.tasks.task.service.TaskService
import space.harbour.tasks.task.service.mapper.toDomain
import space.harbour.tasks.task.service.mapper.toResponse

@RestController
@RequestMapping("/api/tasks")
class TaskController(
    private val taskService: TaskService,
) {

    @GetMapping
    fun getTasks(): List<TaskResponse> {
        return taskService.getTasks().map { it.toResponse() }
    }

    @GetMapping("/{id}")
    fun getTask(
        @PathVariable(name = "id") taskId: Long
    ): TaskResponse {
        val task = taskService.getTask(taskId)
        return task.toResponse()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(
        @RequestBody request: NewTaskRequest
    ): TaskResponse {
        // Validation moved to service layer
        val task = taskService.addTask(request.description)
        return task.toResponse()
    }

    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable(name = "id") taskId: Long,
        @RequestBody updatedTask: TaskResponse
    ): TaskResponse {
        // Validation moved to service layer
        val result = taskService.updateTask(taskId, updatedTask.toDomain())
        return result.toResponse()
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTask(
        @PathVariable(name = "id") taskId: Long
    ) {
        taskService.deleteTask(taskId)
    }
}
