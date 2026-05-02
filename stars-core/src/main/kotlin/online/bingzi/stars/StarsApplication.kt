package online.bingzi.stars

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class StarsApplication

fun main(args: Array<String>) {
    runApplication<StarsApplication>(*args)
}
