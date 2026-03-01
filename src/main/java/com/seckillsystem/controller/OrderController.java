package com.seckillsystem.controller;

import java.util.List;

import com.seckillsystem.domain.entity.Order;
import com.seckillsystem.repository.OrderRepository;

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
@Path("/api/orders")
public class OrderController {

    @Inject
    OrderRepository orderRepository;

    @GET
    public Uni<List<Order>> listAll() {
        return orderRepository.listAllWithProduct();
    }

    @GET
    @Path("/{id}")
    public Uni<Order> getById(@PathParam("id") Long id) {
        return orderRepository.findById(id);
    }
}
