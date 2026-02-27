package com.seckillsystem.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;


@Entity
@Table(name = "products")
@Schema(description = "秒殺商品實體")
public class Product extends BaseEntity {
    
    @Schema(title = "商品名稱", example = "iPhone 15 Pro")
    @Column(name = "product_name", nullable = false)
    public String name;

    @Schema(title = "商品描述")
    @Column(name = "product_description")
    public String description;
    
    @Schema(title = "原始價格", example = "36900")
    @Column(name = "original_price", nullable = false)
    public BigDecimal originalPrice;
    
    @Schema(title = "秒殺特價", example = "999")
    @Column(name = "seckill_price", nullable = false)
    public BigDecimal seckillPrice;
    
    @Schema(title = "總庫存量")
    @Column(name = "stock_total", nullable = false)
    public Integer stockTotal;
    
    @Schema(title = "剩餘可用庫存")
    @Column(name = "stock_available", nullable = false)
    public Integer stockAvailable;
    
    @Schema(title = "秒殺開始時間")
    @Column(name = "start_time", nullable = false)
    public LocalDateTime startTime;
    
    @Schema(title = "秒殺結束時間")
    @Column(name = "end_time", nullable = false)
    public LocalDateTime endTime;
}
