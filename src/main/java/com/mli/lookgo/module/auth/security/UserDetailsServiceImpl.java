package com.mli.lookgo.module.auth.security;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mli.lookgo.module.user.dao.UserDao;
import com.mli.lookgo.module.user.enums.UserRole;
import com.mli.lookgo.module.user.model.entity.User;

/**
 * 實作 {@link UserDetailsService}，依據 email 從資料庫載入使用者資料供 Spring Security 驗證使用。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDao userDao;
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    public UserDetailsServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("[UserDetailsService] 開始載入使用者，email={}", email);

        User user = userDao.getByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("找不到 email: " + email + " 的使用者!"));

        logger.debug("[UserDetailsService] 資料庫查詢成功，userId={}, roleId={}", user.getId(), user.getRoleId());

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
