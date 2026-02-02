package com.groupSWP.centralkitchenplatform.entities.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportTicket;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "system_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Thêm Builder để dễ dàng tạo object khi coding
public class SystemUser {

    @Id
    @Column(name = "user_id", length = 20) // Giới hạn độ dài cho mã nhân viên (NV001)
    private String userId;

    @OneToOne(fetch = FetchType.LAZY) // Thêm Lazy để tối ưu hiệu năng
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    @JsonIgnore
    private Account account;

    @Column(name = "full_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    // NVARCHAR giúp MySQL hỗ trợ tiếng Việt có dấu tốt hơn
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    // Lưu dưới dạng VARCHAR(30) trong MySQL: "ADMIN", "KITCHEN_STAFF",...
    private SystemRole role;

    @OneToMany(mappedBy = "coordinator", fetch = FetchType.LAZY)
    private List<Shipment> managedShipments;

    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    private List<ImportTicket> createdTickets;

    // Định nghĩa Enum ngay bên trong hoặc tách file riêng tùy bạn
    public enum SystemRole {
        ADMIN,
        MANAGER,
        COORDINATOR,
        KITCHEN_STAFF
    }
}