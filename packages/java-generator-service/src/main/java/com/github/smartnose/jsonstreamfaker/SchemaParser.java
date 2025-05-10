package com.github.smartnose.jsonstreamfaker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parses a JSON schema file with semantic tags
 */
public class SchemaParser {
    private File schemaFile; // Can be null if schemaContent is provided
    private String schemaContent; // Can be null if schemaFile is provided
    private final ObjectMapper objectMapper;

    public SchemaParser(File schemaFile) {
        this.schemaFile = schemaFile;
        this.schemaContent = null;
        this.objectMapper = new ObjectMapper();
    }

    public SchemaParser(String schemaContent) {
        this.schemaFile = null;
        this.schemaContent = schemaContent;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parses the JSON schema (from file or string) and returns a JsonSchema object
     */
    public JsonSchema parse() throws IOException {
        JsonNode rootNode;
        if (schemaFile != null) {
            rootNode = objectMapper.readTree(schemaFile);
        } else if (schemaContent != null && !schemaContent.isEmpty()) {
            rootNode = objectMapper.readTree(schemaContent);
        } else {
            throw new IllegalStateException("Schema source (file or content string) not provided or is empty.");
        }
        
        // For simplicity, we assume the root is an object
        if (!rootNode.has("type") || !rootNode.get("type").asText().equals("object")) {
            throw new IllegalArgumentException("Root schema must be of type 'object'");
        }
        
        Map<String, JsonSchema.FieldDefinition> fields = new HashMap<>();
        
        // Parse properties
        if (rootNode.has("properties")) {
            JsonNode propertiesNode = rootNode.get("properties");
            Iterator<String> fieldNames = propertiesNode.fieldNames();
            
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldNode = propertiesNode.get(fieldName);
                
                fields.put(fieldName, parseFieldDefinition(fieldNode));
            }
        }
        
        return new JsonSchema("object", fields);
    }
    
    /**
     * Recursively parses a field definition
     */
    private JsonSchema.FieldDefinition parseFieldDefinition(JsonNode fieldNode) {
        String type = fieldNode.has("type") ? fieldNode.get("type").asText() : "string";
        String semanticTag = fieldNode.has("faker") ? fieldNode.get("faker").asText() : null;
        
        Map<String, Object> constraints = new HashMap<>();
        
        // Add standard JSON Schema constraints
        if (fieldNode.has("minimum")) {
            constraints.put("minimum", fieldNode.get("minimum").asDouble());
        }
        if (fieldNode.has("maximum")) {
            constraints.put("maximum", fieldNode.get("maximum").asDouble());
        }
        if (fieldNode.has("minLength")) {
            constraints.put("minLength", fieldNode.get("minLength").asInt());
        }
        if (fieldNode.has("maxLength")) {
            constraints.put("maxLength", fieldNode.get("maxLength").asInt());
        }
        if (fieldNode.has("pattern")) {
            constraints.put("pattern", fieldNode.get("pattern").asText());
        }
        if (fieldNode.has("format")) {
            constraints.put("format", fieldNode.get("format").asText());
        }
        if (fieldNode.has("enum")) {
            List<String> enumValues = new ArrayList<>();
            for (JsonNode enumNode : fieldNode.get("enum")) {
                enumValues.add(enumNode.asText());
            }
            constraints.put("enum", enumValues);
        }

        // Handle skewed_id configuration
        if ("skewed_id".equals(semanticTag) && fieldNode.has("skewedIdConfig")) {
            JsonNode configNode = fieldNode.get("skewedIdConfig");
            if (configNode.has("distribution")) {
                constraints.put("skewedId_distribution", configNode.get("distribution").asText());
            }
            if (configNode.has("prefix")) {
                constraints.put("skewedId_prefix", configNode.get("prefix").asText());
            }
            // Distribution-specific parameters
            if (configNode.has("logNormalScale")) {
                constraints.put("skewedId_logNormal_scale", configNode.get("logNormalScale").asDouble());
            }
            if (configNode.has("logNormalShape")) { // As per plan, LogNormalDistribution in Commons Math uses scale (mean of log) and shape (std dev of log)
                constraints.put("skewedId_logNormal_shape", configNode.get("logNormalShape").asDouble());
            }
            if (configNode.has("paretoScale")) { // Location parameter for Pareto
                constraints.put("skewedId_pareto_scale", configNode.get("paretoScale").asDouble());
            }
            if (configNode.has("paretoShape")) { // Shape parameter (alpha) for Pareto
                constraints.put("skewedId_pareto_shape", configNode.get("paretoShape").asDouble());
            }
        }
        
        List<JsonSchema.FieldDefinition> items = null;
        Map<String, JsonSchema.FieldDefinition> properties = null;
        
        // Handle arrays
        if (type.equals("array") && fieldNode.has("items")) {
            items = new ArrayList<>();
            JsonNode itemsNode = fieldNode.get("items");
            
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    items.add(parseFieldDefinition(item));
                }
            } else {
                items.add(parseFieldDefinition(itemsNode));
            }
        }
        
        // Handle objects
        if (type.equals("object") && fieldNode.has("properties")) {
            properties = new HashMap<>();
            JsonNode propertiesNode = fieldNode.get("properties");
            Iterator<String> propertyNames = propertiesNode.fieldNames();
            
            while (propertyNames.hasNext()) {
                String propertyName = propertyNames.next();
                JsonNode propertyNode = propertiesNode.get(propertyName);
                
                properties.put(propertyName, parseFieldDefinition(propertyNode));
            }
        }
        
        return new JsonSchema.FieldDefinition(type, semanticTag, constraints, items, properties);
    }
}