package com.groupSWP.centralkitchenplatform.config;

import com.groupSWP.centralkitchenplatform.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod; // DÒNG NÀY MỚI THÊM: Để phân biệt GET, POST, PUT, DELETE
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                        .requestMatchers("/api/auth/login", "/api/auth/verify-otp").permitAll()
                        // --- 1. CÔNG KHAI ---
                        .requestMatchers("/api/auth/**").permitAll()

                        // --- 2. ADMIN & MANAGER (Sếp to) ---
                        // Admin đi đâu cũng được
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Manager quản lý chung nên thường vào được các api quản lý
                        .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")

                        // Manager và Admin quản lý toàn bộ đơn hàng
                        .requestMatchers("/api/orders/**").hasAnyRole("ADMIN", "MANAGER")

                        // Manage Recipes (BOM): chỉ ADMIN/MANAGER được quản lý công thức
                        .requestMatchers("/api/recipes/**").hasAnyRole("ADMIN", "MANAGER")

                        // 🔒 BẢO VỆ DỮ LIỆU CỐT LÕI: Gom gọn chặn Cửa hàng tự ý Thêm/Sửa/Xóa
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("ADMIN", "MANAGER", "KITCHEN_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("ADMIN", "MANAGER", "KITCHEN_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("ADMIN", "MANAGER", "KITCHEN_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/ingredients/**", "/api/stores/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT,  "/api/categories/**", "/api/ingredients/**", "/api/stores/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/ingredients/**", "/api/stores/**").hasAnyRole("ADMIN", "MANAGER")


                        // --- 3. SUPPLY COORDINATOR (Điều phối) ---
                        // API liên quan đến vận chuyển, nhập hàng
                        .requestMatchers("/api/logistics/**", "/api/shipments/**").hasAnyRole("ADMIN", "MANAGER", "COORDINATOR")

                        // --- 4. CENTRAL KITCHEN STAFF (Bếp) ---
                        // API xem công thức, cập nhật trạng thái nấu, nhập kho (inventory)
                        .requestMatchers("/api/kitchen/**", "/api/inventory/**").hasAnyRole("ADMIN", "MANAGER", "KITCHEN_MANAGER")



                        // --- 5. FRANCHISE STORE STAFF (Cửa hàng) ---
                        // API đặt hàng, xem lịch sử đơn hàng của cửa hàng
                        .requestMatchers("/api/store/**").hasAnyRole("STORE_MANAGER", "ADMIN", "MANAGER")

                        // --- 6. API DÙNG CHUNG (Chỉ được XEM) ---
                        // Bất kỳ ai đăng nhập rồi cũng xem được danh sách Món, Danh mục, Nguyên liệu
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/ingredients/**").authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 2. QUAN TRỌNG: Đăng ký Filter để kiểm tra Token trước khi xác thực
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Cho phép frontend từ cổng 3000 gọi vào
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));

        // Cho phép các method này
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Cho phép gửi kèm header (quan trọng để gửi Token Bearer)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // Cho phép gửi cookie/credentials (nếu cần)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}