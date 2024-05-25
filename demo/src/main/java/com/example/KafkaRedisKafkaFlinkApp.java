package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import redis.clients.jedis.Jedis;

public class KafkaRedisKafkaFlinkApp {
    private static Map<String, String> cache = new HashMap<>();

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Kafka source properties
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", "localhost:9092");
        kafkaProps.setProperty("group.id", "flink-consumer-group");

        // Kafka consumer
        FlinkKafkaConsumer<String> kafkaConsumer = new FlinkKafkaConsumer<>("source-topic", new SimpleStringSchema(),
                kafkaProps);
        DataStream<String> kafkaStream = env.addSource(kafkaConsumer).name("Kafka Source");

        // Process stream with in-memory cache and Redis lookup
        DataStream<String> processedStream = kafkaStream.flatMap((String value, SourceContext<String> out) -> {
            String result = cache.get(value);
            if (result == null) {
                Jedis jedis = new Jedis("localhost", 6379);
                result = jedis.get(value);
                if (result != null) {
                    cache.put(value, result);
                }
                jedis.close();
            }
            if (result != null) {
                out.collect(result);
            }
        }).name("In-Memory Cache and Redis Lookup");

        // Kafka producer properties
        Properties kafkaProducerProps = new Properties();
        kafkaProducerProps.setProperty("bootstrap.servers", "localhost:9092");

        // Kafka producer
        FlinkKafkaProducer<String> kafkaProducer = new FlinkKafkaProducer<>(
                "target-topic",
                new SimpleStringSchema(),
                kafkaProducerProps,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE);

        // Add sink
        processedStream.addSink(kafkaProducer).name("Kafka Sink");

        env.execute("Kafka Redis Kafka Flink App");
    }

    // Custom Redis Source Function to populate the cache initially (if required)
    public static class RedisSourceFunction extends RichSourceFunction<String> {
        private transient Jedis jedis;
        private boolean running = true;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            jedis = new Jedis("localhost", 6379);
        }

        @Override
        public void run(SourceContext<String> ctx) throws Exception {
            while (running) {
                String data = jedis.rpop("your_redis_list_key");
                if (data != null) {
                    cache.put(data, data); // Populate the cache
                    ctx.collect(data);
                }
                Thread.sleep(1000); // Adjust sleep time as needed
            }
        }

        @Override
        public void cancel() {
            running = false;
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    // Custom Redis Sink Function (if you need to write back to Redis)
    public static class RedisSinkFunction extends RichSinkFunction<String> {
        private transient Jedis jedis;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            jedis = new Jedis("localhost", 6379);
        }

        @Override
        public void invoke(String value, Context context) {
            jedis.set(value, value); // Adjust the logic as per your requirement
        }

        @Override
        public void close() throws Exception {
            if (jedis != null) {
                jedis.close();
            }
            super.close();
        }
    }
}
