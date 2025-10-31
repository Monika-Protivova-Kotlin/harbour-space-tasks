package space.harbour.coffee.task.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import space.harbour.coffee.task.persistence.TaskEntity

@Repository
interface TaskRepository : JpaRepository<TaskEntity, Long>