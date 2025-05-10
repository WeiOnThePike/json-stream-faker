import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { SchemaProcessorModule } from './schema-processor/schema-processor.module'; // Added import
// import { HttpModule } from '@nestjs/axios'; // Consider making HttpModule global if used by multiple modules

@Module({
  imports: [
    SchemaProcessorModule, // Added SchemaProcessorModule here
    // HttpModule.register({ // Example: If you need HttpModule globally or for AppService
    //   timeout: 5000,
    //   maxRedirects: 5,
    // }),
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
