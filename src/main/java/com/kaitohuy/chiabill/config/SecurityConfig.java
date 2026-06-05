package com.kaitohuy.chiabill.config;

import org.springframework.http.HttpMethod;
import com.kaitohuy.chiabill.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("api/categories/seed").permitAll()
                        .requestMatchers("/healthz").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/places/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/invites/*").permitAll()
                        .requestMatchers("/.well-known/assetlinks.json").permitAll()
                        .requestMatchers("/join/**").permitAll()
                        .requestMatchers("/privacy-policy", "/delete-account-request").permitAll()
                        .requestMatchers("/images/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/feedbacks").authenticated()
                        .requestMatchers("/api/feedbacks/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
