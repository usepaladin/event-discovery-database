import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.4.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.google.protobuf") version "0.9.4"
    id("org.asciidoctor.jvm.convert") version "3.3.2"
    kotlin("plugin.jpa") version "1.9.25"
}

group = "Veridius"
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
val springCloudVersion by extra("2024.0.0")

dependencies {

    // Spring boot + Core
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("io.grpc:grpc-services")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.grpc:spring-grpc-spring-boot-starter")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")


    // Debezium
    implementation("io.debezium:debezium-core:3.0.7.Final")
    implementation("io.debezium:debezium-api:3.0.7.Final")
    implementation("io.debezium:debezium-connector-mongodb:3.0.7.Final")
    implementation("io.debezium:debezium-connector-mysql:3.0.7.Final")
    implementation("io.debezium:debezium-connector-postgres:3.0.7.Final")

    // Database Connections

    // MongoDB
    // KEEP DRIVERS AT 4.11
    // Debezium supports MongoDB drivers at 4.11, waiting on further support to migrate to v5 lol
    implementation("org.mongodb:mongodb-driver-sync:4.11.0")
    implementation("org.mongodb:mongodb-driver-core:4.11.0")
    implementation("org.mongodb:bson:4.11.0")
    implementation("org.mongodb:mongodb-driver-legacy:4.11.0")
    implementation("org.mongodb:mongodb-driver-reactivestreams:4.11.0")
    //Postgres
    implementation("com.zaxxer:HikariCP:6.2.1")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.9.2") // Postgres JsonB storage support
    // Cassandra
    implementation("com.datastax.oss:java-driver-core:4.17.0")
    // MySQL
    implementation("mysql:mysql-connector-java:8.0.33")

    // Logging
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        // Exclude Mockito
        exclude(group = "org.mockito")
    }
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.17")
    testImplementation("org.springframework.grpc:spring-grpc-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("io.debezium:debezium-testing-testcontainers:3.0.7.Final")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.grpc:spring-grpc-dependencies:${property("springGrpcVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
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
