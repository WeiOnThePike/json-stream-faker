FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the jar file
COPY build/libs/json-stream-faker-*.jar /app/json-stream-faker.jar

# Create a directory for schemas and config files
RUN mkdir -p /app/schemas /app/config

# Set the entrypoint to run the jar
ENTRYPOINT ["java", "-jar", "/app/json-stream-faker.jar"]

# By default, show help
CMD ["--help"]