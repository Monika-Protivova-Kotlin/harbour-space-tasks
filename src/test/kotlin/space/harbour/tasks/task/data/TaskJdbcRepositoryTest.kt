package space.harbour.tasks.task.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.jdbc.Sql
import space.harbour.tasks.task.domain.TaskStatus

/**
 * Tests for TaskJdbcRepository using H2 in-memory database.
 *
 * **Testing Strategy:**
 * - @JdbcTest: Spring Boot test slice for JDBC (lighter than full @SpringBootTest)
 * - AutoConfigureTestDatabase: Uses H2 in-memory database automatically
 * - @Import: Brings in our TaskJdbcRepository to test
 * - @Sql: Initializes database schema before tests
 *
 * For students: This demonstrates testing JDBC repositories with Spring Boot's test support.
 * Compare with TaskRepositoryTest (@DataJpaTest) - similar pattern, different technology.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import(TaskJdbcRepository::class)  // Import our JDBC repository to test
@Sql("/schema.sql")  // Initialize database schema (create tables)
class TaskJdbcRepositoryTest : FunSpec() {

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var taskJdbcRepository: TaskJdbcRepository

    init {

        context("save() - Creating new tasks") {

            test("Should save new task and generate ID") {
                val newTask = TaskEntity(
                    description = "JDBC Test Task",
                    status = TaskStatus.NEW
                )

                val savedTask = taskJdbcRepository.save(newTask)

                savedTask.id shouldNotBe null
                savedTask.description shouldBe "JDBC Test Task"
                savedTask.status shouldBe TaskStatus.NEW
            }

            test("Should save multiple tasks with unique IDs") {
                val task1 = taskJdbcRepository.save(
                    TaskEntity(description = "Task 1", status = TaskStatus.NEW)
                )
                val task2 = taskJdbcRepository.save(
                    TaskEntity(description = "Task 2", status = TaskStatus.IN_PROGRESS)
                )

                task1.id shouldNotBe task2.id
                task1.description shouldBe "Task 1"
                task2.description shouldBe "Task 2"
            }
        }

        context("save() - Updating existing tasks") {

            test("Should update existing task") {
                val savedTask = taskJdbcRepository.save(
                    TaskEntity(description = "Original", status = TaskStatus.NEW)
                )

                // Update the task (modify the existing entity)
                savedTask.description = "Updated"
                savedTask.status = TaskStatus.COMPLETED

                val result = taskJdbcRepository.save(savedTask)

                result.id shouldBe savedTask.id
                result.description shouldBe "Updated"
                result.status shouldBe TaskStatus.COMPLETED
            }
        }

        context("findById()") {

            test("Should find existing task by ID") {
                val savedTask = taskJdbcRepository.save(
                    TaskEntity(description = "Find Me", status = TaskStatus.NEW)
                )

                val found = taskJdbcRepository.findById(savedTask.id!!)

                found.isPresent shouldBe true
                found.get().id shouldBe savedTask.id
                found.get().description shouldBe "Find Me"
                found.get().status shouldBe TaskStatus.NEW
            }

            test("Should return empty Optional for non-existent ID") {
                val found = taskJdbcRepository.findById(999L)

                found.isPresent shouldBe false
            }
        }

        context("findAll()") {

            test("Should return empty list when no tasks exist") {
                val tasks = taskJdbcRepository.findAll()

                tasks.size shouldBe 0
            }

            test("Should return all tasks") {
                taskJdbcRepository.save(TaskEntity(description = "Task 1", status = TaskStatus.NEW))
                taskJdbcRepository.save(TaskEntity(description = "Task 2", status = TaskStatus.IN_PROGRESS))
                taskJdbcRepository.save(TaskEntity(description = "Task 3", status = TaskStatus.COMPLETED))

                val tasks = taskJdbcRepository.findAll()

                tasks.size shouldBe 3
            }
        }

        context("existsById()") {

            test("Should return true for existing task") {
                val savedTask = taskJdbcRepository.save(
                    TaskEntity(description = "Exists", status = TaskStatus.NEW)
                )

                val exists = taskJdbcRepository.existsById(savedTask.id!!)

                exists shouldBe true
            }

            test("Should return false for non-existent task") {
                val exists = taskJdbcRepository.existsById(999L)

                exists shouldBe false
            }
        }

        context("deleteById()") {

            test("Should delete existing task") {
                val savedTask = taskJdbcRepository.save(
                    TaskEntity(description = "Delete Me", status = TaskStatus.NEW)
                )

                taskJdbcRepository.deleteById(savedTask.id!!)

                val exists = taskJdbcRepository.existsById(savedTask.id!!)
                exists shouldBe false
            }

            test("Should not throw error when deleting non-existent task") {
                // JDBC delete doesn't fail if row doesn't exist
                taskJdbcRepository.deleteById(999L)

                // No exception should be thrown
            }
        }

        context("JDBC vs JPA comparison") {

            test("JDBC requires explicit insert vs update logic") {
                // For students: Notice how we had to check entity.id == null in TaskJdbcRepository
                // JPA does this automatically with @Id @GeneratedValue

                val newTask = TaskEntity(description = "New", status = TaskStatus.NEW)
                newTask.id shouldBe null  // No ID yet

                val savedTask = taskJdbcRepository.save(newTask)
                savedTask.id shouldNotBe null  // ID generated after save

                // Update requires entity to have ID
                savedTask.description = "Updated"
                taskJdbcRepository.save(savedTask)
            }

            test("JDBC gives us full control over SQL") {
                // For students: With JDBC, we write explicit SQL (see TaskJdbcRepository)
                // With JPA, SQL is generated automatically
                //
                // JDBC advantage: We can optimize queries exactly how we want
                // JPA advantage: Less code, works across different databases

                val task = taskJdbcRepository.save(
                    TaskEntity(description = "SQL Control", status = TaskStatus.NEW)
                )

                task shouldNotBe null
                // The SQL used was our explicit INSERT statement in TaskJdbcRepository
            }
        }
    }
}
