package com.github.smartnose.jsonstreamfaker;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A data sink that sends JSON objects to a Kafka topic
 */
public class KafkaDataSink implements DataSink {
    private static final Logger logger = LoggerFactory.getLogger(KafkaDataSink.class);
    
    private final Producer<String, String> producer;
    private final String topic;
    private final int batchSize;
    private final int intervalMs;
    private final List<JSONObject> batch;
    private final ScheduledExecutorService scheduler;
    
    public KafkaDataSink(File kafkaConfigFile, int batchSize, int intervalMs) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(kafkaConfigFile));
        
        // Extract topic from properties or use default
        this.topic = props.getProperty("topic", "json-faker-data");
        
        // Ensure required properties are set
        if (!props.containsKey("bootstrap.servers")) {
            props.put("bootstrap.servers", "localhost:9092");
        }
        
        // Set default serializers if not specified
        if (!props.containsKey("key.serializer")) {
            props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        }
        if (!props.containsKey("value.serializer")) {
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        }
        
        this.producer = new KafkaProducer<>(props);
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
        this.batch = new ArrayList<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        // Schedule periodic batch flush
        this.scheduler.scheduleAtFixedRate(this::flushBatch, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public synchronized void send(JSONObject jsonObject) throws IOException {
        batch.add(jsonObject);
        
        if (batch.size() >= batchSize) {
            flushBatch();
        }
    }
    
    private synchronized void flushBatch() {
        if (batch.isEmpty()) {
            return;
        }
        
        try {
            for (JSONObject json : batch) {
                String key = java.util.UUID.randomUUID().toString();
                ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, json.toString());
                
                // Send synchronously for simplicity
                producer.send(record).get();
            }
            
            batch.clear();
            logger.info("Sent batch of {} messages to Kafka topic {}", batch.size(), topic);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error sending batch to Kafka", e);
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public synchronized void flush() throws IOException {
        flushBatch();
        producer.flush();
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
        producer.close();
    }
}