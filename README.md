# JSON Stream Faker

A containerized Java application for generating fake data according to JSON schemas with semantic tags. The tool can output data to Kafka, files, or the console.

## Features

- Generate realistic fake data based on JSON Schema
- Supports semantic tags for various data types (names, emails, addresses, etc.)
- Stream data to Kafka topics
- Output data to files or console
- Control generation rate and batch size
- Limit by number of messages or time duration
- Easy to run as a Docker container

## Usage

### Basic Usage

```bash
# Run with console output
java -jar json-stream-faker.jar -s examples/person-schema.json

# Run with file output
java -jar json-stream-faker.jar -s examples/person-schema.json -o output.json

# Run with Kafka output
java -jar json-stream-faker.jar -s examples/person-schema.json -kc examples/kafka-config.properties
```

### Docker Usage

```bash
# Build the Docker image
docker build -t jsonstreamfaker:latest .

# Run with console output
docker run -v $(pwd)/examples:/app/examples jsonstreamfaker:latest -s /app/examples/person-schema.json

# Run with Kafka output (assuming Kafka is accessible)
docker run -v $(pwd)/examples:/app/examples jsonstreamfaker:latest -s /app/examples/person-schema.json -kc /app/examples/kafka-config.properties
```

### Docker Compose Usage

You can also use JSON Stream Faker with docker-compose to integrate it into a larger application stack, especially when working with Kafka, databases, or other services.

Here's a sample `docker-compose.yml` file that sets up JSON Stream Faker alongside Kafka and Zookeeper:

```yaml
version: '3'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:latest
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:latest
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  json-stream-faker:
    image: jsonstreamfaker:latest
    depends_on:
      - kafka
    volumes:
      - ./examples:/app/examples
    command: -s /app/examples/person-schema.json -kc /app/examples/kafka-config.properties -n 1000 -b 100 -i 1000
    environment:
      # You can override settings with environment variables if needed
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
```

To use this configuration:

1. Make sure you've built the JSON Stream Faker Docker image first:
```bash
./build.sh
```

2. Create a `docker-compose.yml` file with the above content

3. Update the Kafka configuration file to use the internal Kafka service name:
```properties
# In examples/kafka-config.properties
bootstrap.servers=kafka:29092
```

4. Run the stack:
```bash
docker-compose up
```

You can customize the command parameters and environment variables as needed. This setup will generate 1000 fake person records and send them to Kafka.

## Publishing Docker Images

The project includes a script for publishing the Docker image to either a local or remote repository.

### Local Registry

To publish to a local Docker registry:

```bash
# Make sure the script is executable
chmod +x publish-docker.sh

# Publish to a local registry (will start one if not running)
./publish-docker.sh --local
```

This will:
1. Build the image if it doesn't exist
2. Start a local registry on port 5000 if one isn't running
3. Tag the image as `localhost:5000/jsonstreamfaker:latest`
4. Push it to the local registry

### Remote Registry (Docker Hub or other)

To publish to Docker Hub:

```bash
# Publish to Docker Hub under your username
./publish-docker.sh --repository username/jsonstreamfaker --username yourusername
```

For other registries:

```bash
# Publish to a custom registry
./publish-docker.sh --registry registry.example.com --repository jsonstreamfaker --username yourusername
```

### All Options

```
Options:
  --version VERSION   Specify the version tag (default: latest)
  --registry URL      Specify the registry URL (default: none for Docker Hub)
  --repository REPO   Specify the repository name (default: jsonstreamfaker)
  --username USER     Specify the registry username
  --password PASS     Specify the registry password (use env var for security)
  --local             Push to local registry (defaults to localhost:5000)
  --help              Show this help message
```

You can also set the `DOCKER_PASSWORD` environment variable for secure authentication.

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

### Supported Semantic Tags

#### String Types
- `name` - Full name
- `firstName` - First name
- `lastName` - Last name
- `email` - Email address
- `phoneNumber` - Phone number
- `address` - Full address
- `street` - Street address
- `city` - City name
- `state` - State or province
- `zipCode` - Postal code
- `country` - Country name
- `company` - Company name
- `uuid` - UUID
- `ipv4` - IPv4 address
- `ipv6` - IPv6 address
- `url` - URL
- `isbn` - ISBN
- `creditCard` - Credit card number

#### Number Types
- `age` - Age (integer)
- `year` - Year (integer)
- `month` - Month (1-12)
- `day` - Day (1-31)
- `price` - Price
- `latitude` - Latitude coordinate
- `longitude` - Longitude coordinate
- `percentage` - Percentage value (0-100)

## Examples

The `examples/` directory contains sample JSON schemas:

- `person-schema.json` - A schema for generating person data
- `iot-sensor-schema.json` - A schema for generating IoT sensor data

## Building from Source

```bash
# Clone the repository
git clone https://github.com/smartnose/json-stream-faker.git
cd json-stream-faker

# Make the build script executable
chmod +x build.sh

# Build the application and Docker image
./build.sh
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
