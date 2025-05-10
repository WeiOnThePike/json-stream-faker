import React, { useState, useCallback } from 'react';
import { uploadSchemaFile } from '../services/api';

interface SchemaUploadFormProps {
  onUploadSuccess: (enhancedSchema: any) => void; // Callback with the enhanced schema
  onUploadError: (errorMessage: string) => void;
}

const SchemaUploadForm: React.FC<SchemaUploadFormProps> = ({ onUploadSuccess, onUploadError }) => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const handleFileChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    if (event.target.files && event.target.files[0]) {
      setSelectedFile(event.target.files[0]);
      setMessage(null); // Clear previous messages
    }
  };

  const handleSubmit = useCallback(async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedFile) {
      setMessage('Please select a schema file to upload.');
      return;
    }

    setIsLoading(true);
    setMessage('Uploading and processing schema...');
    onUploadError(''); // Clear previous errors

    try {
      const result = await uploadSchemaFile(selectedFile);
      setMessage(result.message || 'Schema processed successfully!');
      if (result.enhancedSchema) {
        onUploadSuccess(result.enhancedSchema);
      } else {
        // This case might indicate an issue if enhancedSchema is always expected
        console.warn('Enhanced schema not found in upload response:', result);
        onUploadError('Schema processed, but no enhanced schema was returned.');
      }
    } catch (error: any) {
      const errorMessage = error.message || 'An unknown error occurred during schema upload.';
      setMessage(`Error: ${errorMessage}`);
      onUploadError(errorMessage);
      console.error('Schema upload error:', error);
    } finally {
      setIsLoading(false);
    }
  }, [selectedFile, onUploadSuccess, onUploadError]);

  return (
    <div>
      <h3>Upload JSON Schema</h3>
      <form onSubmit={handleSubmit}>
        <div>
          <input type="file" accept=".json,application/json" onChange={handleFileChange} disabled={isLoading} />
        </div>
        <button type="submit" disabled={isLoading || !selectedFile}>
          {isLoading ? 'Processing...' : 'Upload and Enhance'}
        </button>
      </form>
      {message && <p>{message}</p>}
    </div>
  );
};

export default SchemaUploadForm;