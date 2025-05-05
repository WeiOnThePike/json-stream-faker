#!/bin/bash
set -e

echo "Building JSON Stream Faker..."

# Check if we have gradle installed
if command -v gradle &> /dev/null; then
    echo "Building with local Gradle installation"
    gradle clean shadowJar
elif [ -f "./gradlew" ]; then
    echo "Building with Gradle wrapper"
    ./gradlew clean shadowJar
else
    echo "Gradle not found and no wrapper available."
    echo "Please install Gradle or run this in an environment with Gradle available."
    exit 1
fi

# Check if we have Docker installed
if command -v docker &> /dev/null; then
    echo "Building Docker image..."
    docker build -t jsonstreamfaker:latest .
    
    echo "Build complete! You can run the container with:"
    echo "docker run -v \$(pwd)/examples:/app/examples jsonstreamfaker:latest --schema /app/examples/person-schema.json"
else
    echo "Docker not found. Skipping Docker image build."
    echo "To run the application directly:"
    echo "java -jar build/libs/json-stream-faker-1.0-SNAPSHOT.jar --schema examples/person-schema.json"
fi