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

    // 1. Giữ lại bộ này (Có EnumType.STRING là chuẩn bài)
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    private BigDecimal totalAmount;
    private String note;

    // 2. Giữ lại bộ này
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    private DeliveryWindow deliveryWindow;

    @Column(name = "delivery_date")
    private java.time.LocalDate deliveryDate;

    private BigDecimal surcharge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;


    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public enum OrderStatus {
        NEW,
        AGGREGATED,
        COOKING,
        READY_TO_SHIP,
        SHIPPING,
        DELIVERED,        // MỚI: Tài xế (Coordinator) giao tới nơi, chờ Store Manager đếm hàng
        PARTIAL_RECEIVED, // MỚI: Store Manager xác nhận giao thiếu hàng (Tùy chọn, để vứt sang tab Khiếu nại)
        DONE,
        CANCELLED
    }
    public enum OrderType { STANDARD, URGENT, COMPENSATION }
    public enum DeliveryWindow { MORNING, AFTERNOON }
}