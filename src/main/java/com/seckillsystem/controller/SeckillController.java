package com.seckillsystem.controller;

import java.util.Map;

import com.seckillsystem.service.SeckillService;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
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

    @GET
    @Path("/get-path")
    public Uni<Response> getPathToken(
            @QueryParam("userId") Long userId,
            @QueryParam("productId") Long productId) {
        return seckillService.exportSeckillPath(userId, productId)
                .onItem().transform(pathToken -> Response.ok(Map.of("path", pathToken)).build())
                .onFailure().recoverWithItem(e -> {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "發生未知錯誤";
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", errorMsg))
                            .build();
                });
    }

    @POST
    public Uni<Response> executeSeckill(
            @QueryParam("userId") Long userId,
            @QueryParam("productId") Long productId,
            @QueryParam("pathToken") String pathToken) {
        return seckillService.processSeckill(userId, productId, pathToken).onItem()
                .transform(order -> Response.ok(order).build())
                .onFailure().recoverWithItem(e -> {
                    String errorMsg = e.getMessage() != null ? e.getMessage() : "發生未知錯誤";
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Map.of("error", errorMsg))
                            .build();
                });
    }
}
