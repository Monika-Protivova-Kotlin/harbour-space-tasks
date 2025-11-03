package space.harbour.tasks.task.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import space.harbour.tasks.task.domain.Task
import space.harbour.tasks.task.domain.TaskStatus
import space.harbour.tasks.task.exception.InvalidTaskException
import space.harbour.tasks.task.exception.TaskNotFoundException
import space.harbour.tasks.task.data.TaskEntity
import space.harbour.tasks.task.data.TaskRepository
import java.util.*

class TaskServiceTest : FunSpec() {

    private val taskRepository: TaskRepository = mockk()

    private val taskService = TaskService(
        taskRepository = taskRepository
    )

    init {

        beforeTest {
            clearAllMocks()
        }

        context("getTasks") {

            test("Should return all tasks") {
                every { taskRepository.findAll() } returns listOf(
                    TaskEntity(id = 1L, description = "Task 1", status = TaskStatus.NEW),
                    TaskEntity(id = 2L, description = "Task 2", status = TaskStatus.IN_PROGRESS),
                    TaskEntity(id = 3L, description = "Task 3", status = TaskStatus.COMPLETED)
                )

                taskService.getTasks().shouldBe(
                    listOf(
                        Task(id = 1L, description = "Task 1", status = TaskStatus.NEW),
                        Task(id = 2L, description = "Task 2", status = TaskStatus.IN_PROGRESS),
                        Task(id = 3L, description = "Task 3", status = TaskStatus.COMPLETED)
                    )
                )

                verify(exactly = 1) {
                    taskRepository.findAll()
                }
            }

            test("Should return empty list when no tasks exist") {
                every { taskRepository.findAll() } returns emptyList()

                taskService.getTasks().shouldBe(emptyList())

                verify(exactly = 1) {
                    taskRepository.findAll()
                }
            }
        }

        context("getTask by id") {

            test("Should get task by id - task exists") {
                val taskId = 1L
                val expectedTask = Task(
                    id = taskId,
                    description = "Test Task",
                    status = TaskStatus.NEW
                )

                every {
                    taskRepository.findById(any())
                } returns Optional.of(
                    TaskEntity(
                        id = expectedTask.id,
                        description = expectedTask.description,
                        status = expectedTask.status
                    )
                )

                taskService.getTask(taskId).shouldBe(expectedTask)

                verify(exactly = 1) {
                    taskRepository.findById(taskId)
                }
            }

            test("Should get task by id - task does not exists") {
                val taskId = 999L

                every { taskRepository.findById(any()) } returns Optional.empty()

                val exception = shouldThrow<TaskNotFoundException> {
                    taskService.getTask(taskId)
                }

                exception.message.shouldBe("Task with id $taskId not found")

                verify(exactly = 1) {
                    taskRepository.findById(taskId)
                }
            }
        }

        context("addTask") {

            test("Should add task successfully") {
                val task = Task(
                    id = 1L,
                    description = "New Task",
                    status = TaskStatus.NEW
                )
                val taskEntity = TaskEntity(
                    id = task.id,
                    description = task.description,
                    status = task.status
                )

                every { taskRepository.save(any()) } returns taskEntity

                taskService.addTask(task.description).shouldBe(task)

                verify(exactly = 1) {
                    taskRepository.save(any())
                }
            }

            test("Should throw InvalidTaskException when description is blank") {
                val exception = shouldThrow<InvalidTaskException> {
                    taskService.addTask("")
                }

                exception.message.shouldBe("Task description cannot be blank")

                verify(exactly = 0) {
                    taskRepository.save(any())
                }
            }

            test("Should throw InvalidTaskException when description is whitespace only") {
                val exception = shouldThrow<InvalidTaskException> {
                    taskService.addTask("   ")
                }

                exception.message.shouldBe("Task description cannot be blank")

                verify(exactly = 0) {
                    taskRepository.save(any())
                }
            }
        }

        context("updateTask by id") {

            test("Should update task successfully") {
                val taskId = 1L
                val existingEntity = TaskEntity(
                    id = taskId,
                    description = "Old Description",
                    status = TaskStatus.NEW
                )
                val taskUpdate = Task(
                    id = taskId,
                    description = "Updated Description",
                    status = TaskStatus.COMPLETED
                )
                val updatedEntity = TaskEntity(
                    id = taskId,
                    description = taskUpdate.description,
                    status = taskUpdate.status
                )

                every { taskRepository.findById(any()) } returns Optional.of(existingEntity)

                every {
                    taskRepository.save(any<TaskEntity>())
                } returns updatedEntity

                taskService.updateTask(
                    id = taskId,
                    updatedTask = taskUpdate
                ).shouldBe(taskUpdate)

                verify(exactly = 1) {
                    taskRepository.findById(taskId)
                    taskRepository.save(any())
                }
            }

            test("Should throw TaskNotFoundException when task does not exist") {
                val taskId = 999L
                val updatedTask = Task(
                    id = taskId,
                    description = "Updated",
                    status = TaskStatus.COMPLETED
                )

                every { taskRepository.findById(taskId) } returns Optional.empty()

                val exception = shouldThrow<TaskNotFoundException> {
                    taskService.updateTask(
                        id = taskId,
                        updatedTask = updatedTask
                    )
                }

                exception.message.shouldBe("Task with id $taskId not found")

                verify(exactly = 1) {
                    taskRepository.findById(taskId)
                }
                verify(exactly = 0) {
                    taskRepository.save(any())
                }
            }

            test("Should throw InvalidTaskException when description is blank") {
                val taskId = 1L
                val updatedTask = Task(
                    id = taskId,
                    description = "",
                    status = TaskStatus.COMPLETED
                )

                val exception = shouldThrow<InvalidTaskException> {
                    taskService.updateTask(
                        id = taskId,
                        updatedTask = updatedTask
                    )
                }

                exception.message.shouldBe("Task description cannot be blank")

                verify(exactly = 0) {
                    taskRepository.findById(any())
                    taskRepository.save(any())
                }
            }

            test("Should throw InvalidTaskException when description is whitespace only") {
                val taskId = 1L
                val updatedTask = Task(
                    id = taskId,
                    description = "   ",
                    status = TaskStatus.COMPLETED
                )

                val exception = shouldThrow<InvalidTaskException> {
                    taskService.updateTask(
                        id = taskId,
                        updatedTask = updatedTask
                    )
                }

                exception.message.shouldBe("Task description cannot be blank")

                verify(exactly = 0) {
                    taskRepository.findById(any())
                    taskRepository.save(any())
                }
            }
        }

        context("deleteTask by id") {

            test("Should delete task successfully") {
                val taskId = 1L

                every { taskRepository.existsById(any()) } returns true
                every { taskRepository.deleteById(any()) } returns Unit

                taskService.deleteTask(id = taskId)

                verify(exactly = 1) {
                    taskRepository.existsById(taskId)
                    taskRepository.deleteById(taskId)
                }
            }

            test("Should throw TaskNotFoundException when task does not exist") {
                val taskId = 999L

                every { taskRepository.existsById(any()) } returns false

                val exception = shouldThrow<TaskNotFoundException> {
                    taskService.deleteTask(id = taskId)
                }

                exception.message.shouldBe("Task with id $taskId not found")

                verify(exactly = 1) {
                    taskRepository.existsById(taskId)
                }
                verify(exactly = 0) {
                    taskRepository.deleteById(any())
                }
            }
        }
    }
}
