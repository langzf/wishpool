plugins {
    kotlin("jvm") version "2.3.21"
    application
}

group = "com.wishpool"
version = "0.0.1-SNAPSHOT"
description = "WishPool notification dispatcher"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    val ktorVersion = "3.5.2"

    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
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
    mainClass.set("com.wishpool.notification.NotificationServiceApplicationKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
