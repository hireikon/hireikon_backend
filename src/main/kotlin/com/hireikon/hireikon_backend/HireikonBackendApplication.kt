package com.hireikon.hireikon_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class HireikonBackendApplication

fun main(args: Array<String>) {
	runApplication<HireikonBackendApplication>(*args)
}