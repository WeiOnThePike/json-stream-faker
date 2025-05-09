# Decision Log

This file records architectural and implementation decisions using a list format.
2025-05-10 08:34:03 - Log of updates made.
2025-05-10 08:48:04 - Initial architectural and tech stack planning for Objective 1 (LLM-enhanced schema processing and monitoring dashboard).
2025-05-10 08:52:38 - Refined LLM semantic tagging strategy and few-shot example structure for Objective 1.
2025-05-10 08:59:26 - Confirmed backend tech stack for Objective 1: Node.js/TypeScript with NestJS framework.
2025-05-10 09:06:31 - Detailed plan for Java generator to evolve into a service using Javalin for its API, enabling multiple continuous streams and queryable stats.
2025-05-10 09:08:47 - Decided to adopt a monorepo structure for the project.

*

## Decision

*   **Architecture for Objective 1:** Adopt a multi-component system:
    *   Frontend (JavaScript SPA: React/Vue) for schema upload and dashboard.
    *   Backend API Service (Node.js/TypeScript with NestJS) for orchestration, LLM interaction, and managing the Java generator.
    *   LLM Integration Service (using managed APIs like OpenAI/Gemini) for semantic tagging.
    *   Java Data Generator: To be evolved into a long-running service application, containerized, embedding Javalin for an internal HTTP API.
    *   Optional schema storage.
*   **Project Structure (Monorepo):**
    *   The entire project will be organized as a monorepo.
    *   A root `packages/` directory will house individual services:
        *   `packages/java-generator-service/` (Evolved Java data generator)
        *   `packages/backend-api-service/` (NestJS backend)
        *   `packages/frontend-app/` (React/Vue frontend)
    *   Root `docker-compose.yml` for multi-service orchestration.
    *   Root `README.md` and `.gitignore` to be updated for the monorepo.
*   **Tech Stack for Objective 1:**
    *   Frontend: TypeScript with React or Vue.js.
    *   Backend API: Node.js/TypeScript with NestJS framework.
    *   LLM Integration: Vercel AI SDK or direct LLM provider SDKs (e.g., `openai` for Node.js).
    *   Interaction with Java Data Generator Service:
        *   The NestJS backend will manage one running instance of the Java Data Generator service (container).
        *   NestJS backend will make HTTP API calls to the Java generator's Javalin-powered API (e.g., `POST /streams` to start a new generation stream with a schema, `DELETE /streams/{stream_id}` to stop, `GET /streams/{stream_id}/stats` to get stats).
    *   Containerization: Docker for all new components; `docker-compose` for local orchestration.

## Rationale 

*   **Monorepo Rationale:**
    *   **Atomic Commits:** Changes across multiple services can be versioned together.
    *   **Simplified Dependency Management:** Easier to manage versions and dependencies, especially with optional monorepo tools (e.g., Nx, Turborepo).
    *   **Code Sharing:** Facilitates sharing of common code/types (e.g., between TypeScript backend and frontend).
    *   **Consistent Tooling:** Simplifies enforcement of consistent linting, formatting, and testing practices.
*   **Service Architecture Rationale:**
    *   Separation of concerns: Frontend for UI, Backend for logic, LLM for specialized AI task, existing generator for its core function.
    *   Scalability: Components can be scaled independently.
    *   Flexibility: Allows using the best tools for each part (e.g., Python for LLM, JS for UI).
*   **Tech Stack Rationale:**
    *   Leverages user's existing skills in Java and TypeScript/JavaScript, creating a unified JS/TS stack for frontend and backend.
    *   NestJS provides a structured, modular, and scalable architecture for the backend, with strong TypeScript support.
    *   Managed LLM services reduce operational overhead.
    *   Docker and `docker-compose` for consistent development and deployment environments.

## Implementation Details

*   **Frontend:** Implement file upload, (optional) enhanced schema display, generation initiation button, and a simple dashboard for event counts.
*   **Backend API:**
    *   Endpoints for schema upload, triggering LLM enhancement, starting/stopping the Java generator, and serving dashboard statistics.
    *   Logic for prompt engineering and few-shot example provision to the LLM.
    *   Mechanism to pass the enhanced schema to the Java generator.
    *   Mechanism to receive/retrieve event counts from the Java generator.
*   **Java Data Generator Service Enhancements:**
    *   **Transform into Long-Running Service:** Refactor to run continuously.
    *   **Embed HTTP Server (Javalin):** Add Javalin dependency and implement an HTTP API.
    *   **API for Stream Management & Stats:**
        *   `POST /streams`: Accepts schema and config, starts a new internal generation task (stream), returns `stream_id`.
        *   `DELETE /streams/{stream_id}`: Stops a specific stream.
        *   `GET /streams`: Lists active streams.
        *   `GET /streams/{stream_id}`: Detailed status of a stream.
        *   `GET /streams/{stream_id}/stats`: Returns current stats for a specific stream.
        *   `(Optional) GET /stats`: Aggregate stats for the generator service.
    *   **Internal Concurrency:** Use `ExecutorService` to manage multiple concurrent `FakeDataGenerator` instances (streams).
    *   **Continuous Generation & Stats Tracking:** Each stream runs continuously per its config and tracks its own stats.
*   **LLM Integration & Semantic Tagging Strategy:**
    *   **Identified Semantic Tags:**
        *   Custom tag: `faker` (e.g., "uuid", "firstName", "latitude", "percentage", "email", "age", "street", "city", "state", "zipCode", "country", "phoneNumber", "company"). This is the primary tag for specifying fake data types.
        *   Standard tag: `format` (e.g., "date-time").
    *   **LLM Prompt Engineering:**
        *   Provide clear instructions to the LLM on its role: to analyze a raw JSON schema and add `faker` and `format` tags.
        *   Instruct the LLM to infer tag values based on property names, JSON types, and `description` fields.
    *   **Few-Shot Examples:**
        *   Provide 2-3 diverse examples showing input (raw schema snippet) and output (enhanced schema snippet with `faker` and `format` tags).
        *   Examples should cover different domains (e.g., person data, IoT data) to help the LLM generalize.
        *   Example structure:
            ```
            --- Example N ---
            Input Schema Snippet (Raw):
            { ... properties without 'faker'/'format' ... }

            Output Enhanced Schema Snippet (with 'faker' and 'format' tags):
            { ... properties with 'faker'/'format' added intelligently ... }
            ```
    *   **LLM Input:** The prompt will include the instructions, few-shot examples, and then the user-uploaded JSON schema for enhancement.
    *   **Goal:** Train the LLM to accurately identify fields needing semantic tags and apply the correct tags in the format expected by the Java data generator.