package com.mli.lookgo.module.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisService {

  private final StringRedisTemplate stringRedisTemplate;

  public RedisService(StringRedisTemplate stringRedisTemplate) {
    this.stringRedisTemplate = stringRedisTemplate;
  }

  // ===== Black list of Access Token (jti) =====
  public void saveAccessTokenJtiToBlacklist(String jti, long duration, TimeUnit unit) {
    stringRedisTemplate.opsForValue().set("blacklist:access_token:" + jti, "true", duration, unit);
  }

  public boolean isAccessTokenJtiInBlacklist(String jti) {
    return Boolean.TRUE.equals(stringRedisTemplate.hasKey("blacklist:access_token:" + jti));
  }

  // ===== White list of Refresh Token (jti) =====
  public void saveRefreshTokenJti(String userId, String jti, long duration, TimeUnit unit) {
    stringRedisTemplate.opsForValue().set("refresh_token_jti:" + userId, jti, duration, unit);
  }

  public String getRefreshTokenJti(String userId) {
    return stringRedisTemplate.opsForValue().get("refresh_token_jti:" + userId);
  }

  public void deleteRefreshTokenJti(String userId) {
    stringRedisTemplate.delete("refresh_token_jti:" + userId);
  }

  // ===== Reset Password Token =====
  public void saveResetPasswordToken(String resetPasswordToken, String email, long duration, TimeUnit unit) {
    stringRedisTemplate.opsForValue().set("reset_password_token:" + resetPasswordToken, email, duration, unit);
  }

  public String getEmailByResetPasswordToken(String resetPasswordToken) {
    return stringRedisTemplate.opsForValue().get("reset_password_token:" + resetPasswordToken);
  }

  public void deleteResetPasswordToken(String resetPasswordToken) {
    stringRedisTemplate.delete("reset_password_token:" + resetPasswordToken);
  }
}
