package finpago.gateway.filter.config;

import finpago.gateway.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

//    private final JwtUtil jwtUtil;
//    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        System.out.println("okokokok");
        if (requestURI.startsWith("/v1/api/auth/join") || requestURI.startsWith("/v1/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }
        System.out.println("nononono");

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authorization header가 없습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        Long userId;

//        try {
//            userId = jwtUtil.extractUserId(token);
//        } catch (JwtException e) {
//            log.error("JWT 토큰 검증 실패: {}", e.getMessage());
//            throw new RuntimeException("JWT 토큰 검증 실패", e);
//        }
//
//        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
//            UserDetails userDetails = userDetailsService.loadUserById(userId);
//
//            if (jwtUtil.validateToken(token)) {
//                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
//                        userDetails, null, userDetails.getAuthorities()
//                );
//                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
//                SecurityContextHolder.getContext().setAuthentication(authToken);
//            }
//        }

        filterChain.doFilter(request, response);
    }
}

