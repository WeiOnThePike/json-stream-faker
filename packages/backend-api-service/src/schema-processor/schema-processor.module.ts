import { Module } from '@nestjs/common';
import { SchemaProcessorController } from './schema-processor.controller';
import { SchemaProcessorService } from './schema-processor.service';
import { HttpModule } from '@nestjs/axios';

@Module({
  imports: [
    HttpModule.register({ // Configure HttpModule if needed (e.g., default timeout)
      timeout: 5000,
      maxRedirects: 5,
    }),
  ],
  controllers: [SchemaProcessorController],
  providers: [SchemaProcessorService],
})
export class SchemaProcessorModule {}