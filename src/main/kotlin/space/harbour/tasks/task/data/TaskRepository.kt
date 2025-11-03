package space.harbour.tasks.task.data

import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * JPA-based repository for Task data access.
 *
 * This repository uses Spring Data JPA, which provides:
 * - Automatic CRUD operations (no code needed!)
 * - Query methods from method names (findByStatus, findByDescription, etc.)
 * - Automatic dirty checking (changes tracked automatically)
 * - Lazy loading support
 * - Transaction management
 *
 * **Comparison with TaskJdbcRepository:**
 * This project shows TWO data access approaches:
 * - TaskRepository (JPA) - Used by default, minimal code, automatic SQL generation
 * - TaskJdbcRepository (JDBC) - Alternative implementation, manual SQL, full control
 *
 * For students: Compare this file with TaskJdbcRepository.kt to understand trade-offs.
 * Both accomplish the same goal (data access) but with different approaches.
 */
@Repository
@Primary  // This is the default repository (JPA)
interface TaskRepository : JpaRepository<TaskEntity, Long> {
    // Inherits all CRUD operations from JpaRepository
    // Spring Data JPA automatically implements this interface at runtime
    // No code needed - just method naming conventions!
}
