package com.groupSWP.centralkitchenplatform.entities.logistic;

import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "shipments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coord_id")
    private SystemUser coordinator;

    @OneToMany(mappedBy = "shipment")
    private List<Order> orders;

    public enum ShipmentStatus { NEW, LOADING, DELIVERING, COMPLETED, CANCELLED }
    public enum ShipmentType { MAIN_ROUTE, EXPRESS_ROUTE }
}
