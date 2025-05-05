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

### Command Line Options

```
Usage: json-stream-faker [-hV] [-b=<batchSize>] [-i=<intervalMs>]
                         [-kc=<kafkaConfigFile>] [-n=<maxMessages>]
                         [-o=<outputFile>] -s=<schemaFile> [-t=<maxTimeInSeconds>]
Generates fake data according to a JSON schema with semantic tags and sends it to Kafka
  -b, --batch-size=<batchSize>
                          Batch size for Kafka messages
  -h, --help              Show this help message and exit.
  -i, --interval=<intervalMs>
                          Interval between batches in milliseconds
  -kc, --kafka-config=<kafkaConfigFile>
                          Kafka client configuration file path
  -n, --max-messages=<maxMessages>
                          Maximum number of messages to generate
  -o, --output=<outputFile>
                          Output file path (if not sending to Kafka)
  -s, --schema=<schemaFile>
                          JSON schema file path
  -t, --max-time=<maxTimeInSeconds>
                          Maximum time to run in seconds
  -V, --version           Print version information and exit.
```

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
