plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:8.1.36")
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-gateway-api:8.1.36")
    
    // Perspective specific APIs
    compileOnly("com.inductiveautomation.ignitionsdk:perspective-gateway:8.1.36")
    compileOnly("com.inductiveautomation.ignitionsdk:perspective-common:8.1.36")

    implementation(project(":core"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
