package com.nfu.pettravel.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.pettravel.entity.CampRoute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
@Slf4j
public class RouteCacheService {
    public static final String HOT_ROUTES_GENERATION_KEY = "pettravel:route:hot:generation";
    private static final String HOT_ROUTES_KEY_PREFIX = "pettravel:route:hot:v2:";
    public static final String HOT_ROUTES_KEY = HOT_ROUTES_KEY_PREFIX + "0";
    private static final TypeReference<List<CampRoute>> ROUTE_LIST_TYPE = new TypeReference<>() { };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration baseTtl;

    public RouteCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${pettravel.cache.hot-routes-ttl:30m}") Duration baseTtl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.baseTtl = baseTtl;
    }

    public List<CampRoute> getHotRoutes(Supplier<List<CampRoute>> databaseLoader) {
        String generation = null;
        String cacheKey = null;
        try {
            generation = currentGeneration();
            cacheKey = cacheKey(generation);
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, ROUTE_LIST_TYPE);
            }
        } catch (RuntimeException | JsonProcessingException cacheFailure) {
            log.warn("Hot-route cache read failed; falling back to database: {}", cacheFailure.toString());
            deleteQuietly(cacheKey);
        }

        List<CampRoute> routes = databaseLoader.get();
        if (generation == null || cacheKey == null) {
            return routes;
        }
        try {
            if (!generation.equals(currentGeneration())) {
                return routes;
            }
            Duration ttl = baseTtl.plusSeconds(ThreadLocalRandom.current().nextLong(0, 301));
            redisTemplate.opsForValue().set(
                    cacheKey, objectMapper.writeValueAsString(routes), ttl);
        } catch (RuntimeException | JsonProcessingException cacheFailure) {
            log.warn("Hot-route cache population failed: {}", cacheFailure.toString());
        }
        return routes;
    }

    public void evict() {
        try {
            String oldGeneration = currentGeneration();
            redisTemplate.opsForValue().increment(HOT_ROUTES_GENERATION_KEY);
            redisTemplate.delete(cacheKey(oldGeneration));
        } catch (RuntimeException cacheFailure) {
            // TTL is the fallback if Redis is temporarily unavailable during cache-aside invalidation.
            log.warn("Hot-route cache eviction deferred to TTL: {}", cacheFailure.toString());
        }
    }

    public void evictAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            evict();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evict();
            }
        });
    }

    private String currentGeneration() {
        String value = redisTemplate.opsForValue().get(HOT_ROUTES_GENERATION_KEY);
        return value == null ? "0" : value;
    }

    private String cacheKey(String generation) {
        return HOT_ROUTES_KEY_PREFIX + generation;
    }

    private void deleteQuietly(String key) {
        if (key == null) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException deleteFailure) {
            log.warn("Corrupt hot-route cache deletion failed: {}", deleteFailure.toString());
        }
    }
}
