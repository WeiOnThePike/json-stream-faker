package com.github.smartnose.jsonstreamfaker;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A data sink that outputs JSON objects to the console
 */
public class ConsoleDataSink implements DataSink {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleDataSink.class);
    private static final int DEFAULT_BATCH_SIZE = 10;
    private static final int DEFAULT_INTERVAL_MS = 1000;
    
    private final List<JSONObject> batch;
    private final ScheduledExecutorService scheduler;
    
    public ConsoleDataSink() {
        this.batch = new ArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule periodic batch flush
        this.scheduler.scheduleAtFixedRate(this::flushBatch, DEFAULT_INTERVAL_MS, DEFAULT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public synchronized void send(JSONObject jsonObject) throws IOException {
        batch.add(jsonObject);
        
        if (batch.size() >= DEFAULT_BATCH_SIZE) {
            flushBatch();
        }
    }
    
    private synchronized void flushBatch() {
        if (batch.isEmpty()) {
            return;
        }
        
        for (JSONObject json : batch) {
            System.out.println(json.toString(2)); // Pretty print with 2-space indentation
        }
        
        logger.info("Printed batch of {} messages to console", batch.size());
        batch.clear();
    }
    
    @Override
    public synchronized void flush() throws IOException {
        flushBatch();
    }
    
    @Override
    public void close() throws IOException {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        flush();
    }
}