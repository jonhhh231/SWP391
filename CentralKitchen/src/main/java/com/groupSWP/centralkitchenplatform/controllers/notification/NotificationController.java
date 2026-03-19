package com.groupSWP.centralkitchenplatform.controllers.notification;

import com.groupSWP.centralkitchenplatform.dto.notification.BroadcastRequest;
import com.groupSWP.centralkitchenplatform.dto.notification.NotificationResponse;
import com.groupSWP.centralkitchenplatform.entities.notification.Notification;
import com.groupSWP.centralkitchenplatform.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // =========================================================================
    // 1. NGƯỜI DÙNG XEM DANH SÁCH THÔNG BÁO CỦA MÌNH
    // =========================================================================
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(@RequestParam UUID accountId) {
        // 💡 Note nhỏ cho Sếp: Tạm thời mình truyền accountId qua tham số URL cho dễ test Postman.
        // Sau này tối ưu bảo mật, Sếp có thể móc cái accountId thẳng từ cái Token JWT ra luôn cho pro nha!
        return ResponseEntity.ok(notificationService.getUserNotifications(accountId));
    }

    // =========================================================================
    // 2. LẤY SỐ LƯỢNG CHƯA ĐỌC (HIỂN THỊ CHẤM ĐỎ)
    // =========================================================================
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@RequestParam UUID accountId) {
        return ResponseEntity.ok(notificationService.getUnreadCount(accountId));
    }

    // =========================================================================
    // 3. ĐÁNH DẤU ĐÃ ĐỌC TẤT CẢ (Làm mất hết chấm đỏ)
    // =========================================================================
    @PutMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(@RequestParam UUID accountId) {
        notificationService.markAllAsRead(accountId);
        return ResponseEntity.ok("Đã dọn dẹp sạch sẽ, không còn cái chấm đỏ nào!");
    }

    // =========================================================================
    // 4. ĐÁNH DẤU ĐÃ ĐỌC 1 THÔNG BÁO CỤ THỂ (Khi User click vào 1 dòng)
    // =========================================================================
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<String> markAsRead(@PathVariable UUID notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok("Đã xem thông báo: " + notificationId);
    }

    // =========================================================================
    // 5. [ĐẶC QUYỀN ADMIN] PHÁT LOA THÔNG BÁO TOÀN HỆ THỐNG
    // =========================================================================
    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('ADMIN')") // 🔥 Đã đổi sang hasAnyRole theo lệnh Sếp
    public ResponseEntity<String> broadcastNotification(@RequestBody BroadcastRequest request) {
        log.info("📢 Admin đang lên sóng phát loa...");

        Notification.NotificationType type = Notification.NotificationType.valueOf(request.getType());
        notificationService.broadcastNotification(request.getTargetRoles(), request.getTitle(), request.getMessage(), type);

        return ResponseEntity.ok("Phát loa thông báo thành công rực rỡ!");
    }
}