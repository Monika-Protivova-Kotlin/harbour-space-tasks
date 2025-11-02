package space.harbour.tasks.task.persistence

import jakarta.persistence.*
import space.harbour.tasks.task.domain.TaskStatus

@Entity
@Table(name = "tasks")
data class TaskEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 1000, nullable = false)
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TaskStatus
)
