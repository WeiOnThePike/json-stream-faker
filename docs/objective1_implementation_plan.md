# Objective 1: LLM-Enhanced Schema Processing & Monitoring Dashboard - Implementation Plan

Date: 2025-05-10

## 1. Overall Goal

To evolve the `json-stream-faker` into a containerized system with a JavaScript frontend. This system will allow users to upload an existing JSON schema, have it automatically enhanced with semantic tags by an LLM, and then use the enhanced schema to kick off a mock data generation process. A simple dashboard will monitor the total number of events created.

## 2. Project Structure (Monorepo)

The project will be organized as a monorepo to manage all services and applications effectively.

```
/json-stream-faker/ (Monorepo Root)
|
|-- .git/
|-- .gitignore
|-- README.md                       # Overall project README
|-- docker-compose.yml              # Orchestrates all services locally
|
|-- packages/                       # Houses individual services/applications
|   |
|   |-- java-generator-service/     # Evolved Java data generator service
|   |   |-- src/
|   |   |-- build.gradle
|   |   |-- Dockerfile
|   |   |-- ...
|   |
|   |-- backend-api-service/        # NestJS backend API
|   |   |-- src/
|   |   |-- package.json
|   |   |-- Dockerfile
|   |   |-- ...
|   |
|   |-- frontend-app/               # Frontend application (React/Vue)
|   |   |-- src/
|   |   |-- package.json
|   |   |-- Dockerfile
|   |   |-- ...
|
|-- docs/
|   |-- objective1_implementation_plan.md
|   |-- ...
|
|-- memory-bank/
|   |-- ...
|
|-- examples/
|   |-- ...
```

*   **Root `docker-compose.yml`**: Will define and orchestrate the building and running of all services (`java-generator-service`, `backend-api-service`, `frontend-app`) for local development and testing.

## 3. Component Details & Tech Stack

### 3.1. Frontend Application (`packages/frontend-app/`)

*   **Purpose:**
    *   Allow users to upload their JSON schema file.
    *   (Optional) Display the LLM-enhanced schema for review.
    *   Initiate the data generation process via the backend API.
    *   Display a simple dashboard showing the total number of events generated.
*   **Tech Stack:** TypeScript with **React** or **Vue.js**.

### 3.2. Backend API Service (`packages/backend-api-service/`)

*   **Purpose:**
    *   Handle schema uploads from the frontend.
    *   Orchestrate LLM interaction for schema enhancement.
    *   Manage the Java Data Generator Service (start/stop streams, fetch stats).
    *   Provide data for the frontend monitoring dashboard.
*   **Tech Stack:** Node.js/TypeScript with the **NestJS** framework.
*   **Key API Endpoints (Conceptual):**
    *   `POST /api/v1/schemas/upload`: Upload raw schema, trigger LLM enhancement.
    *   `GET /api/v1/schemas/enhanced/{id}`: Retrieve enhanced schema.
    *   `POST /api/v1/generator/start`: Instruct Java service to start a new generation stream.
    *   `POST /api/v1/generator/stop/{generation_id}`: Instruct Java service to stop a stream.
    *   `GET /api/v1/generator/stats/{generation_id}`: Fetch stats for a stream from Java service for the dashboard.

### 3.3. Java Data Generator Service (`packages/java-generator-service/`)

*   **Purpose:**
    *   Evolve from the current `json-stream-faker` into a long-running service.
    *   Manage multiple continuous data generation streams concurrently.
    *   Expose an HTTP API for external control (by the NestJS backend) and statistics querying.
*   **Tech Stack:** Java, embedding **Javalin** for its HTTP API.
*   **Internal Enhancements:**
    *   Use Java's `ExecutorService` for managing concurrent generation tasks (streams).
    *   Each stream tracks its own event count and other relevant metrics.
*   **Key API Endpoints (Exposed by Javalin):**
    *   `POST /streams`: Accepts schema and config, starts a new internal generation stream, returns `stream_id`.
    *   `DELETE /streams/{stream_id}`: Stops a specific stream.
    *   `GET /streams`: Lists active streams.
    *   `GET /streams/{stream_id}`: Detailed status of a specific stream.
    *   `GET /streams/{stream_id}/stats`: Returns current statistics for a specific stream.

### 3.4. LLM Integration (Handled by Backend API Service)

*   **Purpose:** To automatically add semantic tags (`faker`, `format`) to user-uploaded JSON schemas to guide the Java Data Generator.
*   **Tech Stack:** **Vercel AI SDK** or direct LLM provider SDKs (e.g., `openai` for Node.js) used within the NestJS backend.
*   **Strategy:**
    *   **Few-Shot Prompting:** Provide the LLM with clear instructions and 2-3 examples of raw schema snippets and their desired enhanced counterparts.
    *   **Target Tags:**
        *   Custom: `faker` (e.g., "uuid", "firstName", "latitude", "email").
        *   Standard: `format` (e.g., "date-time").
    *   **Inference:** LLM to infer tags based on property names, JSON types, and descriptions.

### 3.5. Containerization

*   All services (frontend, backend API, Java generator service) will be containerized using **Docker**.
*   The root `docker-compose.yml` will be used for local development orchestration.

## 4. High-Level Data & Control Flow

```mermaid
graph TD
    User[User via Browser] -- 1. Upload JSON Schema --> FrontendApp(Frontend - packages/frontend-app/);
    FrontendApp -- 2. Send Schema to Backend --> BackendAPI(Backend API - NestJS - packages/backend-api-service/);
    BackendAPI -- 3. Send Schema + Few-shot Examples to LLM --> LLMService[LLM Service for Semantic Tagging];
    LLMService -- 4. Return Enhanced Schema --> BackendAPI;
    BackendAPI -- 5. Instruct Java Service to Start Stream (with Enhanced Schema) --> JavaGenerator(Java Generator Service - Javalin API - packages/java-generator-service/);
    JavaGenerator -- 6. Generate Data Stream --> DataSink[Data Sink (e.g., Kafka, Console)];
    JavaGenerator -- 7. Exposes /streams/{id}/stats --> BackendAPI;
    BackendAPI -- 8. Provide Stats to Frontend --> FrontendApp;
    FrontendApp -- 9. Display Stats on Dashboard --> User;
```

## 5. Next Steps (Post-Planning)

1.  Restructure the current project into the defined monorepo layout.
2.  Initialize the NestJS backend and frontend application projects within the `packages/` directory.
3.  Begin development of the Java Data Generator Service API using Javalin.
4.  Develop the NestJS backend API endpoints and LLM integration logic.
5.  Develop the frontend UI for schema upload and dashboard.
6.  Configure `docker-compose.yml` for local multi-service development.

This plan provides a comprehensive roadmap for implementing Objective 1.