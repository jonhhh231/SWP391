package com.groupSWP.centralkitchenplatform.entities.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.ZoneId;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Sự kiện chạy NGAY TRƯỚC KHI lệnh INSERT được thực thi xuống DB
    @PrePersist
    protected void onCreate() {
        // Ép cứng lấy múi giờ Việt Nam (GMT+7)
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
        this.createdAt = LocalDateTime.now(zoneId);
        this.updatedAt = LocalDateTime.now(zoneId);
    }

    // Sự kiện chạy NGAY TRƯỚC KHI lệnh UPDATE được thực thi xuống DB
    @PreUpdate
    protected void onUpdate() {
        // Ép cứng lấy múi giờ Việt Nam (GMT+7)
        this.updatedAt = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }
}