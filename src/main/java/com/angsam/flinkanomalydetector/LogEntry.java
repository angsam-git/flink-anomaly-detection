package com.angsam.flinkanomalydetector;

public class LogEntry {
    public long timestamp;
    public String endpoint;
    public int status;
    public long responseTimeMs;
    public String requestId;

    @Override
    public String toString() {
        return String.format("Log{endpoint=%s, status=%d, latency=%dms}", endpoint, status, responseTimeMs);
    }
}
