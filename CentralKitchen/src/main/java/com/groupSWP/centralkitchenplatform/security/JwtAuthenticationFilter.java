package com.groupSWP.centralkitchenplatform.security;

import com.groupSWP.centralkitchenplatform.entities.auth.Account;
import com.groupSWP.centralkitchenplatform.repositories.auth.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final AccountRepository accountRepository;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        final String role;

        // 1. Kiểm tra Header Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            username = jwtService.extractUsername(jwt);
            role = jwtService.extractRole(jwt); // Lấy role từ token

            // 2. Kiểm tra username và trạng thái Authentication hiện tại
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                Account account = accountRepository.findByUsername(username).orElse(null);
                if (account == null || !jwt.equals(account.getActiveToken())) {
                    // Nếu không khớp -> Bị đăng nhập ở máy khác -> Trả về lỗi 401
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"error\": \"Tài khoản của bạn đã được đăng nhập ở một thiết bị khác. Vui lòng đăng nhập lại!\"}");
                    return; // Dừng luồng xử lý ngay lập tức
                }


                // 3. Kiểm tra xem role có bị null không trước khi tạo Authority
                System.out.println("Username tu token: " + username);
                System.out.println("Role tu token: " + role);
                if (role != null && !role.isEmpty()) {
                    List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    System.out.println("Role dang nhan duoc: " + role);
                    // 4. Xác thực thành công và lưu vào Context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Nếu Token sai, hết hạn hoặc lỗi giải mã, ta không set Authentication
            // Bạn có thể log lỗi ở đây nếu cần: System.out.println("JWT Error: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
