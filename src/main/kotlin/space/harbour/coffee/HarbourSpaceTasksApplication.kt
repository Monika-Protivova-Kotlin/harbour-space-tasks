package space.harbour.coffee

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HarbourSpaceTasksApplication

fun main(args: Array<String>) {
	runApplication<HarbourSpaceTasksApplication>(*args)
}
