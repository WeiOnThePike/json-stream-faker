package com.github.smartnose.jsonstreamfaker;

import net.datafaker.Faker;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.ParetoDistribution;
// Consider if a specific RNG is needed for distributions, otherwise, they use their own.
// import org.apache.commons.math3.random.RandomGenerator;
// import org.apache.commons.math3.random.Well19937c;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates fake data according to a JSON schema with semantic tags
 */
public class FakeDataGenerator {
    private final JsonSchema schema;
    private final Faker faker;
    private final Random random;

    public FakeDataGenerator(JsonSchema schema) {
        this.schema = schema;
        this.faker = new Faker();
        this.random = new Random();
    }

    /**
     * Generates a single JSON object according to the schema
     */
    public JSONObject generateObject() {
        if (!"object".equals(schema.getRootType())) {
            throw new IllegalStateException("Root schema must be of type 'object'");
        }
        
        return generateObjectFromFields(schema.getFields());
    }
    
    private JSONObject generateObjectFromFields(Map<String, JsonSchema.FieldDefinition> fields) {
        JSONObject json = new JSONObject();
        
        for (Map.Entry<String, JsonSchema.FieldDefinition> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            JsonSchema.FieldDefinition fieldDef = entry.getValue();
            
            Object value = generateValue(fieldDef);
            json.put(fieldName, value);
        }
        
        return json;
    }
    
    private Object generateValue(JsonSchema.FieldDefinition fieldDef) {
        String type = fieldDef.getType();
        String semanticTag = fieldDef.getSemanticTag();
        Map<String, Object> constraints = fieldDef.getConstraints();
        
        switch (type) {
            case "string":
                return generateString(semanticTag, constraints);
            case "integer":
                return generateInteger(semanticTag, constraints);
            case "number":
                return generateNumber(semanticTag, constraints);
            case "boolean":
                return generateBoolean(semanticTag);
            case "array":
                return generateArray(fieldDef);
            case "object":
                return generateObject(fieldDef);
            case "null":
                return JSONObject.NULL;
            default:
                throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
    
    private String generateString(String semanticTag, Map<String, Object> constraints) {
        if (semanticTag != null) {
            // Pass constraints to generateSemanticString
            return generateSemanticString(semanticTag, constraints);
        }
        
        // Handle standard string constraints
        if (constraints.containsKey("enum")) {
            List<String> enumValues = (List<String>) constraints.get("enum");
            return enumValues.get(random.nextInt(enumValues.size()));
        }
        
        int minLength = (Integer) constraints.getOrDefault("minLength", 5);
        int maxLength = (Integer) constraints.getOrDefault("maxLength", 10);
        
        return faker.lorem().characters(minLength, maxLength);
    }
    
    // Modified to accept constraints for skewed_id
    private String generateSemanticString(String semanticTag, Map<String, Object> constraints) {
        // Map semantic tags to datafaker methods
        switch (semanticTag) {
            case "skewed_id":
                return generateSkewedIdValue(constraints);
            case "name":
                return faker.name().fullName();
            case "firstName":
                return faker.name().firstName();
            case "lastName":
                return faker.name().lastName();
            case "email":
                return faker.internet().emailAddress();
            case "phoneNumber":
                return faker.phoneNumber().phoneNumber();
            case "address":
                return faker.address().fullAddress();
            case "street":
                return faker.address().streetAddress();
            case "city":
                return faker.address().city();
            case "state":
                return faker.address().state();
            case "zipCode":
                return faker.address().zipCode();
            case "country":
                return faker.address().country();
            case "company":
                return faker.company().name();
            case "uuid":
                return faker.internet().uuid();
            case "ipv4":
                return faker.internet().ipV4Address();
            case "ipv6":
                return faker.internet().ipV6Address();
            case "url":
                return faker.internet().url();
            case "isbn":
                return faker.code().isbn13();
            case "creditCard":
                return faker.finance().creditCard();
            default:
                // For unrecognized tags, fall back to lorem
                return faker.lorem().sentence();
        }
    }
    
    private Number generateInteger(String semanticTag, Map<String, Object> constraints) {
        if (semanticTag != null) {
            return generateSemanticInteger(semanticTag);
        }
        
        double min = (Double) constraints.getOrDefault("minimum", Integer.MIN_VALUE);
        double max = (Double) constraints.getOrDefault("maximum", Integer.MAX_VALUE);
        
        // Ensure we're within Integer bounds
        int minInt = Math.max((int) min, Integer.MIN_VALUE);
        int maxInt = Math.min((int) max, Integer.MAX_VALUE);
        
        // Adjust bounds if needed
        if (maxInt <= minInt) {
            maxInt = minInt + 100;
        }
        
        return faker.number().numberBetween(minInt, maxInt);
    }
    
    private Number generateSemanticInteger(String semanticTag) {
        switch (semanticTag) {
            case "age":
                return faker.number().numberBetween(1, 100);
            case "year":
                return faker.number().numberBetween(1900, 2023);
            case "month":
                return faker.number().numberBetween(1, 12);
            case "day":
                return faker.number().numberBetween(1, 31);
            case "price":
                return faker.number().randomNumber(5, true);
            default:
                return faker.number().randomNumber();
        }
    }
    
    private Number generateNumber(String semanticTag, Map<String, Object> constraints) {
        if (semanticTag != null) {
            return generateSemanticNumber(semanticTag);
        }
        
        double min = (Double) constraints.getOrDefault("minimum", Double.MIN_VALUE);
        double max = (Double) constraints.getOrDefault("maximum", Double.MAX_VALUE);
        
        // Adjust bounds if needed
        if (max <= min) {
            max = min + 100.0;
        }
        
        return faker.number().randomDouble(2, (long) min, (long) max);
    }
    
    private Number generateSemanticNumber(String semanticTag) {
        return switch (semanticTag) {
            case "latitude" -> Float.parseFloat(faker.address().latitude());
            case "longitude" -> Float.parseFloat(faker.address().longitude());
            case "percentage" -> faker.number().randomDouble(2, 0, 100);
            default -> faker.number().randomDouble(2, 0, 1000);
        };
    }
    
    private Boolean generateBoolean(String semanticTag) {
        return faker.bool().bool();
    }
    
    private JSONArray generateArray(JsonSchema.FieldDefinition fieldDef) {
        List<JsonSchema.FieldDefinition> items = fieldDef.getItems();
        
        if (items == null || items.isEmpty()) {
            return new JSONArray();
        }
        
        // For simplicity, we'll use the first item definition for all items
        JsonSchema.FieldDefinition itemDef = items.get(0);
        
        JSONArray array = new JSONArray();
        int count = random.nextInt(5) + 1; // Generate 1-5 items
        
        for (int i = 0; i < count; i++) {
            array.put(generateValue(itemDef));
        }
        
        return array;
    }
    
    private JSONObject generateObject(JsonSchema.FieldDefinition fieldDef) {
        Map<String, JsonSchema.FieldDefinition> properties = fieldDef.getProperties();
        
        if (properties == null) {
            return new JSONObject();
        }
        
        return generateObjectFromFields(properties);
    }

    private String generateSkewedIdValue(Map<String, Object> constraints) {
        String distributionType = (String) constraints.get("skewedId_distribution");
        String prefix = (String) constraints.getOrDefault("skewedId_prefix", "");

        if (distributionType == null) {
            System.err.println("Warning: 'skewedId_distribution' not specified for skewed_id. Falling back to random number.");
            return prefix + faker.number().randomNumber(7, false); // Generate a positive long
        }

        long number;
        // Note: The first argument to distribution constructors is a RandomGenerator.
        // Passing null makes them create their own (typically Well19937c).
        // If consistent seeding across runs for distributions is needed, a shared RandomGenerator instance would be required.
        try {
            switch (distributionType.toLowerCase()) {
                case "log-normal":
                    double scale = (double) constraints.getOrDefault("skewedId_logNormal_scale", 1.0); // Corresponds to 'mu' or mean of the log
                    double shape = (double) constraints.getOrDefault("skewedId_logNormal_shape", 0.5); // Corresponds to 'sigma' or std dev of the log
                    if (shape <= 0) {
                        System.err.println("Warning: 'skewedId_logNormal_shape' (sigma) must be positive for LogNormalDistribution. Using default 0.5.");
                        shape = 0.5;
                    }
                    LogNormalDistribution logNormal = new LogNormalDistribution(null, scale, shape);
                    number = Math.max(1, (long) logNormal.sample());
                    break;
                case "pareto":
                    double paretoScale = (double) constraints.getOrDefault("skewedId_pareto_scale", 1.0); // Location parameter 'xm'
                    double paretoShape = (double) constraints.getOrDefault("skewedId_pareto_shape", 1.16); // Shape parameter 'alpha'
                    if (paretoScale <= 0 || paretoShape <= 0) {
                        System.err.println("Warning: 'skewedId_pareto_scale' (xm) and 'skewedId_pareto_shape' (alpha) must be positive for ParetoDistribution. Using defaults.");
                        paretoScale = 1.0;
                        paretoShape = 1.16;
                    }
                    ParetoDistribution pareto = new ParetoDistribution(null, paretoScale, paretoShape);
                    number = Math.max(1, (long) pareto.sample()); // Pareto samples are >= scale, ensure it's at least 1 if scale is < 1.
                    break;
                default:
                    System.err.println("Warning: Unknown distribution type for skewed_id: " + distributionType + ". Falling back to random number.");
                    number = faker.number().randomNumber(7, false); // Generate a positive long
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error generating skewed_id for distribution '" + distributionType + "': " + e.getMessage() + ". Falling back to random number.");
            e.printStackTrace(); // For more detailed debugging
            number = faker.number().randomNumber(7, false);
        }
        return prefix + number;
    }
}