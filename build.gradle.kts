plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "3.5.7"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
	id("nu.studer.jooq") version "9.0"  // jOOQ code generation plugin
}

group = "space.harbour"
version = "0.0.1-SNAPSHOT"
description = "Learning project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(23)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-jooq")  // jOOQ support
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
	testImplementation("io.kotest:kotest-assertions-core:5.9.1")
	testImplementation("io.kotest.extensions:kotest-extensions-spring:1.3.0")
	testImplementation("io.mockk:mockk:1.13.13")

	// Testcontainers for integration testing with real databases
	testImplementation(platform("org.testcontainers:testcontainers-bom:1.19.3"))
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:junit-jupiter")

	// PostgreSQL driver for Testcontainers tests
	testImplementation("org.postgresql:postgresql")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// jOOQ code generation (uses H2 to generate from schema)
	jooqGenerator("com.h2database:h2")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}

	// Add generated jOOQ sources to Kotlin source sets
	sourceSets {
		main {
			kotlin.srcDir("build/generated-sources/jooq")
		}
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Ensure jOOQ code is generated before compiling Kotlin
tasks.named("compileKotlin") {
	dependsOn("generateJooq")
}

// jOOQ Code Generation Configuration
// Generates type-safe Kotlin code from database schema
jooq {
	// Use the same version as Spring Boot's jOOQ runtime to avoid version mismatch
	version.set("3.19.27")

	configurations {
		create("main") {
			jooqConfiguration.apply {
				// Connect to H2 in-memory database and run schema.sql
				jdbc.apply {
					driver = "org.h2.Driver"
					url = "jdbc:h2:mem:jooq_gen;INIT=RUNSCRIPT FROM 'src/main/resources/schema.sql'"
					user = "sa"
					password = ""
				}

				generator.apply {
					// Generate Java code (works perfectly with Kotlin projects)
					name = "org.jooq.codegen.JavaGenerator"

					database.apply {
						name = "org.jooq.meta.h2.H2Database"
						inputSchema = "PUBLIC"
						includes = ".*"  // Include all tables
						excludes = ""
					}

					target.apply {
						packageName = "space.harbour.tasks.generated.jooq"
						directory = "build/generated-sources/jooq"
					}

					generate.apply {
						isPojos = true        // Generate POJOs (Task class)
						isDaos = false        // Don't generate DAOs (we'll write repository manually)
						isRecords = true      // Generate Records (database row representation)
						isImmutablePojos = false  // Mutable POJOs for easier use
					}
				}
			}
		}
	}
}
