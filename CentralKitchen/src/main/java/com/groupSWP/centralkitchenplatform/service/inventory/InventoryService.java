package com.groupSWP.centralkitchenplatform.service.inventory;

import com.groupSWP.centralkitchenplatform.dto.inventory.ImportRequest;
import com.groupSWP.centralkitchenplatform.dto.inventory.ImportTicketResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.entities.auth.SystemUser;
import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
import com.groupSWP.centralkitchenplatform.entities.kitchen.Ingredient;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportItem;
import com.groupSWP.centralkitchenplatform.entities.procurement.ImportTicket;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import com.groupSWP.centralkitchenplatform.repositories.inventory.ImportTicketRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.IngredientRepository;
import com.groupSWP.centralkitchenplatform.repositories.product.UnitConversionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service quản lý các nghiệp vụ Kho và Nhập xuất nguyên liệu (Inventory Management).
 * <p>
 * Đảm nhiệm việc tạo phiếu nhập kho, tự động tính toán quy đổi đơn vị (Unit Conversion),
 * cập nhật số lượng tồn kho vật lý (Kitchen Stock) và tính toán giá vốn nguyên liệu theo chuẩn kế toán.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ImportTicketRepository ticketRepository;
    private final IngredientRepository ingredientRepository;
    private final AccountRepository accountRepository;
    private final UnitConversionRepository conversionRepository;

    /**
     * Xử lý nghiệp vụ Nhập kho nguyên liệu.
     * <p>
     * Hệ thống sẽ duyệt qua danh sách nguyên liệu đầu vào, đối chiếu với cấu hình quy đổi đơn vị
     * để quy về đơn vị gốc (Base Unit). Sau đó, thực hiện:
     * <ul>
     * <li>Tính toán giá vốn của 1 đơn vị gốc dựa trên tổng tiền dòng nhập.</li>
     * <li>Cộng dồn số lượng vào tồn kho hiện tại.</li>
     * <li>Tạo và lưu trữ Phiếu nhập kho (Import Ticket) kèm chi tiết từng mặt hàng để phục vụ truy xuất FIFO.</li>
     * </ul>
     * </p>
     *
     * @param request  Payload chứa danh sách các nguyên liệu cần nhập, số lượng, đơn vị và tổng tiền.
     * @param username Tên đăng nhập của người dùng thực hiện thao tác (lấy từ Token).
     * @return Đối tượng {@link ImportTicketResponse} chứa thông tin chi tiết phiếu nhập vừa tạo.
     * @throws IllegalArgumentException nếu danh sách trống hoặc số lượng không hợp lệ.
     * @throws RuntimeException nếu không tìm thấy nguyên liệu, tài khoản hoặc luật quy đổi đơn vị.
     */
    @Transactional
    public ImportTicketResponse importIngredients(ImportRequest request, String username) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Danh sách nguyên liệu nhập không được để trống!");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));

        SystemUser currentUser = account.getSystemUser();
        if (currentUser == null) {
            throw new RuntimeException("Tài khoản này chưa được liên kết với hồ sơ nhân viên!");
        }

        ImportTicket ticket = ImportTicket.builder()
                .ticketId("IM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .note(request.getNote())
                .status(ImportTicket.ImportStatus.COMPLETED)
                .createdBy(currentUser)
                .totalAmount(BigDecimal.ZERO)
                .build();

        List<ImportItem> importItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ImportRequest.ItemRequest itemReq : request.getItems()) {
            if (itemReq.getQuantity() == null || itemReq.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Số lượng nhập của nguyên liệu ID " + itemReq.getIngredientId() + " phải lớn hơn 0!");
            }

            Ingredient ingredient = ingredientRepository.findById(itemReq.getIngredientId())
                    .orElseThrow(() -> new RuntimeException("Nguyên liệu không tồn tại: " + itemReq.getIngredientId()));

            BigDecimal conversionFactor = BigDecimal.ONE;

            if (itemReq.getUnit() != null && !itemReq.getUnit().trim().equalsIgnoreCase(ingredient.getUnit().name())) {
                UnitType targetUnit;
                try {
                    targetUnit = UnitType.valueOf(itemReq.getUnit().trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Đơn vị tính không hợp lệ: " + itemReq.getUnit());
                }

                var conversion = conversionRepository.findByIngredientAndUnit(ingredient, targetUnit)
                        .orElseThrow(() -> new RuntimeException("Chưa cài đặt luật quy đổi cho đơn vị: " + itemReq.getUnit()));

                conversionFactor = conversion.getConversionFactor();
            }

            BigDecimal baseQuantity = itemReq.getQuantity().multiply(conversionFactor);

            // 👉 ĐIỂM FIX THEO Ý SẾP: itemReq.getImportPrice() đang là TỔNG TIỀN của dòng nhập này
            BigDecimal lineTotal = itemReq.getImportPrice();
            // 👉 CHIA NGƯỢC: Lấy Tổng tiền / Tổng số lượng gốc = Giá của 1 Đơn Vị Gốc
            // 🌟 FIX LỖI TIỀN LẺ: Chia lấy 4 số thập phân để đảm bảo chính xác tuyệt đối khi nhân ngược lại
            BigDecimal baseUnitCost = lineTotal.divide(baseQuantity, 4, RoundingMode.HALF_UP);

            // 🔥 ĐIỂM ĂN TIỀN LÀ ĐÂY: Thêm remainingQuantity để FIFO có data mà trừ!
            ImportItem importItem = ImportItem.builder()
                    .importTicket(ticket)
                    .ingredient(ingredient)
                    .quantity(itemReq.getQuantity())
                    .remainingQuantity(baseQuantity)      // Dòng sống còn của kho vật lý!
                    .importPrice(baseUnitCost)            // 👉 LƯU GIÁ VỐN 1 ĐƠN VỊ GỐC (Thay vì lưu itemReq.getImportPrice())
                    .build();
            importItems.add(importItem);

            // 👉 CỘNG THẲNG TIỀN VÀO TỔNG PHIẾU NHẬP (Không nhân thêm quantity nữa)
            totalAmount = totalAmount.add(lineTotal);

            BigDecimal currentStock = (ingredient.getKitchenStock() == null) ? BigDecimal.ZERO : ingredient.getKitchenStock();
            ingredient.setKitchenStock(currentStock.add(baseQuantity));
            ingredient.setUnitCost(baseUnitCost);

            ingredientRepository.save(ingredient);
        }

        ticket.setImportItems(importItems);
        ticket.setTotalAmount(totalAmount);
        ImportTicket savedTicket = ticketRepository.save(ticket);

        return mapToResponse(savedTicket);
    }

    /**
     * Hàm Helper: Chuyển đổi Entity ImportTicket sang DTO ImportTicketResponse.
     *
     * @param ticket Thực thể Phiếu nhập kho vừa được lưu.
     * @return DTO chứa dữ liệu phản hồi đã được định dạng cho Frontend.
     */
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