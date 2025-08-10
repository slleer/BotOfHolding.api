package com.botofholding.api.Config;


import com.botofholding.api.Security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF, as we are using JWTs for a stateless API.
                .csrf(csrf -> csrf.disable())
                // 2. Define authorization rules.
                .authorizeHttpRequests(auth -> auth
                        // Allow unauthenticated access to a token generation endpoint for the bot.
                        .requestMatchers("/api/auth/bot-token").permitAll()
                        // Allow unauthenticated access to actuator endpoints.
                        .requestMatchers("/actuator/**").permitAll() // Permit all access to actuator endpoints
                        // Secure all other API endpoints.
                        .requestMatchers("/api/**").authenticated()
                        // Deny any other requests.
                        .anyRequest().denyAll()
                )
                // 3. Configure session management to be stateless.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 4. Add our custom JWT filter before the standard authentication filter.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
