package com.groupSWP.centralkitchenplatform.entities.logistic;

import com.groupSWP.centralkitchenplatform.entities.auth.Store;
import com.groupSWP.centralkitchenplatform.entities.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Column(name = "shipping_start_time")
    private LocalDateTime shippingStartTime;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    public enum OrderStatus {
        NEW,
        PREPARING,        // Bếp đang nấu
        SHIPPING,         // Đang trên xe giao đến cửa hàng
        DELIVERED,        // Xe đã tới nơi (Chờ Store Manager đếm hàng)
        PARTIAL_RECEIVED, // Cửa hàng báo THIẾU HÀNG (Chờ Bếp lên đơn bù)
        DONE,             // Cửa hàng báo NHẬN ĐỦ
        CANCELLED,
        READY_TO_SHIP
    }
    public enum OrderType { STANDARD, URGENT, COMPENSATION }
    public enum DeliveryWindow { MORNING, AFTERNOON }
}