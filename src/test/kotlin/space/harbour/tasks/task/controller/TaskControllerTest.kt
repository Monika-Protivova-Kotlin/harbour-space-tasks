package space.harbour.tasks.task.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import space.harbour.tasks.config.SecurityConfig
import space.harbour.tasks.task.controller.dto.ErrorResponse
import space.harbour.tasks.task.controller.dto.NewTaskRequest
import space.harbour.tasks.task.controller.dto.TaskResponse
import space.harbour.tasks.task.domain.Task
import space.harbour.tasks.task.domain.TaskStatus
import space.harbour.tasks.task.exception.InvalidTaskException
import space.harbour.tasks.task.exception.TaskNotFoundException
import space.harbour.tasks.task.service.TaskService

@WebMvcTest(controllers = [TaskController::class])
@Import(TaskControllerTest.TaskControllerTestConfig::class, SecurityConfig::class)
class TaskControllerTest : FunSpec() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var taskService: TaskService

    @TestConfiguration
    class TaskControllerTestConfig {

        @Bean
        fun taskService(): TaskService = mockk(relaxed = false)
    }

    init {

        beforeTest {
            clearAllMocks()
        }

        context("GET /api/tasks") {

            test("should return all tasks") {
                val tasks = listOf(
                    Task(id = 1, description = "Task 1", status = TaskStatus.NEW),
                    Task(id = 2, description = "Task 2", status = TaskStatus.IN_PROGRESS)
                )
                every { taskService.getTasks() } returns tasks

                val response = mockMvc.perform(
                    get("/api/tasks")
                        .with(httpBasic("admin", "password123"))
                )

                response.andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//                .andExpect(jsonPath("$").isArray)
//                .andExpect(jsonPath("$.length()").value(2))
//                .andExpect(jsonPath("$[0].id").value(1))
//                .andExpect(jsonPath("$[0].description").value("Task 1"))
//                .andExpect(jsonPath("$[0].status").value("NEW"))
//                .andExpect(jsonPath("$[1].id").value(2))
//                .andExpect(jsonPath("$[1].description").value("Task 2"))
//                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))


                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    Array<TaskResponse>::class.java
                )

                responseBody.shouldBe(
                    arrayOf(
                        TaskResponse(id = 1, description = "Task 1", status = TaskStatus.NEW),
                        TaskResponse(id = 2, description = "Task 2", status = TaskStatus.IN_PROGRESS),
                    )
                )

                verify(exactly = 1) { taskService.getTasks() }
            }

            test("should return 401 when not authenticated") {
                val response = mockMvc.perform(get("/api/tasks"))

                response.andExpect(status().isUnauthorized)
            }

            test("should return 401 with wrong credentials") {
                val response = mockMvc.perform(
                    get("/api/tasks")
                        .with(httpBasic("admin", "wrongpassword"))
                )

                response.andExpect(status().isUnauthorized)
            }
        }

        context("POST /api/tasks") {

            test("should create task and return 201") {
                val request = NewTaskRequest(description = "New Task")

                val createdTask = Task(
                    id = 1,
                    description = "New Task",
                    status = TaskStatus.NEW
                )

                every { taskService.addTask(any()) } returns createdTask

                val response = mockMvc.perform(
                    post("/api/tasks")
                        .with(httpBasic("admin", "password123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )

                response.andExpect(status().isCreated)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    TaskResponse::class.java
                )

                responseBody.shouldBe(
                    TaskResponse(
                        id = createdTask.id,
                        description = createdTask.description,
                        status = createdTask.status
                    )
                )

                verify(exactly = 1) {
                    taskService.addTask(description = createdTask.description)
                }
            }

            test("should return 400 when description when task is invalid") {
                val request = NewTaskRequest(description = "")
                val exception = InvalidTaskException("Invalid task error")

                every { taskService.addTask(any()) } throws exception

                val response = mockMvc.perform(
                    post("/api/tasks")
                        .with(httpBasic("admin", "password123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )

                response.andExpect(status().isBadRequest)

                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    ErrorResponse::class.java
                )

                responseBody.shouldBe(
                    ErrorResponse(
                        status = 400,
                        message = "Invalid task error"
                    )
                )

                verify(exactly = 1) {
                    taskService.addTask(any())
                }
            }

            test("should return 401 when not authenticated") {
                val request = NewTaskRequest(description = "New Task")

                val response = mockMvc.perform(
                    post("/api/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )

                response.andExpect(status().isUnauthorized)
            }
        }

        context("PUT /api/tasks/{id}") {

            test("should update task and return 200") {
                val updateRequest = TaskResponse(
                    id = 1,
                    description = "Updated Task",
                    status = TaskStatus.COMPLETED
                )

                val updatedTask = Task(
                    id = 1,
                    description = "Updated Task",
                    status = TaskStatus.COMPLETED
                )

                every { taskService.updateTask(any(), any()) } returns updatedTask

                val response = mockMvc.perform(
                    put("/api/tasks/1")
                        .with(httpBasic("admin", "password123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                )

                response.andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    TaskResponse::class.java
                )

                responseBody.shouldBe(
                    TaskResponse(
                        id = 1,
                        description = "Updated Task",
                        status = TaskStatus.COMPLETED
                    )
                )
                verify(exactly = 1) {
                    taskService.updateTask(
                        id = updatedTask.id,
                        updatedTask = updatedTask
                    )
                }
            }

            test("should return 404 when task does not exist") {
                val updateRequest = TaskResponse(
                    id = 999,
                    description = "Updated",
                    status = TaskStatus.COMPLETED
                )

                every { taskService.updateTask(any(), any()) } throws TaskNotFoundException(updateRequest.id)

                val response = mockMvc.perform(
                    put("/api/tasks/999")
                        .with(httpBasic("admin", "password123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                )

                response.andExpect(status().isNotFound)

                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    ErrorResponse::class.java
                )

                responseBody.shouldBe(
                    ErrorResponse(
                        status = 404,
                        message = "Task with id 999 not found"
                    )
                )

                verify(exactly = 1) { taskService.updateTask(999L, any()) }
            }

            test("should return 400 when description is blank") {
                val updateRequest = TaskResponse(
                    id = 1,
                    description = "",
                    status = TaskStatus.COMPLETED
                )
                val exception = InvalidTaskException("Task description cannot be blank")

                every { taskService.updateTask(any(), any()) } throws exception

                val response = mockMvc.perform(
                    put("/api/tasks/1")
                        .with(httpBasic("admin", "password123"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                )

                response.andExpect(status().isBadRequest)

                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    ErrorResponse::class.java
                )

                responseBody.shouldBe(
                    ErrorResponse(
                        status = 400,
                        message = "Task description cannot be blank"
                    )
                )

                verify(exactly = 1) {
                    taskService.updateTask(
                        id = 1L,
                        updatedTask = any<Task>()
                    )
                }
            }

            test("should return 401 when not authenticated") {
                val updateRequest = TaskResponse(
                    id = 1,
                    description = "Updated",
                    status = TaskStatus.COMPLETED
                )

                val response = mockMvc.perform(
                    put("/api/tasks/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest))
                )

                response.andExpect(status().isUnauthorized)
            }
        }

        context("DELETE /api/tasks/{id}") {

            test("should delete task and return 204") {
                val taskId = 1L
                every { taskService.deleteTask(any()) } returns Unit

                val response = mockMvc.perform(
                    delete("/api/tasks/$taskId")
                        .with(httpBasic("admin", "password123"))
                        .with(csrf())
                )

                response.andExpect(status().isNoContent)

                verify(exactly = 1) {
                    taskService.deleteTask(taskId)
                }
            }

            test("should return 404 when task does not exist") {
                val taskId = 999L
                every { taskService.deleteTask(any()) } throws TaskNotFoundException(taskId)

                val response = mockMvc.perform(
                    delete("/api/tasks/$taskId")
                        .with(httpBasic("admin", "password123"))
                        .with(csrf())
                )

                response.andExpect(status().isNotFound)

                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    ErrorResponse::class.java
                )

                responseBody.shouldBe(
                    ErrorResponse(
                        status = 404,
                        message = "Task with id 999 not found"
                    )
                )

                verify(exactly = 1) {
                    taskService.deleteTask(taskId)
                }
            }

            test("should return 401 when not authenticated") {
                val response = mockMvc.perform(
                    delete("/api/tasks/1")
                        .with(csrf())
                )

                response.andExpect(status().isUnauthorized)
            }
        }

        context("GET /api/tasks/{id}") {

            test("should return task when it exists") {
                val task = Task(
                    id = 1,
                    description = "Task 1",
                    status = TaskStatus.NEW
                )

                every { taskService.getTask(any()) } returns task

                val response = mockMvc.perform(
                    get("/api/tasks/1")
                        .with(httpBasic("admin", "password123"))
                )

                response.andExpect(status().isOk)
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))

                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    TaskResponse::class.java
                )

                responseBody.shouldBe(
                    TaskResponse(
                        id = task.id,
                        description = task.description,
                        status = TaskStatus.NEW
                    )
                )

                verify(exactly = 1) {
                    taskService.getTask(task.id)
                }
            }

            test("should return 404 when task does not exist") {
                val taskId = 999L

                every { taskService.getTask(any()) } throws TaskNotFoundException(taskId)

                val response = mockMvc.perform(
                    get("/api/tasks/999")
                        .with(httpBasic("admin", "password123"))
                )

                response.andExpect(status().isNotFound)

                val responseBody = objectMapper.readValue(
                    response.andReturn().response.contentAsString,
                    ErrorResponse::class.java
                )

                responseBody.shouldBe(
                    ErrorResponse(
                        status = 404,
                        message = "Task with id 999 not found"
                    )
                )

                verify(exactly = 1) {
                    taskService.getTask(taskId)
                }
            }

            test("should return 401 when not authenticated") {
                val response = mockMvc.perform(get("/api/tasks/1"))

                response.andExpect(status().isUnauthorized)
            }
        }
    }
}
