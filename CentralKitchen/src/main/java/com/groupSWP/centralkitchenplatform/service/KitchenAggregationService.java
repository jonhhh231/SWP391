package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.kitchen.KitchenAggregationResponse;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Formula;
import com.groupSWP.centralkitchenplatform.entities.logistic.Order;
import com.groupSWP.centralkitchenplatform.entities.logistic.OrderItem;
import com.groupSWP.centralkitchenplatform.repositories.FormulaRepository;
import com.groupSWP.centralkitchenplatform.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KitchenAggregationService {

    private final OrderRepository orderRepo;
    private final FormulaRepository formulaRepo;

    public KitchenAggregationResponse aggregate(
            LocalDate date,
            Order.DeliveryWindow deliveryWindow,
            Order.OrderStatus status,
            boolean includeIngredients
    ) {
        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate target = (date != null) ? date : LocalDate.now(zone);

        LocalDateTime from = target.atStartOfDay();
        LocalDateTime to = target.plusDays(1).atStartOfDay();

        // default status NEW (PENDING)
        Order.OrderStatus st = (status != null) ? status : Order.OrderStatus.NEW;

        List<Order> orders;

        // Nếu có deliveryWindow → áp rule orderType theo ca
        if (deliveryWindow != null) {
            List<Order.OrderType> types =
                    (deliveryWindow == Order.DeliveryWindow.MORNING)
                            ? List.of(Order.OrderType.STANDARD)
                            : List.of(Order.OrderType.URGENT, Order.OrderType.COMPENSATION);

            orders = orderRepo.findByStatusAndDeliveryWindowAndOrderTypeInAndCreatedAtBetween(
                    st, deliveryWindow, types, from, to
            );
        } else {
            orders = orderRepo.findByStatusAndCreatedAtBetween(st, from, to);
        }

        // 1) Aggregate product qty
        Map<String, KitchenAggregationResponse.ProductAgg> productMap = new LinkedHashMap<>();

        for (Order o : orders) {
            for (OrderItem oi : o.getOrderItems()) {
                String pid = oi.getProduct().getProductId();
                String pname = oi.getProduct().getProductName();
                int qty = oi.getQuantity();

                productMap.compute(pid, (k, v) -> {
                    if (v == null) {
                        v = new KitchenAggregationResponse.ProductAgg();
                        v.setProductId(pid);
                        v.setProductName(pname);
                        v.setTotalQty(0);
                    }
                    v.setTotalQty(v.getTotalQty() + qty);
                    return v;
                });
            }
        }

        List<KitchenAggregationResponse.ProductAgg> products = new ArrayList<>(productMap.values());

        KitchenAggregationResponse res = new KitchenAggregationResponse();
        res.setDate(target.toString());
        res.setDeliveryWindow(deliveryWindow != null ? deliveryWindow.name() : null);
        res.setStatus(st.name());
        res.setProducts(products);

        // 2) Optional: Aggregate ingredients needed by BOM
        if (!includeIngredients) {
            res.setIngredients(null);
            return res;
        }

        if (products.isEmpty()) {
            res.setIngredients(List.of());
            return res;
        }

        // productId -> totalQty
        Map<String, Integer> qtyByProduct = products.stream()
                .collect(Collectors.toMap(KitchenAggregationResponse.ProductAgg::getProductId,
                        KitchenAggregationResponse.ProductAgg::getTotalQty));

        List<String> productIds = new ArrayList<>(qtyByProduct.keySet());

        // lấy BOM
        List<Formula> formulas = formulaRepo.findByProductProductIdIn(productIds);

        // ingredientId -> totalNeeded
        Map<String, KitchenAggregationResponse.IngredientAgg> ingMap = new LinkedHashMap<>();

        for (Formula f : formulas) {
            String pid = f.getProduct().getProductId();
            Integer totalQty = qtyByProduct.get(pid);
            if (totalQty == null) continue;

            String ingId = f.getIngredient().getIngredientId();
            String ingName = f.getIngredient().getName();
            String unit = f.getIngredient().getUnit();

            // amount_needed * totalQty
            BigDecimal needed = f.getAmountNeeded().multiply(BigDecimal.valueOf(totalQty));

            ingMap.compute(ingId, (k, v) -> {
                if (v == null) {
                    v = new KitchenAggregationResponse.IngredientAgg();
                    v.setIngredientId(ingId);
                    v.setIngredientName(ingName);
                    v.setUnit(unit);
                    v.setTotalNeeded(BigDecimal.ZERO);
                }
                v.setTotalNeeded(v.getTotalNeeded().add(needed));
                return v;
            });
        }

        res.setIngredients(new ArrayList<>(ingMap.values()));
        return res;
    }
}