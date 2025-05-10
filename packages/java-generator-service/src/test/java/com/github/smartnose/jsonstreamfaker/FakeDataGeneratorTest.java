package com.github.smartnose.jsonstreamfaker;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FakeDataGeneratorTest {

    @Test
    public void testGenerateSimpleObject() {
        // Create a simple schema
        Map<String, JsonSchema.FieldDefinition> fields = new HashMap<>();
        
        // Add id field with UUID semantic tag
        fields.put("id", new JsonSchema.FieldDefinition(
                "string", 
                "uuid", 
                null, 
                null, 
                null
        ));
        
        // Add name field with Name semantic tag
        fields.put("name", new JsonSchema.FieldDefinition(
                "string", 
                "name", 
                null, 
                null, 
                null
        ));
        
        // Add age field with age semantic tag and constraints
        Map<String, Object> ageConstraints = new HashMap<>();
        ageConstraints.put("minimum", 18.0);
        ageConstraints.put("maximum", 65.0);
        
        fields.put("age", new JsonSchema.FieldDefinition(
                "integer", 
                "age", 
                ageConstraints, 
                null, 
                null
        ));
        
        // Create the schema and generator
        JsonSchema schema = new JsonSchema("object", fields);
        FakeDataGenerator generator = new FakeDataGenerator(schema);
        
        // Generate a few objects and verify they match the schema
        for (int i = 0; i < 10; i++) {
            JSONObject obj = generator.generateObject();
            
            // Check that all fields are present
            assertTrue(obj.has("id"));
            assertTrue(obj.has("name"));
            assertTrue(obj.has("age"));
            
            // Verify field types
            assertTrue(obj.get("id") instanceof String);
            assertTrue(obj.get("name") instanceof String);
            assertTrue(obj.get("age") instanceof Number);
            
            // Verify age constraints
            int age = obj.getInt("age");
            assertTrue(age >= 18 && age <= 65, "Age is out of range: " + age);
        }
    }
}