package com.bytevault.app.auth.config;

import com.bytevault.app.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private JwtUtils jwtUtils;
    private UserDetailsService userDetailsService;
    private TokenService tokenService;
    
    // 不需要验证的路径
    private final List<String> excludedPaths = Arrays.asList(
        "/api/auth/login", 
        "/auth/login",
        "/api/auth/register", 
        "/auth/register"
    );
    
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    public void setJwtUtils(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Autowired
    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }
    
    @Autowired
    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        boolean shouldExclude = excludedPaths.stream()
            .anyMatch(pattern -> pathMatcher.match(pattern, path));
        
        if (shouldExclude) {
            log.debug("不过滤路径: {}", path);
        }
        
        return shouldExclude;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            log.debug("处理请求: {} {}, JWT: {}", request.getMethod(), request.getRequestURI(), jwt != null ? "存在" : "不存在");
            
            if (jwt != null) {
                // 使用TokenService验证令牌
                if (tokenService.validateToken(jwt)) {
                    String username = jwtUtils.extractUsername(jwt);
                    
                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("用户 {} 认证成功", username);
                        
                        // 尝试刷新令牌
                        try {
                            String newToken = tokenService.refreshToken(jwt);
                            if (!newToken.equals(jwt)) {
                                // 如果令牌已更新，在响应头中返回新令牌
                                response.setHeader("Authorization", "Bearer " + newToken);
                                response.setHeader("Access-Control-Expose-Headers", "Authorization");
                                log.debug("令牌已刷新");
                            }
                        } catch (Exception e) {
                            log.warn("令牌刷新失败", e);
                            // 令牌刷新失败不影响请求处理
                        }
                    }
                } else {
                    log.debug("令牌验证失败");
                }
            }
        } catch (Exception e) {
            log.error("无法设置用户认证: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}