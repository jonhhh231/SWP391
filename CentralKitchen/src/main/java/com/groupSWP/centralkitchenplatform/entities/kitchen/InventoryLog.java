package com.groupSWP.centralkitchenplatform.entities.kitchen;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long importItemId; // Lô hàng nào bị trừ?

    @Column(nullable = false)
    private String ingredientId; // Nguyên liệu gì?

    @Column(nullable = false)
    private BigDecimal quantityDeducted; // Bị trừ bao nhiêu?

    private String referenceCode; // Mã mẻ nấu (Ví dụ: RUN-1234) để truy xuất nguồn gốc

    private String note; // Lý do trừ (Ví dụ: "Trừ kho tự động theo nguyên tắc FIFO")

    @Column(nullable = false)
    private LocalDateTime createdAt; // Trừ lúc nào?
}