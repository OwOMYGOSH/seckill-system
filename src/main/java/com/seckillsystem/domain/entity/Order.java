package com.seckillsystem.domain.entity;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.seckillsystem.domain.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "orders", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "user_id", "product_id" })
})
@NamedQuery(name = "Order.allWithProduct", query = "from Order o left join fetch o.product")
@Schema(description = "訂單實體")
public class Order extends BaseEntity {

    @Schema(title = "訂購者編號")
    @Column(name = "user_id", nullable = false)
    public Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @Schema(title = "關聯商品")
    @JoinColumn(name = "product_id")
    public Product product;
    
    @Schema(title = "下單時的商品名稱")
    @Column(name = "product_name", nullable = false)
    public String productName;
    
    @Schema(title = "成交價格")
    @Column(name = "price", nullable = false)
    public BigDecimal price;
    
    @Enumerated(EnumType.STRING)
    @Schema(title = "訂單狀態")
    @Column(name = "status", nullable = false)
    public OrderStatus status;
}
