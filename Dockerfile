# Use Java 21 instead of Java 17 to match the compilation version
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the jar file
COPY build/libs/json-stream-faker-*.jar /app/json-stream-faker.jar

# Create a directory for schemas and config files
RUN mkdir -p /app/schemas /app/config

# Set the entrypoint to run the jar, allowing JVM options to be passed
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:-} -jar /app/json-stream-faker.jar $0 $@"]

# By default, show help
CMD ["--help"]