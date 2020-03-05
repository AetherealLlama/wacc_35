plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.61"

    // Apply the application plugin to add support for building a CLI application.
    application

    // Apply the antlr plugin to add support for ANTLR4
    antlr

    // Apply Shadow plugin for fat jar creation
    id("com.github.johnrengelman.shadow") version "5.2.0"

    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

repositories {
    jcenter()
    mavenCentral()
    maven(url = "https://kotlin.bintray.com/kotlin-dev")
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("info.picocli:picocli:4.1.4")
    implementation("commons-io:commons-io:2.6")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.61")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // Use ANTLR4
    antlr("org.antlr:antlr4:4.8")
}

application {
    // Define the main class for the application.
    mainClassName = "wacc.AppKt"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "wacc.AppKt"
    }
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    // Make ANTLR generate visitor pattern files
    arguments = arguments + listOf("-visitor")
}

// Make sure we generate ANTLR sources before trying to compile kt files
tasks.compileKotlin {
    dependsOn("generateGrammarSource")
}
