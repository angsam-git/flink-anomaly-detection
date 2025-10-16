package com.angsam.flinkanomalydetector;

public class LogEntry {
    private long timestamp;
    private String endpoint;
    private String method;
    private int status;
    private long responseTimeMs;
    private int queryParamCount;
    private int headerCount;
    private int responseSizeBytes;
    private String requestId;

    public LogEntry() {}

    public LogEntry(long timestamp,
                    String endpoint,
                    String method,
                    int status,
                    long responseTimeMs,
                    int queryParamCount,
                    int headerCount,
                    int responseSizeBytes,
                    String requestId) {
        this.timestamp = timestamp;
        this.endpoint = endpoint;
        this.method = method;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
        this.queryParamCount = queryParamCount;
        this.headerCount = headerCount;
        this.responseSizeBytes = responseSizeBytes;
        this.requestId = requestId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public int getQueryParamCount() {
        return queryParamCount;
    }

    public void setQueryParamCount(int queryParamCount) {
        this.queryParamCount = queryParamCount;
    }

    public int getHeaderCount() {
        return headerCount;
    }

    public void setHeaderCount(int headerCount) {
        this.headerCount = headerCount;
    }

    public int getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public void setResponseSizeBytes(int responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

}
