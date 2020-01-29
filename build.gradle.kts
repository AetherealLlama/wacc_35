plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.3.41"

    // Apply the application plugin to add support for building a CLI application.
    application

    // Apply the antlr plugin to add support for ANTLR4
    antlr
}

repositories {
    // Use jcenter for resolving dependencies.
    // You can declare any Maven/Ivy/file repository here.
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
    implementation("commons-codec:commons-codec:1.14")
    implementation("ch.qos.logback:logback-core:1.2.3")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.61")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // Use ANTLR4
    antlr("org.antlr:antlr4:4.7.1")
}

application {
    // Define the main class for the application.
    mainClassName = "wacc.AppKt"
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-visitor")
}