package space.harbour.coffee.task.exception

class TaskNotFoundException(message: String) : RuntimeException(message) {
    constructor(id: Long) : this("Task with id $id not found")
}
