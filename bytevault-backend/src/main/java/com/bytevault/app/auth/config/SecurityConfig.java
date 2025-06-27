package com.bytevault.app.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(
    prePostEnabled = true,      // 启用@PreAuthorize和@PostAuthorize
    securedEnabled = true,      // 启用@Secured
    jsr250Enabled = true        // 启用@RolesAllowed
)
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
            // 禁用CSRF保护，因为我们使用的是无状态的JWT认证
            .csrf(AbstractHttpConfigurer::disable)
            // 启用CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 配置会话管理为无状态，不创建会话
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置请求授权规则
            .authorizeRequests(authorize -> authorize
                // 允许所有OPTIONS请求（预检请求）
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 允许访问Swagger和API文档
                .antMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // 允许访问静态资源
                .antMatchers("/", "/favicon.ico", "/**/*.png", "/**/*.gif", "/**/*.svg", 
                             "/**/*.jpg", "/**/*.html", "/**/*.css", "/**/*.js").permitAll()
                // 允许不需要认证的API路径 - 确保同时匹配/api/auth/**和/auth/**
                .antMatchers("/api/auth/**", "/auth/**").permitAll()
                // 开发环境允许H2数据库控制台
                .antMatchers("/h2-console/**").permitAll()
                // 用户相关API允许匿名访问（注册等）
                .antMatchers(HttpMethod.POST, "/api/users", "/users").permitAll()
                .antMatchers(HttpMethod.GET, "/api/users/exists/**", "/users/exists/**").permitAll()
                
                // 基于角色的访问控制
                // 管理员接口
                .antMatchers("/api/admin/**", "/admin/**").hasRole("admin")
                
                // 基于权限的访问控制
                // 文件上传
                .antMatchers(HttpMethod.POST, "/api/files/**", "/files/**").hasAuthority("file:upload")
                // 文件删除
                .antMatchers(HttpMethod.DELETE, "/api/files/**", "/files/**").hasAuthority("file:delete")
                // 文件分享
                .antMatchers("/api/files/*/share", "/files/*/share").hasAuthority("file:share")
                
                // 所有其他请求需要认证
                .anyRequest().authenticated()
            )
            // 禁用HTTP Basic认证
            .httpBasic(AbstractHttpConfigurer::disable)
            // 禁用表单登录
            .formLogin(AbstractHttpConfigurer::disable);

        // 允许在开发环境中嵌入式数据库控制台使用
        http.headers(headers -> headers.frameOptions().sameOrigin());

        // 添加JWT过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
} 