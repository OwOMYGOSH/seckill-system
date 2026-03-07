package com.seckillsystem.service;

import java.time.LocalDateTime;

import com.seckillsystem.domain.OrderStatus;
import com.seckillsystem.domain.entity.Order;
import com.seckillsystem.repository.OrderRepository;
import com.seckillsystem.repository.ProductRepository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class OrderCleanupService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    ReactiveRedisDataSource reactiveRedisDataSource;

    @WithSession
    @Scheduled(every = "1m") // 每分鐘掃描一次
    public Uni<Void> cleanupExpireOrders() {
        // 設定15分鐘前為過期時間
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(15);

        return orderRepository.findExpiredOrders(OrderStatus.SUCCESS, expirationTime).onItem()
                .transformToUni(orders -> {
                    if (orders.isEmpty())
                        return Uni.createFrom().voidItem();
                    System.out.println("🔎 正在掃描...");
                    System.out.println("🕒 發現 " + orders.size() + " 筆過期訂單...");
                    return Multi.createFrom().iterable(orders)
                            .onItem().transformToUniAndConcatenate(this::cancelOrderAndRestoreStock)
                            .collect().asList()
                            .replaceWithVoid();
                });
    }

    private Uni<Void> cancelOrderAndRestoreStock(Order order) {
        return Panache.withTransaction(() -> {
            order.status = OrderStatus.CANCELED;

            return productRepository.increaseStock(order.product.id)
                    .onItem().transformToUni(updatedRows -> {
                        if (updatedRows == 0)
                            return Uni.createFrom().voidItem();

                        String stockKey = "seckill:stock:" + order.product.id;
                        String buyersKey = "seckill:buyers:" + order.product.id;

                        return Uni.combine().all().unis(
                                reactiveRedisDataSource.value(Integer.class).incrby(stockKey, 1),
                                reactiveRedisDataSource.set(Long.class).srem(buyersKey, order.userId))
                                .discardItems();
                    });
        });
    }

}
