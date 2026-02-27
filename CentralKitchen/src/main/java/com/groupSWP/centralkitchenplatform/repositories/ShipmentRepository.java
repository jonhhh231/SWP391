package com.groupSWP.centralkitchenplatform.repositories;

import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentRepository extends JpaRepository<Shipment, String> {}