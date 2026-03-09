package com.seckillsystem.kafka;

import java.time.LocalDateTime;

import org.eclipse.microprofile.reactive.messaging.Incoming;

import com.seckillsystem.domain.OrderStatus;
import com.seckillsystem.domain.entity.Order;
import com.seckillsystem.domain.entity.Product;
import com.seckillsystem.repository.OrderRepository;
import com.seckillsystem.repository.ProductRepository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.logging.Log;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderConsumer {

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    ReactiveRedisDataSource reactiveRedisDataSource;

    @Incoming("seckill-orders-in")
    public Uni<Void> consume(JsonObject msg) {
        LocalDateTime now = LocalDateTime.now();

        // 從 Kafka 的 JSON 訊息中提取資料
        Long userId = msg.getLong("userId");
        Long productId = msg.getLong("productId");

        // 準備 Redis 的連線對象（萬一出錯要 Rollback 用）
        ReactiveValueCommands<String, Integer> countCommands = reactiveRedisDataSource.value(Integer.class);
        ReactiveSetCommands<String, Long> userSetCommands = reactiveRedisDataSource.set(Long.class);
        String buyersKey = "seckill:buyers:" + productId;

        // 寫入資料庫
        return Panache.withTransaction(() -> productRepository.findById(productId)
                .onItem().ifNull().failWith(new RuntimeException("商品不存在"))
                .onItem().transformToUni(product -> {
                    if (now.isBefore(product.startTime))
                        return Uni.createFrom().failure(new RuntimeException("秒殺尚未開始"));
                    if (now.isAfter(product.endTime))
                        return Uni.createFrom().failure(new RuntimeException("秒殺已經結束"));

                    // 在資料庫中扣減這 1 個庫存
                    return productRepository.decreaseStock(productId)
                            .onItem().transformToUni(updatedRows -> {
                                if (updatedRows == 0) {
                                    return Uni.createFrom().failure(new RuntimeException("商品已售罄 (DB)"));
                                }
                                // 成功扣減資料庫，才建立訂單
                                return createOrder(userId, product);
                            });
                }))
                .onFailure(e -> !e.getMessage().equals("商品已售罄") &&
                        !e.getMessage().equals("商品已售罄 (DB)"))
                .call(err -> {
                    // ❌ 只有在「DB 寫盤異常 / 網路中斷」等不可預期錯誤時，才補回 Redis
                    Log.error("❌ 後台寫入異常，執行 Redis 庫存補回。錯誤原因: " + err.getMessage());
                    return Uni.combine().all().unis(
                            userSetCommands.srem(buyersKey, userId), // 移除黑名單
                            countCommands.incrby("seckill:stock:" + productId, 1)) // 加回 1 個庫存
                            .discardItems();
                })
                .onFailure().recoverWithNull() // 吞掉錯誤，避免讓 Kafka 的 offset 卡住無限重試
                .replaceWithVoid();
    }

    // 建立訂單，原本在 SeckillService 的邏輯移到 Consumer 來做
    private Uni<Order> createOrder(Long userId, Product product) {
        Order order = new Order();
        order.userId = userId;
        order.product = product;
        order.productName = product.name;
        order.price = product.seckillPrice;
        order.status = OrderStatus.SUCCESS;

        return orderRepository.persist(order)
                .invoke(() -> Log.info("Order successfully created in background via Kafka! UserId: " + userId))
                .replaceWith(order);
    }
}
