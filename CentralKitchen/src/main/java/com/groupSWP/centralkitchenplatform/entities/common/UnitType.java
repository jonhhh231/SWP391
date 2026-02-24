package com.groupSWP.centralkitchenplatform.entities.common;

import lombok.Getter;

@Getter
public enum UnitType {
    // 1. TRỌNG LƯỢNG
    KG("Kilogram"), G("Gram"), MG("Milligram"),

    // 2. THỂ TÍCH
    L("Lít"), ML("Millilit"),

    // 3. ĐÓNG GÓI (Nhập kho)
    LON("Lon"), HOP("Hộp"), CHAI("Chai"), GOI("Gói"), VI("Vỉ"), THUNG("Thùng"), BAO("Bao"),

    // 4. ĐƠN VỊ ĐẾM
    CAI("Cái"), TRAI("Trái"), CU("Củ"),

    // 5. ĐƠN VỊ BÁN (Product)
    TO("Tô"), DIA("Dĩa"), LY("Ly"), PHAN("Phần"), COMBO("Combo");

    private final String label;
    UnitType(String label) { this.label = label; }
}