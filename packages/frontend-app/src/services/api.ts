// Basic API service to interact with the backend

const BACKEND_API_BASE_URL = import.meta.env.VITE_BACKEND_API_URL || 'http://localhost:3000/api/v1'; // Default if not set in .env

interface UploadSchemaResponse {
  message: string;
  enhancedSchema?: any; // Define a proper type for your enhanced schema
  // enhancedSchemaId?: string; 
}

interface GeneratorStats {
  generation_id: string;
  current_event_count: number;
  last_updated: string;
  status: 'running' | 'stopped' | 'error'; // Or other relevant statuses
}

/**
 * Uploads a schema file to the backend for processing.
 * @param schemaFile The schema file to upload.
 * @returns Promise<UploadSchemaResponse>
 */
export const uploadSchemaFile = async (schemaFile: File): Promise<UploadSchemaResponse> => {
  const formData = new FormData();
  formData.append('schemaFile', schemaFile);

  try {
    const response = await fetch(`${BACKEND_API_BASE_URL}/schema-processor/upload`, {
      method: 'POST',
      body: formData,
      // Headers are not typically needed for FormData with fetch, browser sets them.
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to upload schema. Unknown error.' }));
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error uploading schema:', error);
    throw error; // Re-throw to be caught by the calling component
  }
};

/**
 * Starts the data generation process on the backend.
 * @param enhancedSchema The LLM-enhanced schema object.
 * @param generatorConfig Optional configuration for the generator.
 * @returns Promise<any> // Define a proper response type
 */
export const startDataGeneration = async (enhancedSchema: any, generatorConfig?: any): Promise<any> => {
  try {
    const response = await fetch(`${BACKEND_API_BASE_URL}/generator/start`, { // Assuming this endpoint exists
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        // schema_id: enhancedSchema.id, // Or pass schema_content
        schema_content: enhancedSchema,
        generator_config: generatorConfig || {}, 
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to start generation. Unknown error.' }));
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error starting data generation:', error);
    throw error;
  }
};

/**
 * Fetches generation statistics from the backend.
 * @param generationId The ID of the generation process.
 * @returns Promise<GeneratorStats>
 */
export const getGeneratorStats = async (generationId: string): Promise<GeneratorStats> => {
  try {
    const response = await fetch(`${BACKEND_API_BASE_URL}/generator/stats/${generationId}`); // Assuming this endpoint
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to fetch stats. Unknown error.' }));
      throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error fetching generator stats:', error);
    throw error;
  }
};

// TODO: Add function to stop generation
// export const stopDataGeneration = async (generationId: string): Promise<any> => { ... }