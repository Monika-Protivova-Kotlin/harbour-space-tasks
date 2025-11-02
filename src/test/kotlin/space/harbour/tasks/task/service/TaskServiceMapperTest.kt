package space.harbour.tasks.task.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import space.harbour.tasks.task.controller.dto.TaskResponse
import space.harbour.tasks.task.domain.Task
import space.harbour.tasks.task.domain.TaskStatus
import space.harbour.tasks.task.persistence.TaskEntity
import space.harbour.tasks.task.service.mapper.toDomain
import space.harbour.tasks.task.service.mapper.toEntity
import space.harbour.tasks.task.service.mapper.toResponse

class TaskServiceMapperTest : FunSpec() {

    init {

        context("TaskEntity.toDomain()") {

            test("Should convert TaskEntity with non-null id to Task domain") {
                val taskEntity = TaskEntity(
                    id = 1L,
                    description = "Test Task",
                    status = TaskStatus.IN_PROGRESS
                )

                val result = taskEntity.toDomain()

                result.shouldBe(
                    Task(
                        id = 1L,
                        description = "Test Task",
                        status = TaskStatus.IN_PROGRESS
                    )
                )
            }

            test("Should convert TaskEntity with null id to Task domain with 0L") {
                TaskEntity(
                    id = null,
                    description = "New Task",
                    status = TaskStatus.NEW
                ).toDomain()
                    .shouldBe(
                        Task(
                            id = 0L,
                            description = "New Task",
                            status = TaskStatus.NEW
                        )
                    )
            }
        }

        context("Task.toEntity()") {

            test("Should convert Task with non-zero id to TaskEntity") {
                Task(
                    id = 42L,
                    description = "Existing Task",
                    status = TaskStatus.COMPLETED
                ).toEntity()
                    .shouldBe(
                        TaskEntity(
                            id = 42L,
                            description = "Existing Task",
                            status = TaskStatus.COMPLETED
                        )
                )
            }

            test("Should convert Task with 0L id to TaskEntity with null id") {
                Task(
                    id = 0L,
                    description = "New Task",
                    status = TaskStatus.NEW
                ).toEntity()
                    .shouldBe(
                        TaskEntity(
                            id = null,
                            description = "New Task",
                            status = TaskStatus.NEW
                        )
                    )
            }
        }

        context("Task.toResponse()") {

            test("Should convert Task domain to TaskResponse DTO") {
                Task(
                    id = 5L,
                    description = "Response Task",
                    status = TaskStatus.REJECTED
                ).toResponse()
                    .shouldBe(
                        TaskResponse(
                            id = 5L,
                            description = "Response Task",
                            status = TaskStatus.REJECTED
                        )
                    )
            }
        }

        context("TaskResponse.toDomain()") {

            test("Should convert TaskResponse DTO to Task domain") {
                TaskResponse(
                    id = 10L,
                    description = "Domain Task",
                    status = TaskStatus.IN_PROGRESS
                ).toDomain()
                    .shouldBe(
                        Task(
                            id = 10L,
                            description = "Domain Task",
                            status = TaskStatus.IN_PROGRESS
                        )
                    )
            }
        }
    }
}
