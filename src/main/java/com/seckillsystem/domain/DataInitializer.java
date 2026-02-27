package com.seckillsystem.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.seckillsystem.domain.entity.Product;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DataInitializer {
    
    @Inject
    RedisDataSource redisDataSource;

    @Transactional
    public void onStart(@Observes StartupEvent ev) {
        // 防止重複寫入
        if (Product.count() > 0) {
            return;
        }

        System.out.println("🚀 正在初始化秒殺商品資料...");

        Product p1 = new Product();
        p1.name = "iPhone 17 Pro";
        p1.description = "地表最強手機秒殺價";
        p1.originalPrice = new BigDecimal("36900");
        p1.seckillPrice = new BigDecimal("10"); // 只要 10 塊！
        p1.stockTotal = 100;
        p1.stockAvailable = 10; // 庫存很少，容易測完
        p1.startTime = LocalDateTime.now().minusHours(1); // 一小時前開始
        p1.endTime = LocalDateTime.now().plusHours(2);    // 兩小時後結束
        p1.persist(); // <--- Panache 的存檔方法，非常核心！
        // 3. 建立一個還沒開始的商品 (可以用來測試「活動未開始」的邏輯)
        Product p2 = new Product();
        p2.name = "iPad Pro M3";
        p2.description = "未來生產力工具";
        p2.originalPrice = new BigDecimal("25000");
        p2.seckillPrice = new BigDecimal("12500");
        p2.stockTotal = 50;
        p2.stockAvailable = 50;
        p2.startTime = LocalDateTime.now().plusDays(1); // 明天才開始
        p2.endTime = LocalDateTime.now().plusDays(2);
        p2.persist();
        System.out.println("✅ 資料初始化完成！");

        // 將商品存入 Redis
        ValueCommands<String, Integer> countCommands = redisDataSource.value(Integer.class);

        countCommands.set("seckill:stock:" + p1.id, p1.stockAvailable);
        countCommands.set("seckill:stock:" + p2.id, p2.stockAvailable);

        System.out.println("Redis 商品庫存預熱完成");
    }
}
