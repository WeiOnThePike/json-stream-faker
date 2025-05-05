package com.github.smartnose.jsonstreamfaker;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SchemaParserTest {

    @Test
    public void testParseSimpleSchema() throws IOException {
        // Create a temporary schema file
        File tempFile = File.createTempFile("schema-", ".json");
        tempFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("{\n" +
                    "  \"type\": \"object\",\n" +
                    "  \"properties\": {\n" +
                    "    \"id\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"faker\": \"uuid\"\n" +
                    "    },\n" +
                    "    \"name\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"faker\": \"name\"\n" +
                    "    },\n" +
                    "    \"email\": {\n" +
                    "      \"type\": \"string\",\n" +
                    "      \"faker\": \"email\"\n" +
                    "    },\n" +
                    "    \"age\": {\n" +
                    "      \"type\": \"integer\",\n" +
                    "      \"minimum\": 18,\n" +
                    "      \"maximum\": 65,\n" +
                    "      \"faker\": \"age\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}");
        }
        
        // Parse the schema
        SchemaParser parser = new SchemaParser(tempFile);
        JsonSchema schema = parser.parse();
        
        // Verify the parsed schema
        assertEquals("object", schema.getRootType());
        assertNotNull(schema.getFields());
        assertEquals(4, schema.getFields().size());
        
        // Check that the fields were parsed correctly
        assertTrue(schema.getFields().containsKey("id"));
        assertTrue(schema.getFields().containsKey("name"));
        assertTrue(schema.getFields().containsKey("email"));
        assertTrue(schema.getFields().containsKey("age"));
        
        // Verify a field's properties
        JsonSchema.FieldDefinition ageDef = schema.getFields().get("age");
        assertEquals("integer", ageDef.getType());
        assertEquals("age", ageDef.getSemanticTag());
        assertEquals(18.0, ageDef.getConstraints().get("minimum"));
        assertEquals(65.0, ageDef.getConstraints().get("maximum"));
    }
}