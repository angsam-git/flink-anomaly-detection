package com.angsam.flinkanomalydetector;

public class PredictRequest {
    public double[] vector;

    public PredictRequest() {}

    public PredictRequest(double[] vector) {
        this.vector = vector;
    }
}
