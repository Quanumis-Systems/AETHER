plugins {
    kotlin("jvm")
}

dependencies {
    // Ignition Gateway & Common APIs - compileOnly because they are provided by the Ignition platform at runtime
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-common:8.1.36")
    compileOnly("com.inductiveautomation.ignitionsdk:ignition-gateway-api:8.1.36")

    // Dependency on our shared common module
    implementation(project(":core"))

    // Phase 1: High-Fidelity Ledger - Cryptographic Hashing
    // BouncyCastle is used here to securely hash 1,000-sample blocks into our Merkle Tree
    implementation("org.bouncycastle:bcprov-jdk18on:1.77")

    // Phase 2: Predictive Trajectory Engine - Multi-dimensional Forecasting
    // ONNX Runtime allows us to run lightweight Transformers or LSTMs natively in the Java Gateway Hook
    implementation("com.microsoft.onnxruntime:onnxruntime:1.17.1")

    // Phase 3: Agentic RAG Layer - Semantic Telemetry & What-If Simulations
    // LangChain4j provides the abstraction for prompt construction and agentic feedback loops
    implementation("dev.langchain4j:langchain4j:0.30.0")
    implementation("dev.langchain4j:langchain4j-qdrant:0.30.0") // Or milvus depending on vector store selection
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
