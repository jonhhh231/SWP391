package com.groupSWP.centralkitchenplatform.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    // Map lưu: Username -> OTP
    private final Map<String, String> otpCache = new ConcurrentHashMap<>();

    public String generateOtp(String username) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpCache.put(username, otp);
        // Lưu ý: Trong thực tế nên dùng Scheduler để xóa sau 5 phút
        return otp;
    }

    public boolean validateOtp(String username, String otp) {
        return otp.equals(otpCache.get(username));
    }

    public void clearOtp(String username) {
        otpCache.remove(username);
    }
}