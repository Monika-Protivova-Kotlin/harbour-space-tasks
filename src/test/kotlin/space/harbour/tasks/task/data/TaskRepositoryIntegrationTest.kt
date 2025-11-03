package space.harbour.tasks.task.data

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import space.harbour.tasks.task.domain.TaskStatus

/**
 * Integration tests using Testcontainers with real PostgreSQL database.
 *
 * **What is Testcontainers?**
 * - Starts real Docker containers for your tests (PostgreSQL, MySQL, Redis, etc.)
 * - Containers are automatically started before tests and stopped after
 * - Tests run against a REAL database, not in-memory H2
 * - Perfect for integration testing with production-like environment
 *
 * **Why use Testcontainers?**
 * 1. **Real database behavior** - H2 is great but has differences from PostgreSQL/MySQL
 * 2. **Database-specific features** - Test PostgreSQL JSONB, MySQL full-text search, etc.
 * 3. **Migration testing** - Test Flyway/Liquibase migrations against real DB
 * 4. **Confidence** - If tests pass here, they'll likely work in production
 *
 * **When to use Testcontainers vs H2:**
 * - H2: Fast unit tests, basic CRUD operations, CI/CD (if Docker not available)
 * - Testcontainers: Integration tests, database-specific features, pre-production validation
 *
 * **Requirements:**
 * - Docker must be running on your machine
 * - Takes longer than H2 tests (starts container each time)
 * - Uses more resources (Docker container)
 *
 * For students: This demonstrates production-like testing.
 * Many companies use Testcontainers for integration tests before deployment.
 */
@DataJpaTest
@Testcontainers  // Enables Testcontainers for this test class
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)  // Don't replace with H2
class TaskRepositoryIntegrationTest : FunSpec() {

    companion object {
        /**
         * PostgreSQL container definition.
         *
         * @Container tells Testcontainers to start this container before tests.
         * The container runs PostgreSQL 16 image from Docker Hub.
         *
         * For students: This is a REAL PostgreSQL database running in Docker!
         */
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("testuser")
            withPassword("testpass")
        }

        /**
         * Configure Spring to use the Testcontainers database.
         *
         * @DynamicPropertySource allows us to override application properties at runtime.
         * We use the container's JDBC URL, username, and password.
         *
         * For students: This is how we connect Spring to the container database!
         */
        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl)
            registry.add("spring.datasource.username", postgresContainer::getUsername)
            registry.add("spring.datasource.password", postgresContainer::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }  // Auto-create schema
        }
    }

    @Autowired
    private lateinit var taskRepository: TaskRepository

    init {

        context("Testcontainers PostgreSQL Integration") {

            test("Should save and retrieve task from real PostgreSQL database") {
                // This test runs against a REAL PostgreSQL instance, not H2!
                val newTask = TaskEntity(
                    description = "Integration Test Task",
                    status = TaskStatus.NEW
                )

                val savedTask = taskRepository.save(newTask)

                savedTask.id shouldNotBe null
                savedTask.description shouldBe "Integration Test Task"
                savedTask.status shouldBe TaskStatus.NEW

                println("✅ Task saved to PostgreSQL container! ID: ${savedTask.id}")
            }

            test("Should handle all CRUD operations on PostgreSQL") {
                // CREATE
                val task = taskRepository.save(
                    TaskEntity(description = "PostgreSQL Test", status = TaskStatus.NEW)
                )
                task.id shouldNotBe null

                // READ
                val found = taskRepository.findById(task.id!!)
                found.isPresent shouldBe true
                found.get().description shouldBe "PostgreSQL Test"

                // UPDATE
                val updated = found.get()
                updated.description = "Updated in PostgreSQL"
                updated.status = TaskStatus.COMPLETED
                taskRepository.save(updated)

                val reloaded = taskRepository.findById(task.id!!)
                reloaded.get().description shouldBe "Updated in PostgreSQL"
                reloaded.get().status shouldBe TaskStatus.COMPLETED

                // DELETE
                taskRepository.deleteById(task.id!!)
                val deleted = taskRepository.findById(task.id!!)
                deleted.isPresent shouldBe false

                println("✅ Full CRUD operations work on PostgreSQL!")
            }

            test("Should demonstrate PostgreSQL-specific behavior") {
                // For students: This is where you'd test PostgreSQL-specific features
                // Examples:
                // - JSONB columns
                // - Full-text search
                // - PostgreSQL arrays
                // - Specific index types
                // - Database-specific SQL functions

                val task = taskRepository.save(
                    TaskEntity(
                        description = "Testing PostgreSQL enum storage",
                        status = TaskStatus.IN_PROGRESS
                    )
                )

                val retrieved = taskRepository.findById(task.id!!).get()

                // In PostgreSQL, enums are stored as strings (VARCHAR)
                // We can verify this works correctly
                retrieved.status shouldBe TaskStatus.IN_PROGRESS

                println("✅ PostgreSQL enum handling verified!")
            }

            test("Should demonstrate container isolation between tests") {
                // Each test method gets a fresh database state
                // Testcontainers can be configured to:
                // 1. Reuse container across tests (faster)
                // 2. Fresh container per test (more isolated)
                // 3. Fresh container per class (balance)

                val count = taskRepository.count()
                println("✅ Tasks in database: $count")
                println("✅ Container is running at: ${postgresContainer.jdbcUrl}")
            }
        }

        context("Benefits of Testcontainers") {

            test("Verifies code works with production database") {
                // For students: If this test passes, you have high confidence
                // that your code will work with PostgreSQL in production!

                val task = taskRepository.save(
                    TaskEntity(description = "Production-like test", status = TaskStatus.NEW)
                )

                task shouldNotBe null
                println("✅ This test ran against real PostgreSQL, just like production!")
            }

            test("Catches database-specific issues early") {
                // For students: Some SQL works in H2 but not PostgreSQL (or vice versa)
                // Examples:
                // - Different date/time handling
                // - Different string functions (CONCAT vs ||)
                // - Different transaction isolation levels
                // - Different constraint handling

                // This simple test would catch if our schema doesn't work on PostgreSQL
                val task = TaskEntity(description = "A".repeat(1000), status = TaskStatus.NEW)
                val saved = taskRepository.save(task)

                saved.description.length shouldBe 1000
                println("✅ VARCHAR(1000) works correctly in PostgreSQL!")
            }
        }

        afterSpec {
            // Testcontainers automatically stops and removes the container
            println("🛑 PostgreSQL container will be stopped automatically")
            println("   Container logs can help debug test failures!")
        }
    }
}
