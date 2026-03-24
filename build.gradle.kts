plugins {
    id("com.inductiveautomation.ignition") version "1.3.1"
    kotlin("jvm") version "1.9.22" apply false
}

ignitionModule {
    name.set("AETHER")
    fileName.set("Aether.modl")
    id.set("com.example.aether")
    moduleVersion.set(project.version.toString())
    moduleDescription.set("A multi-tiered Java/Kotlin-based Ignition module fusing telemetry with Agentic RAG and TBL.")
    requiredIgnitionVersion.set("8.1.36")
    
    // Define the subprojects and their scopes within the Module
    projectScopes.put(project(":core"), "G")
    projectScopes.put(project(":brain"), "G")
    projectScopes.put(project(":perspective"), "G")
    
    // Gateway Hook
    hooks.put("com.example.aether.AetherGatewayHook", "G")
}

allprojects {
    group = "com.example.aether"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-releases/") }
        maven { url = uri("https://nexus.inductiveautomation.com/repository/inductiveautomation-thirdparty/") }
    }
}
