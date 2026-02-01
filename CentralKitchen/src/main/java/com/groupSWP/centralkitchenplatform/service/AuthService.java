package com.groupSWP.centralkitchenplatform.service;

import com.groupSWP.centralkitchenplatform.dto.auth.AuthRequest;
import com.groupSWP.centralkitchenplatform.dto.auth.AuthResponse;
import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.repositories.AccountRepository;
import com.groupSWP.centralkitchenplatform.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AccountRepository repository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(AuthRequest request) {
        Account account = repository.findByUsername(request.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        // So sánh mật khẩu (Bây giờ chắc chắn khớp 100% vì bước trước đã update rồi)
        if (!passwordEncoder.matches(request.password(), account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai mật khẩu!");
        }
        // Trả về Token
        String token = jwtService.generateToken(account);
        return new AuthResponse(token, account.getUsername(), account.getRole());
    }
}