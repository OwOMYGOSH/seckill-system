package com.seckillsystem.domain.entity;

import java.time.LocalDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public class BaseEntity extends PanacheEntityBase {

    @Schema(title = "編號")
    @Id
    @GeneratedValue
    public Long id;

    @Schema(title = "創建時間")
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    public LocalDateTime createdAt;

    @Schema(title = "最後更新時間")
    @UpdateTimestamp
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @Version
    @Schema(title = "版本號")
    public Long version;

}
