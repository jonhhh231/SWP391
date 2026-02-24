package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.ConfirmReceipt.ConfirmReceiptRequest;
import com.groupSWP.centralkitchenplatform.dto.ConfirmReceipt.ConfirmReceiptResponse;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.entities.product.Stock;
import com.groupSWP.centralkitchenplatform.entities.product.StockKey;
import com.groupSWP.centralkitchenplatform.repositories.OrderItemRepository;
import com.groupSWP.centralkitchenplatform.repositories.OrderRepository;
import com.groupSWP.centralkitchenplatform.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReceiptService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final StockRepository stockRepo;

    @Transactional
    public ConfirmReceiptResponse confirmReceipt(String orderId, String storeId, ConfirmReceiptRequest req) {

        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy order: " + orderId));

        // ✅ StoreId getter: sửa theo Store entity của bạn
        String orderStoreId = order.getStore().getStoreId();

        if (!orderStoreId.equals(storeId)) {
            throw new RuntimeException("Order không thuộc cửa hàng này");
        }

        if (order.getStatus() == Order.OrderStatus.DONE) {
            throw new RuntimeException("Đơn đã được xác nhận trước đó");
        }



        if (order.getStatus() != Order.OrderStatus.SHIPPING) {
            throw new RuntimeException("Chỉ xác nhận khi đơn ở trạng thái SHIPPING");
        }

        List<OrderItem> orderItems = orderItemRepo.findByIdOrderId(orderId);
        if (orderItems.isEmpty()) throw new RuntimeException("Đơn hàng không có sản phẩm");

        // map actualReceived
        Map<String, Integer> actualMap = new HashMap<>();
        if (req != null && req.getItems() != null) {
            for (ConfirmReceiptRequest.Item it : req.getItems()) {
                if (it.getProductId() == null) continue;
                if (it.getActualReceived() == null) {
                    throw new RuntimeException("Số lượng nhận thực tế không được null");
                }
                actualMap.put(it.getProductId(), it.getActualReceived());
            }
        }

        boolean shortage = false;
        List<ConfirmReceiptResponse.ItemResult> results = new ArrayList<>();

        for (OrderItem oi : orderItems) {
            String productId = oi.getProduct().getProductId();
            int orderedQty = oi.getQuantity();
            int receivedQty = actualMap.getOrDefault(productId, orderedQty);


            if (receivedQty > orderedQty) {
                throw new RuntimeException("Số lượng thực nhận không được lớn hơn số lượng đặt");
            }

            if (receivedQty < 0) throw new RuntimeException("Số lượng nhận thực tế không hợp lệ");
            if (receivedQty < orderedQty) shortage = true;

            // upsert stock by (store_id, product_id)
            StockKey key = new StockKey(storeId, productId);
            Stock stock = stockRepo.findById(key).orElseGet(() -> {
                Stock s = new Stock();
                s.setId(key);
                s.setStore(order.getStore());
                s.setProduct(oi.getProduct());
                s.setQuantity(0);
                return s;
            });

            stock.setQuantity(stock.getQuantity() + receivedQty);
            stock.setLastUpdated(LocalDateTime.now());
            stockRepo.save(stock);

            ConfirmReceiptResponse.ItemResult r = new ConfirmReceiptResponse.ItemResult();
            r.setProductId(productId);
            r.setOrderedQty(orderedQty);
            r.setReceivedQty(receivedQty);
            results.add(r);
        }

        String note = (req != null) ? req.getNote() : null;
        if (shortage && (note == null || note.trim().isEmpty())) {
            throw new RuntimeException("Thiếu hàng: bắt buộc nhập note/lý do");
        }

        order.setStatus(Order.OrderStatus.DONE);
        order.setNote(note);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepo.save(order);



        ConfirmReceiptResponse res = new ConfirmReceiptResponse();
        res.setOrderId(order.getOrderId());
        res.setStatus(order.getStatus().name());
        res.setNote(order.getNote());
        res.setItems(results);
        return res;
    }
}