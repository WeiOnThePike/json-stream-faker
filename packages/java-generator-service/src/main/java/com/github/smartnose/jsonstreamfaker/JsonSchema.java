package com.github.smartnose.jsonstreamfaker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a JSON schema with semantic tags for data generation
 */
public class JsonSchema {
    private final Map<String, FieldDefinition> fields;
    private final String rootType;

    public JsonSchema(String rootType, Map<String, FieldDefinition> fields) {
        this.rootType = rootType;
        this.fields = fields;
    }

    public Map<String, FieldDefinition> getFields() {
        return fields;
    }

    public String getRootType() {
        return rootType;
    }

    /**
     * Represents a field definition in the JSON schema
     */
    public static class FieldDefinition {
        private final String type;
        private final String semanticTag;
        private final Map<String, Object> constraints;
        private final List<FieldDefinition> items; // For array types
        private final Map<String, FieldDefinition> properties; // For object types

        public FieldDefinition(String type, String semanticTag, Map<String, Object> constraints, 
                              List<FieldDefinition> items, Map<String, FieldDefinition> properties) {
            this.type = type;
            this.semanticTag = semanticTag;
            this.constraints = constraints != null ? constraints : new HashMap<>();
            this.items = items;
            this.properties = properties;
        }

        public String getType() {
            return type;
        }

        public String getSemanticTag() {
            return semanticTag;
        }

        public Map<String, Object> getConstraints() {
            return constraints;
        }

        public List<FieldDefinition> getItems() {
            return items;
        }

        public Map<String, FieldDefinition> getProperties() {
            return properties;
        }
    }
}