package com.groupSWP.centralkitchenplatform.security;

import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtService {

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

    public String generateToken(Account account) {
        Map<String, Object> claims = new HashMap<>();

        // Đảm bảo lấy đúng Role từ entity Account
        claims.put("role", account.getRole());

        return Jwts.builder()
                .setClaims(claims) // Thiết lập claims trước
                .setSubject(account.getUsername()) // Sau đó mới set Subject
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 86400000))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Thêm hàm lấy Role từ Token
    public String extractRole(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        // Ép kiểu về String và kiểm tra null
        System.out.println("Full Claims: " + claims);
        return claims.get("role", String.class);
    }
}