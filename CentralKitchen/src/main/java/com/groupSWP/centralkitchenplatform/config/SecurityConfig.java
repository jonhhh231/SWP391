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
                        // PUBLIC
                        .requestMatchers("/api/auth/login", "/api/auth/verify-otp").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // ADMIN
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // MANAGER
                        .requestMatchers("/api/manager/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/orders/**").hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/recipes/**").hasAnyAuthority("ADMIN", "MANAGER")

                        // ✅ CHỈ GIỮ DÒNG NÀY (bỏ dòng permitAll() ở trên)
                        .requestMatchers(HttpMethod.POST, "/api/products/**", "/api/categories/**", "/api/ingredients/**", "/api/stores/**").hasAnyAuthority("ADMIN", "KITCHEN_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**", "/api/categories/**", "/api/ingredients/**", "/api/stores/**").hasAnyAuthority("ADMIN", "KITCHEN_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**", "/api/categories/**", "/api/ingredients/**", "/api/stores/**").hasAnyAuthority("ADMIN", "KITCHEN_MANAGER")

                        // COORDINATOR
                        .requestMatchers("/api/logistics/**", "/api/shipments/**").hasAnyAuthority("ADMIN", "MANAGER", "COORDINATOR")

                        // KITCHEN
                        .requestMatchers("/api/kitchen/**", "/api/inventory/**").hasAnyAuthority("ADMIN", "MANAGER", "KITCHEN_MANAGER")

                        // STORE
                        .requestMatchers("/api/store/**").hasAnyAuthority("ADMIN", "STORE_MANAGER")

                        // GET công khai
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