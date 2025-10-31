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
            val id = 1L

            test("should create and save new task with default NEW status") {
                val savedTaskEntity = TaskEntity(
                    id = id,
                    description = "New Task",
                    status = TaskStatus.NEW
                )

                every { taskRepository.save(any()) } returns savedTaskEntity

                val result = taskService.addTask("New Task")

                result shouldNotBe null
                result.id shouldBe id
                result.description shouldBe "New Task"
                result.status shouldBe TaskStatus.NEW

                verify(exactly = 1) {
                    taskRepository.save(match {
                        it.description == "New Task" && it.status == TaskStatus.NEW
                    })
                }
            }

            test("should create and save new task with custom status") {
                val savedTaskEntity = TaskEntity(
                    id = id,
                    description = "New Task",
                    status = TaskStatus.IN_PROGRESS
                )

                every { taskRepository.save(any()) } returns savedTaskEntity

                val result = taskService.addTask("New Task", TaskStatus.IN_PROGRESS)

                result shouldNotBe null
                result.id shouldBe id
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
            val id = 1L

            test("should throw TaskNotFoundException when task does not exist") {
                every { taskRepository.findById(id) } returns Optional.empty()

                val updatedTask = Task(
                    id = id,
                    description = "Updated Task",
                    status = TaskStatus.COMPLETED
                )

                val exception = shouldThrow<TaskNotFoundException> {
                    taskService.updateTask(id, updatedTask)
                }

                exception.message shouldBe "Task with id 1 not found"

                verify(exactly = 1) { taskRepository.findById(id) }
                verify(exactly = 0) { taskRepository.save(any()) }
            }

            test("should update existing task") {
                val existingTaskEntity = TaskEntity(
                    id = id,
                    description = "Old Task",
                    status = TaskStatus.NEW
                )
                every { taskRepository.findById(id) } returns Optional.of(existingTaskEntity)
                every { taskRepository.save(any()) } answers { firstArg() }

                val updatedTask = Task(
                    id = id,
                    description = "Updated Task",
                    status = TaskStatus.COMPLETED
                )

                val result = taskService.updateTask(id, updatedTask)

                result shouldNotBe null
                result.id shouldBe id
                result.description shouldBe "Updated Task"
                result.status shouldBe TaskStatus.COMPLETED

                verify(exactly = 1) { taskRepository.findById(id) }
                verify(exactly = 1) {
                    taskRepository.save(match {
                        it.description == "Updated Task" && it.status == TaskStatus.COMPLETED
                    })
                }
            }

            test("should only update description and status, not id") {
                val id = 5L
                val existingTaskEntity = TaskEntity(id = id, description = "Old Task", status = TaskStatus.NEW)
                every { taskRepository.findById(id) } returns Optional.of(existingTaskEntity)
                every { taskRepository.save(any()) } answers { firstArg() }

                val updatedTask = Task(id = 999, description = "Updated Task", status = TaskStatus.IN_PROGRESS)
                val result = taskService.updateTask(id, updatedTask)

                result shouldNotBe null
                result.id shouldBe id  // Should keep original ID, not 999
                result.description shouldBe "Updated Task"
                result.status shouldBe TaskStatus.IN_PROGRESS


                verify(exactly = 1) { taskRepository.findById(id) }
            }
        }

        context("deleteTask") {
            val id = 1L

            test("should throw TaskNotFoundException when task does not exist") {
                every { taskRepository.existsById(id) } returns false

                val exception = shouldThrow<TaskNotFoundException> {
                    taskService.deleteTask(id)
                }

                exception.message shouldBe "Task with id 1 not found"
                verify(exactly = 1) { taskRepository.existsById(id) }
                verify(exactly = 0) { taskRepository.deleteById(any()) }
            }

            test("should delete task successfully when it exists") {
                every { taskRepository.existsById(id) } returns true
                every { taskRepository.deleteById(id) } just Runs

                taskService.deleteTask(1L)

                verify(exactly = 1) { taskRepository.existsById(id) }
                verify(exactly = 1) { taskRepository.deleteById(id) }
            }
        }
    }
}
