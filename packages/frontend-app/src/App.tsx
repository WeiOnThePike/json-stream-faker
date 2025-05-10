import { useState } from 'react';
import './App.css'; // Assuming you'll keep or modify this for basic styling
import SchemaUploadForm from './components/SchemaUploadForm';
import { startDataGeneration, getGeneratorStats } from './services/api'; // Assuming getGeneratorStats might be used

// Define a type for the enhanced schema if you have a specific structure
type EnhancedSchema = any; 
// Define a type for generation status
interface GenerationStatus {
  generationId: string | null;
  eventCount: number;
  statusMessage: string;
  isRunning: boolean;
  error: string | null;
}

function App() {
  const [enhancedSchema, setEnhancedSchema] = useState<EnhancedSchema | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [generationStatus, setGenerationStatus] = useState<GenerationStatus>({
    generationId: null,
    eventCount: 0,
    statusMessage: '',
    isRunning: false,
    error: null,
  });

  const handleUploadSuccess = (schema: EnhancedSchema) => {
    setEnhancedSchema(schema);
    setUploadError(null);
    // Optionally, display the enhanced schema or parts of it
    console.log('Enhanced Schema Received:', schema);
    // You could automatically trigger generation here or provide a button
  };

  const handleUploadError = (errorMessage: string) => {
    setUploadError(errorMessage);
    setEnhancedSchema(null);
  };

  const handleStartGeneration = async () => {
    if (!enhancedSchema) {
      setGenerationStatus(prev => ({ ...prev, error: 'No enhanced schema available to start generation.' }));
      return;
    }
    setGenerationStatus(prev => ({ 
      ...prev, 
      isRunning: true, 
      statusMessage: 'Starting generation...', 
      error: null,
      generationId: null, // Reset previous ID
      eventCount: 0,    // Reset count
    }));

    try {
      // Pass the enhanced schema and any specific generator config
      const result = await startDataGeneration(enhancedSchema, { /* your generator config if any */ });
      setGenerationStatus(prev => ({
        ...prev,
        statusMessage: result.message || 'Generation started successfully.',
        generationId: result.generation_id, // Assuming backend returns this
        isRunning: true, // Or set based on actual response if it's synchronous start
      }));
      // TODO: Implement polling or WebSocket for live stats updates using result.generation_id
      // For now, we'll just log it.
      console.log('Generation started:', result);
      if (result.generation_id) {
        // Example: Start polling for stats (implement with care for production)
        // pollStats(result.generation_id); 
      }
    } catch (error: any) {
      setGenerationStatus(prev => ({
        ...prev,
        statusMessage: 'Failed to start generation.',
        error: error.message || 'Unknown error starting generation.',
        isRunning: false,
      }));
    }
  };
  
  // Basic polling example - replace with WebSockets for better UX in production
  // const pollStats = async (generationId: string) => {
  //   if (!generationStatus.isRunning && generationStatus.generationId !== generationId) return; // Stop if not running or new generation started

  //   try {
  //     const stats = await getGeneratorStats(generationId);
  //     setGenerationStatus(prev => ({
  //       ...prev,
  //       eventCount: stats.current_event_count,
  //       statusMessage: `Generating... ${stats.current_event_count} events. Status: ${stats.status}`,
  //       isRunning: stats.status === 'running', // Update based on actual status
  //     }));
  //     if (stats.status === 'running') {
  //       setTimeout(() => pollStats(generationId), 2000); // Poll every 2 seconds
  //     }
  //   } catch (error: any) {
  //     console.error("Error polling stats:", error);
  //     setGenerationStatus(prev => ({
  //       ...prev,
  //       statusMessage: `Error fetching stats: ${error.message}`,
  //       isRunning: false, // Stop polling on error
  //     }));
  //   }
  // };


  return (
    <>
      <header>
        <h1>JSON Stream Faker</h1>
      </header>
      <main>
        <section id="schema-upload">
          <h2>1. Upload & Enhance Schema</h2>
          <SchemaUploadForm 
            onUploadSuccess={handleUploadSuccess} 
            onUploadError={handleUploadError} 
          />
          {uploadError && <p style={{ color: 'red' }}>Upload Error: {uploadError}</p>}
        </section>

        {enhancedSchema && (
          <section id="generation-control">
            <h2>2. Generate Data Stream</h2>
            {/* Optionally display parts of the enhanced schema */}
            {/* <pre>{JSON.stringify(enhancedSchema, null, 2)}</pre> */}
            <button onClick={handleStartGeneration} disabled={generationStatus.isRunning}>
              {generationStatus.isRunning ? 'Generation in Progress...' : 'Start Data Generation'}
            </button>
            {generationStatus.statusMessage && <p>Status: {generationStatus.statusMessage}</p>}
            {generationStatus.isRunning && <p>Events Generated: {generationStatus.eventCount}</p>}
            {generationStatus.error && <p style={{ color: 'red' }}>Generation Error: {generationStatus.error}</p>}
          </section>
        )}
      </main>
    </>
  );
}

export default App;
