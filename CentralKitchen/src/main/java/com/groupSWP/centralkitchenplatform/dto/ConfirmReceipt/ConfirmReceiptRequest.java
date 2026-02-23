package com.groupSWP.centralkitchenplatform.dto.ConfirmReceipt;

import lombok.Data;
import java.util.List;

@Data
public class ConfirmReceiptRequest {
    private List<Item> items;   // ✅ phải có items
    private String note;

    @Data
    public static class Item {
        private String productId;
        private Integer actualReceived;
    }
}