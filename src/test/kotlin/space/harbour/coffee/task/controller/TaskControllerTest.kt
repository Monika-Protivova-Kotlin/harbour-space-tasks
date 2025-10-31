package space.harbour.coffee.task.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import space.harbour.coffee.task.controller.TaskController
import space.harbour.coffee.task.controller.dto.NewTaskRequest
import space.harbour.coffee.task.domain.Task
import space.harbour.coffee.task.domain.TaskStatus
import space.harbour.coffee.task.exception.InvalidTaskException
import space.harbour.coffee.task.exception.TaskNotFoundException
import space.harbour.coffee.task.service.TaskService

@WebMvcTest(TaskController::class)
class TaskControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var taskService: TaskService

    // GET /api/tasks
    @Test
    fun `should return empty list when no tasks exist`() {
        every { taskService.getTasks() } returns emptyList()

        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)

        verify(exactly = 1) { taskService.getTasks() }
    }

    @Test
    fun `should return all tasks`() {
        val tasks = listOf(
            Task(id = 1, description = "Task 1", status = TaskStatus.NEW),
            Task(id = 2, description = "Task 2", status = TaskStatus.IN_PROGRESS)
        )
        every { taskService.getTasks() } returns tasks

        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].description").value("Task 1"))
            .andExpect(jsonPath("$[0].status").value("NEW"))
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].description").value("Task 2"))
            .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"))

        verify(exactly = 1) { taskService.getTasks() }
    }

    // GET /api/tasks/{id}
    @Test
    fun `should return 404 when task does not exist`() {
        every { taskService.getTask(1L) } throws TaskNotFoundException(1L)

        mockMvc.perform(get("/api/tasks/1"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Task with id 1 not found"))

        verify(exactly = 1) { taskService.getTask(1L) }
    }

    @Test
    fun `should return task when it exists`() {
        val task = Task(id = 1, description = "Task 1", status = TaskStatus.NEW)
        every { taskService.getTask(1L) } returns task

        mockMvc.perform(get("/api/tasks/1"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.description").value("Task 1"))
            .andExpect(jsonPath("$.status").value("NEW"))

        verify(exactly = 1) { taskService.getTask(1L) }
    }

    // POST /api/tasks
    @Test
    fun `should return 400 when description is blank`() {
        val request = NewTaskRequest(description = "")
        every { taskService.addTask("") } throws InvalidTaskException("Task description cannot be blank")

        mockMvc.perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Task description cannot be blank"))

        verify(exactly = 1) { taskService.addTask("") }
    }

    @Test
    fun `should return 400 when description is only whitespace`() {
        val request = NewTaskRequest(description = "   ")
        every { taskService.addTask("   ") } throws InvalidTaskException("Task description cannot be blank")

        mockMvc.perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Task description cannot be blank"))

        verify(exactly = 1) { taskService.addTask("   ") }
    }

    @Test
    fun `should create task and return 201 with TaskResponse`() {
        val request = NewTaskRequest(description = "New Task")
        val createdTask = Task(id = 1, description = "New Task", status = TaskStatus.NEW)
        every { taskService.addTask("New Task") } returns createdTask

        mockMvc.perform(
            post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.description").value("New Task"))
            .andExpect(jsonPath("$.status").value("NEW"))

        verify(exactly = 1) { taskService.addTask("New Task") }
    }

    // PUT /api/tasks/{id}
    @Test
    fun `should return 404 when updating non-existent task`() {
        every { taskService.updateTask(1L, any()) } throws TaskNotFoundException(1L)

        val updateRequest = mapOf(
            "id" to 1,
            "description" to "Updated",
            "status" to "COMPLETED"
        )

        mockMvc.perform(
            put("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Task with id 1 not found"))

        verify(exactly = 1) { taskService.updateTask(1L, any()) }
    }

    @Test
    fun `should return 400 when updating with blank description`() {
        every { taskService.updateTask(1L, any()) } throws InvalidTaskException("Task description cannot be blank")

        val updateRequest = mapOf(
            "id" to 1,
            "description" to "",
            "status" to "COMPLETED"
        )

        mockMvc.perform(
            put("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Task description cannot be blank"))

        verify(exactly = 1) { taskService.updateTask(1L, any()) }
    }

    @Test
    fun `should update task and return TaskResponse`() {
        val updatedTask = Task(id = 1, description = "Updated Task", status = TaskStatus.COMPLETED)
        every { taskService.updateTask(1L, any()) } returns updatedTask

        val updateRequest = mapOf(
            "id" to 1,
            "description" to "Updated Task",
            "status" to "COMPLETED"
        )

        mockMvc.perform(
            put("/api/tasks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.description").value("Updated Task"))
            .andExpect(jsonPath("$.status").value("COMPLETED"))

        verify(exactly = 1) { taskService.updateTask(1L, any()) }
    }

    // DELETE /api/tasks/{id}
    @Test
    fun `should return 404 when deleting non-existent task`() {
        every { taskService.deleteTask(1L) } throws TaskNotFoundException(1L)

        mockMvc.perform(delete("/api/tasks/1"))
            .andExpect(status().isNotFound)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Task with id 1 not found"))

        verify(exactly = 1) { taskService.deleteTask(1L) }
    }

    @Test
    fun `should delete task successfully and return 204`() {
        every { taskService.deleteTask(1L) } returns Unit

        mockMvc.perform(delete("/api/tasks/1"))
            .andExpect(status().isNoContent)

        verify(exactly = 1) { taskService.deleteTask(1L) }
    }
}
