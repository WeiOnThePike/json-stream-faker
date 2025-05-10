package com.github.smartnose.jsonstreamfaker.dto;

import java.util.Map;

// This class can be a record if using Java 14+ and you prefer records for DTOs.
// For broader compatibility, a class with getters/setters or public fields is fine.
// Javalin's default Jackson mapper can handle public fields or getters/setters.
public class CreateStreamRequest {
    public String schemaContent; // The JSON schema as a string
    public OutputConfig outputConfig;
    public Long maxMessages; // Optional: Maximum number of messages to generate
    public Long maxTimeInSeconds; // Optional: Maximum time to generate for in seconds
    // public Integer ratePerSecond; // Optional: Desired generation rate

    // Inner class for output configuration
    public static class OutputConfig {
        public String type; // "console", "file", "kafka"
        public String filePath; // For "file" type
        public KafkaConfig kafka; // For "kafka" type
        // Add other common sink params like batchSize, intervalMs if they are per-stream
    }

    // Inner class for Kafka-specific configuration
    public static class KafkaConfig {
        public String bootstrapServers; // e.g., "localhost:9092"
        public String topic;
        public Integer batchSize;
        public Integer intervalMs;
        // You can add other Kafka producer properties here if needed
        // public Map<String, String> additionalProperties; 
    }

    // Default constructor for JSON deserialization (e.g., by Jackson)
    public CreateStreamRequest() {}

    // Getters and Setters can be added if you prefer private fields, 
    // but public fields are often simpler for DTOs with Jackson.
}