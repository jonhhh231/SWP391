package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.logistics.AllocateRoutesRequest;
import com.groupSWP.centralkitchenplatform.dto.logistics.RouteAllocationResponse;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.repositories.OrderRepository;
import com.groupSWP.centralkitchenplatform.repositories.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteAllocationService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;

    @Transactional
    public RouteAllocationResponse allocate(AllocateRoutesRequest req) {
        LocalDate today = LocalDate.now();

        int maxOrdersPerTrip = (req != null && req.getMaxOrdersPerTrip() != null)
                ? req.getMaxOrdersPerTrip()
                : 10;

        int maxUrgentPerTrip = (req != null && req.getMaxUrgentPerTrip() != null)
                ? req.getMaxUrgentPerTrip()
                : 2;

        // IMPORTANT: Order entity của bạn không có deliveryDate, nên deliveryDate trong request hiện chưa dùng.
        // Đang lọc theo status + shipment is null (đúng với schema hiện tại).
        List<Order> candidates = orderRepository.findByStatusAndShipmentIsNull(Order.OrderStatus.AGGREGATED);

        List<Order> urgent = candidates.stream()
                .filter(o -> o.getOrderType() == Order.OrderType.URGENT)
                .collect(Collectors.toList());

        List<Order> standard = candidates.stream()
                .filter(o -> o.getOrderType() == Order.OrderType.STANDARD)
                .collect(Collectors.toList());

        int urgentTrips = allocateUrgent(urgent, maxUrgentPerTrip, today);
        int standardTrips = allocateStandard(standard, maxOrdersPerTrip, today);

        return RouteAllocationResponse.builder()
                .urgentOrders(urgent.size())
                .standardOrders(standard.size())
                .urgentTripsCreated(urgentTrips)
                .standardTripsCreated(standardTrips)
                .totalTripsCreated(urgentTrips + standardTrips)
                .build();
    }

    private int allocateUrgent(List<Order> urgentOrders, int maxUrgentPerTrip, LocalDate today) {
        if (urgentOrders.isEmpty()) return 0;

        int tripsCreated = 0;

        for (int i = 0; i < urgentOrders.size(); i += maxUrgentPerTrip) {
            List<Order> batch = urgentOrders.subList(i, Math.min(i + maxUrgentPerTrip, urgentOrders.size()));

            Shipment shipment = new Shipment();
            shipment.setShipmentId(UUID.randomUUID().toString());
            shipment.setDeliveryDate(today.atTime(14, 0)); // urgent: giao chiều trong ngày (bạn chỉnh giờ tùy nghiệp vụ)
            shipment.setStatus(Shipment.ShipmentStatus.NEW);
            shipment.setShipmentType(Shipment.ShipmentType.EXPRESS_ROUTE);

            // FIX: persist shipment trước + flush để đảm bảo nó là "persistent"
            Shipment savedShipment = shipmentRepository.saveAndFlush(shipment);

            for (Order o : batch) {
                o.setShipment(savedShipment);
                o.setStatus(Order.OrderStatus.SHIPPING);
            }

            // FIX: save orders + flush
            orderRepository.saveAll(batch);
            orderRepository.flush();

            tripsCreated++;
        }

        return tripsCreated;
    }

    private int allocateStandard(List<Order> standardOrders, int maxOrdersPerTrip, LocalDate today) {
        if (standardOrders.isEmpty()) return 0;

        Map<String, List<Order>> grouped = standardOrders.stream()
                .collect(Collectors.groupingBy(o -> {
                    if (o.getStore() == null) return "UNKNOWN";
                    return safeStoreKey(o);
                }));

        int tripsCreated = 0;

        for (List<Order> groupOrders : grouped.values()) {
            for (int i = 0; i < groupOrders.size(); i += maxOrdersPerTrip) {
                List<Order> batch = groupOrders.subList(i, Math.min(i + maxOrdersPerTrip, groupOrders.size()));

                Shipment shipment = new Shipment();
                shipment.setShipmentId(UUID.randomUUID().toString());
                shipment.setDeliveryDate(today.plusDays(1).atTime(8, 0)); // standard: sáng ngày mai
                shipment.setStatus(Shipment.ShipmentStatus.NEW);
                shipment.setShipmentType(Shipment.ShipmentType.MAIN_ROUTE);

                // FIX: persist shipment trước + flush
                Shipment savedShipment = shipmentRepository.saveAndFlush(shipment);

                for (Order o : batch) {
                    o.setShipment(savedShipment);
                    o.setStatus(Order.OrderStatus.SHIPPING);
                }

                // FIX: save orders + flush
                orderRepository.saveAll(batch);
                orderRepository.flush();

                tripsCreated++;
            }
        }

        return tripsCreated;
    }

    private String safeStoreKey(Order o) {
        // Bạn đang dùng findByStore_StoreId... => Store có getStoreId() là hợp lý.
        try {
            return (String) o.getStore().getClass().getMethod("getStoreId").invoke(o.getStore());
        } catch (Exception e) {
            return "STORE";
        }
    }
}