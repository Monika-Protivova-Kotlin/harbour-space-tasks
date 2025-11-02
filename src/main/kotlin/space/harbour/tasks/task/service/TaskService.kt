package space.harbour.tasks.task.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import space.harbour.tasks.task.domain.Task
import space.harbour.tasks.task.domain.TaskStatus
import space.harbour.tasks.task.exception.InvalidTaskException
import space.harbour.tasks.task.exception.TaskNotFoundException
import space.harbour.tasks.task.exception.TaskOperationException
import space.harbour.tasks.task.persistence.TaskEntity
import space.harbour.tasks.task.repository.TaskRepository
import space.harbour.tasks.task.service.mapper.toDomain

@Service
class TaskService(
    private val taskRepository: TaskRepository,
) {
    fun getTasks(): List<Task> {
        return try {
            taskRepository.findAll().map { it.toDomain() }
        } catch (ex: Exception) {
            throw TaskOperationException("Failed to retrieve tasks", ex)
        }
    }

    fun getTask(id: Long): Task {
        return taskRepository.findById(id)
            .map { it.toDomain() }
            .orElseThrow { TaskNotFoundException(id) }
    }

    @Transactional
    fun addTask(description: String): Task {
        // Validation logic moved from controller
        if (description.isBlank()) {
            throw InvalidTaskException("Task description cannot be blank")
        }

        return try {
            val newTask = TaskEntity(
                description = description,
                status = TaskStatus.NEW
            )
            val savedTask = taskRepository.save(newTask)
            savedTask.toDomain()
        } catch (ex: InvalidTaskException) {
            throw ex
        } catch (ex: Exception) {
            throw TaskOperationException("Failed to create task", ex)
        }
    }

    @Transactional
    fun updateTask(id: Long, updatedTask: Task): Task {
        // Validation logic moved from controller
        if (updatedTask.description.isBlank()) {
            throw InvalidTaskException("Task description cannot be blank")
        }

        val existingTask = taskRepository.findById(id)
            .orElseThrow { TaskNotFoundException(id) }

        return try {
            existingTask.description = updatedTask.description
            existingTask.status = updatedTask.status
            val savedTask = taskRepository.save(existingTask)
            savedTask.toDomain()
        } catch (ex: InvalidTaskException) {
            throw ex
        } catch (ex: Exception) {
            throw TaskOperationException("Failed to update task with id $id", ex)
        }
    }

    fun deleteTask(id: Long) {
        if (!taskRepository.existsById(id)) {
            throw TaskNotFoundException(id)
        }
        try {
            taskRepository.deleteById(id)
        } catch (ex: Exception) {
            throw TaskOperationException("Failed to delete task with id $id", ex)
        }
    }
}
