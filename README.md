# AETHER: Agentic ESG & Trajectory Engine for Industrial Operations

AETHER is a multi-tiered, production-grade Ignition 8 module designed to bridge high-speed industrial telemetry with advanced Local Large Language Models (LLMs) and Model Context Protocols (MCP). It operates via a "Negative Feedback Loop," ensuring manufacturing facilities dynamically balance Yield, Safety, and Compliance (ESG) in real-time.

## System Architecture Overview

AETHER eschews traditional linear control logic in favor of an **Agentic Retrieval-Augmented Generation (RAG)** architecture. It acts directly upon the Ignition `GatewayContext` and features three distinct sub-systems.

### 1. The Ingestion Layer: High-Fidelity Ledger & MCP
- **Telemetry Capture**: The `TagProcessInterceptor` buffers real-time EtherNet/IP, Modbus, and DNP3 tag streams into immutable, cryptographically hashed `LedgerBlock` structures (Merkle Trees) for auditable history.
- **Native MCP Server**: Exposes internal Ignition gateway structures directly to local Agentic bundles. The `AetherMcpServer` dynamically serializes the Unified Data Model (UDT) states via standard JSON-RPC (`ignition://tags/current_state`).

### 2. The Intelligence Layer: Agentic ESG RAG & Digital Twin
- **Local-First Execution**: Integrating **LangChain4j**, AETHER ensures no factory telemetry leaves the network. It communicates with localized models (e.g. Mistral/LLaMA 3) via the Ollama endpoint adapters.
- **Embedded Semantic Vectorization**: Machine state trajectories are vectorized using the lightweight ONNX `AllMiniLmL6V2` model and ingested directly into a local Qdrant memory instance.
- **Trajectory Forecaster**: A specialized ONNX inference node consumes 60-minute slices of high-throughput data (from embedded QuestDB models) and predicts the subsequent 15-minute horizon for Carbon Intensity (CI) and power draw.

### 3. The Orchestration Layer: TBL Boardroom Negotiation
When the trajectory forecaster detects an impending anomaly or an emission limit breach, it fires the `TblNegotiationBoardroom`. Instead of strict procedural cut-offs, the Boardroom utilizes a Multi-Agent persona pattern:
- **Profit Agent**: Optimizes for Overall Equipment Effectiveness (OEE).
- **Planet Agent**: Constrains Carbon Intensity (CI) and Scope 1 emissions.
- **People Agent**: Minimizes alarm density and ensures cognitive safety margins.

These agents debate to satisfy the unified **Adaptive Optimization Function**:
`Utility = α(Efficiency) + β(Safety) + γ(Compliance)`

### 4. Perspective UI: The Cognitive HMI
The user-facing controls reside in the `aether-perspective-components` React application, engineered for ISA/IEC 62443 compliance.
- **Native Independent Capability**: Developed with a hyper-robust `perspective.d.ts` mock implementation, the module UI is tested entirely offline via Jest, proving flawless React resiliency.
- **Quality Code Awareness**: All interactions detect OPC-UA level quality (`Bad_Stale`, `Uncertain`), degrading controls gracefully across the factory floor.
- **Debounced Gateway RPC**: Employs deep React hooks to mitigate network flood attacks on the main Ignition gateway during setpoint shifts.
- **Pareto Front Visualization & Copilot Chat**: Operators don't just change parameters—they converse with the RAG Copilot to understand the exact regulatory Standard (GRI/SASB) justifying the AI's autonomous throttle recommendations.

## Module Structure

*   `core/`: Shared DTOs, `TblScore` models, and cryptographic data classes restricted under BSL 1.1.
*   `brain/`: The core Kotlin backend containing the Gateway Hook, MCP server, ONNX trajectory execution, and LangChain memory loops (restricted under BSL 1.1).
*   `perspective/`: Webpack/React-based frontend UI modules compiled for deployment within the Ignition Perspective rendering engine (open under Apache 2.0).

## Getting Started

### Prerequisites
*   **Java Development Kit (JDK) 17+**
*   **Node.js 18+** & NPM
*   **Ignition SDK 8.1.36+ / 8.3**
*   **Local LLM Service** (e.g., Ollama running Mistral mapped to `:11434`)

### Build Instructions
The project relies on standard Ignition gradle plugins and NPM tooling.

**1. Frontend Compilation**
```bash
cd perspective/perspective-components
npm install
npm run build 
```

**2. Backend Compilation & Assembly**
```bash
# In the AETHER root directory
./gradlew build
```
The resulting artifact `aether-module.modl` will be generated in `build/libs/` to be hot-deployed onto the Ignition Gateway.

### Testing Standard
*   **React Offline UI**: `npx jest` executes offline validations.
*   **Kotlin Gateway Service**: Execute natively via IntelliJ IDEA using embedded JUnit targets.
