plugins {
    kotlin("jvm")
}

dependencies {
    // Ignition Common API
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:8.1.36")
    
    // Core Kotlin & Coroutines for asynchronous, non-blocking agentic loops
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
