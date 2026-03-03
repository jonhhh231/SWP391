package com.groupSWP.centralkitchenplatform.controllers.inventory;

import com.groupSWP.centralkitchenplatform.dto.logistics.AllocateRoutesRequest;
import com.groupSWP.centralkitchenplatform.dto.logistics.RouteAllocationResponse;
import com.groupSWP.centralkitchenplatform.service.inventory.RouteAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final RouteAllocationService routeAllocationService;

    @PreAuthorize("hasRole('COORDINATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    @PostMapping("/allocate-routes")
    public ResponseEntity<RouteAllocationResponse> allocateRoutes(
            @RequestBody(required = false) AllocateRoutesRequest req
    ) {
        return ResponseEntity.ok(routeAllocationService.allocate(req));
    }
}