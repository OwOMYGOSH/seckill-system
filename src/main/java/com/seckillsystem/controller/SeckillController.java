package com.seckillsystem.controller;

import java.util.Map;

import com.seckillsystem.domain.entity.Order;
import com.seckillsystem.service.SeckillService;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path("/api/seckill")
public class SeckillController {

    @Inject
    SeckillService seckillService;

    @POST
    public Response executeSeckill(
            @QueryParam("userId") Long userId,
            @QueryParam("productId") Long productId) {
        try {
            Order order = seckillService.processSeckill(userId, productId);
            return Response.ok(order).build();
        } catch (RuntimeException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "發生未知錯誤";
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", errorMsg))
                    .build();
        }
    }
}
