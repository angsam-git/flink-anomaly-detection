#!/usr/bin/env bash
set -e

# Shell script to set up and run Kafka and Flink containers, create a Kafka topic, and submit a Flink job. For use on Linux and other UNIX based systems

# Build the Flink job JAR
echo "Building Flink job JAR..."
mvn clean install
echo "Maven finished"

NETWORK_NAME="shared-kafka-net"

# Create Docker network if it doesn't already exist
if ! docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
  echo "Creating Docker network: $NETWORK_NAME"
  docker network create "$NETWORK_NAME"
else
  echo "Docker network $NETWORK_NAME already exists"
fi

# Set up kafka and flink containers
echo "Starting Kafka and Flink containers..."
docker-compose down -v >/dev/null 2>&1 && docker-compose up -d >/dev/null 2>&1

# Create topic 'api-logs'
echo "Creating topic 'api-logs'..."
docker-compose exec kafka kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic api-logs --partitions 1 --replication-factor 1 >/dev/null 2>&1

# Wait for topic leader assignment
while ! docker-compose exec kafka kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic api-logs 2>/dev/null | grep -q "Leader:"; do
    echo "Waiting for topic leader assignment..."
    sleep 2
done

# Create topic 'sink-topic'
echo "Creating topic 'sink-topic'..."
docker-compose exec kafka kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic sink-topic --partitions 1 --replication-factor 1 >/dev/null 2>&1

# Wait for topic leader assignment
while ! docker-compose exec kafka kafka-topics.sh --bootstrap-server kafka:9092 --describe --topic sink-topic 2>/dev/null | grep -q "Leader:"; do
    echo "Waiting for topic leader assignment..."
    sleep 2
done

# Submit Flink Job
echo "Submitting Flink job..."
FLINK_OUTPUT=$(docker-compose exec jobmanager /opt/flink/bin/flink run -d --jobmanager jobmanager:8081 /flink/job/flink-anomaly-detector-1.0.0.jar 2>/dev/null)
echo "$FLINK_OUTPUT"

# Extract the Job ID from output
JOB_ID=$(echo "$FLINK_OUTPUT" | grep -oE '[a-f0-9]{32}')
if [[ -z "$JOB_ID" ]]; then
    echo "Could not extract Flink Job ID!"
    exit 1
fi

# Loop until job is RUNNING
while true; do
    JOB_STATE=$(docker-compose exec jobmanager curl -s http://localhost:8081/jobs/$JOB_ID 2>/dev/null | grep -o '"state":"[^"]*"' | cut -d: -f2 | tr -d '"')
    if [[ "$JOB_STATE" == "RUNNING" ]]; then
        break
    fi
    echo "Waiting for job $JOB_ID to reach RUNNING status..."
    sleep 2
done

echo "Successfully ran job $JOB_ID."
