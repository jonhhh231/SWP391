package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.inventory.ImportRequest;
import com.groupSWP.centralkitchenplatform.dto.inventory.ImportTicketResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportTicket;
import com.groupSWP.centralkitchenplatform.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ImportTicketRepository ticketRepository;
    private final IngredientRepository ingredientRepository;
    private final AccountRepository accountRepository;

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
            // Kiểm tra số lượng nhập phải > 0
            if (itemReq.getQuantity() == null || itemReq.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số lượng nhập của nguyên liệu ID " + itemReq.getIngredientId() + " phải lớn hơn 0!");
            }

            Ingredient ingredient = ingredientRepository.findById(itemReq.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Nguyên liệu không tồn tại: " + itemReq.getIngredientId()));

            // Tạo dòng chi tiết (Detail)
            ImportItem importItem = ImportItem.builder()
                    .importTicket(ticket)
                    .ingredient(ingredient)
                    .quantity(itemReq.getQuantity())
                    .importPrice(itemReq.getImportPrice())
                    .build();

            importItems.add(importItem);

            // Tính toán tổng tiền
            BigDecimal lineTotal = itemReq.getImportPrice().multiply(itemReq.getQuantity());
            totalAmount = totalAmount.add(lineTotal);

            // Cập nhật tồn kho thực tế
            BigDecimal currentStock = (ingredient.getKitchenStock() == null) ? BigDecimal.ZERO : ingredient.getKitchenStock();
            ingredient.setKitchenStock(currentStock.add(itemReq.getQuantity()));

            // Cập nhật giá vốn mới nhất
            ingredient.setUnitCost(itemReq.getImportPrice());

            // Lưu lại thông tin nguyên liệu
            ingredientRepository.save(ingredient);
        }

        // --- 5. LƯU PHIẾU NHẬP ---
        ticket.setImportItems(importItems);
        ticket.setTotalAmount(totalAmount);
        ImportTicket savedTicket = ticketRepository.save(ticket);

        return mapToResponse(savedTicket);
    }

    private ImportTicketResponse mapToResponse(ImportTicket ticket) {
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