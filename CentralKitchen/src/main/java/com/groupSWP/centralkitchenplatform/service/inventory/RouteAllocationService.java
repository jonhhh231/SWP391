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
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAllocationService {

    private final OrderRepository orderRepository;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentDetailRepository shipmentDetailRepository;

    // --- TÍNH NĂNG CHẠY TỰ ĐỘNG ĐÚNG 14:00 MỖI NGÀY ---
    @Scheduled(cron = "0 0 14 * * ?")
    public void autoAllocateAt2PM() {
        log.info("Bắt đầu tiến trình tự động chia tuyến xe lúc 14:00...");
        allocate(new AllocateRoutesRequest()); // Gọi lại hàm chia tuyến với tham số rỗng
    }

    @Transactional
    public RouteAllocationResponse allocate(AllocateRoutesRequest req) {
        LocalDate today = LocalDate.now();

        int maxOrdersPerTrip = (req != null && req.getMaxOrdersPerTrip() != null) ? req.getMaxOrdersPerTrip() : 10;
        int maxUrgentPerTrip = (req != null && req.getMaxUrgentPerTrip() != null) ? req.getMaxUrgentPerTrip() : 2;

        // [CHỈNH SỬA 1]: Đổi AGGREGATED thành READY_TO_SHIP theo Spec
        List<Order> candidates = orderRepository.findByStatusAndShipmentIsNull(Order.OrderStatus.READY_TO_SHIP);

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
            shipment.setShipmentId("EXP-" + UUID.randomUUID().toString().substring(0,8));

            // [CHỈNH SỬA 2]: Đơn Urgent giao ngay sau 2 tiếng
            shipment.setDeliveryDate(LocalDateTime.now().plusHours(2));
            shipment.setStatus(Shipment.ShipmentStatus.PENDING);

            // [CHỈNH SỬA 3]: Đổi thành xe EXPRESS theo Spec
            shipment.setShipmentType(Shipment.ShipmentType.EXPRESS);

            Shipment savedShipment = shipmentRepository.saveAndFlush(shipment);

            Map<String, ShipmentDetail> detailMap = new HashMap<>();

            for (Order o : batch) {
                o.setShipment(savedShipment);
                o.setStatus(Order.OrderStatus.SHIPPING);

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
            if (!detailMap.isEmpty()) shipmentDetailRepository.saveAll(detailMap.values());
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
                shipment.setShipmentId("MAIN-" + UUID.randomUUID().toString().substring(0,8));

                // [CHỈNH SỬA 4]: Đơn Standard chốt 14h, sáng hôm sau 08:00 đi giao
                shipment.setDeliveryDate(today.plusDays(1).atTime(8, 0));
                shipment.setStatus(Shipment.ShipmentStatus.PENDING);

                // [CHỈNH SỬA 5]: Đổi thành xe MAIN_ROUTE theo Spec
                shipment.setShipmentType(Shipment.ShipmentType.MAIN_ROUTE);

                Shipment savedShipment = shipmentRepository.saveAndFlush(shipment);

                Map<String, ShipmentDetail> detailMap = new HashMap<>();

                for (Order o : batch) {
                    o.setShipment(savedShipment);
                    o.setStatus(Order.OrderStatus.SHIPPING);

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
                if (!detailMap.isEmpty()) shipmentDetailRepository.saveAll(detailMap.values());
                tripsCreated++;
            }
        }
        return tripsCreated;
    }

    private String safeStoreKey(Order o) {
        return (o.getStore() != null && o.getStore().getStoreId() != null)
                ? o.getStore().getStoreId()
                : "UNKNOWN_STORE";
    }
}