package com.seckillsystem.repository;

import java.util.List;

import com.seckillsystem.domain.entity.Product;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheRepository<Product> {

    public Product findById(Long id) {
        return Product.findById(id);
    }

    public List<Product> listAll() {
        return Product.listAll();
    }
}
