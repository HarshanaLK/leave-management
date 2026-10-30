package com.omibits.leave.config;

import com.omibits.leave.employee.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(UserRepository users) {
    return username ->
      users
        .findByEmailIgnoreCase(username)
        .map(u ->
          User.withUsername(u.email)
            .password(u.passwordHash)
            .roles(u.role.name())
            .disabled(!u.active)
            .build()
        )
        .orElseThrow(() ->
          new org.springframework.security.core.userdetails.UsernameNotFoundException(
            "User not found"
          )
        );
  }

  @Bean
  HttpSessionSecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  SecurityFilterChain filterChain(
    HttpSecurity http,
    HttpSessionSecurityContextRepository contexts
  ) throws Exception {
    return http
      .csrf(csrf ->
        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
      )
      .securityContext(c -> c.securityContextRepository(contexts))
      .authorizeHttpRequests(a ->
        a
          .requestMatchers(
            "/api/auth/login",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/csrf"
          )
          .permitAll()
          .requestMatchers(
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/error"
          )
          .permitAll()
          .requestMatchers("/api/**")
          .authenticated()
          .anyRequest()
          .denyAll()
      )
      .exceptionHandling(e ->
        e
          .authenticationEntryPoint((req, res, ex) ->
            jsonError(res, 401, "Authentication required")
          )
          .accessDeniedHandler((req, res, ex) ->
            jsonError(res, 403, "Permission denied")
          )
      )
      .formLogin(f -> f.disable())
      .httpBasic(b -> b.disable())
      .build();
  }

  private static void jsonError(
    HttpServletResponse response,
    int code,
    String message
  ) throws IOException {
    response.setStatus(code);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response
      .getWriter()
      .write("{\"status\":" + code + ",\"error\":\"" + message + "\"}");
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration conf)
    throws Exception {
    return conf.getAuthenticationManager();
  }
}
