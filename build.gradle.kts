plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.wahab"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Ktor Server Core & Netty
    implementation("io.ktor:ktor-server-core-jvm:2.3.8")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.8")
    implementation("io.ktor:ktor-server-html-builder-jvm:2.3.8")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.8")

    // SQLite JDBC Driver
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.wahab.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
