package com.github.smartnose.jsonstreamfaker;

import io.javalin.Javalin;
import com.github.smartnose.jsonstreamfaker.dto.CreateStreamRequest;
import com.github.smartnose.jsonstreamfaker.dto.CreateStreamResponse;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Picocli might be used later for initial service configuration if needed
// import picocli.CommandLine;
// import picocli.CommandLine.Command;
// import picocli.CommandLine.Option;

// import java.io.File;
// import java.util.concurrent.Callable;

public class JsonStreamFaker {
    private static final Logger logger = LoggerFactory.getLogger(JsonStreamFaker.class);

    private static StreamManager streamManager;

    public static void main(String[] args) {
        streamManager = new StreamManager(); // Initialize stream manager

        Javalin app = Javalin.create(config -> {
            // Javalin configuration options can go here
            // e.g., config.jsonMapper(new JavalinJackson()); // If using Jackson for request/response mapping
            config.requestLogger.http((ctx, ms) -> {
                logger.info("{} {} took {} ms", ctx.method(), ctx.path(), ms);
            });
        }).start(8080); // Default port for the Java service

        logger.info("JSON Stream Faker service started on port 8080");

        // Define API endpoints
        app.get("/", ctx -> ctx.result("JSON Stream Faker Service is running!"));

        app.post("/streams", ctx -> {
            try {
                CreateStreamRequest request = ctx.bodyAsClass(CreateStreamRequest.class);
                // TODO: Validate the request object (e.g., ensure schemaContent is not null, outputConfig is valid)
                
                // For now, pass schemaContent and a map representation of outputConfig to StreamManager
                // StreamManager's createStream will need to be adapted to handle this.
                // We are passing outputConfig as Map<String, Object> for now, matching StreamManager's current signature.
                // A more robust solution would be for StreamManager to also use the DTOs or have a dedicated config object.
                
                // Convert OutputConfig DTO to a Map for the current StreamManager.createStream signature
                // This is a temporary step; ideally, StreamManager would accept CreateStreamRequest or its parts directly.
                java.util.Map<String, Object> outputConfigMap = new java.util.HashMap<>();
                if (request.outputConfig != null) {
                    outputConfigMap.put("type", request.outputConfig.type);
                    if ("file".equalsIgnoreCase(request.outputConfig.type)) {
                        outputConfigMap.put("filePath", request.outputConfig.filePath);
                    } else if ("kafka".equalsIgnoreCase(request.outputConfig.type) && request.outputConfig.kafka != null) {
                        outputConfigMap.put("kafka.bootstrapServers", request.outputConfig.kafka.bootstrapServers);
                        outputConfigMap.put("kafka.topic", request.outputConfig.kafka.topic);
                        if (request.outputConfig.kafka.batchSize != null) {
                             outputConfigMap.put("kafka.batchSize", request.outputConfig.kafka.batchSize);
                        }
                        if (request.outputConfig.kafka.intervalMs != null) {
                            outputConfigMap.put("kafka.intervalMs", request.outputConfig.kafka.intervalMs);
                        }
                    }
                }
                 if (request.maxMessages != null) {
                    outputConfigMap.put("maxMessages", request.maxMessages);
                }
                if (request.maxTimeInSeconds != null) {
                    outputConfigMap.put("maxTimeInSeconds", request.maxTimeInSeconds);
                }


                String streamId = streamManager.createStream(request.schemaContent, outputConfigMap);

                if (streamId != null) {
                    ctx.json(new CreateStreamResponse(streamId, "SUBMITTED", "Stream creation request submitted."));
                    ctx.status(HttpStatus.ACCEPTED);
                } else {
                    ctx.json(new CreateStreamResponse(null, "ERROR", "Failed to create stream. Check server logs."));
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } catch (Exception e) {
                logger.error("Error processing /streams request: {}", e.getMessage(), e);
                ctx.json(new CreateStreamResponse(null, "ERROR", "Error processing request: " + e.getMessage()));
                ctx.status(HttpStatus.BAD_REQUEST); // Or INTERNAL_SERVER_ERROR depending on error
            }
        });

        // TODO: Endpoint to stop a stream
        // Example: app.delete("/streams/{stream_id}", StreamController::stopStream);

        // TODO: Endpoint to get stats for a stream
        // Example: app.get("/streams/{stream_id}/stats", StreamController::getStreamStats);
        
        // TODO: Endpoint to list all streams
        // Example: app.get("/streams", StreamController::listStreams);


        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down JSON Stream Faker service...");
            if (streamManager != null) {
                streamManager.shutdown(); // Ensure all generation tasks are stopped
            }
            app.stop();
            logger.info("Service stopped.");
        }));
    }

    // The old CLI logic (call() method and @Option fields) would be removed or significantly refactored
    // if the service is purely API driven. For now, it's commented out.
    /*
    @Option(names = {"-s", "--schema"}, description = "JSON schema file path", required = true)
    private File schemaFile;
    // ... other @Option fields ...

    @Override
    public Integer call() throws Exception {
        // This logic would be triggered by an API call now, not directly on startup.
        // It would likely be part of the StreamManager or a specific stream task.
        return 0;
    }
    */
}