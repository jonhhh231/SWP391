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

                        // 🔥 ĐÃ FIX ĐƯỜNG DẪN + ROLE CHO PRODUCT, CATEGORY, INGREDIENT
                        .requestMatchers(HttpMethod.POST,
                                "/api/products", "/api/products/**",
                                "/api/categories", "/api/categories/**",
                                "/api/ingredients", "/api/ingredients/**",
                                "/api/stores", "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        .requestMatchers(HttpMethod.PUT,
                                "/api/products/**", "/api/categories/**", "/api/ingredients/**", "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        .requestMatchers(HttpMethod.DELETE,
                                "/api/products/**", "/api/categories/**", "/api/ingredients/**", "/api/stores/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        // COORDINATOR
                        .requestMatchers("/api/logistics/**", "/api/shipments/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "COORDINATOR", "ROLE_COORDINATOR")

                        // KITCHEN
                        .requestMatchers("/api/kitchen/**", "/api/inventory/**")
                        .hasAnyAuthority("ADMIN", "ROLE_ADMIN", "MANAGER", "ROLE_MANAGER", "KITCHEN_MANAGER", "ROLE_KITCHEN_MANAGER")

                        // STORE
                        .requestMatchers("/api/store/**").hasAnyAuthority("ADMIN", "ROLE_ADMIN", "STORE_MANAGER", "ROLE_STORE_MANAGER")

                        // GET CÔNG KHAI (Chỉ cần có Token là xem được)
                        .requestMatchers(HttpMethod.GET,
                                "/api/products", "/api/products/**",
                                "/api/categories", "/api/categories/**",
                                "/api/ingredients", "/api/ingredients/**")
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
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
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