package com.seckillsystem.repository;

import com.seckillsystem.domain.entity.Product;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheRepositoryBase<Product, Long> {

    public Uni<Integer> decreaseStock(Long productId) {
        return update("#Product.decreaseStock", Parameters.with("id", productId));
    }

    public Uni<Integer> increaseStock(Long productId) {
        return update("#Product.increaseStock", Parameters.with("id", productId));
    }
}
