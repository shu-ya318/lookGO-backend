package com.mli.lookgo.module.auth.security;

import java.util.Collections;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mli.lookgo.module.auth.dao.UserDao;
import com.mli.lookgo.module.auth.model.entity.User;

/**
 * 實作 {@link UserDetailsService}，依據 email 從資料庫載入使用者資料供 Spring Security 驗證使用。
 *
 * @author D5042101
 * @since 2026.06.06
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserDao userDao;

    public UserDetailsServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userDao.getByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("找不到 email: " + email + " 的使用者!"));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.emptyList());
    }
}
