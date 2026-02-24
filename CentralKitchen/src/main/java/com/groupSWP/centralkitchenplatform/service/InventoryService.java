package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.inventory.ImportRequest;
import com.groupSWP.centralkitchenplatform.dto.inventory.ImportTicketResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportTicket;
import com.groupSWP.centralkitchenplatform.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ImportTicketRepository ticketRepository;
    private final IngredientRepository ingredientRepository;
    private final AccountRepository accountRepository;
    // BỔ SUNG: Tiêm trái tim quy đổi đơn vị vào đây
    private final UnitConversionRepository conversionRepository;

    @Transactional
    public ImportTicketResponse importIngredients(ImportRequest request, String username) {
        // --- 1. KIỂM TRA DỮ LIỆU ĐẦU VÀO (FAIL-FAST) ---
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Danh sách nguyên liệu nhập không được để trống!");
        }

        // --- 2. XÁC THỰC NGƯỜI DÙNG ---
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        SystemUser currentUser = account.getSystemUser();
        if (currentUser == null) {
            throw new RuntimeException("Tài khoản này chưa được liên kết với hồ sơ nhân viên!");
        }

        // --- 3. KHỞI TẠO PHIẾU NHẬP (HEADER) ---
        ImportTicket ticket = ImportTicket.builder()
                .ticketId("IM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .note(request.getNote())
                .status(ImportTicket.ImportStatus.COMPLETED)
                .createdBy(currentUser)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<ImportItem> importItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // --- 4. XỬ LÝ CHI TIẾT VÀ CẬP NHẬT KHO ---
        for (ImportRequest.ItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số lượng nhập của nguyên liệu ID " + itemReq.getIngredientId() + " phải lớn hơn 0!");
            }

            Ingredient ingredient = ingredientRepository.findById(itemReq.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Nguyên liệu không tồn tại: " + itemReq.getIngredientId()));

            // === BƯỚC THẦN THÁNH: LẤY HỆ SỐ QUY ĐỔI ===
            BigDecimal conversionFactor = BigDecimal.ONE;

            if (itemReq.getUnit() != null && !itemReq.getUnit().trim().equalsIgnoreCase(ingredient.getUnit().name())) {

                // 1. Ép kiểu cái chữ String (VD: "THUNG") thành Enum UnitType chuẩn của Sếp
                UnitType targetUnit;
                try {
                    targetUnit = UnitType.valueOf(itemReq.getUnit().trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Đơn vị tính không hợp lệ: " + itemReq.getUnit());
                }

                // 2. Dùng đúng cái hàm có sẵn trong Repository của Sếp! (Truyền Object Ingredient và Enum)
                var conversion = conversionRepository.findByIngredientAndUnit(
                        ingredient, targetUnit
                ).orElseThrow(() -> new RuntimeException("Chưa cài đặt luật quy đổi cho đơn vị: " + itemReq.getUnit()));

                conversionFactor = conversion.getConversionFactor();
            }

            // === TÍNH TOÁN QUY ĐỔI VỀ BASE UNIT (Cho Tồn kho) ===
            // 1. Số lượng thực tế = Số lượng nhập * Hệ số
            BigDecimal baseQuantity = itemReq.getQuantity().multiply(conversionFactor);
            // 2. Giá vốn thực tế = Giá nhập 1 đơn vị / Hệ số (Làm tròn 2 chữ số thập phân)
            BigDecimal baseUnitCost = itemReq.getImportPrice().divide(conversionFactor, 2, RoundingMode.HALF_UP);

            // --- LƯU SỔ KẾ TOÁN (Giữ nguyên con số giấy tờ gửi lên) ---
            ImportItem importItem = ImportItem.builder()
                    .importTicket(ticket)
                    .ingredient(ingredient)
                    .quantity(itemReq.getQuantity())      // Giữ nguyên VD: 5 Thùng
                    .importPrice(itemReq.getImportPrice()) // Giữ nguyên VD: 1.000.000đ
                    .build();
            importItems.add(importItem);

            BigDecimal lineTotal = itemReq.getImportPrice().multiply(itemReq.getQuantity());
            totalAmount = totalAmount.add(lineTotal);

            // --- CẬP NHẬT KHO VẬT LÝ (Dùng con số đã quy đổi) ---
            BigDecimal currentStock = (ingredient.getKitchenStock() == null) ? BigDecimal.ZERO : ingredient.getKitchenStock();
            ingredient.setKitchenStock(currentStock.add(baseQuantity)); // Cộng đúng VD: 100 KG
            ingredient.setUnitCost(baseUnitCost);                       // Set đúng VD: 50.000đ/KG

            ingredientRepository.save(ingredient);
        }

        // --- 5. LƯU PHIẾU NHẬP ---
        ticket.setImportItems(importItems);
        ticket.setTotalAmount(totalAmount);
        ImportTicket savedTicket = ticketRepository.save(ticket);

        return mapToResponse(savedTicket);
    }

    private ImportTicketResponse mapToResponse(ImportTicket ticket) {
        // ... (Giữ nguyên phần Map Response của Sếp) ...
        List<ImportTicketResponse.ImportItemResponse> itemResponses = ticket.getImportItems().stream()
                .map(item -> ImportTicketResponse.ImportItemResponse.builder()
                        .ingredientName(item.getIngredient().getName())
                        .unit(item.getIngredient().getUnit().name())
                        .quantity(item.getQuantity())
                        .importPrice(item.getImportPrice())
                        .totalPrice(item.getImportPrice().multiply(item.getQuantity()))
                        .build())
                .toList();

        return ImportTicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .importDate(ticket.getCreatedAt())
                .note(ticket.getNote())
                .totalAmount(ticket.getTotalAmount())
                .status(ticket.getStatus().name())
                .createdByName(ticket.getCreatedBy().getFullName())
                .items(itemResponses)
                .build();
    }
}