package space.harbour.tasks.task.controller

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import space.harbour.tasks.task.controller.dto.NewTaskRequest
import space.harbour.tasks.task.controller.dto.TaskResponse
import space.harbour.tasks.task.service.TaskService
import space.harbour.tasks.task.service.mapper.toDomain
import space.harbour.tasks.task.service.mapper.toResponse

/**
 * REST Controller for Task management API.
 *
 * This is the HTTP/web layer that:
 * 1. Receives HTTP requests from clients (Postman, browsers, mobile apps, etc.)
 * 2. Converts HTTP requests to method calls
 * 3. Delegates business logic to the Service layer (TaskService)
 * 4. Converts results back to HTTP responses
 *
 * IMPORTANT ARCHITECTURAL PRINCIPLE: "Thin Controllers"
 * - Controllers should NOT contain business logic
 * - Controllers should NOT directly access the database (Repository)
 * - Controllers ONLY handle HTTP concerns (status codes, request/response mapping)
 * - Business logic belongs in the Service layer
 *
 * @RestController: Combines @Controller + @ResponseBody
 *   - Automatically converts return values to JSON
 *   - Spring knows this is a REST API, not a web page controller
 *
 * @RequestMapping("/api/tasks"): All endpoints start with /api/tasks
 *   - Base URL for all methods in this controller
 *   - Example: getTasks() is accessible at GET /api/tasks
 */
@RestController
@RequestMapping("/api/tasks")
class TaskController(
    /**
     * Dependency injection using constructor injection (recommended pattern).
     *
     * Spring automatically creates and injects the TaskService.
     * Benefits:
     * - Easy to test (can mock TaskService in tests)
     * - Immutable (val, not var)
     * - Clear dependencies (you see all dependencies in constructor)
     */
    private val taskService: TaskService,
) {

    /**
     * GET /api/tasks - Retrieve all tasks
     *
     * Example request:
     *   GET http://localhost:8081/api/tasks
     *   Headers: Authorization: Basic admin:password123
     *
     * Example response (200 OK):
     *   [
     *     {"id": 1, "description": "Learn Spring Boot", "status": "NEW"},
     *     {"id": 2, "description": "Write tests", "status": "IN_PROGRESS"}
     *   ]
     *
     * @GetMapping: Maps HTTP GET requests to this method
     * @return List of all tasks as TaskResponse DTOs (auto-converted to JSON)
     */
    @GetMapping
    fun getTasks(): List<TaskResponse> {
        // 1. Call service to get domain objects (Task)
        // 2. Map each Task to TaskResponse (DTO for HTTP response)
        // 3. Spring automatically converts List<TaskResponse> to JSON
        return taskService.getTasks().map { it.toResponse() }
    }

    /**
     * GET /api/tasks/{id} - Retrieve a specific task by ID
     *
     * Example request:
     *   GET http://localhost:8081/api/tasks/1
     *
     * Example response (200 OK):
     *   {"id": 1, "description": "Learn Spring Boot", "status": "NEW"}
     *
     * Example error response (404 Not Found):
     *   {"status": 404, "message": "Task with id 999 not found"}
     *
     * @PathVariable: Extracts {id} from URL path
     *   - URL: /api/tasks/123 → taskId = 123
     *   - Spring automatically converts String to Long
     */
    @GetMapping("/{id}")
    fun getTask(
        @PathVariable(name = "id") taskId: Long
    ): TaskResponse {
        // Service layer handles the business logic and exception throwing
        val task = taskService.getTask(taskId)
        return task.toResponse()
    }

    /**
     * POST /api/tasks - Create a new task
     *
     * Example request:
     *   POST http://localhost:8081/api/tasks
     *   Content-Type: application/json
     *   {"description": "New task"}
     *
     * Example response (201 Created):
     *   {"id": 3, "description": "New task", "status": "NEW"}
     *
     * Example error response (400 Bad Request):
     *   {"status": 400, "message": "Task description cannot be blank"}
     *
     * @PostMapping: Maps HTTP POST requests (for creating resources)
     * @ResponseStatus(HttpStatus.CREATED): Returns 201 status code on success
     *   - 201 means "resource created successfully"
     *   - This is RESTful best practice (not 200 OK for creation)
     *
     * @RequestBody: Spring converts JSON request body to NewTaskRequest object
     *   - Automatic deserialization from JSON to Kotlin object
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTask(
        @RequestBody request: NewTaskRequest
    ): TaskResponse {
        // Validation is handled in service layer, not here
        // Controller just passes data through
        val task = taskService.addTask(request.description)
        return task.toResponse()
    }

    /**
     * PUT /api/tasks/{id} - Update an existing task
     *
     * Example request:
     *   PUT http://localhost:8081/api/tasks/1
     *   Content-Type: application/json
     *   {"id": 1, "description": "Updated task", "status": "COMPLETED"}
     *
     * Example response (200 OK):
     *   {"id": 1, "description": "Updated task", "status": "COMPLETED"}
     *
     * Example error responses:
     *   404 Not Found: {"status": 404, "message": "Task with id 999 not found"}
     *   400 Bad Request: {"status": 400, "message": "Task description cannot be blank"}
     *
     * @PutMapping: Maps HTTP PUT requests (for full resource update)
     *   - PUT is idempotent: same request multiple times = same result
     *   - For partial updates, use PATCH instead
     */
    @PutMapping("/{id}")
    fun updateTask(
        @PathVariable(name = "id") taskId: Long,
        @RequestBody updatedTask: TaskResponse
    ): TaskResponse {
        // Convert TaskResponse (DTO) to Task (domain model)
        // Service layer handles validation and business logic
        val result = taskService.updateTask(taskId, updatedTask.toDomain())
        return result.toResponse()
    }

    /**
     * DELETE /api/tasks/{id} - Delete a task
     *
     * Example request:
     *   DELETE http://localhost:8081/api/tasks/1
     *
     * Example response (204 No Content):
     *   (empty body - task deleted successfully)
     *
     * Example error response (404 Not Found):
     *   {"status": 404, "message": "Task with id 999 not found"}
     *
     * @DeleteMapping: Maps HTTP DELETE requests (for resource deletion)
     * @ResponseStatus(HttpStatus.NO_CONTENT): Returns 204 on success
     *   - 204 means "success, but no content to return"
     *   - RESTful best practice for DELETE operations
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTask(
        @PathVariable(name = "id") taskId: Long
    ) {
        // Service handles checking if task exists before deleting
        taskService.deleteTask(taskId)
        // No return value needed - 204 No Content
    }
}
