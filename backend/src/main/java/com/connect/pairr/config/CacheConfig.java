package com.connect.pairr.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Value("${cache.user-existence.ttl-minutes:60}")
    private int userExistenceTtlMinutes;

    @Value("${cache.user-existence.max-size:10000}")
    private int userExistenceMaxSize;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("userExistence");
        cacheManager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(userExistenceTtlMinutes, TimeUnit.MINUTES)
                        .maximumSize(userExistenceMaxSize)
                        .recordStats()
        );
        return cacheManager;
    }
}