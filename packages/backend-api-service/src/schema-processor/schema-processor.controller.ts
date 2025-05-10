import { Controller, Post, UploadedFile, UseInterceptors, Body, Get, Param, HttpException, HttpStatus } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { SchemaProcessorService } from './schema-processor.service';
// Multer types might be needed if you work with the file object directly
// import { Express } from 'express';

// Import DTOs that might be used in request bodies for new endpoints
// Assuming OutputConfigDto is defined in the service or a shared DTOs file
// For now, let's assume it's implicitly handled or we define a simple one here.

interface StartGenerationBodyDto {
  enhancedSchema: any; // The schema object enhanced by LLM
  outputConfig: { // This should match OutputConfigDto in SchemaProcessorService
    type: 'console' | 'file' | 'kafka';
    filePath?: string;
    kafka?: {
      bootstrapServers: string;
      topic: string;
      batchSize?: number;
      intervalMs?: number;
    };
  };
  maxMessages?: number;
  maxTimeInSeconds?: number;
}


@Controller('schema-processor')
export class SchemaProcessorController {
  constructor(private readonly schemaProcessorService: SchemaProcessorService) {}

  @Post('upload')
  @UseInterceptors(FileInterceptor('schemaFile')) // 'schemaFile' is the field name in the form-data
  async uploadSchema(@UploadedFile() file: Express.Multer.File) {
    if (!file) {
      throw new HttpException('Schema file is required.', HttpStatus.BAD_REQUEST);
    }
    try {
      // Assuming the service handles parsing and LLM enhancement
      const processingResult = await this.schemaProcessorService.processSchema(file.buffer.toString('utf-8'));
      return processingResult;
    } catch (error) {
      // Log the error internally
      console.error('Error processing schema:', error);
      throw new HttpException('Failed to process schema.', HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Post('start-generation')
  async startGeneration(@Body() body: StartGenerationBodyDto) {
    if (!body.enhancedSchema || !body.outputConfig) {
      throw new HttpException(
        'Enhanced schema and output configuration are required.',
        HttpStatus.BAD_REQUEST,
      );
    }
    try {
      this.schemaProcessorService.logger.log(`Received request to start generation for schema: ${body.enhancedSchema.title || 'Untitled Schema'}`);
      const result = await this.schemaProcessorService.startDataGeneration(
        body.enhancedSchema,
        body.outputConfig,
        body.maxMessages,
        body.maxTimeInSeconds,
      );
      return result;
    } catch (error) {
      // The service method already logs detailed errors
      // We re-throw the HttpException as is, or wrap other errors
      if (error instanceof HttpException) {
        throw error;
      }
      console.error('Error in start-generation controller:', error);
      throw new HttpException(
        'Failed to start data generation process.',
        HttpStatus.INTERNAL_SERVER_ERROR,
      );
    }
  }

  // Example endpoint to get an enhanced schema (if stored and identified by an ID)
  // @Get('enhanced/:id')
  // async getEnhancedSchema(@Param('id') id: string) {
  //   try {
  //     const schema = await this.schemaProcessorService.getEnhancedSchema(id);
  //     if (!schema) {
  //       throw new HttpException('Enhanced schema not found.', HttpStatus.NOT_FOUND);
  //     }
  //     return schema;
  //   } catch (error) {
  //     console.error('Error fetching enhanced schema:', error);
  //     throw new HttpException('Failed to fetch enhanced schema.', HttpStatus.INTERNAL_SERVER_ERROR);
  //   }
  // }

  // TODO: Add endpoints for starting/stopping generation via the Java service
  // These would likely call methods in SchemaProcessorService that then interact with another service
  // (e.g., a GeneratorOrchestratorService) which calls the Java Generator API.
}
