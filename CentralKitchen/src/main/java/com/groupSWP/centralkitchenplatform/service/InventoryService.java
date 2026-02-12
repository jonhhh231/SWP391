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
        // 1. Lấy thông tin người nhập (SystemUser) từ Account
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        SystemUser currentUser = account.getSystemUser();
        if (currentUser == null) {
            throw new RuntimeException("Tài khoản này chưa được liên kết với hồ sơ nhân viên!");
        }

        // 2. Tạo Header phiếu nhập
        ImportTicket ticket = ImportTicket.builder()
                .ticketId("IM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .note(request.getNote())
                .status(ImportTicket.ImportStatus.COMPLETED) // Nhập là hoàn thành luôn
                .createdBy(currentUser)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<ImportItem> importItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 3. Duyệt danh sách item nhập
        for (ImportRequest.ItemRequest itemReq : request.getItems()) {
            Ingredient ingredient = ingredientRepository.findById(itemReq.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Nguyên liệu không tồn tại: " + itemReq.getIngredientId()));

            // Tạo chi tiết dòng nhập
            ImportItem importItem = ImportItem.builder()
                    .importTicket(ticket)
                    .ingredient(ingredient)
                    .quantity(itemReq.getQuantity())
                    .importPrice(itemReq.getImportPrice())
                    .build();

            importItems.add(importItem);

            // Tính tổng tiền: Total += (Qty * Price)
            BigDecimal lineTotal = itemReq.getImportPrice().multiply(itemReq.getQuantity());
            totalAmount = totalAmount.add(lineTotal);

            // === 4. CẬP NHẬT KHO (Dùng BigDecimal) ===
            BigDecimal currentStock = (ingredient.getKitchenStock() == null) ? BigDecimal.ZERO : ingredient.getKitchenStock();
            BigDecimal newStock = currentStock.add(itemReq.getQuantity());

            ingredient.setKitchenStock(newStock);

            // Cập nhật giá vốn mới nhất (Optional - để tham khảo giá thị trường)
            ingredient.setUnitCost(itemReq.getImportPrice());

            ingredientRepository.save(ingredient);
        }

        // 5. Gán danh sách item vào ticket và lưu
        ticket.setImportItems(importItems);
        ticket.setTotalAmount(totalAmount);
        ImportTicket savedTicket = ticketRepository.save(ticket);

        // 5. Chuyển đổi sang DTO và trả về (Tránh lỗi vòng lặp)
        return mapToResponse(savedTicket);
    }

    private ImportTicketResponse mapToResponse(ImportTicket ticket) {
        List<ImportTicketResponse.ImportItemResponse> itemResponses = ticket.getImportItems().stream()
                .map(item -> ImportTicketResponse.ImportItemResponse.builder()
                        .ingredientName(item.getIngredient().getName()) // Hoặc .getName() tuỳ entity
                        // Thêm .name() để lấy tên Enum ra (VD: "KG", "L")
                        .unit(item.getIngredient().getUnit().name())
                        .quantity(item.getQuantity())
                        .importPrice(item.getImportPrice())
                        .totalPrice(item.getImportPrice().multiply(item.getQuantity()))
                        .build())
                .toList();

        return ImportTicketResponse.builder()
                .ticketId(ticket.getTicketId())
                .importDate(ticket.getCreatedAt()) // Lấy từ BaseEntity
                .note(ticket.getNote())
                .totalAmount(ticket.getTotalAmount())
                .status(ticket.getStatus().name())
                .createdByName(ticket.getCreatedBy().getFullName()) // Lấy tên người nhập
                .items(itemResponses)
                .build();
    }
}