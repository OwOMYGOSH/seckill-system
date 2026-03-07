package com.seckillsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.seckillsystem.domain.OrderStatus;
import com.seckillsystem.domain.entity.Order;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<Order, Long> {

    public Uni<List<Order>> listAllWithProduct() {
        return find("#Order.allWithProduct").list();
    }

    public Uni<List<Order>> findExpiredOrders(OrderStatus status, LocalDateTime expirationTime) {
        return find("#Order.findExpired", status, expirationTime).list();
    }
}
