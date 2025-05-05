package com.github.smartnose.jsonstreamfaker;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Interface for data sinks that receive generated JSON objects
 */
public interface DataSink {
    /**
     * Sends a JSON object to the sink
     * 
     * @param jsonObject The JSON object to send
     * @throws IOException If an I/O error occurs
     */
    void send(JSONObject jsonObject) throws IOException;
    
    /**
     * Flushes any buffered data to the sink
     * 
     * @throws IOException If an I/O error occurs
     */
    void flush() throws IOException;
    
    /**
     * Closes the sink
     * 
     * @throws IOException If an I/O error occurs
     */
    void close() throws IOException;
}