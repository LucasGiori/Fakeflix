package com.fakeflix

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FakeflixApplication

fun main(args: Array<String>) {
	runApplication<FakeflixApplication>(*args)
}
