package com.angsam.flinkanomalydetector;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class KafkaLogConsumerJob {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Yaml yaml = new Yaml();
        InputStream input = KafkaLogConsumerJob.class.getClassLoader().getResourceAsStream("application.yml");
        Map<String, Object> yamlMap = yaml.load(input);

        Map<String, Object> kafka = (Map<String, Object>) yamlMap.get("kafka");
        String bootstrapServers = (String) kafka.get("bootstrap-servers");
        String topic = (String) kafka.get("topic");
        String groupId = (String) kafka.get("group-id");

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics(topic)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        env.fromSource(source, org.apache.flink.api.common.eventtime.WatermarkStrategy.noWatermarks(), "Kafka Source")
            .map(json -> {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    LogEntry entry = mapper.readValue(json, LogEntry.class);
                    System.out.println("[LOG] " + entry);
                    return entry;
                } catch (Exception e) {
                    System.err.println("Failed to parse log: " + json);
                    return null;
                }
            });

        env.execute("Kafka Log Consumer");
    }
}