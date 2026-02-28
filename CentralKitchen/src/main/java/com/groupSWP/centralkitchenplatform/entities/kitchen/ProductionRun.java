package com.groupSWP.centralkitchenplatform.entities.kitchen;

import com.groupSWP.centralkitchenplatform.entities.common.BaseEntity;
import com.groupSWP.centralkitchenplatform.entities.product.Product;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "production_runs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductionRun extends BaseEntity {
    @Id
    private String runId;

    private BigDecimal plannedQty;
    private BigDecimal actualQty;
    private BigDecimal wasteQty;

    // 🔥 NEW: Tổng chi phí nguyên liệu thực tế (Giá vốn)
    private BigDecimal totalCostAtProduction;

    private LocalDateTime productionDate;
    private String batchCode;

    // Bổ sung cho khớp với thiết kế DBML
    private LocalDate expiryDate;
    private String note;

    @Enumerated(EnumType.STRING)
    private ProductionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    public enum ProductionStatus { PLANNED, COOKING, COMPLETED, CANCELLED }
}