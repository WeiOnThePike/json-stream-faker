package com.github.smartnose.jsonstreamfaker;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controls the generation of fake data according to specified stopping criteria
 */
public class GenerationController {
    private static final Logger logger = LoggerFactory.getLogger(GenerationController.class);
    
    private final FakeDataGenerator generator;
    private final DataSink dataSink;
    private final Long maxMessages;
    private final Long maxTimeInSeconds;
    
    /**
     * Creates a new GenerationController
     * 
     * @param generator The fake data generator
     * @param dataSink The data sink to send generated data to
     * @param maxMessages Maximum number of messages to generate, or null for unlimited
     * @param maxTimeInSeconds Maximum time to generate for in seconds, or null for unlimited
     */
    public GenerationController(FakeDataGenerator generator, DataSink dataSink, Long maxMessages, Long maxTimeInSeconds) {
        this.generator = generator;
        this.dataSink = dataSink;
        this.maxMessages = maxMessages;
        this.maxTimeInSeconds = maxTimeInSeconds;
    }
    
    /**
     * Starts the data generation process
     * 
     * @throws IOException If an I/O error occurs
     */
    public void start() throws IOException {
        try {
            // Set up stopping criteria
            final long startTimeMs = System.currentTimeMillis();
            final AtomicLong messageCount = new AtomicLong(0);
            
            logger.info("Starting data generation.");
            if (maxMessages != null) {
                logger.info("Will generate up to {} messages", maxMessages);
            }
            if (maxTimeInSeconds != null) {
                logger.info("Will generate for up to {} seconds", maxTimeInSeconds);
            }
            if (maxMessages == null && maxTimeInSeconds == null) {
                logger.info("Running in unlimited mode - will generate messages indefinitely until interrupted");
            }
            
            boolean shouldContinue = true;
            
            while (shouldContinue) {
                // Check message count limit
                if (maxMessages != null && messageCount.get() >= maxMessages) {
                    logger.info("Reached maximum message count of {}", maxMessages);
                    break;
                }
                
                // Check time limit
                if (maxTimeInSeconds != null) {
                    long elapsedTimeSeconds = (System.currentTimeMillis() - startTimeMs) / 1000;
                    if (elapsedTimeSeconds >= maxTimeInSeconds) {
                        logger.info("Reached maximum time of {} seconds", maxTimeInSeconds);
                        break;
                    }
                }
                
                // Generate and send a message
                JSONObject jsonObject = generator.generateObject();
                dataSink.send(jsonObject);
                
                long count = messageCount.incrementAndGet();
                if (count % 1000 == 0) {
                    logger.info("Generated {} messages", count);
                }
                
                // Small pause to avoid overwhelming the CPU
                if (count % 100 == 0) {
                    Thread.yield();
                }
                
                // Check for interruption
                if (Thread.currentThread().isInterrupted()) {
                    logger.info("Generation interrupted");
                    break;
                }
            }
            
            logger.info("Generation complete. Generated {} messages", messageCount.get());
        } finally {
            dataSink.flush();
            dataSink.close();
        }
    }
}