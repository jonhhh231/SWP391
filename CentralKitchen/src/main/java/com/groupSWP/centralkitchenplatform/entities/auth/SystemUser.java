package com.groupSWP.centralkitchenplatform.entities.auth;

import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportTicket;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "system_users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SystemUser {
    @Id
    private String userId;
    private String username;
    private String password;
    private String fullName;

    @Enumerated(EnumType.STRING)
    private SystemRole role; // Enum definition needed

    @OneToMany(mappedBy = "coordinator")
    private List<Shipment> managedShipments;

    @OneToMany(mappedBy = "createdBy")
    private List<ImportTicket> createdTickets;

    public enum SystemRole { ADMIN, MANAGER, COORDINATOR, KITCHEN_STAFF }
}