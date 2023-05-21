plugins {
    kotlin("jvm") version "1.8.20"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("dev.kord:kord-core:0.9.0")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:0.41.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}