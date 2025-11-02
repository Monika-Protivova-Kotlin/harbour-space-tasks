package space.harbour.tasks.task.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import space.harbour.tasks.task.domain.TaskStatus
import space.harbour.tasks.task.persistence.TaskEntity

@DataJpaTest
class TaskRepositoryTest : FunSpec() {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Autowired
    private lateinit var taskRepository: TaskRepository

    init {

        context("save() - Creating new tasks") {

            test("Should save new task and generate ID") {
                val newTask = TaskEntity(
                    description = "Test Task",
                    status = TaskStatus.NEW
                )

                val savedTask = taskRepository.save(newTask)

                savedTask.id.shouldNotBe(null)
                savedTask.description.shouldBe("Test Task")
                savedTask.status.shouldBe(TaskStatus.NEW)
            }

            test("Should save task with all fields correctly") {
                val newTask = TaskEntity(
                    description = "Important Task",
                    status = TaskStatus.IN_PROGRESS
                )

                val savedTask = taskRepository.save(newTask)
                entityManager.flush()
                entityManager.clear()

                val foundTask = entityManager.find(TaskEntity::class.java, savedTask.id)

                foundTask.shouldNotBe(null)
                foundTask.id.shouldBe(savedTask.id)
                foundTask.description.shouldBe("Important Task")
                foundTask.status.shouldBe(TaskStatus.IN_PROGRESS)
            }
        }

        context("findById()") {

            test("Should find existing task by ID") {
                val task = TaskEntity(
                    description = "Find Me",
                    status = TaskStatus.COMPLETED
                )
                val persistedTask = entityManager.persist(task)
                entityManager.flush()

                val foundTask = taskRepository.findById(persistedTask.id!!)

                foundTask.isPresent.shouldBe(true)
                with(foundTask.get()) {
                    id.shouldBe(persistedTask.id)
                    description.shouldBe("Find Me")
                    status.shouldBe(TaskStatus.COMPLETED)
                }
            }

            test("Should return empty Optional when task doesn't exist") {
                val foundTask = taskRepository.findById(999L)

                foundTask.isPresent.shouldBe(false)
            }
        }

        context("findAll()") {

            test("Should return all tasks") {
                entityManager.persist(TaskEntity(description = "Task 1", status = TaskStatus.NEW))
                entityManager.persist(TaskEntity(description = "Task 2", status = TaskStatus.IN_PROGRESS))
                entityManager.persist(TaskEntity(description = "Task 3", status = TaskStatus.COMPLETED))
                entityManager.flush()

                val tasks = taskRepository.findAll()

                tasks.size.shouldBe(3)
                tasks.map { it.description }.shouldBe(listOf("Task 1", "Task 2", "Task 3"))
                tasks.map { it.status }.shouldBe(
                    listOf(TaskStatus.NEW, TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED)
                )
            }

            test("Should return empty list when no tasks exist") {
                val tasks = taskRepository.findAll()

                tasks.size.shouldBe(0)
                tasks.shouldBe(emptyList())
            }
        }

        context("save() - Updating existing tasks") {

            test("Should update existing task fields") {
                val task = TaskEntity(
                    description = "Original Description",
                    status = TaskStatus.NEW
                )
                val persistedTask = entityManager.persist(task)
                entityManager.flush()
                entityManager.clear()

                val taskToUpdate = taskRepository.findById(persistedTask.id!!).get()
                taskToUpdate.description = "Updated Description"
                taskToUpdate.status = TaskStatus.COMPLETED

                taskRepository.save(taskToUpdate)
                entityManager.flush()
                entityManager.clear()

                val verifiedTask = entityManager.find(TaskEntity::class.java, persistedTask.id)

                verifiedTask.shouldNotBe(null)
                verifiedTask.id.shouldBe(persistedTask.id)
                verifiedTask.description.shouldBe("Updated Description")
                verifiedTask.status.shouldBe(TaskStatus.COMPLETED)
            }
        }

        context("existsById()") {

            test("Should return true when task exists") {
                val task = TaskEntity(
                    description = "Existing Task",
                    status = TaskStatus.NEW
                )
                val persistedTask = entityManager.persist(task)
                entityManager.flush()

                val exists = taskRepository.existsById(persistedTask.id!!)

                exists.shouldBe(true)
            }

            test("Should return false when task doesn't exist") {
                val exists = taskRepository.existsById(999L)

                exists.shouldBe(false)
            }
        }

        context("deleteById()") {

            test("Should delete existing task") {
                val task = TaskEntity(
                    description = "Task to Delete",
                    status = TaskStatus.REJECTED
                )
                val persistedTask = entityManager.persist(task)
                entityManager.flush()
                val taskId = persistedTask.id!!

                taskRepository.deleteById(taskId)
                entityManager.flush()

                val exists = taskRepository.existsById(taskId)
                exists.shouldBe(false)
            }

            test("Should verify task is deleted from database") {
                val task = TaskEntity(
                    description = "Another Task to Delete",
                    status = TaskStatus.NEW
                )
                val persistedTask = entityManager.persist(task)
                entityManager.flush()
                val taskId = persistedTask.id!!

                taskRepository.deleteById(taskId)
                entityManager.flush()
                entityManager.clear()

                val deletedTask = entityManager.find(TaskEntity::class.java, taskId)
                deletedTask.shouldBe(null)
            }
        }

        context("count()") {

            test("Should return correct count of tasks") {
                entityManager.persist(TaskEntity(description = "Task 1", status = TaskStatus.NEW))
                entityManager.persist(TaskEntity(description = "Task 2", status = TaskStatus.IN_PROGRESS))
                entityManager.persist(TaskEntity(description = "Task 3", status = TaskStatus.COMPLETED))
                entityManager.persist(TaskEntity(description = "Task 4", status = TaskStatus.REJECTED))
                entityManager.flush()

                val count = taskRepository.count()

                count.shouldBe(4)
            }
        }
    }
}
