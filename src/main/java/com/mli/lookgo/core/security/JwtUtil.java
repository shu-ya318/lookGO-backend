package com.mli.lookgo.core.security;

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
 * 處理 JWT token的生成與驗證。
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
     * @param secret
     * @param accessTokenExpiration
     * @param refreshTokenExpiration
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException("JWT 密鑰不得為空!");
        }

        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
    }

    // ----- 通用 -----

    /**
     * 從(存取或刷新)token中取得 JWT ID (jti)。
     *
     * @param token
     * @return JWT ID (jti)
     */
    public String getJtiFromToken(String token) {
        return jwtParser.parseClaimsJws(token).getBody().getId();
    }

    /**
     * 從(存取或刷新)token中取得過期時間。
     *
     * @param token
     * @return token過期時間
     */
    public Date getExpirationDateFromToken(String token) {
        return jwtParser.parseClaimsJws(token).getBody().getExpiration();
    }

    // ----- Access Token -----

    /**
     * 依據傳入資訊生成存取token。
     *
     * @param id
     * @param uuid
     * @param email
     * @param roles
     * @return 存取token字串
     */
    public String generateAccessToken(Integer id, UUID uuid, String email, Set<String> roles) {
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
     * 驗證存取token的有效性。
     *
     * @param accessToken
     * @return 驗證存取token是否有效的結果
     */
    public boolean validateAccessToken(String accessToken) {
        try {
            jwtParser.parseClaimsJws(accessToken);

            return true;
        } catch (JwtException | IllegalArgumentException exception) {

            return false;
        }
    }

    /**
     * 從存取token中取得 Email。
     *
     * @param accessToken
     * @return Email
     */
    public String getEmailFromAccessToken(String accessToken) {
        return jwtParser.parseClaimsJws(accessToken).getBody().get("email", String.class);
    }

    /**
     * 從存取token中取得剩餘有效時間。
     *
     * @param accessToken 存取token
     * @return 剩餘有效時間（毫秒）
     */
    public long getRemainingTtlFromAccessToken(String accessToken) {
        Date expiration = getExpirationDateFromToken(accessToken);

        return expiration.getTime() - System.currentTimeMillis();
    }

    // ----- Refresh Token -----

    /**
     * 依據傳入資訊生成刷新token。
     *
     * @param email
     * @return String (刷新token)
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
     * 驗證刷新token的有效性。
     *
     * @param refreshToken
     * @return 驗證刷新token是否有效的結果
     */
    public boolean validateRefreshToken(String refreshToken) {
        try {
            jwtParser.parseClaimsJws(refreshToken);

            return true;
        } catch (JwtException | IllegalArgumentException exception) {

            return false;
        }
    }

    /**
     * 取得刷新token的過期時間設定。
     *
     * @return 刷新token過期時間（毫秒）
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }

    /**
     * 從刷新token中取得 Email。
     *
     * @param refreshToken
     * @return Email
     * @throws JwtException
     */
    public String getEmailFromRefreshToken(String refreshToken) throws JwtException {
        return jwtParser.parseClaimsJws(refreshToken).getBody().getSubject();
    }
}
