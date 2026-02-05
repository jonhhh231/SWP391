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
                        .requestMatchers("/api/auth/**").permitAll() // Công khai endpoint login/logout
                        .requestMatchers("/api/products/**").permitAll()
                        .requestMatchers("/api/ingredients/**").permitAll()
                        .requestMatchers("/api/auth/check-me").permitAll()// de test jwt
                        // 1. Cấu hình phân quyền theo đường dẫn (URL-based)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/kitchen/**").hasAnyRole("ADMIN", "KITCHEN_MANAGER")
                        .requestMatchers("/api/store/**").hasAnyRole("ADMIN", "STORE_MANAGER")
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
