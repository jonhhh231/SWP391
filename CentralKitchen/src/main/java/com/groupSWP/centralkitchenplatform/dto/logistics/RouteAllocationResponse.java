package com.groupSWP.centralkitchenplatform.dto.logistics;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RouteAllocationResponse {
    private int urgentOrders;
    private int standardOrders;
    private int urgentTripsCreated;
    private int standardTripsCreated;
    private int totalTripsCreated;
}