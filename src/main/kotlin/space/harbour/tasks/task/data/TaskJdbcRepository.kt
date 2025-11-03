package space.harbour.tasks.task.data

import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository
import space.harbour.tasks.task.domain.TaskStatus
import java.sql.ResultSet
import java.util.*

/**
 * JDBC-based repository implementation for Task data access.
 *
 * This class demonstrates manual database operations using Spring's JdbcTemplate.
 * Compare this with TaskRepository (JPA) to understand the trade-offs:
 *
 * **JDBC Advantages:**
 * - Full control over SQL queries (optimization, complex joins)
 * - No "magic" - you see exactly what SQL runs
 * - Better performance for complex queries
 * - Works with any SQL database
 *
 * **JDBC Disadvantages:**
 * - More boilerplate code (manual mapping, SQL writing)
 * - No automatic dirty checking or lazy loading
 * - Need to handle SQL syntax differences between databases
 * - More code to maintain
 *
 * **When to use JDBC over JPA:**
 * - Complex reporting queries with specific optimizations
 * - Batch operations with precise control
 * - Legacy databases with non-standard schemas
 * - Performance-critical operations
 *
 * **NamedParameterJdbcTemplate:**
 * Instead of "?" placeholders (like raw JDBC), we use named parameters (:id, :description)
 * This makes SQL more readable and less error-prone.
 *
 * For students: This is Spring's recommended way to use JDBC.
 * It's cleaner than raw JDBC (Connection, PreparedStatement) but gives you full SQL control.
 */
@Repository
// @Profile("jdbc")  // Uncomment to only activate this repository with 'jdbc' profile
class TaskJdbcRepository(
    /**
     * Spring's JDBC template for executing SQL with named parameters.
     * Automatically handles connection management, exception translation, etc.
     */
    private val jdbcTemplate: NamedParameterJdbcTemplate
) : TaskDataRepository {

    /**
     * SQL queries as constants for clarity and reusability.
     * For students: It's good practice to keep SQL separate from logic.
     */
    companion object {
        private const val INSERT_SQL = """
            INSERT INTO tasks (description, status)
            VALUES (:description, :status)
        """

        private const val UPDATE_SQL = """
            UPDATE tasks
            SET description = :description, status = :status
            WHERE id = :id
        """

        private const val SELECT_BY_ID_SQL = """
            SELECT id, description, status
            FROM tasks
            WHERE id = :id
        """

        private const val SELECT_ALL_SQL = """
            SELECT id, description, status
            FROM tasks
        """

        private const val DELETE_BY_ID_SQL = """
            DELETE FROM tasks
            WHERE id = :id
        """

        private const val EXISTS_BY_ID_SQL = """
            SELECT COUNT(*)
            FROM tasks
            WHERE id = :id
        """
    }

    /**
     * RowMapper converts SQL ResultSet to TaskEntity.
     *
     * For students: This is how JDBC bridges the gap between database rows and objects.
     * Each column from the result set is manually mapped to an object field.
     */
    private val rowMapper = RowMapper { rs: ResultSet, _: Int ->
        TaskEntity(
            id = rs.getLong("id"),
            description = rs.getString("description"),
            status = TaskStatus.valueOf(rs.getString("status"))
        )
    }

    /**
     * Save a task (insert if new, update if exists).
     *
     * Unlike JPA which automatically detects if entity is new,
     * we must explicitly check using the ID.
     */
    override fun save(entity: TaskEntity): TaskEntity {
        return if (entity.id == null) {
            insert(entity)
        } else {
            update(entity)
            entity
        }
    }

    /**
     * Insert a new task and return it with generated ID.
     *
     * For students: Notice how we manually:
     * 1. Create parameter map with named parameters
     * 2. Use KeyHolder to retrieve auto-generated ID
     * 3. Return new entity with the generated ID
     */
    private fun insert(entity: TaskEntity): TaskEntity {
        val params = MapSqlParameterSource()
            .addValue("description", entity.description)
            .addValue("status", entity.status.name)

        // KeyHolder captures the auto-generated ID from the database
        val keyHolder = GeneratedKeyHolder()

        jdbcTemplate.update(INSERT_SQL, params, keyHolder)

        // Get the generated ID and create new entity with it
        val generatedId = keyHolder.key?.toLong()
            ?: throw IllegalStateException("Failed to retrieve generated ID")

        return TaskEntity(
            id = generatedId,
            description = entity.description,
            status = entity.status
        )
    }

    /**
     * Update an existing task.
     *
     * For students: Compare this with JPA - there's no automatic dirty checking.
     * We must explicitly call update() with all fields.
     */
    private fun update(entity: TaskEntity) {
        val params = MapSqlParameterSource()
            .addValue("id", entity.id)
            .addValue("description", entity.description)
            .addValue("status", entity.status.name)

        jdbcTemplate.update(UPDATE_SQL, params)
    }

    /**
     * Find a task by ID.
     *
     * For students: Notice we use queryForObject which throws exception if not found.
     * We catch it and return Optional.empty() to match the interface contract.
     */
    override fun findById(id: Long): Optional<TaskEntity> {
        val params = MapSqlParameterSource("id", id)

        return try {
            val entity = jdbcTemplate.queryForObject(SELECT_BY_ID_SQL, params, rowMapper)
            Optional.ofNullable(entity)
        } catch (e: Exception) {
            // No result found
            Optional.empty()
        }
    }

    /**
     * Find all tasks.
     *
     * For students: query() returns a List<TaskEntity> using our rowMapper.
     * The rowMapper is called for each row in the result set.
     */
    override fun findAll(): List<TaskEntity> {
        return jdbcTemplate.query(SELECT_ALL_SQL, rowMapper)
    }

    /**
     * Delete a task by ID.
     *
     * For students: Simple UPDATE/DELETE operations are straightforward with JDBC.
     * No need to load the entity first (unlike JPA's entityManager.remove(entity)).
     */
    override fun deleteById(id: Long) {
        val params = MapSqlParameterSource("id", id)
        jdbcTemplate.update(DELETE_BY_ID_SQL, params)
    }

    /**
     * Check if a task exists by ID.
     *
     * For students: We use COUNT(*) which is more efficient than loading the full entity.
     * JPA's existsById does something similar under the hood.
     */
    override fun existsById(id: Long): Boolean {
        val params = MapSqlParameterSource("id", id)
        val count = jdbcTemplate.queryForObject(EXISTS_BY_ID_SQL, params, Int::class.java)
        return count != null && count > 0
    }
}
