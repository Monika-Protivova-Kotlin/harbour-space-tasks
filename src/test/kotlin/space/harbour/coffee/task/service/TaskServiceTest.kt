package space.harbour.coffee.task.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.*
import space.harbour.coffee.task.domain.Task
import space.harbour.coffee.task.domain.TaskStatus
import space.harbour.coffee.task.exception.TaskNotFoundException
import space.harbour.coffee.task.persistence.TaskEntity
import space.harbour.coffee.task.repository.TaskRepository
import space.harbour.coffee.task.service.TaskService
import java.util.*

class TaskServiceTest : FunSpec() {

    private val taskRepository: TaskRepository = mockk()
    private val taskService = TaskService(taskRepository)

    init {

        beforeEach {
            clearAllMocks()
        }

        context("getTasks") {
            test("should return empty list when no tasks exist") {
                every { taskRepository.findAll() } returns emptyList()

                val result = taskService.getTasks()

                result.shouldBeEmpty()
                verify(exactly = 1) { taskRepository.findAll() }
            }

            test("should return all tasks") {
                val taskEntities = listOf(
                    TaskEntity(id = 1L, description = "Task 1", status = TaskStatus.NEW),
                    TaskEntity(id = 2L, description = "Task 2", status = TaskStatus.IN_PROGRESS)
                )
                every { taskRepository.findAll() } returns taskEntities

                val result = taskService.getTasks()

                result.shouldBe(
                    listOf(
                        Task(id = 1, description = "Task 1", status = TaskStatus.NEW),
                        Task(id = 2, description = "Task 2", status = TaskStatus.IN_PROGRESS)
                    )
                )

                verify(exactly = 1) {
                    taskRepository.findAll()
                }
            }
        }

        context("getTask") {
            test("should throw TaskNotFoundException when task does not exist") {
                val id = 999L
                every { taskRepository.findById(id) } returns Optional.empty()

                val exception = shouldThrow<TaskNotFoundException> {
                    taskService.getTask(id)
                }

                exception.message shouldBe "Task with id $id not found"

                verify(exactly = 1) {
                    taskRepository.findById(id)
                }
            }

            test("should return task when it exists") {
                val id = 1L
                val taskEntity = TaskEntity(
                    id = id,
                    description = "Task 1",
                    status = TaskStatus.NEW
                )

                every { taskRepository.findById(id) } returns Optional.of(taskEntity)

                val result = taskService.getTask(1L)

                result.shouldBe(
                    Task(
                        id = id,
                        description = "Task 1",
                        status = TaskStatus.NEW
                    )
                )

                verify(exactly = 1) {
                    taskRepository.findById(id)
                }
            }
        }

        context("addTask") {
            test("should create and save new task with default NEW status") {
                val taskEntity = TaskEntity(description = "New Task", status = TaskStatus.NEW)
                val savedTaskEntity = TaskEntity(id = 1L, description = "New Task", status = TaskStatus.NEW)

                every { taskRepository.save(any()) } returns savedTaskEntity

                val result = taskService.addTask("New Task")

                result shouldNotBe null
                result.id shouldBe 1
                result.description shouldBe "New Task"
                result.status shouldBe TaskStatus.NEW
                verify(exactly = 1) {
                    taskRepository.save(match {
                        it.description == "New Task" && it.status == TaskStatus.NEW
                    })
                }
            }

            test("should create and save new task with custom status") {
                val savedTaskEntity = TaskEntity(id = 1L, description = "New Task", status = TaskStatus.IN_PROGRESS)

                every { taskRepository.save(any()) } returns savedTaskEntity

                val result = taskService.addTask("New Task", TaskStatus.IN_PROGRESS)

                result shouldNotBe null
                result.id shouldBe 1
                result.description shouldBe "New Task"
                result.status shouldBe TaskStatus.IN_PROGRESS
                verify(exactly = 1) {
                    taskRepository.save(match {
                        it.description == "New Task" && it.status == TaskStatus.IN_PROGRESS
                    })
                }
            }
        }

        context("updateTask") {
            test("should throw TaskNotFoundException when task does not exist") {
                every { taskRepository.findById(1L) } returns Optional.empty()

                val updatedTask = Task(id = 1, description = "Updated Task", status = TaskStatus.COMPLETED)

                val exception = shouldThrow<TaskNotFoundException> {
                    taskService.updateTask(1L, updatedTask)
                }

                exception.message shouldBe "Task with id 1 not found"
                verify(exactly = 1) { taskRepository.findById(1L) }
                verify(exactly = 0) { taskRepository.save(any()) }
            }

            test("should update existing task") {
                val existingTaskEntity = TaskEntity(id = 1L, description = "Old Task", status = TaskStatus.NEW)
                every { taskRepository.findById(1L) } returns Optional.of(existingTaskEntity)
                every { taskRepository.save(any()) } answers { firstArg() }

                val updatedTask = Task(id = 1, description = "Updated Task", status = TaskStatus.COMPLETED)
                val result = taskService.updateTask(1L, updatedTask)

                result shouldNotBe null
                result.id shouldBe 1
                result.description shouldBe "Updated Task"
                result.status shouldBe TaskStatus.COMPLETED

                verify(exactly = 1) { taskRepository.findById(1L) }
                verify(exactly = 1) {
                    taskRepository.save(match {
                        it.description == "Updated Task" && it.status == TaskStatus.COMPLETED
                    })
                }
            }

            test("should only update description and status, not id") {
                val existingTaskEntity = TaskEntity(id = 5L, description = "Old Task", status = TaskStatus.NEW)
                every { taskRepository.findById(5L) } returns Optional.of(existingTaskEntity)
                every { taskRepository.save(any()) } answers { firstArg() }

                val updatedTask = Task(id = 999, description = "Updated Task", status = TaskStatus.IN_PROGRESS)
                val result = taskService.updateTask(5L, updatedTask)

                result shouldNotBe null
                result.id shouldBe 5  // Should keep original ID
                result.description shouldBe "Updated Task"
                result.status shouldBe TaskStatus.IN_PROGRESS

                verify(exactly = 1) { taskRepository.findById(5L) }
            }
        }

        context("deleteTask") {
            test("should throw TaskNotFoundException when task does not exist") {
                every { taskRepository.existsById(1L) } returns false

                val exception = shouldThrow<TaskNotFoundException> {
                    taskService.deleteTask(1L)
                }

                exception.message shouldBe "Task with id 1 not found"
                verify(exactly = 1) { taskRepository.existsById(1L) }
                verify(exactly = 0) { taskRepository.deleteById(any()) }
            }

            test("should delete task successfully when it exists") {
                every { taskRepository.existsById(1L) } returns true
                every { taskRepository.deleteById(1L) } just Runs

                taskService.deleteTask(1L)

                verify(exactly = 1) { taskRepository.existsById(1L) }
                verify(exactly = 1) { taskRepository.deleteById(1L) }
            }
        }
    }
}
