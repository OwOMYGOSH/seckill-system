package com.seckillsystem.controller;

import java.util.List;

import com.seckillsystem.domain.entity.Product;
import com.seckillsystem.repository.ProductRepository;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/api/products")
public class ProductController {

    @Inject
    ProductRepository productRepository;

    @GET
    public Uni<List<Product>> listAll() {
        return productRepository.listAll();
    }

    @GET
    @Path("/{id}")
    public Uni<Product> getById(@PathParam("id") Long id) {
        return productRepository.findById(id);
    }
}
