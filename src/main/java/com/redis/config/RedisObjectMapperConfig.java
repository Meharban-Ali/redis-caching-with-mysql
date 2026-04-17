package com.redis.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class RedisObjectMapperConfig {

    /**
     * PRIMARY ObjectMapper — Spring MVC ke liye
     * activateDefaultTyping NAHI hai — Postman se normal JSON aayega
     * @Primary — Spring MVC yahi use karega request/response ke liye
     */
    @Bean
    @Primary                        // ← Spring MVC ke liye
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // activateDefaultTyping NAHI — MVC ke liye zarurat nahi
    }

    /**
     * REDIS ObjectMapper — sirf Redis cache ke liye
     * activateDefaultTyping HAI — Redis ko type info chahiye
     * @Qualifier("redisObjectMapper") — sirf Redis inject karega
     */
    @Bean("redisObjectMapper")      // ← Sirf Redis ke liye
    public ObjectMapper redisObjectMapper() {
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator
                .builder()
                .allowIfBaseType(Object.class)
                .build();

        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(     // ← Sirf Redis ke liye
                        typeValidator,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY
                );
    }
}