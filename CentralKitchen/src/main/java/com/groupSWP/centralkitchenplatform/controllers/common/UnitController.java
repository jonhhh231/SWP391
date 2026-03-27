package com.groupSWP.centralkitchenplatform.controllers.common;

import com.groupSWP.centralkitchenplatform.entities.common.UnitType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/units")
public class UnitController {

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> getAllUnits() {
        List<Map<String, String>> unitDictionary = Arrays.stream(UnitType.values())
                .map(unit -> Map.of(
                        "value", unit.name(),           // Mã không dấu: "THUNG"
                        "label", unit.getLabel(),       // Tên có dấu: "Thùng"
                        "group", unit.getGroup()        // Nhóm: "Đóng gói"
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(unitDictionary);
    }
}