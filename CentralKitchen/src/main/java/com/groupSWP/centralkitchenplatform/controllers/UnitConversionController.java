package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.manager.ConversionRequest;
import com.groupSWP.centralkitchenplatform.entities.kitchen.UnitConversion;
import com.groupSWP.centralkitchenplatform.service.UnitConversionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/conversions")
@RequiredArgsConstructor
public class UnitConversionController {

    private final UnitConversionService conversionService;

    // API Tạo Quy đổi mới
    // POST /api/manager/conversions
    @PostMapping
    public ResponseEntity<?> createConversion(@RequestBody ConversionRequest request) {
        try {
            UnitConversion result = conversionService.createConversion(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}