package com.mli.lookgo.module.user.config;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mli.lookgo.core.dao.AuthDAO;
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

    private final AuthDAO authDAO;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.username}")
    private String adminUsername;

    public AdminInitializer(AuthDAO authDAO, PasswordEncoder passwordEncoder) {
        this.authDAO = authDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (authDAO.existsByEmail(adminEmail)) {
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

        authDAO.createUser(admin);
    }
}
