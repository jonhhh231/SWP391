package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.kitchen.WastageRequest;
import com.groupSWP.centralkitchenplatform.dto.kitchen.WastageResponse;
import com.groupSWP.centralkitchenplatform.service.WastageService;
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
     * POST /api/kitchen/wastage
     * Ghi nhận hao hụt / hỏng hóc nguyên liệu
     * Role: KITCHEN_MANAGER, MANAGER, ADMIN
     */
    @PostMapping("/wastage")
    @PreAuthorize("hasAnyRole('KITCHEN_MANAGER','MANAGER','ADMIN')")
    public ResponseEntity<WastageResponse> recordWastage(
            @Valid @RequestBody WastageRequest request) {
        return ResponseEntity.ok(wastageService.recordWastage(request));
    }
}