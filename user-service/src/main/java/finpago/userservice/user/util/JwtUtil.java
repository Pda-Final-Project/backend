package finpago.userservice.util;

import finpago.common.global.exception.error.ExpiredJwtException;
import finpago.common.global.exception.error.JwtValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;


@Component
public class JwtUtil {

    private final SecretKey key;

    @Value("${jwt.expiration}")
    private long expirationTime;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 🔹 JWT 토큰 생성 (userId 기반)
     * @param userId 사용자 ID
     * @return JWT 토큰
     */
    public String generateToken(Long userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))  // Subject에 userId 저장
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 🔹 JWT에서 userId 추출
     * @param token JWT 토큰
     * @return userId (Long 타입)
     */
    public Long extractUserId(String token) {
        return Long.parseLong(extractClaim(token, Claims::getSubject));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 🔹 JWT 검증
     * @param token JWT 토큰
     * @return 유효 여부 (true/false)
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true; // 검증 성공
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new ExpiredJwtException("JWT 토큰이 만료되었습니다.", e);
        } catch (JwtException e) {
            throw new JwtValidationException("JWT 검증 실패", e);
        }
    }
}