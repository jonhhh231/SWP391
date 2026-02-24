package com.groupSWP.centralkitchenplatform.dto.ConfirmReceipt;

import lombok.Data;

import java.util.List;

@Data
public class ConfirmReceiptResponse {
    private String orderId;
    private String status;
    private String note;
    private List<ItemResult> items;

    @Data
    public static class ItemResult {
        private String productId;
        private Integer orderedQty;
        private Integer receivedQty;
    }
}
