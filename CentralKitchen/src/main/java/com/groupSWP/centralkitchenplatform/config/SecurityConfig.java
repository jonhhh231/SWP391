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
                        // 1. PUBLIC (Không cần Token)
                        .requestMatchers("/api/auth/login", "/api/auth/verify-otp").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()

                        // =========================================================
                        // 2. ƯU TIÊN 1: CÁC LUỒNG CỤ THỂ (Phải đặt lên trên cùng)
                        // =========================================================

                        // GET CÔNG KHAI (Chỉ cần có Token là xem được)
                        .requestMatchers(HttpMethod.GET,
                                "/api/products", "/api/products/**",
                                "/api/categories", "/api/categories/**",
                                "/api/ingredients", "/api/ingredients/**",
                                "/api/stores", "/api/stores/**",
                                "/api/manager/conversions/**")
                        .authenticated()

                        // POST, PUT, DELETE cho Product, Category, Ingredient, Conversion
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

                        // =========================================================
                        // 3. ƯU TIÊN 2: CÁC LUỒNG TỔNG QUÁT (Dùng /**)
                        // =========================================================

                        // ADMIN
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                        // MANAGER (Vì Conversions đã được vớt ở trên, nên xuống đây không lo bị đè)
                        .requestMatchers("/api/manager/**", "/api/orders/**", "/api/recipes/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                        // QUẢN LÝ CỬA HÀNG (Stores CRUD)
                        .requestMatchers(HttpMethod.POST, "/api/stores", "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                        .requestMatchers(HttpMethod.PUT, "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                        .requestMatchers(HttpMethod.DELETE, "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER")

                        // COORDINATOR
                        .requestMatchers("/api/logistics/**", "/api/shipments/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "COORDINATOR", "ROLE_COORDINATOR")

                        // KITCHEN
                        .requestMatchers("/api/kitchen/**", "/api/inventory/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        // STORE
                        .requestMatchers("/api/store/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "STORE_MANAGER", "ROLE_STORE_MANAGER")

                        // Các API còn lại bắt buộc có Token
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