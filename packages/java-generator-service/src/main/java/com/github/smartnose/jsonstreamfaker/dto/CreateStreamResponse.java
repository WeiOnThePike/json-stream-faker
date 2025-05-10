package com.github.smartnose.jsonstreamfaker.dto;

// Can also be a record if using Java 14+
public class CreateStreamResponse {
    public String streamId;
    public String status; // e.g., "SUBMITTED", "STARTED", "ERROR"
    public String message; // Optional message, e.g., error details

    // Default constructor for JSON serialization
    public CreateStreamResponse() {}

    public CreateStreamResponse(String streamId, String status, String message) {
        this.streamId = streamId;
        this.status = status;
        this.message = message;
    }

    // Getters and Setters (or public fields)
    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}