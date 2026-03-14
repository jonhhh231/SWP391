package com.groupSWP.centralkitchenplatform.entities.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

@Getter
public enum UnitType {
    // 1. TRỌNG LƯỢNG
    KG("Kilogram", "Trọng lượng"),
    G("Gram", "Trọng lượng"),
    MG("Milligram", "Trọng lượng"),

    // 2. THỂ TÍCH
    L("Lít", "Thể tích"),
    ML("Millilit", "Thể tích"),

    // 3. ĐÓNG GÓI (Nhập kho)
    LON("Lon", "Đóng gói"),
    HOP("Hộp", "Đóng gói"),
    CHAI("Chai", "Đóng gói"),
    GOI("Gói", "Đóng gói"),
    VI("Vỉ", "Đóng gói"),
    THUNG("Thùng", "Đóng gói"),
    BAO("Bao", "Đóng gói"),

    // 4. ĐƠN VỊ ĐẾM
    CAI("Cái", "Đơn vị đếm"),
    TRAI("Trái", "Đơn vị đếm"),
    CU("Củ", "Đơn vị đếm"),

    // 5. ĐƠN VỊ BÁN (Product)
    TO("Tô", "Đơn vị bán"),
    DIA("Dĩa", "Đơn vị bán"),
    LY("Ly", "Đơn vị bán"),
    PHAN("Phần", "Đơn vị bán"),
    COMBO("Combo", "Đơn vị bán");

    private final String label;
    private final String group; // 🌟 THÊM BIẾN NHÓM VÀO ĐÂY

    UnitType(String label, String group) {
        this.label = label;
        this.group = group;
    }

    @JsonCreator
    public static UnitType from(String value) {
        if (value == null) return null;
        return UnitType.valueOf(value.trim().toUpperCase());
    }
}