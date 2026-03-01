package com.seckillsystem.controller;

import com.seckillsystem.domain.DataInitializer;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api/admin")
public class AdminController {

    @Inject
    DataInitializer dataInitializer;

    @GET
    @Path("/init")
    public Uni<String> init() {
        return dataInitializer.doInit()
                .replaceWith("資料初始化與 Redis 預熱成功！")
                .onFailure().recoverWithItem(e -> "初始化失敗: " + e.getMessage());
    }
}
