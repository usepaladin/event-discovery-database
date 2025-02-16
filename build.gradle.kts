import com.google.protobuf.gradle.id

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.2"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.protobuf") version "0.9.4"
	id("org.asciidoctor.jvm.convert") version "3.3.2"
}

group = "Veredius"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")
extra["springGrpcVersion"] = "0.3.0"

dependencies {

	// Spring boot + Core
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.grpc:grpc-services")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")
	implementation("org.springframework.kafka:spring-kafka")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	runtimeOnly("io.micrometer:micrometer-registry-prometheus")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	// Debezium
	implementation("io.debezium:debezium-core:3.0.7.Final")
	implementation("io.debezium:debezium-api:3.0.7.Final")
	implementation("io.debezium:debezium-connector-mongodb:3.0.7.Final")
	implementation("io.debezium:debezium-connector-mysql:3.0.7.Final")
	implementation("io.debezium:debezium-connector-postgres:3.0.7.Final")

	// Database Connections
	implementation("org.mongodb:mongo-driver-sync:4.11.1")
	implementation("org.postgresql:postgresql:42.7.4")
	implementation("com.datastax.oss:java-driver-core:4.17.0")
	implementation("mysql:mysql-connector-java:8.0.33")

	// Testing

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.springframework.grpc:spring-grpc-test")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
	testImplementation("io.debezium:debezium-testing-testcontainers:3.0.7.Final")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc"
	}
	plugins {
		id("grpc") {
			artifact = "io.grpc:protoc-gen-grpc-java"
		}
	}
	generateProtoTasks {
		all().forEach {
			it.plugins {
				id("grpc") {
					option("jakarta_omit")
					option("@generated=omit")
				}
			}
		}
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
	outputs.dir(project.extra["snippetsDir"]!!)
}

tasks.asciidoctor {
	inputs.dir(project.extra["snippetsDir"]!!)
	dependsOn(tasks.test)
}
