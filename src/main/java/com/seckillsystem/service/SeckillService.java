package com.seckillsystem.service;

import java.time.LocalDateTime;

import com.seckillsystem.domain.OrderStatus;
import com.seckillsystem.domain.entity.Order;
import com.seckillsystem.domain.entity.Product;
import com.seckillsystem.repository.OrderRepository;
import com.seckillsystem.repository.ProductRepository;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SeckillService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    ProductRepository productRepository;

    @Inject
    RedisDataSource redisDataSource;

    @Transactional
    public Order processSeckill(Long userId, Long productId) {
        ValueCommands<String, Integer> countCommands = redisDataSource.value(Integer.class);
        long remain = countCommands.decrby("seckill:stock:" + productId, 1);
        if (remain < 0) {
            countCommands.incrby("seckill:stock:" + productId, 1); // 將商品補回至 0
            throw new RuntimeException("Redis 檢查：商品已售罄");
        }

        try {
            Product product = productRepository.findById(productId);
            if (product == null) {
                throw new RuntimeException("商品不存在");
            }

            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(product.startTime) || now.isAfter(product.endTime)) {
                throw new RuntimeException("秒殺尚未開始或已經結束");
            }

            if (product.stockAvailable <= 0) {
                throw new RuntimeException("商品已售罄");
            }

            product.stockAvailable -= 1;
            return createOrder(userId, product);

        } catch (OptimisticLockException e) {
            throw new RuntimeException("目前搶購太火熱了!請稍後再試~");
        } catch (Exception e) {
            String fullMessage = e.getMessage() != null ? e.getMessage() : "";
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                fullMessage += " " + e.getCause().getMessage();
            }
            // 檢查是否為重複購買
            if (fullMessage.contains("ConstraintViolation") || fullMessage.contains("duplicate key")) {
                throw new RuntimeException("您已經購買過此商品，每人限購一份");
            }

            // 如果是其他錯誤，就把原訊息往外丟，避免丟失原始報錯
            throw new RuntimeException("秒殺執行失敗: " + (e.getMessage() != null ? e.getMessage() : "未知原因"));
        }
    }

    // 建立訂單
    private Order createOrder(Long userId, Product product) {
        Order order = new Order();
        order.userId = userId;
        order.product = product;
        order.productName = product.name;
        order.price = product.seckillPrice;
        order.status = OrderStatus.SUCCESS;

        orderRepository.persist(order);
        orderRepository.flush();

        return order;
    }
}
