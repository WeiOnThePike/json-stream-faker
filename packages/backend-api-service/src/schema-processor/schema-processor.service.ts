import { Injectable, HttpException, HttpStatus, Logger } from '@nestjs/common';
import { HttpService } from '@nestjs/axios';
import { firstValueFrom } from 'rxjs';
import OpenAI from 'openai';

// DTOs for Java Service Interaction
interface CreateStreamRequestDto {
  schemaContent: string; // JSON schema as a string
  outputConfig: OutputConfigDto;
  maxMessages?: number;
  maxTimeInSeconds?: number;
}

interface OutputConfigDto {
  type: 'console' | 'file' | 'kafka';
  filePath?: string; // For "file" type
  kafka?: KafkaConfigDto; // For "kafka" type
}

interface KafkaConfigDto {
  bootstrapServers: string;
  topic: string;
  batchSize?: number;
  intervalMs?: number;
}

interface CreateStreamResponseDto {
  streamId: string | null;
  status: string; // e.g., "SUBMITTED", "STARTED", "ERROR"
  message?: string;
}

@Injectable()
export class SchemaProcessorService {
  private readonly logger = new Logger(SchemaProcessorService.name);
  private readonly openai: OpenAI | null = null;

  constructor(
    private readonly httpService: HttpService,
  ) {
    if (process.env.OPENAI_API_KEY) {
      this.openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });
      this.logger.log('OpenAI client initialized.');
    } else {
      this.logger.warn('OPENAI_API_KEY not found in environment variables. LLM functionality will be disabled.');
    }
  }

  private buildLlmPrompt(rawSchemaContent: string): string {
    // This prompt is based on our discussion and docs/objective1_implementation_plan.md
    return `
You are an expert AI assistant specializing in enhancing JSON schemas for a mock data generator.
Your primary task is to analyze an input JSON schema and add specific semantic tags: 'faker' and 'format'.
These tags guide our Java-based data generator to produce realistic and contextually appropriate mock data.

- The 'faker' tag (e.g., "faker": "firstName", "faker": "uuid", "faker": "latitude") instructs the generator on the specific type of fake data to create.
- The 'format' tag (e.g., "format": "date-time") is used for standard data formats.

Please infer the most appropriate 'faker' or 'format' value by carefully considering:
1. The property name (e.g., 'user_email', 'sensorId', 'registrationDate').
2. The JSON 'type' of the property (e.g., 'string', 'number', 'integer').
3. The 'description' field associated with the property, if available, as it often contains strong hints.

Output ONLY the enhanced JSON schema. Do not include any other explanatory text or markdown.

--- Example 1: Person Data ---
Input Schema Snippet (Raw):
{
  "type": "object",
  "properties": {
    "unique_user_id": { "type": "string", "description": "The unique system identifier for a user." },
    "first_name": { "type": "string", "description": "The given name of the person." },
    "surname": { "type": "string", "description": "The family name of the person." },
    "electronic_mail": { "type": "string", "description": "Primary email address for the user." },
    "person_age_years": { "type": "integer", "description": "Current age of the individual in full years." },
    "account_creation_timestamp": { "type": "string", "description": "The exact date and time the user account was created." }
  }
}

Output Enhanced Schema Snippet (with 'faker' and 'format' tags):
{
  "type": "object",
  "properties": {
    "unique_user_id": { "type": "string", "description": "The unique system identifier for a user.", "faker": "uuid" },
    "first_name": { "type": "string", "description": "The given name of the person.", "faker": "firstName" },
    "surname": { "type": "string", "description": "The family name of the person.", "faker": "lastName" },
    "electronic_mail": { "type": "string", "description": "Primary email address for the user.", "faker": "email" },
    "person_age_years": { "type": "integer", "description": "Current age of the individual in full years.", "faker": "age" },
    "account_creation_timestamp": { "type": "string", "description": "The exact date and time the user account was created.", "format": "date-time" }
  }
}

--- Example 2: IoT Device Data ---
Input Schema Snippet (Raw):
{
  "type": "object",
  "properties": {
    "device_identifier": { "type": "string", "description": "Unique ID for the sensor device." },
    "event_time": { "type": "string", "description": "Timestamp of the sensor reading." },
    "geo_latitude": { "type": "number", "description": "Geographic latitude of the device." },
    "geo_longitude": { "type": "number", "description": "Geographic longitude of the device." },
    "charge_level_percent": { "type": "number", "description": "Remaining battery charge as a percentage." }
  }
}

Output Enhanced Schema Snippet (with 'faker' and 'format' tags):
{
  "type": "object",
  "properties": {
    "device_identifier": { "type": "string", "description": "Unique ID for the sensor device.", "faker": "uuid" },
    "event_time": { "type": "string", "description": "Timestamp of the sensor reading.", "format": "date-time" },
    "geo_latitude": { "type": "number", "description": "Geographic latitude of the device.", "faker": "latitude" },
    "geo_longitude": { "type": "number", "description": "Geographic longitude of the device.", "faker": "longitude" },
    "charge_level_percent": { "type": "number", "description": "Remaining battery charge as a percentage.", "faker": "percentage" }
  }
}

--- New Schema to Enhance ---
${rawSchemaContent}

Now, please enhance the "New Schema to Enhance" above, adding 'faker' and 'format' tags according to the patterns demonstrated. Output ONLY the enhanced JSON schema:
`;
  }

  async processSchema(rawSchemaContent: string): Promise<any> {
    let parsedRawSchema;
    try {
      parsedRawSchema = JSON.parse(rawSchemaContent);
    } catch (error) {
      this.logger.error('Invalid JSON schema provided for processing.', error.stack);
      throw new HttpException('Invalid JSON schema provided.', HttpStatus.BAD_REQUEST);
    }

    if (!this.openai) {
      this.logger.warn('OpenAI client not initialized. Returning schema without LLM enhancement.');
      // Simulate enhancement for now if LLM is not available
      parsedRawSchema.description = (parsedRawSchema.description || "") + " (LLM enhancement SKIPPED - API key missing)";
      return {
        message: 'Schema processed (LLM enhancement SKIPPED - API key missing).',
        enhancedSchema: parsedRawSchema,
      };
    }
    
    const prompt = this.buildLlmPrompt(rawSchemaContent);
    this.logger.log('Sending schema to LLM for enhancement...');

    try {
      const completion = await this.openai.chat.completions.create({
        model: "gpt-3.5-turbo", // Or your preferred model e.g. gpt-4
        messages: [
          // The prompt variable now contains the full instruction including few-shot examples
          { role: "user", content: prompt } 
        ],
        temperature: 0.2, // Lower temperature for more deterministic output for schema generation
      });

      const llmResponseContent = completion.choices[0]?.message?.content;
      if (!llmResponseContent) {
        this.logger.error('LLM returned an empty response content.');
        throw new HttpException('LLM returned an empty response.', HttpStatus.INTERNAL_SERVER_ERROR);
      }
      
      this.logger.log('LLM enhancement successful.');
      // Attempt to parse the LLM response as JSON
      try {
        const enhancedSchema = JSON.parse(llmResponseContent);
        return {
          message: 'Schema enhanced successfully by LLM.',
          enhancedSchema: enhancedSchema,
        };
      } catch (parseError) {
        this.logger.error('Failed to parse LLM response as JSON.', parseError.stack);
        this.logger.debug('Raw LLM response content:', llmResponseContent); // Log the raw response for debugging
        throw new HttpException('LLM response was not valid JSON.', HttpStatus.INTERNAL_SERVER_ERROR);
      }

    } catch (llmError) {
      this.logger.error('LLM interaction failed.', llmError.stack);
      throw new HttpException('Failed to enhance schema with LLM.', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  async startDataGeneration(
    enhancedSchema: any, // This is the JSON object of the schema
    outputConfig: OutputConfigDto, // Configuration for the output sink
    maxMessages?: number,
    maxTimeInSeconds?: number,
  ): Promise<CreateStreamResponseDto> {
    
    const schemaContentString = JSON.stringify(enhancedSchema);

    const requestBody: CreateStreamRequestDto = {
      schemaContent: schemaContentString,
      outputConfig,
      maxMessages,
      maxTimeInSeconds,
    };

    const javaGeneratorUrl = process.env.JAVA_GENERATOR_URL || 'http://java-generator-service:8080';
    const endpoint = `${javaGeneratorUrl}/streams`;
    this.logger.log(`Requesting data generation start. Endpoint: ${endpoint}, Schema title: ${enhancedSchema.title || 'Untitled Schema'}`);

    try {
      const response = await firstValueFrom(
        this.httpService.post<CreateStreamResponseDto>(endpoint, requestBody, {
          headers: { 'Content-Type': 'application/json' },
        }),
      );
      this.logger.log(`Java service responded for stream creation: ${JSON.stringify(response.data)}`);
      return response.data;
    } catch (error) {
      this.logger.error('Error starting data generation via Java service.', error.response?.data || error.message, error.stack);
      throw new HttpException(
        `Failed to start data generation with Java service: ${error.response?.data?.message || error.message}`,
        error.response?.status || HttpStatus.INTERNAL_SERVER_ERROR,
      );
    }
  }

  // TODO: Implement methods for stopping a stream and getting stats, calling the Java service
  // async stopDataGeneration(streamId: string): Promise<any> { ... }
  // async getStreamStats(streamId: string): Promise<any> { ... }
}
