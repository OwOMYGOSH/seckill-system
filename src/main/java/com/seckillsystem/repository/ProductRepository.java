package com.seckillsystem.repository;

import com.seckillsystem.domain.entity.Product;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductRepository implements PanacheRepositoryBase<Product, Long> {

}
