package com.github.smartnose.jsonstreamfaker;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A data sink that writes JSON objects to a file
 */
public class FileDataSink implements DataSink {
    private static final Logger logger = LoggerFactory.getLogger(FileDataSink.class);
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_INTERVAL_MS = 1000;
    
    private final BufferedWriter writer;
    private final List<JSONObject> batch;
    private final ScheduledExecutorService scheduler;
    
    public FileDataSink(File outputFile) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(outputFile));
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
        
        try {
            for (JSONObject json : batch) {
                writer.write(json.toString());
                writer.newLine();
            }
            
            writer.flush();
            logger.info("Wrote batch of {} messages to file", batch.size());
            batch.clear();
        } catch (IOException e) {
            logger.error("Error writing batch to file", e);
        }
    }
    
    @Override
    public synchronized void flush() throws IOException {
        flushBatch();
        writer.flush();
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
        writer.close();
    }
}