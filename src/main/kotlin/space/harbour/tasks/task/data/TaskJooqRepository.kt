package space.harbour.tasks.task.data

import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import space.harbour.tasks.generated.jooq.tables.Tasks.TASKS
import space.harbour.tasks.task.domain.TaskStatus
import java.util.*

/**
 * jOOQ Repository Implementation - Type-Safe SQL with Code Generation
 *
 * **What is jOOQ?**
 * jOOQ (Java Object Oriented Querying) is a library that generates Java code from your database schema
 * and provides a type-safe DSL (Domain Specific Language) for building SQL queries.
 *
 * **How is jOOQ Different from JPA and JDBC?**
 *
 * 1. **JPA (TaskRepository)**: Object-Relational Mapping
 *    - You work with objects, JPA generates SQL
 *    - Magic: Automatic SQL generation
 *    - Downside: Less control, can generate inefficient queries
 *
 * 2. **JDBC (TaskJdbcRepository)**: Raw SQL with Spring's JdbcTemplate
 *    - You write SQL as strings: "SELECT * FROM tasks WHERE id = :id"
 *    - Full control over SQL
 *    - Downside: No compile-time checking, typos found at runtime
 *
 * 3. **jOOQ (This class)**: Type-Safe SQL DSL
 *    - You write SQL using generated code: dsl.selectFrom(TASKS).where(TASKS.ID.eq(id))
 *    - Full control over SQL + compile-time type checking
 *    - If you rename a column in database, code won't compile until you update it!
 *
 * **jOOQ Advantages:**
 * ✅ Type-safe: Compiler catches typos in table/column names
 * ✅ Refactoring-friendly: Rename column → code breaks → fix everywhere
 * ✅ Full SQL power: Can write complex joins, subqueries, window functions
 * ✅ Database-agnostic: Can generate SQL for PostgreSQL, MySQL, Oracle, etc.
 * ✅ Less boilerplate than raw JDBC: No ResultSet mapping, parameter binding
 *
 * **jOOQ Disadvantages:**
 * ❌ Requires code generation step (must run ./gradlew generateJooq)
 * ❌ More verbose than JPA for simple CRUD
 * ❌ Learning curve: Need to understand both SQL and jOOQ DSL
 * ❌ Generated code adds to project size
 *
 * **When to Use jOOQ:**
 * - Complex queries with joins, subqueries, aggregations
 * - Performance-critical code where you need control over SQL
 * - Projects where you want type-safety but don't want ORM magic
 * - When your team is strong in SQL and wants to use it directly
 *
 * **DSLContext:**
 * The main entry point for jOOQ queries. It's like JdbcTemplate but type-safe.
 * Injected by Spring Boot automatically when you add spring-boot-starter-jooq.
 *
 * **Generated Code:**
 * - TASKS: Table reference (in space.harbour.tasks.generated.jooq.tables.Tasks)
 * - TASKS.ID, TASKS.DESCRIPTION, TASKS.STATUS: Column references (type-safe!)
 * - TasksRecord: Represents a database row (like ResultSet but type-safe)
 * - Tasks POJO: Simple data class (in tables.pojos package)
 *
 * For students: Compare this implementation with TaskRepository (JPA) and TaskJdbcRepository (JDBC)!
 */
@Repository
class TaskJooqRepository(
    /**
     * DSLContext is jOOQ's main API for building and executing queries.
     * Think of it as a type-safe query builder.
     *
     * Injected by Spring Boot when you have spring-boot-starter-jooq dependency.
     */
    private val dsl: DSLContext
) : TaskDataRepository {

    /**
     * Save a task (insert if new, update if exists).
     *
     * jOOQ Query Pattern:
     * 1. Check if task has ID (new vs existing)
     * 2. Use insertInto() for new tasks
     * 3. Use update() for existing tasks
     *
     * Compare with JDBC:
     * - JDBC: SQL strings, manual parameter binding
     * - jOOQ: Type-safe DSL, automatic parameter binding
     *
     * Example generated SQL (insert):
     *   INSERT INTO tasks (description, status) VALUES (?, ?)
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
     * Insert a new task.
     *
     * jOOQ INSERT Query:
     * dsl.insertInto(TASKS)                          // INSERT INTO tasks
     *    .set(TASKS.DESCRIPTION, entity.description) // SET description = ?
     *    .set(TASKS.STATUS, entity.status.name)      // SET status = ?
     *    .returning(TASKS.ID)                        // RETURNING id
     *    .fetchOne()                                 // Execute and fetch result
     *
     * Type Safety:
     * - TASKS.DESCRIPTION expects String (compile error if you pass Int!)
     * - TASKS.STATUS expects String (we convert enum to String)
     * - If column doesn't exist, code won't compile
     *
     * Compare with JDBC:
     * - JDBC: "INSERT INTO tasks (description, status) VALUES (:description, :status)"
     * - jOOQ: Type-safe method calls, no string typos possible!
     */
    private fun insert(entity: TaskEntity): TaskEntity {
        // Execute insert and get generated ID
        // returning(TASKS.ID) is like JDBC's GeneratedKeyHolder
        val generatedId = dsl.insertInto(TASKS)
            .set(TASKS.DESCRIPTION, entity.description)
            .set(TASKS.STATUS, entity.status.name)  // Convert enum to String
            .returning(TASKS.ID)  // Return the auto-generated ID
            .fetchOne()  // Execute and fetch the result
            ?.getValue(TASKS.ID)  // Extract the ID value
            ?: throw IllegalStateException("Failed to retrieve generated ID")

        // Return new entity with generated ID
        return TaskEntity(
            id = generatedId,
            description = entity.description,
            status = entity.status
        )
    }

    /**
     * Update an existing task.
     *
     * jOOQ UPDATE Query:
     * dsl.update(TASKS)                              // UPDATE tasks
     *    .set(TASKS.DESCRIPTION, entity.description) // SET description = ?
     *    .set(TASKS.STATUS, entity.status.name)      // SET status = ?
     *    .where(TASKS.ID.eq(entity.id))              // WHERE id = ?
     *    .execute()                                  // Execute
     *
     * Type-Safe WHERE Clause:
     * - TASKS.ID.eq(value) → id = ?
     * - TASKS.ID.ne(value) → id != ?
     * - TASKS.ID.gt(value) → id > ?
     * - TASKS.DESCRIPTION.like("%value%") → description LIKE ?
     *
     * If you try TASKS.ID.eq("string"), compiler error! ID is Long, not String.
     */
    private fun update(entity: TaskEntity) {
        dsl.update(TASKS)
            .set(TASKS.DESCRIPTION, entity.description)
            .set(TASKS.STATUS, entity.status.name)
            .where(TASKS.ID.eq(entity.id))  // Type-safe: ID must be Long
            .execute()
    }

    /**
     * Find task by ID.
     *
     * jOOQ SELECT Query:
     * dsl.selectFrom(TASKS)        // SELECT * FROM tasks
     *    .where(TASKS.ID.eq(id))   // WHERE id = ?
     *    .fetchOne()               // Execute and fetch single result
     *
     * fetchOne() vs fetch():
     * - fetchOne(): Returns single record or null (like SQL LIMIT 1)
     * - fetch(): Returns list of records
     * - fetchSingle(): Returns single record or throws exception if 0 or 2+ records
     *
     * Type Safety:
     * - TASKS.ID.eq(id) requires Long, compile error if you pass String
     * - fetchOne() returns TasksRecord? (nullable)
     */
    override fun findById(id: Long): Optional<TaskEntity> {
        // Execute SELECT query and convert result to TaskEntity
        val record = dsl.selectFrom(TASKS)
            .where(TASKS.ID.eq(id))  // Type-safe WHERE clause
            .fetchOne()  // Returns TasksRecord? (nullable)

        // Convert jOOQ Record to our TaskEntity
        // record?.let { } is Kotlin's safe call - only executes if record is not null
        return Optional.ofNullable(record?.let { toEntity(it) })
    }

    /**
     * Find all tasks.
     *
     * jOOQ SELECT Query:
     * dsl.selectFrom(TASKS).fetch()  // SELECT * FROM tasks
     *
     * fetch() returns Result<TasksRecord> which is like List<TasksRecord>
     * but with extra jOOQ-specific methods.
     *
     * map { toEntity(it) } converts each TasksRecord to TaskEntity.
     *
     * For students: Try adding WHERE, ORDER BY, LIMIT:
     * dsl.selectFrom(TASKS)
     *    .where(TASKS.STATUS.eq("NEW"))
     *    .orderBy(TASKS.ID.desc())
     *    .limit(10)
     *    .fetch()
     */
    override fun findAll(): List<TaskEntity> {
        return dsl.selectFrom(TASKS)
            .fetch()  // Returns Result<TasksRecord>
            .map { toEntity(it) }  // Convert each record to TaskEntity
    }

    /**
     * Check if task exists.
     *
     * jOOQ fetchCount():
     * dsl.selectCount()                 // SELECT COUNT(*)
     *    .from(TASKS)                   // FROM tasks
     *    .where(TASKS.ID.eq(id))        // WHERE id = ?
     *    .fetchOne(0, Int::class.java)  // Execute and get count as Int
     *
     * fetchOne(0, Int::class.java):
     * - 0: Get first column (COUNT(*))
     * - Int::class.java: Cast to Int
     * - Returns Int? (nullable)
     *
     * Alternative approach:
     * dsl.fetchExists(dsl.selectFrom(TASKS).where(TASKS.ID.eq(id)))
     */
    override fun existsById(id: Long): Boolean {
        val count = dsl.selectCount()
            .from(TASKS)
            .where(TASKS.ID.eq(id))
            .fetchOne(0, Int::class.java) ?: 0  // Get count or 0 if null

        return count > 0
    }

    /**
     * Delete task by ID.
     *
     * jOOQ DELETE Query:
     * dsl.deleteFrom(TASKS)        // DELETE FROM tasks
     *    .where(TASKS.ID.eq(id))   // WHERE id = ?
     *    .execute()                // Execute
     *
     * execute() returns number of affected rows (Int).
     *
     * Type Safety:
     * - Can't accidentally delete all rows (need .where() or explicit .execute())
     * - Compiler ensures correct types in WHERE clause
     */
    override fun deleteById(id: Long) {
        dsl.deleteFrom(TASKS)
            .where(TASKS.ID.eq(id))
            .execute()
    }

    /**
     * Convert jOOQ Record to our domain TaskEntity.
     *
     * TasksRecord is generated by jOOQ and has type-safe getters:
     * - record.id → returns Long? (typed!)
     * - record.description → returns String? (typed!)
     * - record.status → returns String? (typed!)
     *
     * Compare with JDBC ResultSet:
     * - JDBC: rs.getLong("id") → runtime error if column doesn't exist
     * - jOOQ: record.id → compile error if column doesn't exist
     *
     * We convert status String to TaskStatus enum using valueOf().
     * If database has invalid enum value, this will throw IllegalArgumentException.
     */
    private fun toEntity(record: org.jooq.Record): TaskEntity {
        return TaskEntity(
            id = record.getValue(TASKS.ID),  // Type-safe getter
            description = record.getValue(TASKS.DESCRIPTION)!!,  // !! asserts non-null
            status = TaskStatus.valueOf(record.getValue(TASKS.STATUS)!!)  // Convert String to enum
        )
    }
}
