package com.groupSWP.centralkitchenplatform.repositories.logistic;

import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {
    List<Shipment> findByStatusAndDeliveredAtBefore(Shipment.ShipmentStatus status, LocalDateTime time);
}