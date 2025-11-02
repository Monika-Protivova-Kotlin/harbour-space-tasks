package space.harbour.tasks.task.exception

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest : FunSpec() {

    private val globalExceptionHandler = GlobalExceptionHandler()

    init {

        context("handleTaskNotFoundException") {

            test("should return 404 NOT_FOUND with correct error response") {
                val exception = TaskNotFoundException("Task not found")

                val response = globalExceptionHandler.handleTaskNotFoundException(exception)

                response.statusCode.shouldBe(HttpStatus.NOT_FOUND)
                response.body?.status.shouldBe(404)
                response.body?.message.shouldBe("Task not found")
            }

            test("should handle TaskNotFoundException with ID constructor") {
                val exception = TaskNotFoundException(123L)

                val response = globalExceptionHandler.handleTaskNotFoundException(exception)

                response.statusCode.shouldBe(HttpStatus.NOT_FOUND)
                response.body?.status.shouldBe(404)
                response.body?.message.shouldBe("Task with id 123 not found")
            }
        }

        context("handleInvalidTaskException") {

            test("should return 400 BAD_REQUEST with correct error response") {
                val exception = InvalidTaskException("Invalid task data")

                val response = globalExceptionHandler.handleInvalidTaskException(exception)

                response.statusCode.shouldBe(HttpStatus.BAD_REQUEST)
                response.body?.status.shouldBe(400)
                response.body?.message.shouldBe("Invalid task data")
            }

            test("should handle blank description error") {
                val exception = InvalidTaskException("Task description cannot be blank")

                val response = globalExceptionHandler.handleInvalidTaskException(exception)

                response.statusCode.shouldBe(HttpStatus.BAD_REQUEST)
                response.body?.status.shouldBe(400)
                response.body?.message.shouldBe("Task description cannot be blank")
            }
        }

        context("handleTaskAlreadyExistsException") {

            test("should return 409 CONFLICT with correct error response") {
                val exception = TaskAlreadyExistsException("Task already exists with this ID")

                val response = globalExceptionHandler.handleTaskAlreadyExistsException(exception)

                response.statusCode.shouldBe(HttpStatus.CONFLICT)
                response.body?.status.shouldBe(409)
                response.body?.message.shouldBe("Task already exists with this ID")
            }
        }

        context("handleTaskOperationException") {

            test("should return 500 INTERNAL_SERVER_ERROR with correct error response") {
                val exception = TaskOperationException("Failed to perform operation")

                val response = globalExceptionHandler.handleTaskOperationException(exception)

                response.statusCode.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
                response.body?.status.shouldBe(500)
                response.body?.message.shouldBe("Failed to perform operation")
            }

            test("should handle TaskOperationException with cause") {
                val cause = RuntimeException("Database connection failed")
                val exception = TaskOperationException("Failed to save task", cause)

                val response = globalExceptionHandler.handleTaskOperationException(exception)

                response.statusCode.shouldBe(HttpStatus.INTERNAL_SERVER_ERROR)
                response.body?.status.shouldBe(500)
                response.body?.message.shouldBe("Failed to save task")
            }
        }
    }
}
