package com.angsam.flinkanomalydetector;

public class LogWithAnomaly {
    public LogEntry log;
    public double error;
    public boolean anomaly;

    public LogWithAnomaly() {}

    public LogWithAnomaly(LogEntry log, double error, boolean anomaly) {
        this.log     = log;
        this.error   = error;
        this.anomaly = anomaly;
    }
}
