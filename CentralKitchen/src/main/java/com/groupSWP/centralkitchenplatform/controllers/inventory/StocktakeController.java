package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.dto.inventory.StocktakeRequest;
import com.groupSWP.centralkitchenplatform.service.inventory.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    /**
     * API Kiểm kê kho định kỳ (Stocktake).
     * <p>
     * Dành cho Manager/Admin thực hiện vào cuối ngày hoặc cuối tuần.
     * Hệ thống sẽ nhận số lượng đếm tay thực tế, so sánh với số lượng trên phần mềm
     * để tìm ra chênh lệch. Nếu có hao hụt, tự động chạy thuật toán FIFO trừ kho
     * và ghi log báo cáo.
     * </p>
     *
     * @param request Payload chứa danh sách nguyên liệu và số lượng thực tế đếm được.
     * @return Phản hồi HTTP 200 xác nhận hoàn tất kiểm kê.
     */
    @PostMapping("/stocktake")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> processStocktake(@Valid @RequestBody StocktakeRequest request) {
        stocktakeService.processStocktake(request);
        // Trả về JSON cho FE dễ xài: { "message": "..." }
        return ResponseEntity.ok(java.util.Map.of("message", "Đã hoàn tất quá trình đối soát và kiểm kê kho!"));
    }
}