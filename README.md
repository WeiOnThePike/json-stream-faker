# JSON Stream Faker - Evolved

**Disclaimer:** This project is undergoing a significant evolution. Please vibe-check the results, and kindly let me know if things don't work by creating a GitHub issue.

## Vision

JSON Stream Faker is evolving into a comprehensive, containerized system with a user-friendly web interface to significantly ease the creation and management of mock JSON data streams for testing and development purposes.

**Key Enhancements (Objective 1 - In Progress):**

1.  **LLM-Powered Schema Enhancement:** Users will be able to upload an existing JSON schema. The system will leverage a Large Language Model (LLM) with few-shot examples to automatically add semantic tags, making the schema ready for rich data generation.
2.  **Web UI for Control & Monitoring:** A JavaScript-based frontend will allow users to manage schema uploads, initiate data generation, and monitor the process (e.g., total events created) via a simple dashboard.
3.  **Service-Oriented Architecture:** The system is being refactored into a monorepo containing distinct services for the frontend, backend API, and the core Java data generator.

## Project Structure (Monorepo)

This project is now organized as a monorepo to manage its different services:

*   `packages/`: Contains the individual applications/services.
    *   `java-generator-service/`: The core Java application, evolved into a long-running service with its own API (using Javalin) for generating data streams based on enhanced schemas. It supports multiple concurrent streams and provides statistics.
    *   `backend-api-service/`: A Node.js/TypeScript application built with the NestJS framework. It orchestrates the LLM schema enhancement, manages the `java-generator-service` (starting/stopping streams, fetching stats), and serves data to the frontend.
    *   `frontend-app/`: A TypeScript application (React/Vue.js) providing the user interface for schema upload, LLM enhancement initiation, and a monitoring dashboard.
*   `docs/`: Contains documentation, including implementation plans.
*   `examples/`: Contains example JSON schemas and configurations.
*   `docker-compose.yml`: (To be created at the root) Will orchestrate all services for local development.

## Original Features (Now part of `java-generator-service`)

The original capabilities of JSON Stream Faker are now encapsulated within the `java-generator-service`:

*   Generate realistic fake data based on JSON Schema.
*   Supports semantic tags for various data types (names, emails, addresses, etc.).
*   Stream data to Kafka topics.
*   Output data to files or console.
*   Control generation rate and batch size.
*   Limit by number of messages or time duration.
*   Easy to run as a Docker container.

For detailed usage of the standalone Java generator, please refer to the README within the `packages/java-generator-service/` directory (once created).

## Getting Started (Monorepo - High Level)

1.  **Prerequisites:** Docker, Node.js, JDK (for Java service development).
2.  **Clone the repository:**
    ```bash
    git clone https://github.com/smartnose/json-stream-faker.git
    cd json-stream-faker
    ```
3.  **Build & Run (using Docker Compose - details to be finalized):**
    A root `docker-compose.yml` will be provided to build and run all services (`frontend-app`, `backend-api-service`, `java-generator-service`) together.
    ```bash
    docker-compose up --build
    ```
4.  Access the frontend application via your browser (details to be provided).

*(Specific build and run instructions for each package will be available in their respective README files within the `packages/` directory.)*

## JSON Schema with Semantic Tags

The application uses standard JSON Schema format with an additional `faker` property that specifies the semantic tag for a field. For example:

```json
{
  "firstName": {
    "type": "string",
    "faker": "firstName",
    "description": "Person's first name"
  }
}
```

### Supported Semantic Tags (for `java-generator-service`)

#### String Types
- `name`, `firstName`, `lastName`, `email`, `phoneNumber`, `address`, `street`, `city`, `state`, `zipCode`, `country`, `company`, `uuid`, `ipv4`, `ipv6`, `url`, `isbn`, `creditCard`

#### Number Types
- `age`, `year`, `month`, `day`, `price`, `latitude`, `longitude`, `percentage`

(Refer to `docs/objective1_implementation_plan.md` for details on LLM-based semantic tag generation).

## Examples

The `examples/` directory contains sample JSON schemas:
- `person-schema.json` - A schema for generating person data.
- `iot-sensor-schema.json` - A schema for generating IoT sensor data.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
