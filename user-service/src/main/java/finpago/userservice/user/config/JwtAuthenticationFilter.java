package finpago.userservice.config;

import finpago.common.global.exception.error.JwtValidationException;
import finpago.userservice.util.JwtUtil;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        // 회원가입 및 로그인 요청은 필터 제외
        if (requestURI.startsWith("/v1/api/auth/signup") || requestURI.startsWith("/v1/api/auth/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 헤더에서 JWT 토큰 가져오기
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.error(" Authorization 헤더가 없음");
            throw new JwtValidationException("Authorization 헤더가 없습니다.");
        }

        String token = authHeader.substring(7);
        Long userId;

        try {
            userId = jwtUtil.extractUserId(token);
            log.info("추출된 userId: {}", userId);
        } catch (JwtException e) {
            log.error("JWT 검증 실패: {}", e.getMessage());
            throw new JwtValidationException("JWT 토큰 검증 실패", e);
        }

        // SecurityContext에 인증 정보가 없는 경우
        if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(String.valueOf(userId));

            if (jwtUtil.validateToken(token)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("SecurityContext에 userId({}) 인증 완료", userId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
