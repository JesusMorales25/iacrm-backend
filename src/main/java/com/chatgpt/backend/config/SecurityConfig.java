package com.chatgpt.backend.config;

import com.chatgpt.backend.security.ApiKeyAuthFilter;
import com.chatgpt.backend.security.JwtAuthFilter;
import com.chatgpt.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${bot.api.key}")
    private String botApiKey;

    @Value("${cors.allowed.origins}")
    private String corsAllowedOrigins;
    
    @Value("${cors.allowed.methods}")
    private String corsAllowedMethods;
    
    @Value("${cors.allowed.headers}")
    private String corsAllowedHeaders;
    
    @Value("${cors.allow.credentials}")
    private boolean corsAllowCredentials;

    private final JwtUtil jwtUtil;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    // ==================== Beans ====================
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Convertir las cadenas separadas por comas en listas
        String[] originsArray = corsAllowedOrigins.split(",");
        String[] methodsArray = corsAllowedMethods.split(",");
        String[] headersArray = corsAllowedHeaders.split(",");
        
        configuration.setAllowedOrigins(List.of(originsArray));
        configuration.setAllowedMethods(List.of(methodsArray));
        configuration.setAllowedHeaders(List.of(headersArray));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(corsAllowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // ==================== Filtros ====================
    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        // "X-API-KEY" debe coincidir con el header enviado por tu bot
        return new ApiKeyAuthFilter("X-API-KEY", botApiKey);
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter() {
        return new JwtAuthFilter(jwtUtil, userDetailsService);
    }

    // ==================== Security Filter Chain ====================
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/api/chat/send").permitAll() // 👈 Permitir acceso sin JWT
                        .requestMatchers("/superadmin/**").hasRole("SUPERADMIN")
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPERADMIN")
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN", "SUPERADMIN")
                        .requestMatchers("/api/reportes/**").authenticated()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                // 🚀 API Key primero, JWT después
                .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
