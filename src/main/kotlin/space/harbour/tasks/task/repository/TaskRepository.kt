package space.harbour.tasks.task.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import space.harbour.tasks.task.persistence.TaskEntity

@Repository
interface TaskRepository : JpaRepository<TaskEntity, Long>
