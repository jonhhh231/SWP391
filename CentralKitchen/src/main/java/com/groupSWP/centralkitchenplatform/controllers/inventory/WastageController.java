package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.dto.kitchen.WastageRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.WastageResponse;
import com.groupSWP.centralkitchenplatform.service.inventory.WastageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kitchen")
@RequiredArgsConstructor
public class WastageController {

    private final WastageService wastageService;

    /**
     * API Ghi nhận hao hụt hoặc hỏng hóc nguyên liệu.
     * <p>
     * Dành cho nhân sự Bếp trung tâm báo cáo các trường hợp nguyên liệu bị hỏng,
     * hết hạn hoặc hao hụt trong quá trình chế biến. Hệ thống sẽ lưu vết lịch sử
     * và tự động trừ số lượng tồn kho tương ứng.
     * </p>
     *
     * @param request Payload chứa danh sách nguyên liệu và số lượng hao hụt.
     * @return Phản hồi HTTP 200 chứa đối tượng {@link WastageResponse} xác nhận kết quả ghi nhận.
     */
    @PostMapping("/wastage")
    @PreAuthorize("hasAnyRole('KITCHEN_MANAGER','ADMIN')")
    public ResponseEntity<WastageResponse> recordWastage(
            @Valid @RequestBody WastageRequest request) {
        return ResponseEntity.ok(wastageService.recordWastage(request));
    }
}