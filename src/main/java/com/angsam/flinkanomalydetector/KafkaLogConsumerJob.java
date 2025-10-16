package com.angsam.flinkanomalydetector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SerializationSchema.InitializationContext;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class KafkaLogConsumerJob {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env =
            StreamExecutionEnvironment.getExecutionEnvironment();

        Yaml yaml = new Yaml();
        InputStream in = KafkaLogConsumerJob.class
            .getClassLoader()
            .getResourceAsStream("application.yml");
        Map<String, Object> cfg = yaml.load(in);

        Map<String, Object> kafkaCfg =
            (Map<String, Object>) cfg.get("kafka");
        String bootstrap  = (String) kafkaCfg.get("bootstrap-servers");
        String topic      = (String) kafkaCfg.get("topic");
        String sinkTopic  = (String) kafkaCfg.get("sink-topic");
        String groupId    = (String) kafkaCfg.get("group-id");

        Map<String, Object> modelCfg =
            (Map<String, Object>) cfg.get("model");
        String modelUrl   = (String) modelCfg.get("url");

        KafkaSource<String> source = KafkaSource.<String>builder()
            .setBootstrapServers(bootstrap)
            .setTopics(topic)
            .setGroupId(groupId)
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new SimpleStringSchema())
            .build();

        DataStream<LogEntry> logs = env
          .fromSource(source, WatermarkStrategy.noWatermarks(), "kafka-source")
          .map(json -> new ObjectMapper().readValue(json, LogEntry.class))
          .filter(java.util.Objects::nonNull);

        DataStream<LogWithAnomaly> enriched = AsyncDataStream.unorderedWait(
            logs,
            new AsyncModelPredictFunction(modelUrl),
            5, TimeUnit.SECONDS,
            50  // max concurrent requests
        );

        DataStream<LogWithAnomaly> anomalies = enriched
            .filter(lwa -> lwa.anomaly);

        KafkaSink<LogWithAnomaly> sink = KafkaSink.<LogWithAnomaly>builder()
            .setBootstrapServers(bootstrap)
            .setRecordSerializer(
                KafkaRecordSerializationSchema.builder()
                    .setTopic(sinkTopic)
                    .setValueSerializationSchema(
                        new SerializationSchema<LogWithAnomaly>() {
                            private final ObjectMapper mapper = new ObjectMapper();
                            @Override public void open(InitializationContext ctx) {}
                            @Override public byte[] serialize(LogWithAnomaly element) {
                                try { return mapper.writeValueAsBytes(element); }
                                catch (Exception e) { return new byte[0]; }
                            }
                        }
                    )
                    .build()
            )
            .build();

        anomalies.sinkTo(sink);

        env.execute("Kafka Log Consumer with Async Anomaly Detection");
    }
}
