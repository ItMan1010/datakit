package com.itman.datakit.admin.service;

import com.itman.datakit.admin.common.entity.User;
import org.springframework.stereotype.Service;

/**
 * TODO
 *
 * @author: ItMan
 * @since: 2023/10/19  16:51
 */

@Service
public class UserService {
    public User validate(String username, String password) {
        User user = new User();
        user.setId(100);
        user.setUsername("ItMan");
        user.setPassword("password");
        if (user == null || !user.getPassword().equals(password)) {
            return null;
        }
        return user;
    }
}
