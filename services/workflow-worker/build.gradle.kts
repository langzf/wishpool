plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "com.wishpool"
version = "0.0.1-SNAPSHOT"
description = "WishPool durable workflow worker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("io.temporal:temporal-sdk:1.38.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.15.4")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.4")
    implementation("tools.jackson.module:jackson-module-kotlin:3.1.4")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

application {
    mainClass.set("com.wishpool.workflow.WorkflowWorkerApplicationKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
