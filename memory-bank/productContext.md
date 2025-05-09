# Product Context

This file provides a high-level overview of the project and the expected product that will be created. Initially it is based upon projectBrief.md (if provided) and all other available project-related information in the working directory. This file is intended to be updated as the project evolves, and should be used to inform all other modes of the project's goals and context.
2025-05-10 08:33:28 - Log of updates made will be appended as footnotes to the end of this file.
2025-05-10 08:46:49 - Updated with initial plan for new feature: LLM-enhanced schema processing and monitoring dashboard.
2025-05-10 09:08:33 - Decision to adopt a monorepo structure for all services. Updated tech stack details.

*

## Project Goal

*   Evolve the current `json-stream-faker` into a containerized system with a JavaScript frontend to further ease the work of creating mock JSON streams for testing purposes. Initially, focus on enabling users to upload existing schemas for LLM-powered semantic enhancement and generation monitoring.

## Key Features

*   User can upload an existing JSON schema definition.
*   System uses an LLM with few-shot examples to automatically add semantic tags, enhancing the schema for the existing Java generator.
*   System kicks off the mock data generation process using the enhanced schema.
*   A simple dashboard displays the total number of events created.

## Overall Architecture

*   **Project Structure:** Monorepo under the root `json-stream-faker/` directory, with services in a `packages/` subdirectory.
*   **Frontend (`packages/frontend-app/`):** TypeScript with React or Vue.js for schema upload and a simple monitoring dashboard.
*   **Backend API Service (`packages/backend-api-service/`):** Node.js/TypeScript with NestJS framework to:
    *   Handle schema uploads.
    *   Interface with an LLM service for schema enhancement.
    *   Orchestrate the Java Data Generator service (start/stop streams, fetch stats).
    *   Provide data for the monitoring dashboard.
*   **Java Data Generator Service (`packages/java-generator-service/`):** The existing Java `json-stream-faker` program, evolved into a long-running service embedding Javalin to expose an HTTP API for managing multiple continuous data generation streams and reporting their statistics.
*   **LLM Integration:** Vercel AI SDK or direct LLM provider SDKs (e.g., `openai` for Node.js) used by the NestJS backend for semantic tag generation.
*   **Containerization:** All services (frontend, backend, Java generator) will be containerized using Docker, with a root `docker-compose.yml` for local orchestration.