package com.groupSWP.centralkitchenplatform.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtService {

    // Tạo một Key an toàn đủ độ dài cho thuật toán HS256
//    private final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
//
//    public String generateToken(String username) {
//        return Jwts.builder()
//                .setSubject(username)
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // Hết hạn sau 24h
//                .signWith(SECRET_KEY) // Sử dụng đối tượng Key thay vì String
//                .compact();
//    }

    private final String SECRET_KEY = "nha_be_trung_tam_nen_tang_quan_ly_chuoi_cung_ung_thuc_pham_2026";

    private Key getSigningKey() {
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Thêm hàm này vào cuối class JwtService
    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}