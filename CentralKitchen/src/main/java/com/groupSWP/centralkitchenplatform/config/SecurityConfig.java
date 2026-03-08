package com.groupSWP.centralkitchenplatform.config;

import com.groupSWP.centralkitchenplatform.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
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
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // PUBLIC
                        .requestMatchers("/api/auth/login", "/api/auth/verify-otp").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // ADMIN
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // MANAGER
                        .requestMatchers("/api/manager/**", "/api/orders/**", "/api/recipes/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                        // 🔥 [FIX LỖI 2]: Tách riêng quyền Quản lý Cửa hàng (/api/stores) khỏi Bếp trưởng.
                        // Chỉ Admin và Manager mới được thêm/sửa/xóa Cửa hàng.
                        .requestMatchers(HttpMethod.POST, "/api/stores", "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                        .requestMatchers(HttpMethod.PUT, "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                        .requestMatchers(HttpMethod.DELETE, "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                        // 🔥 ĐÃ FIX ĐƯỜNG DẪN + ROLE CHO PRODUCT, CATEGORY, INGREDIENT
                        // [FIX LỖI 1]: Thêm MANAGER vào để Manager cũng sửa được món ăn, và thêm api conversions
                        .requestMatchers(HttpMethod.POST,
                                "/api/products", "/api/products/**",
                                "/api/categories", "/api/categories/**",
                                "/api/ingredients", "/api/ingredients/**",
                                "/api/manager/conversions/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/products/**", "/api/categories/**", "/api/ingredients/**", "/api/manager/conversions/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/products/**", "/api/categories/**", "/api/ingredients/**", "/api/manager/conversions/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        // COORDINATOR
                        .requestMatchers("/api/logistics/**", "/api/shipments/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "COORDINATOR", "ROLE_COORDINATOR")

                        // KITCHEN
                        // (Luồng này Sếp viết chuẩn rồi, KITCHEN_MANAGER đã có quyền đụng vào /api/inventory/**)
                        .requestMatchers("/api/kitchen/**", "/api/inventory/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        // STORE
                        // 🔥 [FIX LỖI 4]: Đã cấp thêm quyền cho MANAGER vào hỗ trợ Cửa hàng lúc cần thiết (Ví dụ Check Cart/Order hộ)
                        .requestMatchers("/api/store/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STORE_MANAGER", "ROLE_STORE_MANAGER")

                        // GET CÔNG KHAI (Chỉ cần có Token là xem được)
                        .requestMatchers(HttpMethod.GET,
                                "/api/products", "/api/products/**",
                                "/api/categories", "/api/categories/**",
                                "/api/ingredients", "/api/ingredients/**",
                                "/api/stores", "/api/stores/**", // 🔥 Cho phép GET để load dropdown cửa hàng
                                "/api/manager/conversions/**")   // 🔥 Cho phép GET để load dropdown đơn vị
                        .authenticated()

                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        // 🔥 Bổ sung thêm "PATCH" để chạy được API Dispatch Shipment
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
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