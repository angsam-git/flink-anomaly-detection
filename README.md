# AI Anomaly Detection - Kafka, Flink & PyTorch

This is the real-time analytics pipeline including the Flink Java application which consumes logs from Kafka. It inspects and flags abnormal patterns in API usage with a PyTorch model using autoencoders and writes to a sink topic. 

---

## Dependencies

- Docker (with compose)
- Java 11+
- Python 3.8+

Monitored Spring Boot app can be found at https://github.com/angsam-git/ai-anomaly-monitored-api

PyTorch model can be found at https://github.com/angsam-git/pytorch-anomaly-service

## Features

- Creates Kafka topics for streaming API logs
- Runs a Flink job to consume and preprocess API request logs
- Uses a PyTorch model using autoencoders to detect anomalies in real time
- Writes events and detected anomalies to a sink Kafka topic
- Fully containerized

## Build & Run

Bat and shell scripts are provided for quick building and running. These scripts perform the building of the artifact for the job as well as the setup for Kafka and Flink containers.

### Build and Run on Windows

```batch
run.bat
```

### Build and Run on Linux / UNIX Systems
```bash
./run.sh
```
