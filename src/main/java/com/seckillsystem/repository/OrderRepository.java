package com.seckillsystem.repository;

import java.util.List;

import com.seckillsystem.domain.entity.Order;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {

    public Order findById(Long id) {
        return Order.findById(id);
    }

    public List<Order> listAll() {
        return Order.listAll();
    }
}
