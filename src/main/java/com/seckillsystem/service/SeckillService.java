package com.seckillsystem.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Channel;

import com.seckillsystem.domain.OrderStatus;
import com.seckillsystem.domain.entity.Order;
import com.seckillsystem.repository.OrderRepository;
import com.seckillsystem.repository.ProductRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SeckillService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    ReactiveRedisDataSource reactiveRedisDataSource;

    // Kafka 非同步發射器
    @Inject
    @Channel("seckill-orders-out")
    MutinyEmitter<JsonObject> orderEmitter;

    // Prometheus 監控計數器
    @Inject
    MeterRegistry registry;

    private Counter successCounter; // 成功訂單數
    private Counter soldOutCounter; // 售罄訂單數

    public Uni<Order> processSeckill(Long userId, Long productId, String pathToken) {
        // 先驗證路徑
        return checkPath(userId, productId, pathToken).onItem().transformToUni(isValid -> {
            if (!isValid) {
                return Uni.createFrom().failure(new RuntimeException("秒殺路徑驗證失敗"));
            }

            // Redis Value 用於記錄庫存
            ReactiveValueCommands<String, Integer> countCommands = reactiveRedisDataSource.value(Integer.class);

            // Redis Set 用於記錄購買者
            ReactiveSetCommands<String, Long> userSetCommands = reactiveRedisDataSource.set(Long.class);
            String buyersKey = "seckill:buyers:" + productId;

            return userSetCommands.sadd(buyersKey, userId).onItem().transformToUni(count -> {
                if (count == 0) {
                    return Uni.createFrom().failure(new RuntimeException("您已經購買過此商品，每人限購一份"));
                }

                return countCommands.decrby("seckill:stock:" + productId, 1).onItem().transformToUni(remain -> {
                    if (remain < 0) {
                        soldOutCounter.increment();
                        // 處理「售罄」：補回 Redis
                        return Uni.combine().all().unis(
                                userSetCommands.srem(buyersKey, userId),
                                countCommands.incrby("seckill:stock:" + productId, 1)).discardItems()
                                .replaceWith(Uni.createFrom().failure(new RuntimeException("商品已售罄")));
                    }

                    // 這邊是發送訊息至 Kafka
                    JsonObject msg = new JsonObject()
                            .put("userId", userId)
                            .put("productId", productId);

                    return orderEmitter.send(msg)
                            .onItem().transformToUni(v -> {
                                // 成功送訊息給 Kafka 之後，包裝一個假訂單回前端
                                Order pendingOrder = new Order();
                                pendingOrder.userId = userId;
                                pendingOrder.status = OrderStatus.PENDING;

                                successCounter.increment();
                                return Uni.createFrom().item(pendingOrder);
                            })
                            .onFailure().recoverWithUni(err -> {
                                Log.error("送入 Kafka 排隊失敗，執行 Redis 回滾。錯誤原因: " + err.getMessage());
                                return Uni.combine().all().unis(
                                        userSetCommands.srem(buyersKey, userId),
                                        countCommands.incrby("seckill:stock:" + productId, 1)).discardItems()
                                        .replaceWith(Uni.createFrom().failure(new RuntimeException("系統繁忙，請稍後再試")));
                            });
                });
            }).onFailure().transform(e -> {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("ConstraintViolation") || msg.contains("duplicate key"))
                    return new RuntimeException("您已經購買過此商品，每人限購一份");
                return e;
            });
        });
    }

    @PostConstruct
    void init() {
        successCounter = registry.counter("seckill.orders.success", "type", "iphone 17 pro");
        soldOutCounter = registry.counter("seckill.orders.soldout", "type", "iphone 17 pro");
    }

    @WithSession
    public Uni<String> exportSeckillPath(Long userId, Long productId) {
        LocalDateTime now = LocalDateTime.now();

        return productRepository.findById(productId)
                .onItem().ifNull().failWith(new RuntimeException("商品不存在"))
                .onItem().transformToUni(product -> {
                    if (now.isBefore(product.startTime))
                        return Uni.createFrom().failure(new RuntimeException("秒殺尚未開始"));
                    if (now.isAfter(product.endTime))
                        return Uni.createFrom().failure(new RuntimeException("秒殺已經結束"));

                    String token = UUID.randomUUID().toString().replace("-", "");
                    String pathKey = "seckill:path:" + productId + ":" + userId;

                    return reactiveRedisDataSource.value(String.class)
                            .setex(pathKey, 60, token)
                            .replaceWith(token);
                });
    }

    public Uni<Boolean> checkPath(Long userId, Long productId, String pathToken) {
        if (pathToken == null)
            return Uni.createFrom().item(false);

        String pathKey = "seckill:path:" + productId + ":" + userId;
        return reactiveRedisDataSource.value(String.class)
                .get(pathKey)
                .onItem().transform(storedToken -> pathToken.equals(storedToken));
    }
}
