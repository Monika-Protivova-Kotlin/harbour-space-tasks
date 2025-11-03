package space.harbour.tasks.task.data

import java.util.*

/**
 * Common repository interface for Task data access.
 *
 * This interface defines the contract that both JPA and JDBC implementations must follow.
 * By programming to an interface rather than a concrete implementation, we achieve:
 *
 * 1. **Abstraction** - Service layer doesn't care about HOW data is accessed
 * 2. **Flexibility** - Can switch between JPA and JDBC without changing service code
 * 3. **Testability** - Easy to mock this interface in tests
 * 4. **Dependency Inversion Principle** - High-level modules depend on abstractions
 *
 * For students: This demonstrates the "Dependency Inversion Principle" from SOLID.
 * The Service depends on this interface (abstraction), not on JPA or JDBC (details).
 *
 * Why not just use JpaRepository?
 * - JpaRepository is specific to JPA (has methods like flush(), getOne(), etc.)
 * - We want a clean interface that works for ANY data access technology
 * - This keeps our domain layer independent of persistence technology
 */
interface TaskDataRepository {

    /**
     * Save a task entity (insert if new, update if exists).
     *
     * @param entity The task entity to save
     * @return The saved entity (with generated ID if it was new)
     */
    fun save(entity: TaskEntity): TaskEntity

    /**
     * Find a task by its ID.
     *
     * @param id The task ID
     * @return Optional containing the task if found, empty otherwise
     */
    fun findById(id: Long): Optional<TaskEntity>

    /**
     * Find all tasks.
     *
     * @return List of all tasks in the database
     */
    fun findAll(): List<TaskEntity>

    /**
     * Delete a task by its ID.
     *
     * @param id The task ID to delete
     */
    fun deleteById(id: Long)

    /**
     * Check if a task with given ID exists.
     *
     * @param id The task ID to check
     * @return true if task exists, false otherwise
     */
    fun existsById(id: Long): Boolean
}
