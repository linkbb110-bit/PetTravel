package com.nfu.pettravel.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nfu.pettravel.entity.CampRoute;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RouteCacheServiceTest {

    @Test
    void shouldDeleteCorruptCacheAndPopulateFromDatabaseWithTtl() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(RouteCacheService.HOT_ROUTES_KEY)).thenReturn("not-json");
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RouteCacheService cache = new RouteCacheService(redis, objectMapper, Duration.ofMinutes(30));
        CampRoute route = new CampRoute();
        route.setId(8L);

        List<CampRoute> routes = cache.getHotRoutes(() -> List.of(route));

        assertThat(routes).extracting(CampRoute::getId).containsExactly(8L);
        verify(redis).delete(RouteCacheService.HOT_ROUTES_KEY);
        verify(values).set(eq(RouteCacheService.HOT_ROUTES_KEY), anyString(), any(Duration.class));
    }

    @Test
    void shouldNotRepopulateStaleValueAfterConcurrentInvalidation() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(RouteCacheService.HOT_ROUTES_GENERATION_KEY))
                .thenReturn("0", "1");
        when(values.get(RouteCacheService.HOT_ROUTES_KEY)).thenReturn(null);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RouteCacheService cache = new RouteCacheService(redis, objectMapper, Duration.ofMinutes(30));

        cache.getHotRoutes(List::of);

        verify(values, never()).set(eq(RouteCacheService.HOT_ROUTES_KEY), anyString(), any(Duration.class));
    }
}
