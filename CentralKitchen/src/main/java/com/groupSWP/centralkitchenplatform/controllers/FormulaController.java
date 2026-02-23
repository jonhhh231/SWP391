package com.groupSWP.centralkitchenplatform.controllers;

import com.groupSWP.centralkitchenplatform.dto.formula.FormulaRequestDTO;
import com.groupSWP.centralkitchenplatform.dto.formula.FormulaResponseDTO;
import com.groupSWP.centralkitchenplatform.service.FormulaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/formulas")
@RequiredArgsConstructor
public class FormulaController {

    private final FormulaService formulaService;

    @GetMapping("/{productId}")
    public ResponseEntity<List<FormulaResponseDTO>> getByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(formulaService.getByProductId(productId));
    }

    @PostMapping("/overwrite")
    public ResponseEntity<Map<String, Object>> overwrite(@Valid @RequestBody FormulaRequestDTO request) {
        formulaService.overwriteFormula(request);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Cập nhật công thức thành công");
        res.put("productId", request.getProductId());

        return ResponseEntity.ok(res);
    }

}
