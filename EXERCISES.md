# Student Exercises - Hands-On Labs

**These are OPTIONAL hands-on exercises to deepen your understanding of the Spring Boot Task Management application.**

## 🎯 Learning Approach

1. **Read the existing code** - Understand how current features work
2. **Follow the patterns** - Implement new features using the same style
3. **Write tests first** - Test-driven development (TDD)
4. **Run tests often** - Make sure you didn't break anything!

---

## Lab 1: Add Task Priority Field ⭐ (Beginner)

**Goal**: Learn how to modify domain models and propagate changes through all layers.

### Requirements
Add a `priority` field to tasks with values: LOW, MEDIUM, HIGH

### Steps

1. **Create Priority Enum**
   - File: `src/main/kotlin/space/harbour/tasks/task/domain/TaskPriority.kt`
   - Values: `LOW`, `MEDIUM`, `HIGH`

2. **Update Domain Model**
   - Add `priority: TaskPriority` to `Task.kt`
   - Default value: `TaskPriority.MEDIUM`

3. **Update Persistence**
   - Add `priority` field to `TaskEntity.kt`
   - Use `@Enumerated(EnumType.STRING)`

4. **Update DTOs**
   - Add `priority` to `TaskResponse.kt`
   - Add `priority` to `NewTaskRequest.kt` (optional with default)

5. **Update Mappers**
   - Modify `TaskMapper.kt` to include priority in all conversions

6. **Update Service**
   - Modify `TaskService.addTask()` to handle priority parameter
   - Make it optional with default `TaskPriority.MEDIUM`

7. **Write Tests**
   - Update `TaskServiceTest` - test creating tasks with different priorities
   - Update `TaskControllerTest` - test API with priority field
   - Update `TaskRepositoryTest` - test saving/loading priority

8. **Test It**
   ```bash
   ./gradlew test
   curl -X POST http://localhost:8081/api/tasks \
     -u admin:password123 \
     -H "Content-Type: application/json" \
     -d '{"description": "High priority task", "priority": "HIGH"}'
   ```

### Learning Points
- How changes propagate through layers (domain → entity → DTO → controller)
- Why we separate these concerns
- How JPA enum mapping works
- Importance of updating tests

---

## Lab 2: Implement Pagination 📄 (Intermediate)

**Goal**: Learn Spring Data pagination for large datasets.

### Requirements
Add pagination support to `GET /api/tasks` endpoint.

### Steps

1. **Update Controller**
   ```kotlin
   @GetMapping
   fun getTasks(
       @RequestParam(defaultValue = "0") page: Int,
       @RequestParam(defaultValue = "10") size: Int,
       @RequestParam(defaultValue = "id") sortBy: String
   ): Page<TaskResponse> {
       // Implementation here
   }
   ```

2. **Update Service**
   ```kotlin
   fun getTasks(page: Int, size: Int, sortBy: String): Page<Task> {
       val pageable = PageRequest.of(page, size, Sort.by(sortBy))
       return taskRepository.findAll(pageable).map { it.toDomain() }
   }
   ```

3. **Test Pagination**
   - Create 25 test tasks
   - Request page 0, size 10 → should return 10 tasks
   - Request page 2, size 10 → should return 5 tasks
   - Test sorting by different fields

4. **Test It**
   ```bash
   curl "http://localhost:8081/api/tasks?page=0&size=5&sortBy=description" \
     -u admin:password123
   ```

### Learning Points
- Spring Data pagination (`Page`, `Pageable`)
- Query parameters in controllers
- Sorting with Spring Data
- How to test paginated results

---

## Lab 3: Add Search/Filtering 🔍 (Intermediate)

**Goal**: Learn query methods and specifications.

### Requirements
Add ability to filter tasks by status and search in descriptions.

### Steps

1. **Add Query Method to Repository**
   ```kotlin
   interface TaskRepository : JpaRepository<TaskEntity, Long> {
       fun findByStatus(status: TaskStatus): List<TaskEntity>

       fun findByDescriptionContainingIgnoreCase(query: String): List<TaskEntity>

       fun findByStatusAndDescriptionContainingIgnoreCase(
           status: TaskStatus,
           query: String
       ): List<TaskEntity>
   }
   ```

2. **Add Service Method**
   ```kotlin
   fun searchTasks(status: TaskStatus?, query: String?): List<Task> {
       return when {
           status != null && query != null ->
               taskRepository.findByStatusAndDescriptionContainingIgnoreCase(status, query)
           status != null ->
               taskRepository.findByStatus(status)
           query != null ->
               taskRepository.findByDescriptionContainingIgnoreCase(query)
           else ->
               taskRepository.findAll()
       }.map { it.toDomain() }
   }
   ```

3. **Add Controller Endpoint**
   ```kotlin
   @GetMapping("/search")
   fun searchTasks(
       @RequestParam(required = false) status: TaskStatus?,
       @RequestParam(required = false) query: String?
   ): List<TaskResponse> {
       return taskService.searchTasks(status, query).map { it.toResponse() }
   }
   ```

4. **Write Tests**
   - Test search by status only
   - Test search by query only
   - Test search by both status and query
   - Test with no filters (returns all)

5. **Test It**
   ```bash
   # Search by status
   curl "http://localhost:8081/api/tasks/search?status=IN_PROGRESS" \
     -u admin:password123

   # Search by description
   curl "http://localhost:8081/api/tasks/search?query=spring" \
     -u admin:password123

   # Combined search
   curl "http://localhost:8081/api/tasks/search?status=NEW&query=learn" \
     -u admin:password123
   ```

### Learning Points
- Spring Data JPA query methods (naming conventions)
- Optional request parameters
- Method naming patterns (`findBy`, `ContainingIgnoreCase`)
- Testing search functionality

---

## Lab 4: Add Audit Fields (CreatedAt/UpdatedAt) ⏰ (Advanced)

**Goal**: Learn JPA Auditing for automatic timestamp tracking.

### Requirements
Track when tasks are created and last modified.

### Steps

1. **Enable JPA Auditing**
   ```kotlin
   @Configuration
   @EnableJpaAuditing
   class JpaConfig
   ```

2. **Update TaskEntity**
   ```kotlin
   @Entity
   @Table(name = "tasks")
   @EntityListeners(AuditingEntityListener::class)
   class TaskEntity(
       // existing fields...

       @CreatedDate
       @Column(nullable = false, updatable = false)
       var createdAt: Instant? = null,

       @LastModifiedDate
       @Column(nullable = false)
       var updatedAt: Instant? = null
   )
   ```

3. **Update DTOs**
   - Add `createdAt` and `updatedAt` to `TaskResponse`
   - Do NOT add to `NewTaskRequest` (these are auto-generated)

4. **Update Mappers**
   - Include timestamps in `toDomain()` and `toResponse()`

5. **Write Tests**
   - Verify `createdAt` is set when task is created
   - Verify `updatedAt` changes when task is modified
   - Verify `createdAt` doesn't change on update

6. **Test It**
   ```bash
   # Create task
   curl -X POST http://localhost:8081/api/tasks \
     -u admin:password123 \
     -H "Content-Type: application/json" \
     -d '{"description": "Test timestamps"}'

   # Note the timestamps in response

   # Update task after 5 seconds
   curl -X PUT http://localhost:8081/api/tasks/1 \
     -u admin:password123 \
     -H "Content-Type: application/json" \
     -d '{"id": 1, "description": "Updated", "status": "COMPLETED"}'

   # createdAt same, updatedAt different!
   ```

### Learning Points
- JPA Auditing (`@CreatedDate`, `@LastModifiedDate`)
- Why timestamps are important
- Fields that shouldn't be in request DTOs
- Testing time-based functionality

---

## Lab 5: Add Custom Validation 🛡️ (Advanced)

**Goal**: Learn Bean Validation (JSR-380) for declarative validation.

### Requirements
Add validation annotations to enforce business rules.

### Steps

1. **Add Dependency** (if not present)
   ```kotlin
   implementation("org.springframework.boot:spring-boot-starter-validation")
   ```

2. **Add Validation to NewTaskRequest**
   ```kotlin
   data class NewTaskRequest(
       @field:NotBlank(message = "Description is required")
       @field:Size(min = 3, max = 1000, message = "Description must be 3-1000 characters")
       val description: String,

       @field:Pattern(
           regexp = "^[A-Z_]+$",
           message = "Status must be uppercase (NEW, IN_PROGRESS, etc.)"
       )
       val status: String? = null
   )
   ```

3. **Enable Validation in Controller**
   ```kotlin
   @PostMapping
   @ResponseStatus(HttpStatus.CREATED)
   fun createTask(@Valid @RequestBody request: NewTaskRequest): TaskResponse {
       // Spring automatically validates before method is called
   }
   ```

4. **Add Validation Exception Handler**
   ```kotlin
   @ExceptionHandler(MethodArgumentNotValidException::class)
   fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
       val errors = ex.bindingResult.fieldErrors
           .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
       return ResponseEntity
           .badRequest()
           .body(ErrorResponse(400, errors))
   }
   ```

5. **Write Tests**
   - Test description too short (< 3 characters)
   - Test description too long (> 1000 characters)
   - Test blank description
   - Test invalid status format

6. **Test It**
   ```bash
   # Should fail - description too short
   curl -X POST http://localhost:8081/api/tasks \
     -u admin:password123 \
     -H "Content-Type: application/json" \
     -d '{"description": "Hi"}'
   ```

### Learning Points
- Bean Validation annotations
- `@Valid` in controllers
- Custom validation messages
- Handling validation exceptions

---

## Lab 6: Migrate to PostgreSQL 🐘 (Advanced)

**Goal**: Learn to switch databases and use Docker for local development.

### Requirements
Replace H2 with PostgreSQL using Docker.

### Steps

1. **Create docker-compose.yml**
   ```yaml
   version: '3.8'
   services:
     postgres:
       image: postgres:16
       environment:
         POSTGRES_DB: tasks_db
         POSTGRES_USER: tasks_user
         POSTGRES_PASSWORD: tasks_pass
       ports:
         - "5432:5432"
       volumes:
         - postgres_data:/var/lib/postgresql/data

   volumes:
     postgres_data:
   ```

2. **Update Dependencies**
   ```kotlin
   // Remove or comment out
   // runtimeOnly("com.h2database:h2")

   // Add
   runtimeOnly("org.postgresql:postgresql")
   ```

3. **Update application.yml**
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/tasks_db
       username: tasks_user
       password: tasks_pass
       driver-class-name: org.postgresql.Driver
     jpa:
       hibernate:
         ddl-auto: update
       properties:
         hibernate:
           dialect: org.hibernate.dialect.PostgreSQLDialect
   ```

4. **Start Database**
   ```bash
   docker-compose up -d
   ```

5. **Run Application**
   ```bash
   ./gradlew bootRun
   ```

6. **Verify**
   - Application should start without errors
   - Create/read/update/delete tasks
   - Data persists across application restarts!

### Learning Points
- Docker for local development
- Database configuration in Spring Boot
- Difference between H2 and PostgreSQL
- Connection pooling
- Production-ready database setup

---

## Bonus Challenges 🚀

### Challenge 1: Add Task Assignment
- Add `assignedTo: String?` field
- Create endpoint to assign tasks to users
- Filter tasks by assignee

### Challenge 2: Add Due Dates
- Add `dueDate: LocalDate?` field
- Add endpoint to get overdue tasks
- Implement sorting by due date

### Challenge 3: Add Task Comments
- Create `Comment` entity with OneToMany relationship
- Add endpoints to add/view comments on tasks
- Learn JPA relationships

### Challenge 4: Add Swagger/OpenAPI Documentation
- Add `springdoc-openapi` dependency
- Access auto-generated docs at `/swagger-ui.html`
- Add `@Operation` and `@ApiResponse` annotations

### Challenge 5: Add Caching
- Add `spring-boot-starter-cache` dependency
- Cache `getTasks()` result
- Learn cache eviction on updates

---

## 📚 Tips for Success

### Before Starting Each Lab
1. Read the existing code related to the feature
2. Understand the pattern being used
3. Plan your changes on paper first

### While Implementing
1. Make small changes
2. Run tests after each change
3. Commit working code often

### When Stuck
1. Read the error message carefully
2. Check the existing code for similar examples
3. Use `./gradlew test` to see what broke
4. Ask yourself: "Which layer does this belong in?"

### After Completing
1. Run all tests: `./gradlew test`
2. Test manually with curl/Postman
3. Review your code - could it be cleaner?
4. Compare with the original code style

---

## 🎯 Learning Goals Checklist

After completing these labs, you should be able to:

- ✅ Add new fields to domain models and propagate through all layers
- ✅ Implement pagination and sorting
- ✅ Create search/filter functionality
- ✅ Use JPA Auditing for automatic fields
- ✅ Apply validation annotations
- ✅ Switch databases
- ✅ Write comprehensive tests for new features
- ✅ Follow Spring Boot best practices
- ✅ Understand layered architecture deeply

---

**Good luck! Remember: The best way to learn is by doing. Don't just read - code!** 🚀
