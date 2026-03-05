package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.AllocateRoutesRequest;
import com.groupSWP.centralkitchenplatform.dto.logistics.RouteAllocationResponse;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.logistic.Shipment;
import com.groupSWP.centralkitchenplatform.entities.logistic.ShipmentDetail;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentDetailRepository;
import com.groupSWP.centralkitchenplatform.repositories.order.OrderRepository;
import com.groupSWP.centralkitchenplatform.repositories.logistic.ShipmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteAllocationService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    // [THÊM MỚI 1]
    private final ShipmentDetailRepository shipmentDetailRepository;

    @Transactional
    public RouteAllocationResponse allocate(AllocateRoutesRequest req) {
        LocalDate today = LocalDate.now();

        int maxOrdersPerTrip = (req != null && req.getMaxOrdersPerTrip() != null) ? req.getMaxOrdersPerTrip() : 10;
        int maxUrgentPerTrip = (req != null && req.getMaxUrgentPerTrip() != null) ? req.getMaxUrgentPerTrip() : 2;

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
            shipment.setDeliveryDate(today.atTime(14, 0));
            shipment.setStatus(Shipment.ShipmentStatus.PENDING);
            shipment.setShipmentType(Shipment.ShipmentType.STANDARD); // Có thể đổi thành URGENT nếu Entity bạn có

            Shipment savedShipment = shipmentRepository.saveAndFlush(shipment);

            // [THÊM MỚI 2] Tạo Phiếu xuất kho tổng cho chuyến xe này
            Map<String, ShipmentDetail> detailMap = new HashMap<>();

            for (Order o : batch) {
                o.setShipment(savedShipment);
                o.setStatus(Order.OrderStatus.SHIPPING); // Hoặc ALLOCATED

                // [THÊM MỚI 3] Lặp qua các món ăn trong đơn để cộng dồn
                if (o.getOrderItems() != null) {
                    for (OrderItem item : o.getOrderItems()) {
                        String productId = item.getProduct().getProductId();

                        // Nếu món này chưa có trên xe -> Tạo dòng mới. Có rồi -> Lấy ra cộng dồn.
                        ShipmentDetail detail = detailMap.getOrDefault(productId, ShipmentDetail.builder()
                                .shipment(savedShipment)
                                .product(item.getProduct())
                                .productName(item.getProduct().getProductName())
                                .expectedQuantity(0)
                                .receivedQuantity(0)
                                .build());

                        // Cộng dồn số lượng
                        detail.setExpectedQuantity(detail.getExpectedQuantity() + item.getQuantity());
                        detailMap.put(productId, detail);
                    }
                }
            }

            orderRepository.saveAll(batch);

            // [THÊM MỚI 4] Lưu toàn bộ chi tiết xuất kho xuống DB
            if (!detailMap.isEmpty()) {
                shipmentDetailRepository.saveAll(detailMap.values());
            }

            tripsCreated++;
        }

        return tripsCreated;
    }

    private int allocateStandard(List<Order> standardOrders, int maxOrdersPerTrip, LocalDate today) {
        if (standardOrders.isEmpty()) return 0;

        Map<String, List<Order>> grouped = standardOrders.stream()
                .collect(Collectors.groupingBy(o -> safeStoreKey(o)));

        int tripsCreated = 0;

        for (List<Order> groupOrders : grouped.values()) {
            for (int i = 0; i < groupOrders.size(); i += maxOrdersPerTrip) {
                List<Order> batch = groupOrders.subList(i, Math.min(i + maxOrdersPerTrip, groupOrders.size()));

                Shipment shipment = new Shipment();
                shipment.setShipmentId(UUID.randomUUID().toString());
                shipment.setDeliveryDate(today.atTime(22, 0));
                shipment.setStatus(Shipment.ShipmentStatus.PENDING);
                shipment.setShipmentType(Shipment.ShipmentType.STANDARD);

                Shipment savedShipment = shipmentRepository.saveAndFlush(shipment);

                // [THÊM MỚI 2] Tạo Phiếu xuất kho tổng cho chuyến xe
                Map<String, ShipmentDetail> detailMap = new HashMap<>();

                for (Order o : batch) {
                    o.setShipment(savedShipment);
                    o.setStatus(Order.OrderStatus.SHIPPING);

                    // [THÊM MỚI 3] Cộng dồn món ăn
                    if (o.getOrderItems() != null) {
                        for (OrderItem item : o.getOrderItems()) {
                            String productId = item.getProduct().getProductId();

                            ShipmentDetail detail = detailMap.getOrDefault(productId, ShipmentDetail.builder()
                                    .shipment(savedShipment)
                                    .product(item.getProduct())
                                    .productName(item.getProduct().getProductName())
                                    .expectedQuantity(0)
                                    .receivedQuantity(0)
                                    .build());

                            detail.setExpectedQuantity(detail.getExpectedQuantity() + item.getQuantity());
                            detailMap.put(productId, detail);
                        }
                    }
                }

                orderRepository.saveAll(batch);

                // [THÊM MỚI 4] Lưu chi tiết xuất kho
                if (!detailMap.isEmpty()) {
                    shipmentDetailRepository.saveAll(detailMap.values());
                }

                tripsCreated++;
            }
        }

        return tripsCreated;
    }

    // [THÊM MỚI 5] Sửa lại hàm an toàn, bỏ Reflection
    private String safeStoreKey(Order o) {
        return (o.getStore() != null && o.getStore().getStoreId() != null)
                ? o.getStore().getStoreId()
                : "UNKNOWN_STORE";
    }
}