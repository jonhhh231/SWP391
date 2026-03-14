package com.groupSWP.centralkitchenplatform.service.order;

import com.groupSWP.centralkitchenplatform.dao.LogisticDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service xử lý các truy vấn và logic liên quan đến điều phối vận chuyển.
 */
@Service
@RequiredArgsConstructor
public class LogisticsService {

    // Tiêm Repository vào thay vì dùng JdbcTemplate trực tiếp
    private final LogisticDao logisticDao;

    public List<Map<String, Object>> getReadyOrders() {
        return logisticDao.findReadyOrders();
    }

    public List<Map<String, Object>> getActiveShipments() {
        return logisticDao.findActiveShipments();
    }

    public List<Map<String, Object>> getShipmentDetails(String shipmentId) {
        return logisticDao.findShipmentDetails(shipmentId);
    }

    public List<Map<String, Object>> getCompletedShipments() {
        return logisticDao.findCompletedShipments();
    }

    public List<Map<String, Object>> getCoordinatorAccounts() {
        // 1. Lấy data thô từ Repository
        List<Map<String, Object>> coordinators = logisticDao.findCoordinatorAccountsRaw();

        // 2. Logic nghiệp vụ (Dịch mảng byte Base64 sang chuỗi UUID) được giữ lại ở Service
        for (Map<String, Object> map : coordinators) {
            Object idObj = map.get("id");
            if (idObj instanceof byte[]) {
                byte[] bytes = (byte[]) idObj;
                ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
                UUID uuid = new UUID(byteBuffer.getLong(), byteBuffer.getLong());
                map.put("id", uuid.toString());
            }
        }
        return coordinators;
    }
}