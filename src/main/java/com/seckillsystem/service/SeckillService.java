package com.seckillsystem.service;

import java.time.LocalDateTime;

import com.seckillsystem.domain.OrderStatus;
import com.seckillsystem.domain.entity.Order;
import com.seckillsystem.domain.entity.Product;
import com.seckillsystem.repository.OrderRepository;
import com.seckillsystem.repository.ProductRepository;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
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

    @Inject
    MeterRegistry registry;

    private Counter successCounter;
    private Counter soldOutCounter;

    public Uni<Order> processSeckill(Long userId, Long productId) {
        LocalDateTime now = LocalDateTime.now();

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

                return Panache.withTransaction(() -> productRepository.findById(productId)
                        .onItem().ifNull().failWith(new RuntimeException("商品不存在"))
                        .onItem().transformToUni(product -> {
                            if (now.isBefore(product.startTime))
                                return Uni.createFrom().failure(new RuntimeException("秒殺尚未開始"));
                            if (now.isAfter(product.endTime))
                                return Uni.createFrom().failure(new RuntimeException("秒殺已經結束"));

                            return productRepository.decreaseStock(productId)
                                    .onItem().transformToUni(updatedRows -> {
                                        if (updatedRows == 0) {
                                            return Uni.createFrom().failure(new RuntimeException("商品已售罄 (DB)"));
                                        }
                                        // 成功扣減資料庫，才建立訂單
                                        return createOrder(userId, product);
                                    });
                        }))
                        .onFailure(e -> !e.getMessage().equals("商品已售罄") && !e.getMessage().equals("商品已售罄 (DB)"))
                        .call(err -> {
                            // 只有在非「預計中的售罄」出錯時(如DB掛掉)，才需要補回 Redis
                            Log.error("❌ 系統異常或資料庫衝突，執行 Redis 回滾。錯誤原因: " + err.getMessage());
                            return Uni.combine().all().unis(
                                    userSetCommands.srem(buyersKey, userId),
                                    countCommands.incrby("seckill:stock:" + productId, 1)).discardItems();
                        });
            });
        }).onFailure().transform(e -> {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("ConstraintViolation") || msg.contains("duplicate key"))
                return new RuntimeException("您已經購買過此商品，每人限購一份");
            return e;
        });
    }

    // 建立訂單
    private Uni<Order> createOrder(Long userId, Product product) {
        Order order = new Order();
        order.userId = userId;
        order.product = product;
        order.productName = product.name;
        order.price = product.seckillPrice;
        order.status = OrderStatus.SUCCESS;

        return orderRepository.persist(order)
                .invoke(() -> successCounter.increment())
                .replaceWith(order);
    }

    @PostConstruct
    void init() {
        successCounter = registry.counter("seckill.orders.success", "type", "iphone 17 pro");
        soldOutCounter = registry.counter("seckill.orders.soldout", "type", "iphone 17 pro");
    }
}
