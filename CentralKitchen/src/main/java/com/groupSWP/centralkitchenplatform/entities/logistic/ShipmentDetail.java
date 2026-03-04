package com.groupSWP.centralkitchenplatform.entities.logistic;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "shipment_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShipmentDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    private Shipment shipment;

    private String productId; // ID của món ăn hoặc nguyên liệu
    private String productName;

    private int expectedQuantity; // Số lượng xuất kho đi (Bếp gửi đi 10)
    private int receivedQuantity; // Số lượng thực nhận (Cửa hàng nhận 8)

    private String issueNote; // Ghi chú (VD: "2 hộp bị đổ", "Thiếu 2 hộp")

    // Hàm phụ trợ tính số lượng thiếu
    public int getMissingQuantity() {
        return Math.max(0, expectedQuantity - receivedQuantity);
    }
}