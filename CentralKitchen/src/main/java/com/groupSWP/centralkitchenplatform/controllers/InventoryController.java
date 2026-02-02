package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.inventory.ImportRequest;
import com.groupSWP.centralkitchenplatform.dto.inventory.ImportTicketResponse; // <--- Import cái này
// import com.groupSWP.centralkitchenplatform.entities.procurement.ImportTicket; // <--- Bỏ cái này đi (hoặc giữ nếu cần dùng ở chỗ khác)
import com.groupSWP.centralkitchenplatform.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/import")
    // Sửa ImportTicket -> ImportTicketResponse
    public ResponseEntity<ImportTicketResponse> importIngredients(@RequestBody ImportRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        return ResponseEntity.ok(inventoryService.importIngredients(request, username));
    }
}