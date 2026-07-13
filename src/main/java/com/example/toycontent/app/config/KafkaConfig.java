package com.example.toycontent.app.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
@ConditionalOnProperty(name = "kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${spring.kafka.producer.acks}")
  private String acks;

  @Value("${spring.kafka.producer.retries}")
  private Integer retries;

  @Value("${spring.kafka.producer.batch-size}")
  private Integer batchSize;

  @Value("${spring.kafka.producer.linger-ms}")
  private Integer lingerMs;

  @Value("${spring.kafka.producer.buffer-memory}")
  private Integer bufferMemory;

  @Value("${spring.kafka.topic.notification}")
  private String notificationTopic;

  @Value("${spring.kafka.topic.notification-dlq}")
  private String notificationDlqTopic;

  @Bean
  public KafkaAdmin kafkaAdmin() {
    Map<String, Object> configs = Map.of(
        AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers
    );
    return new KafkaAdmin(configs);
  }

  @Bean
  public NewTopic notificationTopic() {
    return TopicBuilder.name(notificationTopic)
        .partitions(6)
        .replicas(1)
        .config("retention.ms", "604800000")
        .config("compression.type", "lz4")
        .build();
  }

  @Bean
  public NewTopic notificationDlqTopic() {
    return TopicBuilder.name(notificationDlqTopic)
        .partitions(3)
        .replicas(1)
        .config("retention.ms", "259200000")
        .build();
  }

  @Bean
  public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    props.put(ProducerConfig.ACKS_CONFIG, acks);
    props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    props.put(ProducerConfig.RETRIES_CONFIG, retries);
    props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
    props.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
    props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
    return new DefaultKafkaProducerFactory<>(props);
  }

  /**
   * KafkaTemplate의 Micrometer Observation을 활성화한다.
   *
   * <p>이걸 켜면 {@code kafkaTemplate.send()} 호출이 자체 span을 만들고, traceId를 Kafka
   * 헤더로 자동 전파한다. 결과적으로 <b>content → Kafka → chat</b>이 하나의 trace로 연결되고,
   * send 실패 시 error 태그도 observation 컨텍스트에 자동 기록된다. Brave 경로에서
   * "Agent였다면 공짜였을" 항목을 명시적으로 켜주는 지점.
   */
  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() {
    KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
    template.setObservationEnabled(true);
    return template;
  }
}
