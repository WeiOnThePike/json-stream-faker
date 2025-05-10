package com.github.smartnose.jsonstreamfaker;

import com.github.smartnose.jsonstreamfaker.dto.CreateStreamRequest; // Import the DTO
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File; // For FileDataSink
import java.io.IOException; // For DataSink operations
// import java.util.List; // Not directly used in this snippet, but might be for listStreams
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future; // To manage individual stream tasks
import java.util.concurrent.TimeUnit;

public class StreamManager {
    private static final Logger logger = LoggerFactory.getLogger(StreamManager.class);
    private final ExecutorService executorService;
    private final Map<String, StreamTaskDetails> activeStreams; // K: streamId, V: Details about the task

    // Inner class or record to hold details about each stream task
    // This would include the Future, the GenerationController (or its runnable part), DataSink etc.
    private static class StreamTaskDetails {
        Future<?> future;
        GenerationController controller; // Or a refactored Runnable task
        DataSink dataSink;
        long startTime;
        // Add other relevant details like config, schema used, current count etc.

        StreamTaskDetails(Future<?> future, GenerationController controller, DataSink dataSink) {
            this.future = future;
            this.controller = controller;
            this.dataSink = dataSink;
            this.startTime = System.currentTimeMillis();
        }

        void stopTask() {
            if (future != null && !future.isDone()) {
                future.cancel(true); // Attempt to interrupt the task
            }
            // Ensure resources like DataSink are closed by the task itself or here
            try {
                if (dataSink != null) {
                    dataSink.close();
                }
            } catch (Exception e) {
                logger.error("Error closing DataSink for stream task", e);
            }
        }
    }

    public StreamManager() {
        // Using a cached thread pool, but a fixed-size pool might be better depending on expected load
        this.executorService = Executors.newCachedThreadPool(); 
        this.activeStreams = new ConcurrentHashMap<>();
        logger.info("StreamManager initialized.");
    }

    public String createStream(CreateStreamRequest request) throws IOException {
        String streamId = java.util.UUID.randomUUID().toString();
        logger.info("Attempting to create new stream with ID: {}. Schema content length: {}", streamId, request.schemaContent != null ? request.schemaContent.length() : "null");

        if (request.schemaContent == null || request.schemaContent.trim().isEmpty()) {
            logger.error("Schema content is null or empty for stream creation request.");
            throw new IllegalArgumentException("Schema content cannot be null or empty.");
        }
        if (request.outputConfig == null) {
            logger.error("Output configuration is null for stream creation request.");
            throw new IllegalArgumentException("Output configuration cannot be null.");
        }

        try {
            // 1. Parse schemaContent into JsonSchema object using the new SchemaParser constructor
            SchemaParser schemaParser = new SchemaParser(request.schemaContent);
            JsonSchema schema = schemaParser.parse();
            FakeDataGenerator dataGenerator = new FakeDataGenerator(schema);

            // 2. Create DataSink based on request.outputConfig DTO
            DataSink dataSink;
            CreateStreamRequest.OutputConfig outConf = request.outputConfig;
            String outputType = outConf.type != null ? outConf.type.toLowerCase() : "console";

            switch (outputType) {
                case "kafka":
                    if (outConf.kafka == null) {
                        throw new IllegalArgumentException("Kafka output type selected, but kafka configuration is missing.");
                    }
                    // TODO: Create a proper File object for kafkaConfigFile if it's still needed by KafkaDataSink,
                    // or refactor KafkaDataSink to accept Properties directly or individual config strings.
                    // For now, assuming KafkaDataSink might need a properties file path, which is not ideal here.
                    // This part needs careful review of KafkaDataSink's constructor.
                    // As a placeholder, if bootstrapServers and topic are present, we can try to make it work.
                    if (outConf.kafka.bootstrapServers == null || outConf.kafka.topic == null) {
                        throw new IllegalArgumentException("Kafka bootstrapServers and topic must be specified.");
                    }
                    // This is a HACK: KafkaDataSink expects a File for config.
                    // We should refactor KafkaDataSink to take Properties or individual settings.
                    // For now, let's simulate a properties object or assume it can be null if defaults are okay.
                    // This will likely fail or use defaults if KafkaDataSink strictly needs a file.
                    File dummyKafkaConfigFile = null; // This is problematic.
                    // A better approach would be to pass Properties directly to KafkaDataSink constructor.
                    // For this example, we'll assume KafkaDataSink can be refactored or we use a default/dummy.
                    // Let's assume for now we'll use a ConsoleDataSink if Kafka setup is complex from DTO.
                    logger.warn("Kafka sink selected. KafkaDataSink needs refactoring to accept config from DTO directly. Using ConsoleDataSink as placeholder if direct init fails.");
                    // For a real implementation, you'd construct Properties from outConf.kafka
                    // and pass it to a modified KafkaDataSink constructor.
                    // Example:
                    // Properties kafkaProps = new Properties();
                    // kafkaProps.setProperty("bootstrap.servers", outConf.kafka.bootstrapServers);
                    // kafkaProps.setProperty("topic", outConf.kafka.topic);
                    // dataSink = new KafkaDataSink(kafkaProps, outConf.kafka.batchSize, outConf.kafka.intervalMs);
                    dataSink = new ConsoleDataSink(); // Placeholder until KafkaDataSink is refactored
                    logger.info("Kafka sink requested for stream {} (using Console placeholder due to config complexity)", streamId);
                    break;
                case "file":
                    if (outConf.filePath == null || outConf.filePath.trim().isEmpty()) {
                        throw new IllegalArgumentException("File output type selected, but filePath is missing.");
                    }
                    dataSink = new FileDataSink(new File(outConf.filePath));
                    logger.info("File sink configured for stream {} to path: {}", streamId, outConf.filePath);
                    break;
                case "console":
                default:
                    dataSink = new ConsoleDataSink();
                    logger.info("Console sink configured for stream {}", streamId);
                    break;
            }
            
            // 3. Create and submit GenerationController task
            GenerationController controller = new GenerationController(
                dataGenerator,
                dataSink,
                request.maxMessages,
                request.maxTimeInSeconds
            );
            
            // Wrap the controller's start method in a Runnable
            Runnable streamTask = () -> {
                try {
                    logger.info("Stream task {} started.", streamId);
                    controller.start(); // This method blocks until generation is done or interrupted
                } catch (Exception e) {
                    if (e instanceof InterruptedException) {
                        logger.info("Stream task {} was interrupted.", streamId);
                        Thread.currentThread().interrupt(); // Preserve interrupt status
                    } else {
                        logger.error("Error in stream task {}: {}", streamId, e.getMessage(), e);
                    }
                } finally {
                    logger.info("Stream task {} finished.", streamId);
                    activeStreams.remove(streamId); // Clean up when task is truly done
                    // DataSink is closed by GenerationController's finally block currently
                }
            };
            
            Future<?> future = executorService.submit(streamTask);
            activeStreams.put(streamId, new StreamTaskDetails(future, controller, dataSink));
            logger.info("Stream {} submitted for execution.", streamId);
            return streamId;

        } catch (Exception e) {
            logger.error("Failed to create stream {}: {}", streamId, e.getMessage(), e);
            // Clean up if partially created, e.g., if DataSink was made but task submission failed
            return null; // Or throw a specific exception
        }
    }

    public boolean stopStream(String streamId) {
        StreamTaskDetails taskDetails = activeStreams.get(streamId);
        if (taskDetails != null) {
            logger.info("Attempting to stop stream: {}", streamId);
            taskDetails.stopTask();
            // activeStreams.remove(streamId); // Removal should happen when task actually finishes
            logger.info("Stop signal sent to stream: {}", streamId);
            return true;
        }
        logger.warn("Stream not found for stopping: {}", streamId);
        return false;
    }

    // TODO: Implement getStreamStats(String streamId)
    // This would involve getting the messageCount from the GenerationController or StreamTaskDetails
    // public Map<String, Object> getStreamStats(String streamId) { ... }

    // TODO: Implement listStreams()
    // This would iterate over activeStreams and return summary info
    // public List<Map<String, Object>> listStreams() { ... }


    public void shutdown() {
        logger.info("Shutting down StreamManager and all active streams...");
        executorService.shutdown(); // Disable new tasks from being submitted
        for (StreamTaskDetails taskDetails : activeStreams.values()) {
            taskDetails.stopTask(); // Attempt to interrupt all running tasks
        }
        try {
            // Wait a while for existing tasks to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.error("Executor service did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("StreamManager shutdown complete.");
    }
}