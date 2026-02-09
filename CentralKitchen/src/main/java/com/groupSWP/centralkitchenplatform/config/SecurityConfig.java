package com.groupSWP.centralkitchenplatform.config;

import com.groupSWP.centralkitchenplatform.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Cho phép sử dụng @PreAuthorize ở Controller
@RequiredArgsConstructor // Tự động inject JwtAuthenticationFilter
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter; // Thêm bộ lọc đã viết

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // --- 1. CÔNG KHAI ---
                        .requestMatchers("/api/auth/**").permitAll()

                        // --- 2. ADMIN & MANAGER (Sếp to) ---
                        // Admin đi đâu cũng được
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Manager quản lý chung nên thường vào được các api quản lý
                        .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")

                        // --- 3. SUPPLY COORDINATOR (Điều phối) ---
                        // API liên quan đến vận chuyển, nhập hàng
                        .requestMatchers("/api/logistics/**", "/api/shipments/**").hasAnyRole("ADMIN", "MANAGER", "COORDINATOR")

                        // --- 4. CENTRAL KITCHEN STAFF (Bếp) ---
                        // API xem công thức, cập nhật trạng thái nấu
                        .requestMatchers("/api/kitchen/**").hasAnyRole("ADMIN", "MANAGER", "KITCHEN_STAFF")

                        // --- 5. FRANCHISE STORE STAFF (Cửa hàng) ---
                        // API đặt hàng, xem lịch sử đơn hàng của cửa hàng
                        .requestMatchers("/api/store/**").hasAnyRole("ADMIN", "STORE_STAFF")

                        // API dùng chung (Ví dụ: Xem danh sách món ăn để đặt)
                        .requestMatchers("/api/products/**").authenticated() // Ai đăng nhập rồi cũng xem được SP

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. QUAN TRỌNG: Đăng ký Filter để kiểm tra Token trước khi xác thực
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
