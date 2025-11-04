package space.harbour.tasks.task.data

import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Primary
@Repository
interface TaskRepository : JpaRepository<TaskEntity, Long>
