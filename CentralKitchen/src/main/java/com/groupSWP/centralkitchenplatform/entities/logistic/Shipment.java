package com.groupSWP.centralkitchenplatform.entities.logistic;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "shipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Shipment {
    @Id
    private String shipmentId;
    private LocalDateTime deliveryDate;
    private String driverName;
    private String vehiclePlate;

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    @Enumerated(EnumType.STRING)
    private ShipmentType shipmentType;

    @ManyToOne
    @JoinColumn(name = "coord_id", columnDefinition = "VARCHAR(20)")
    private SystemUser coordinator;

    @OneToMany(mappedBy = "shipment")
    private List<Order> orders;

    public enum ShipmentStatus {
        PENDING,       // Chờ xử lý
        SHIPPING,      // Đang giao
        DELIVERED,     // Đã giao đủ
        ISSUE_REPORTED,// Đã giao nhưng có báo cáo thiếu/sai hàng
        RESOLVED       // Đã xử lý xong sự cố (đã lên đơn bù)
    }

    public enum ShipmentType {
        STANDARD,      // Đơn giao hàng bình thường
        REPLACEMENT
    }
}
