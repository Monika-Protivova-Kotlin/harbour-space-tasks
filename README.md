# Spring Boot Task Management API - Learning Project

A comprehensive Spring Boot RESTful API demonstrating professional backend development practices with Kotlin.

## 🎯 Learning Objectives

After studying this project, students will understand:

- **Layered Architecture** - Proper separation of concerns (Controller → Service → Repository)
- **Domain-Driven Design** - Separating domain models from persistence and DTOs
- **RESTful API Design** - HTTP methods, status codes, resource naming
- **Spring Boot Fundamentals** - Dependency injection, annotations, configuration
- **Spring Security** - Authentication, authorization, password encryption
- **Spring Data JPA** - Database operations, entity management
- **Testing** - Unit tests, integration tests, slice tests (65 tests!)
- **Kotlin for Backend** - Data classes, extension functions, null safety

## 🏗️ Project Structure

```
src/main/kotlin/space/harbour/tasks/
├── HarbourSpaceTasksApplication.kt    # Main application entry point
├── config/
│   └── SecurityConfig.kt              # Security configuration
├── exception/                         # Global exception handling (application-wide)
│   ├── GlobalExceptionHandler.kt      # Centralized error handling for all domains
│   └── ErrorResponse.kt               # Standard error response DTO
└── task/                              # Task domain (can add user/, order/, etc. later)
    ├── controller/
    │   ├── TaskController.kt          # REST endpoints (HTTP layer)
    │   └── dto/
    │       ├── NewTaskRequest.kt      # Input DTO
    │       └── TaskResponse.kt        # Output DTO
    ├── service/
    │   ├── TaskService.kt             # Business logic layer
    │   └── mapper/
    │       └── TaskMapper.kt          # DTO ↔ Domain ↔ Entity mappers
    ├── data/                          # Data access layer (entities + repositories)
    │   ├── TaskDataRepository.kt      # Common interface for both JPA and JDBC
    │   ├── TaskEntity.kt              # JPA entity (database representation)
    │   ├── TaskRepository.kt          # JPA repository implementation (@Primary)
    │   └── TaskJdbcRepository.kt      # JDBC repository implementation (alternative)
    ├── domain/
    │   ├── Task.kt                    # Domain model (business logic)
    │   └── TaskStatus.kt              # Enum for task states
    └── exception/                     # Task-specific exceptions
        ├── TaskNotFoundException.kt
        ├── InvalidTaskException.kt
        ├── TaskAlreadyExistsException.kt
        └── TaskOperationException.kt
```

## 📚 Architecture Explained

### Layered Architecture

```
┌─────────────────────────────────────────────┐
│          HTTP Request (JSON)                │
└──────────────────┬──────────────────────────┘
                   │
         ┌─────────▼──────────┐
         │   CONTROLLER       │  ← HTTP concerns (status codes, @RestController)
         │  TaskController    │    Thin layer, no business logic!
         └─────────┬──────────┘
                   │
         ┌─────────▼──────────┐
         │     SERVICE        │  ← Business logic (@Service, @Transactional)
         │   TaskService      │    Validation, rules, orchestration
         └─────────┬──────────┘
                   │
         ┌─────────▼──────────┐
         │    DATA LAYER      │  ← Database access (entities + repositories)
         │  TaskRepository    │    JpaRepository provides CRUD operations
         │  TaskEntity        │    @Entity maps to database table
         └─────────┬──────────┘
                   │
         ┌─────────▼──────────┐
         │     DATABASE       │  ← H2 in-memory database
         │     (tasks)        │
         └────────────────────┘
```

### Package Organization

**Why `data/` instead of separate `persistence/` and `repository/`?**
- `TaskEntity` and `TaskRepository` are tightly coupled (work together)
- Simpler for students to understand: "this is the data access layer"
- Reduces package complexity without losing clarity

**Why top-level `exception/` package?**
- `GlobalExceptionHandler` is application-wide (handles all domains)
- When you add `User` or `Order` domains, they share the same handler
- `ErrorResponse` is a common DTO used across all error responses
- Demonstrates scalability for multi-domain applications

**Domain-specific exceptions stay in `task/exception/`:**
- `TaskNotFoundException`, `InvalidTaskException`, etc. are task-specific
- Each domain can have its own exception types
- Global handler catches them all

### Domain-Driven Design

We separate three types of objects:

1. **Domain Models** (`Task.kt`) - Pure business logic, no annotations
2. **Data Entities** (`TaskEntity.kt`) - JPA entities with @Entity in `data/` package
3. **DTOs** (`TaskResponse`, `NewTaskRequest`) - Data transfer for HTTP

Why?
- Domain models stay clean and testable
- Database structure can change without affecting business logic
- API contracts (DTOs) can evolve independently

### Data Access Strategies: JPA vs JDBC

This project demonstrates **two approaches** to database access, allowing students to compare:

#### **JPA (Java Persistence API)** - `TaskRepository`
- **What**: Object-Relational Mapping (ORM) framework
- **Advantages**:
  - Minimal code (CRUD operations automatic)
  - Database-independent (works with PostgreSQL, MySQL, Oracle, etc.)
  - Automatic dirty checking and lazy loading
  - Query methods from method names (`findByStatus`, etc.)
- **Best for**: Standard CRUD operations, rapid development, database portability
- **Used by default** (marked with `@Primary`)

#### **JDBC (Java Database Connectivity)** - `TaskJdbcRepository`
- **What**: Direct SQL execution with Spring's `JdbcTemplate`
- **Advantages**:
  - Full control over SQL queries
  - Better performance for complex queries
  - No "magic" - explicit about what runs
  - Easier to optimize and debug
- **Best for**: Complex queries, batch operations, performance-critical code, legacy databases
- **Available as alternative** implementation

#### **Common Interface** - `TaskDataRepository`
Both implementations share a common interface, demonstrating:
- **Dependency Inversion Principle** - Service depends on abstraction, not implementation
- **Flexibility** - Switch implementations by changing `@Primary` annotation
- **Testability** - Easy to mock the interface

**For students**: You can switch between JPA and JDBC by:
1. Moving `@Primary` annotation from `TaskRepository` to `TaskJdbcRepository`
2. Using `@Qualifier("taskJdbcRepository")` when injecting
3. Using Spring profiles for different environments

### Mapper Pattern

Extension functions convert between layers:
- `TaskEntity.toDomain()` → `Task`
- `Task.toResponse()` → `TaskResponse`
- `TaskResponse.toDomain()` → `Task`

## 🚀 Getting Started

### Prerequisites
- JDK 23 or later
- Kotlin 2.2.21
- Gradle (included via wrapper)

### Running the Application

```bash
# Linux/Mac
./gradlew bootRun

# Windows
gradlew.bat bootRun
```

Application starts on: http://localhost:8081

### Default Credentials

```
Username: admin
Password: password123
```

⚠️ **For learning purposes only!** Never hardcode credentials in production.

## 📡 API Endpoints

### Authentication
All `/api/**` endpoints require HTTP Basic Auth.

Example using curl:
```bash
curl -u admin:password123 http://localhost:8081/api/tasks
```

### Available Endpoints

| Method | Endpoint | Description | Status Code |
|--------|----------|-------------|-------------|
| GET | `/api/tasks` | Get all tasks | 200 OK |
| GET | `/api/tasks/{id}` | Get task by ID | 200 OK, 404 Not Found |
| POST | `/api/tasks` | Create new task | 201 Created, 400 Bad Request |
| PUT | `/api/tasks/{id}` | Update task | 200 OK, 404 Not Found, 400 Bad Request |
| DELETE | `/api/tasks/{id}` | Delete task | 204 No Content, 404 Not Found |

### Example Requests

**Create a task:**
```bash
curl -X POST http://localhost:8081/api/tasks \
  -u admin:password123 \
  -H "Content-Type: application/json" \
  -d '{"description": "Learn Spring Boot"}'
```

**Get all tasks:**
```bash
curl http://localhost:8081/api/tasks \
  -u admin:password123
```

**Update a task:**
```bash
curl -X PUT http://localhost:8081/api/tasks/1 \
  -u admin:password123 \
  -H "Content-Type: application/json" \
  -d '{"id": 1, "description": "Learn Kotlin", "status": "IN_PROGRESS"}'
```

## 🗄️ Database

- **Type**: H2 in-memory database
- **Console**: http://localhost:8081/h2-console
- **JDBC URL**: `jdbc:h2:mem:testdb`
- **Username**: `sa`
- **Password**: (empty)

Note: Data is lost when application stops (in-memory).

## 🧪 Testing

This project includes **65 comprehensive tests** demonstrating different testing strategies:

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "TaskControllerTest"
```

### Testing Strategies

1. **Controller Tests** (`TaskControllerTest`)
   - Uses `@WebMvcTest` for testing just the web layer
   - MockMvc for HTTP request simulation
   - Tests authentication, CSRF protection, status codes

2. **Service Tests** (`TaskServiceTest`)
   - Unit tests with MockK for mocking
   - Tests business logic in isolation

3. **Repository Tests** (`TaskRepositoryTest`)
   - Uses `@DataJpaTest` for JPA slice testing
   - TestEntityManager for database verification
   - Tests with H2 in-memory database

4. **JDBC Repository Tests** (`TaskJdbcRepositoryTest`)
   - Uses `@JdbcTest` for JDBC slice testing
   - Tests manual SQL operations with JdbcTemplate
   - Compares JDBC approach with JPA

5. **Integration Tests with Testcontainers** (`TaskRepositoryIntegrationTest`)
   - Tests with **real PostgreSQL database** in Docker container
   - Demonstrates production-like testing environment
   - Requires Docker to be running
   - Slower but higher confidence than H2 tests

6. **Mapper Tests** (`TaskServiceMapperTest`)
   - Pure function testing
   - No mocking needed

7. **Security Tests** (`SecurityConfigTest`)
   - Integration tests for security configuration
   - Tests authentication and authorization

8. **Exception Handler Tests** (`GlobalExceptionHandlerTest`)
   - Unit tests for exception handling

### Test Framework

- **Kotest** - Modern Kotlin testing framework (FunSpec style)
- **MockK** - Kotlin-friendly mocking library
- **Spring Boot Test** - Integration testing support
- **Testcontainers** - Real Docker containers for integration tests (PostgreSQL)

### Testing Levels

This project demonstrates the **testing pyramid**:

1. **Unit Tests** (Fast, Many)
   - Service tests with mocked dependencies
   - Mapper tests (pure functions)
   - Run in milliseconds, no database needed

2. **Slice Tests** (Medium Speed)
   - `@WebMvcTest` for controllers only
   - `@DataJpaTest` for repositories with H2
   - `@JdbcTest` for JDBC repositories with H2
   - Faster than full integration, still realistic

3. **Integration Tests** (Slower, Fewer)
   - Testcontainers with real PostgreSQL
   - Full Spring context
   - Production-like environment
   - High confidence, but takes seconds to run

**For students**: Run unit tests frequently (fast feedback), integration tests before commits (confidence)

## 🔒 Security

- **Authentication**: HTTP Basic (username/password in headers)
- **Password Encryption**: BCrypt (industry standard)
- **CSRF Protection**: Enabled for API endpoints
- **Credentials**: Hardcoded for learning (⚠️ never do this in production!)

### What's Protected
- ✅ All `/api/**` endpoints require authentication
- ✅ Passwords are encrypted with BCrypt
- ✅ CSRF tokens required for POST/PUT/DELETE

### What's Public
- `/h2-console/**` - Database console (development only)
- `/actuator/health` - Health check endpoint

## 📝 Key Concepts Demonstrated

### 1. Dependency Injection
```kotlin
class TaskController(
    private val taskService: TaskService  // Injected by Spring
)
```

### 2. RESTful Design
- Resources as nouns (`/tasks`, not `/getTasks`)
- HTTP methods for actions (GET, POST, PUT, DELETE)
- Proper status codes (201 Created, 204 No Content)

### 3. Exception Handling
- Custom exceptions for domain errors
- `@RestControllerAdvice` for global handling
- Consistent error responses

### 4. Transactions
```kotlin
@Transactional  // Automatic commit/rollback
fun updateTask(id: Long, task: Task): Task
```

### 5. JPA Best Practices
- Regular class (not data class) for @Entity
- ID-based equals/hashCode
- EnumType.STRING for enums

## 🎓 Learning Tips

### Start Here
1. Run the application and try the API with curl/Postman
2. Look at the H2 console to see the database
3. Read TaskController.kt - see how HTTP requests are handled
4. Read TaskService.kt - see where business logic lives
5. Read the tests - see how each layer is tested

### Common Gotchas

| Issue | Explanation |
|-------|-------------|
| Why separate Task and TaskEntity? | Domain model vs database representation. Allows each to evolve independently. |
| Why validation in Service, not Controller? | Business rules should be enforced regardless of entry point (HTTP, scheduled job, etc.). |
| When to use @Transactional? | For any operation that modifies data. Read operations don't need it. |
| Data class for JPA entity? | ❌ Don't do it! Use regular class to avoid equals/hashCode issues with lazy loading. |
| Why data/ instead of persistence/ + repository/? | They're tightly coupled and work together. Simpler package structure = easier to learn. |
| Why is GlobalExceptionHandler at top level? | It's application-wide, not task-specific. Prepares for multi-domain apps (User, Order, etc.). |
| Should I use JPA or JDBC? | JPA for standard CRUD (faster development). JDBC for complex queries or performance-critical operations. |
| What are Testcontainers for? | Running tests against real databases (PostgreSQL, MySQL, etc.) in Docker. Higher confidence than H2. |

### Next Steps
1. See `EXERCISES.md` for hands-on labs
2. Try adding new features (see exercises)
3. Experiment with breaking things to understand error handling
4. Study the tests to understand testing strategies

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.7 | Application framework |
| Kotlin | 2.2.21 | Programming language |
| Spring Data JPA | 3.5.7 | ORM database access (primary) |
| Spring Data JDBC | 3.5.7 | Manual SQL database access (alternative) |
| Spring Security | 3.5.7 | Authentication/authorization |
| H2 Database | Runtime | In-memory database (development & tests) |
| PostgreSQL | Test | Production database (Testcontainers) |
| Kotest | 5.9.1 | Testing framework |
| MockK | 1.13.13 | Mocking library |
| Testcontainers | 1.19.3 | Docker containers for integration tests |
| Java | 23 | Runtime platform |

## 📖 Further Reading

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Kotest Documentation](https://kotest.io/)
- [Kotlin for Spring](https://spring.io/guides/tutorials/spring-boot-kotlin/)

## 📄 License

This is a learning project for educational purposes.

---

**Questions?** Study the code comments - they explain the "why" behind each decision!
