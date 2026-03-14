package com.groupSWP.centralkitchenplatform.repositories.order;

import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.groupSWP.centralkitchenplatform.dto.analytics.ProductReportDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    // Lấy danh sách đơn hàng của 1 cửa hàng cụ thể, sắp xếp cái nào mới đặt lên đầu
    // LƯU Ý: Nếu khóa chính của Store Sếp đặt là "id" thì đổi thành findByStore_Id
    List<Order> findByStore_StoreIdOrderByCreatedAtDesc(String storeId);

    List<Order> findByStatus(Order.OrderStatus status);

    List<Order> findByStore_StoreId(String storeId);// Lấy tất cả đơn hàng của 1 cửa hàng

    List<Order> findByCreatedAtGreaterThanEqualAndStatusNot(LocalDateTime startTime, Order.OrderStatus status);


    boolean existsByShipment_ShipmentIdAndStatusNot(String shipmentId, Order.OrderStatus status);


    @EntityGraph(attributePaths = {"orderItems", "orderItems.product", "store"})
    List<Order> findByStatusAndShipmentIsNull(Order.OrderStatus status);

    // Tìm các đơn hàng đang giao và thời gian bắt đầu giao trước một mốc thời gian cụ thể
    List<Order> findByStatusAndShippingStartTimeBefore(Order.OrderStatus status, LocalDateTime time);

    // 1. Lấy tất cả đơn hàng ĐÃ XUẤT KHO trong khoảng thời gian (Chỉ tính hàng đã lên xe hoặc đã giao)
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
            "AND o.status IN ('SHIPPING', 'DELIVERED', 'PARTIAL_RECEIVED', 'DONE')")
    List<Order> findValidOrdersBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 2. TÌM TOP MÓN XUẤT KHO NHIỀU NHẤT (Chỉ tính những đơn đã thực sự rời khỏi bếp)
    @Query("SELECT new com.groupSWP.centralkitchenplatform.dto.analytics.ProductReportDto(" +
            "p.productId, p.productName, SUM(oi.quantity), SUM(oi.quantity * oi.priceAtOrder)) " +
            "FROM OrderItem oi JOIN oi.order o JOIN oi.product p " +
            "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
            "AND o.status IN ('SHIPPING', 'DELIVERED', 'PARTIAL_RECEIVED', 'DONE') " +
            "GROUP BY p.productId, p.productName " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<ProductReportDto> findTopExportedProducts(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable);
}