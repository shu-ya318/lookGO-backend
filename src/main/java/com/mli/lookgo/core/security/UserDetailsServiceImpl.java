package com.mli.lookgo.core.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mli.lookgo.module.user.dao.UserDAO;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.enums.UserStatus;
import com.mli.lookgo.module.user.model.entity.User;

/**
 * 實作 {@link UserDetailsService}，依據 email 從資料庫載入使用者資料供 Spring Security 驗證使用。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDAO userDAO;
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    public UserDetailsServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("[UserDetailsService] 開始載入使用者，email={}", email);

        User user = userDAO.getByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("找不到 email: " + email + " 的使用者!"));

        logger.debug("[UserDetailsService] 資料庫查詢成功，userId={}, roleId={}", user.getId(), user.getRoleId());

        if (user.getStatus() == UserStatus.DISABLED.getCode()) {
            logger.warn("[UserDetailsService] 使用者帳號已被禁用，email={}", email);
            throw new UsernameNotFoundException("該帳號已被禁用，如有問題請洽管理者!");
        }

        UserRole userRole = UserRole.fromId(user.getRoleId());
        String role = "ROLE_" + userRole.getLabel();

        logger.debug("[UserDetailsService] 組成的 authority 字串: {} (UserRole={})", role, userRole);

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        logger.debug("[UserDetailsService] 最終 authorities={}", authorities);

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                authorities);
    }
}
