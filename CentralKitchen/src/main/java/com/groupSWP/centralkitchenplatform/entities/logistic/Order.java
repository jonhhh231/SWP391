package com.groupSWP.centralkitchenplatform.entities.logistic;

import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Order extends BaseEntity {
    @Id
    private String orderId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private BigDecimal totalAmount;
    private String note;


    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    private DeliveryWindow deliveryWindow;

    private BigDecimal surcharge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public enum OrderStatus { NEW, AGGREGATED, COOKING, SHIPPING, DONE, CANCELLED }
    public enum OrderType { STANDARD, URGENT, COMPENSATION }
    public enum DeliveryWindow { MORNING, AFTERNOON }
}