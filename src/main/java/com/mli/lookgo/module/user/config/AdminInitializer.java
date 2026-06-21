package com.mli.lookgo.module.user.config;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mli.lookgo.module.auth.dao.AuthDao;
import com.mli.lookgo.module.user.enums.MembershipTier;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.enums.UserStatus;
import com.mli.lookgo.module.user.model.entity.User;

/**
 * 應用程式啟動時自動建立預設管理員帳號（若不存在）。
 *
 * @author D5042101
 * @since 2026.06.21
 */
@Component
public class AdminInitializer implements ApplicationRunner {

    private final AuthDao authDao;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    public AdminInitializer(AuthDao authDao, PasswordEncoder passwordEncoder) {
        this.authDao = authDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (authDao.existsByEmail(adminEmail)) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        User admin = new User();

        admin.setMembershipTierId(MembershipTier.PREMIUM.getId());
        admin.setRoleId(UserRole.ADMIN.getId());
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setUsername(adminUsername);
        admin.setStatus(UserStatus.ACTIVE.getCode());
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin.setLastLoginAt(now);

        authDao.createUser(admin);
    }
}
