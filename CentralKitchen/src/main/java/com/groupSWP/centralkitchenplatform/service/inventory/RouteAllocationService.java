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

    @Scheduled(cron = "0 0 14 * * ?")
    public void autoAllocateAt2PM() {
        log.info("Bắt đầu tiến trình tự động chia tuyến xe lúc 14:00...");
        allocate(new AllocateRoutesRequest());
    }

    @Transactional
    public RouteAllocationResponse allocate(AllocateRoutesRequest req) {
        LocalDate today = LocalDate.now();
        log.info("--- BẮT ĐẦU CHẠY AI PHÂN BỔ TUYẾN ---");

        int maxOrdersPerTrip = (req != null && req.getMaxOrdersPerTrip() != null) ? req.getMaxOrdersPerTrip() : 10;
        int maxUrgentPerTrip = (req != null && req.getMaxUrgentPerTrip() != null) ? req.getMaxUrgentPerTrip() : 2;

        // Lấy danh sách đơn hàng sẵn sàng xuất kho
        List<Order> candidates = orderRepository.findByStatusAndShipmentIsNull(Order.OrderStatus.SHIPPING);

        // 🔥 QUAN TRỌNG: In ra để debug xem có tìm thấy đơn không
        log.info("AI quét được: {} đơn hàng READY_TO_SHIP", candidates.size());

        if (candidates.isEmpty()) {
            return RouteAllocationResponse.builder().totalTripsCreated(0).build();
        }

        List<Order> urgent = candidates.stream()
                .filter(o -> o.getOrderType() == Order.OrderType.URGENT)
                .collect(Collectors.toList());

        List<Order> standard = candidates.stream()
                .filter(o -> o.getOrderType() == Order.OrderType.STANDARD || o.getOrderType() == Order.OrderType.COMPENSATION)
                .collect(Collectors.toList());

        int urgentTrips = allocateUrgent(urgent, maxUrgentPerTrip);
        int standardTrips = allocateStandard(standard, maxOrdersPerTrip, today);

        return RouteAllocationResponse.builder()
                .urgentOrders(urgent.size())
                .standardOrders(standard.size())
                .urgentTripsCreated(urgentTrips)
                .standardTripsCreated(standardTrips)
                .totalTripsCreated(urgentTrips + standardTrips)
                .build();
    }

    private int allocateUrgent(List<Order> urgentOrders, int maxUrgentPerTrip) {
        if (urgentOrders.isEmpty()) return 0;
        int tripsCreated = 0;

        for (int i = 0; i < urgentOrders.size(); i += maxUrgentPerTrip) {
            List<Order> batch = urgentOrders.subList(i, Math.min(i + maxUrgentPerTrip, urgentOrders.size()));

            Shipment shipment = new Shipment();
            shipment.setShipmentId("EXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            shipment.setDeliveryDate(LocalDateTime.now().plusHours(2));
            shipment.setStatus(Shipment.ShipmentStatus.PENDING);
            shipment.setShipmentType(Shipment.ShipmentType.EXPRESS);

            Shipment savedShipment = shipmentRepository.saveAndFlush(shipment);
            processShipmentDetails(batch, savedShipment);
            tripsCreated++;
        }
        return tripsCreated;
    }

    private int allocateStandard(List<Order> standardOrders, int maxOrdersPerTrip, LocalDate today) {
        if (standardOrders.isEmpty()) return 0;

        // Gom theo cửa hàng
        Map<String, List<Order>> grouped = standardOrders.stream()
                .collect(Collectors.groupingBy(this::safeStoreKey));

        int tripsCreated = 0;
        for (List<Order> groupOrders : grouped.values()) {
            for (int i = 0; i < groupOrders.size(); i += maxOrdersPerTrip) {
                List<Order> batch = groupOrders.subList(i, Math.min(i + maxOrdersPerTrip, groupOrders.size()));

                Shipment shipment = new Shipment();
                shipment.setShipmentId("MAIN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                shipment.setDeliveryDate(today.plusDays(1).atTime(8, 0));
                shipment.setStatus(Shipment.ShipmentStatus.PENDING);
                shipment.setShipmentType(Shipment.ShipmentType.MAIN_ROUTE);

                Shipment savedShipment = shipmentRepository.saveAndFlush(shipment);
                processShipmentDetails(batch, savedShipment);
                tripsCreated++;
            }
        }
        return tripsCreated;
    }

    private void processShipmentDetails(List<Order> batch, Shipment savedShipment) {
        Map<String, ShipmentDetail> detailMap = new HashMap<>();

        for (Order o : batch) {
            // Liên kết đơn hàng với chuyến xe và đổi trạng thái
            o.setShipment(savedShipment);
            o.setStatus(Order.OrderStatus.SHIPPING);

            // Kiểm tra xem đơn hàng có món ăn (items) không
            if (o.getOrderItems() != null && !o.getOrderItems().isEmpty()) {
                log.info("Đang xử lý hàng hóa cho đơn: {}", o.getOrderId());

                for (OrderItem item : o.getOrderItems()) {
                    if (item.getProduct() == null) {
                        log.warn("Đơn hàng {} có item nhưng Product bị NULL!", o.getOrderId());
                        continue;
                    }

                    String productId = item.getProduct().getProductId();
                    // Lấy detail cũ từ map hoặc tạo mới nếu sản phẩm này chưa có trong xe
                    ShipmentDetail detail = detailMap.get(productId);

                    if (detail == null) {
                        detail = ShipmentDetail.builder()
                                .shipment(savedShipment)
                                .product(item.getProduct())
                                .productName(item.getProduct().getProductName())
                                .expectedQuantity(0) // Khởi tạo 0 để tránh lỗi NOT NULL trong DB
                                .receivedQuantity(0) // Khởi tạo 0 cho cột nhận hàng
                                .build();
                    }

                    // Cộng dồn số lượng (Aggregation)
                    detail.setExpectedQuantity(detail.getExpectedQuantity() + item.getQuantity());
                    detailMap.put(productId, detail);
                }
            } else {
                log.warn("CẢNH BÁO: Đơn hàng {} KHÔNG có món ăn nào (orderItems trống)!", o.getOrderId());
            }
        }

        // Bước quan trọng nhất: Ép lưu dữ liệu xuống DB ngay lập tức (Flush)
        try {
            orderRepository.saveAllAndFlush(batch);
            log.info("Đã cập nhật trạng thái SHIPPING cho {} đơn hàng.", batch.size());

            if (!detailMap.isEmpty()) {
                shipmentDetailRepository.saveAllAndFlush(detailMap.values());
                log.info("Đã tạo {} dòng chi tiết hàng hóa (ShipmentDetails) cho xe {}", detailMap.size(), savedShipment.getShipmentId());
            } else {
                log.error("LỖI: Xe {} không có món hàng nào để lưu!", savedShipment.getShipmentId());
            }
        } catch (Exception e) {
            log.error("LỖI KHI LƯU DB: {}", e.getMessage());
            throw e; // Throw để Transaction tự động Rollback nếu có lỗi nghiêm trọng
        }
    }

    private String safeStoreKey(Order o) {
        if (o.getStore() != null && o.getStore().getStoreId() != null) {
            return o.getStore().getStoreId();
        }
        return "UNKNOWN_STORE";
    }
}