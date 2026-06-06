package com.mli.lookgo.module.auth.security;

import java.util.UUID;
import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;

/**
 * 處理 JWT 令牌的生成與驗證。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Component
public class JwtUtil {

    private long accessTokenExpiration;
    private long refreshTokenExpiration;
    private final SecretKey key;
    private final JwtParser jwtParser;

    /**
     * 讓 Spring 容器能在應用程式啟動時，自動注入 JWT 相關配置。
     *
     * @param secret                 JWT 密鑰
     * @param accessTokenExpiration  存取令牌過期時間（毫秒）
     * @param refreshTokenExpiration 刷新令牌過期時間（毫秒）
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密鑰不得為空");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
    }

    /**
     * 依據傳入資訊生成存取令牌。
     *
     * @param id    使用者 id
     * @param uuid  唯一識別碼
     * @param email 電子郵件地址
     * @param roles 使用者角色清單
     * @return 存取令牌字串
     */
    public String generateAccessToken(Long id, UUID uuid, String email, Set<String> roles) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(uuid.toString())
                .claim("id", id)
                .claim("email", email)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 依據傳入資訊生成刷新令牌。
     *
     * @param email 電子郵件地址
     * @return 刷新令牌字串
     */
    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 驗證存取令牌的有效性。
     *
     * @param token 存取令牌
     * @return 令牌是否有效
     */
    public boolean validateAccessToken(String token) {
        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException error) {
            return false;
        }
    }

    /**
     * 驗證刷新令牌的有效性。
     *
     * @param refreshToken 刷新令牌
     * @return 令牌是否有效
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            jwtParser.parseClaimsJws(refreshToken);
            return true;
        } catch (JwtException | IllegalArgumentException error) {
            return false;
        }
    }

    /**
     * 取得刷新令牌的過期時間設定。
     *
     * @return 刷新令牌過期時間（毫秒）
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * 從存取令牌中取得電子郵件地址。
     *
     * @param token 存取令牌
     * @return 電子郵件地址
     */
    public String getEmailFromToken(String token) {
        return jwtParser.parseClaimsJws(token).getBody().get("email", String.class);
    }

    /**
     * 從刷新令牌中取得電子郵件地址。
     *
     * @param refreshToken 刷新令牌
     * @return 電子郵件地址
     * @throws JwtException 令牌無效或解析失敗時拋出例外。
     */
    public String getEmailFromRefreshToken(String refreshToken) throws JwtException {
        return jwtParser.parseClaimsJws(refreshToken).getBody().getSubject();
    }

    /**
     * 從存取令牌中取得 JWT ID (jti)。
     *
     * @param token 存取令牌
     * @return JWT ID
     */
    public String getJtiFromToken(String token) {
        return jwtParser.parseClaimsJws(token).getBody().getId();
    }

    /**
     * 從存取令牌中取得過期時間。
     *
     * @param token 存取令牌
     * @return 令牌過期時間
     */
    public Date getExpirationDateFromToken(String token) {
        return jwtParser.parseClaimsJws(token).getBody().getExpiration();
    }
}
