package binod.suman.kafka_flink_demo;

import java.util.HashMap;
import java.util.Map;

import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import redis.clients.jedis.Jedis;

public class FlinkRedisApplication {

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // Sample data stream
        DataStream<String> dataStream = env.fromElements("key1", "key2", "key3");

        // Apply custom map function
        DataStream<String> resultStream = dataStream.map(new RedisMapFunction());

        resultStream.print();

        env.execute("Flink Redis Example");
    }

    public static class RedisMapFunction extends RichMapFunction<String, String> {
        private transient Map<String, String> inMemoryMap;
        private transient Jedis jedis;

        @Override
        public void open(Configuration parameters) throws Exception {
            super.open(parameters);
            // Initialize in-memory map
            inMemoryMap = new HashMap<>();
            inMemoryMap.put("key1", "value1");
            inMemoryMap.put("key2", "value2");

            // Initialize Redis connection
            jedis = new Jedis("localhost", 6379);
        }

        @Override
        public String map(String key) throws Exception {
            // Check in-memory map first
            String value = inMemoryMap.get(key);
            if (value == null) {
                // Fetch from Redis if not in in-memory map
                value = jedis.get(key);
            }
            return value != null ? value : "default_value";
        }

        @Override
        public void close() throws Exception {
            // Close Redis connection
            if (jedis != null) {
                jedis.close();
            }
            super.close();
        }
    }
}
