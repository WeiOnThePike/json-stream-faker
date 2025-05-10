package com.github.smartnose.jsonstreamfaker;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
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
    private final boolean autoCreateTopic;
    private final short replicationFactor;
    private final int numPartitions;
    
    /**
     * Creates a Kafka data sink with automatic topic creation enabled
     */
    public KafkaDataSink(File kafkaConfigFile, int batchSize, int intervalMs) throws IOException {
        this(kafkaConfigFile, batchSize, intervalMs, true);
    }
    
    /**
     * Creates a Kafka data sink with configurable topic creation behavior
     *
     * @param kafkaConfigFile The Kafka configuration file
     * @param batchSize The number of messages to batch before sending
     * @param intervalMs The maximum interval between batch sends
     * @param autoCreateTopic Whether to automatically create the topic if it doesn't exist
     * @throws IOException If there is an error reading the config file or connecting to Kafka
     */
    public KafkaDataSink(File kafkaConfigFile, int batchSize, int intervalMs, boolean autoCreateTopic) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(kafkaConfigFile));
        
        // Extract topic from properties or use default
        this.topic = props.getProperty("topic", "json-faker-data");
        this.autoCreateTopic = autoCreateTopic;
        
        // Get topic configuration if specified
        this.replicationFactor = Short.parseShort(props.getProperty("topic.replication.factor", "1"));
        this.numPartitions = Integer.parseInt(props.getProperty("topic.num.partitions", "1"));
        
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
        
        // Check if topic exists and create it if needed
        if (!topicExists(props, topic)) {
            if (autoCreateTopic) {
                createTopic(props, topic, numPartitions, replicationFactor);
                logger.info("Created Kafka topic: {}", topic);
            } else {
                throw new IOException("Kafka topic '" + topic + "' does not exist and auto-creation is disabled");
            }
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
            
            logger.info("Sent batch of {} messages to Kafka topic {}", batch.size(), topic);
            batch.clear();
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
    
    private boolean topicExists(Properties props, String topic) throws IOException {
        try (AdminClient adminClient = AdminClient.create(props)) {
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> names = topics.names().get();
            return names.contains(topic);
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error checking if topic exists", e);
        }
    }
    
    private void createTopic(Properties props, String topic, int numPartitions, short replicationFactor) throws IOException {
        try (AdminClient adminClient = AdminClient.create(props)) {
            NewTopic newTopic = new NewTopic(topic, numPartitions, replicationFactor);
            CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
            result.all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException("Error creating topic", e);
        }
    }
}