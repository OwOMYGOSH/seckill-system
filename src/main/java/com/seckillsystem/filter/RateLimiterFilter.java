package com.seckillsystem.filter;

import java.time.Duration;

import org.jboss.resteasy.reactive.server.ServerRequestFilter;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class RateLimiterFilter {

    @Inject
    ReactiveRedisDataSource reactiveRedisDataSource;

    @ServerRequestFilter(preMatching = true)
    public Uni<Response> filter(ContainerRequestContext requestContext) {
        // 針對秒殺 API 限流
        String path = requestContext.getUriInfo().getPath();
        if (!path.contains("/api/seckill")) {
            return Uni.createFrom().nullItem();
        }

        String clientIp = requestContext.getHeaderString("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = "unknown-ip";
        }

        String key = "rate:limit:" + clientIp;
        ReactiveValueCommands<String, Long> valueCommands = reactiveRedisDataSource.value(Long.class);

        return valueCommands.get(key).onItem().transformToUni(count -> {
            if (count != null && count >= 3) {
                return Uni.createFrom().item(
                        Response.status(Response.Status.TOO_MANY_REQUESTS)
                                .entity("操作頻率過高，請稍後再試")
                                .build());
            }

            return valueCommands.incr(key).onItem().transformToUni(newCount -> {
                if (newCount == 1) {
                    return reactiveRedisDataSource.key().expire(key, Duration.ofSeconds(1))
                            .onItem().transform(v -> null);
                }
                return Uni.createFrom().nullItem();
            });
        });
    }
}
