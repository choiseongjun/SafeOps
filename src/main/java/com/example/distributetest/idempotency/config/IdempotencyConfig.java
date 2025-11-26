package com.example.distributetest.idempotency.config;

import com.example.distributetest.idempotency.service.IdempotencyService;
import com.example.distributetest.idempotency.service.InMemoryIdempotencyService;
import com.example.distributetest.idempotency.service.RedisIdempotencyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class IdempotencyConfig {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyConfig.class);

    @Bean
    @ConditionalOnProperty(name = "idempotency.storage", havingValue = "redis", matchIfMissing = false)
    public RedisTemplate<String, String> idempotencyRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        log.info("Redis template configured for idempotency");
        return template;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "idempotency.storage", havingValue = "redis", matchIfMissing = false)
    public IdempotencyService redisIdempotencyService(RedisTemplate<String, String> redisTemplate) {
        log.info("Using Redis-based idempotency service");
        return new RedisIdempotencyService(redisTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(IdempotencyService.class)
    public IdempotencyService inMemoryIdempotencyService() {
        log.info("Using in-memory idempotency service");
        return new InMemoryIdempotencyService();
    }
}
