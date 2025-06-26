package com.bytevault.app.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bytevault.app.mapper.UserMapper;
import com.bytevault.app.model.User;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    public List<User> getAllUsers() {
        return this.list();
    }
    
    public User getUserByUsername(String username) {
        return this.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
    }
    
    public void addUser(User user) {
        this.save(user);
    }
    
    // 初始化一些测试数据
    public void initSampleData() {
        if (this.count() == 0) {
            // 使用BCrypt加密的密码，对应"admin"
            this.save(new User("admin", "$2a$10$CehLbipOZVv0VqQtxx3L2ehR5OXZtEvQWPL1DaU4TXsGfq66yINjW"));
            // 使用BCrypt加密的密码，对应"password"
            this.save(new User("user1", "$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW"));
        }
    }
} 