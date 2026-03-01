package com.seckillsystem.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.seckillsystem.domain.entity.Product;
import com.seckillsystem.repository.OrderRepository;
import com.seckillsystem.repository.ProductRepository;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DataInitializer {

    @Inject
    ReactiveRedisDataSource reactiveRedisDataSource;

    @Inject
    ProductRepository productRepository;

    @Inject
    OrderRepository orderRepository;

    public Uni<Void> doInit() {
        return Panache.withTransaction(() -> productRepository.deleteAll().onItem().transformToUni(d -> {
            System.out.println("🚀 正在重新初始化反應式商品資料與清空 Redis...");

            Product p1 = new Product();
            p1.name = "iPhone 17 Pro";
            p1.originalPrice = BigDecimal.valueOf(39900);
            p1.seckillPrice = BigDecimal.valueOf(36900);
            p1.stockAvailable = 10;
            p1.stockTotal = 10;
            p1.startTime = LocalDateTime.now();
            p1.endTime = LocalDateTime.now().plusHours(5);
            p1.createdAt = LocalDateTime.now();
            p1.updatedAt = LocalDateTime.now();

            Product p2 = new Product();
            p2.name = "iPhone 17 Pro Max";
            p2.originalPrice = BigDecimal.valueOf(42900);
            p2.seckillPrice = BigDecimal.valueOf(39900);
            p2.stockAvailable = 20;
            p2.stockTotal = 20;
            p2.startTime = LocalDateTime.now().plusDays(1);
            p2.endTime = LocalDateTime.now().plusDays(2);
            p2.createdAt = LocalDateTime.now();
            p2.updatedAt = LocalDateTime.now();

            return Uni.combine().all().unis(productRepository.persist(p1), productRepository.persist(p2)).discardItems()
                    .onItem().transformToUni(result -> {
                        ReactiveValueCommands<String, Integer> commands = reactiveRedisDataSource.value(Integer.class);
                        // 清空 Redis 所有資料，確保實驗環境乾淨
                        return reactiveRedisDataSource.flushall().onItem().transformToUni(f -> 
                            Uni.combine().all().unis(
                                commands.set("seckill:stock:" + p1.id, p1.stockAvailable),
                                commands.set("seckill:stock:" + p2.id, p2.stockAvailable)).discardItems()
                        );
                    });
        }));
    }

}
