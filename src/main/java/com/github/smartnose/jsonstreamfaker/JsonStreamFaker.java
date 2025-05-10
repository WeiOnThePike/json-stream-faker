package com.github.smartnose.jsonstreamfaker;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "json-stream-faker", mixinStandardHelpOptions = true, 
        description = "Generates fake data according to a JSON schema with semantic tags and sends it to Kafka")
public class JsonStreamFaker implements Callable<Integer> {

    @Option(names = {"-s", "--schema"}, description = "JSON schema file path", required = true)
    private File schemaFile;

    @Option(names = {"-kc", "--kafka-config"}, description = "Kafka client configuration file path")
    private File kafkaConfigFile;

    @Option(names = {"-n", "--max-messages"}, description = "Maximum number of messages to generate")
    private Long maxMessages;

    @Option(names = {"-t", "--max-time"}, description = "Maximum time to run in seconds")
    private Long maxTimeInSeconds;

    @Option(names = {"-b", "--batch-size"}, description = "Batch size for Kafka messages", defaultValue = "100")
    private int batchSize;

    @Option(names = {"-i", "--interval"}, description = "Interval between batches in milliseconds", defaultValue = "1000")
    private int intervalMs;

    @Option(names = {"-o", "--output"}, description = "Output file path (if not sending to Kafka)")
    private File outputFile;

    @Option(names = {"--auto-create-topic"}, description = "Automatically create Kafka topic if it doesn't exist", defaultValue = "true")
    private boolean autoCreateTopic;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new JsonStreamFaker()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        try {
            // Parse the JSON schema
            SchemaParser schemaParser = new SchemaParser(schemaFile);
            JsonSchema schema = schemaParser.parse();
            
            // Create the data generator
            FakeDataGenerator generator = new FakeDataGenerator(schema);
            
            // Create the data sink (Kafka or file)
            DataSink dataSink;
            if (kafkaConfigFile != null) {
                dataSink = new KafkaDataSink(kafkaConfigFile, batchSize, intervalMs, autoCreateTopic);
            } else if (outputFile != null) {
                dataSink = new FileDataSink(outputFile);
            } else {
                dataSink = new ConsoleDataSink();
            }
            
            // Create the generation controller
            GenerationController controller = new GenerationController(
                    generator, 
                    dataSink, 
                    maxMessages, 
                    maxTimeInSeconds
            );
            
            // Start the generation
            controller.start();
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}