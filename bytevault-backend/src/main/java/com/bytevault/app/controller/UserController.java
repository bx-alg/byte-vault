package com.bytevault.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.bytevault.app.model.User;
import com.bytevault.app.service.UserService;

import java.util.List;
import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @PostConstruct
    public void init() {
        // 初始化示例数据
        userService.initSampleData();
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{username}")
    public User getUserByUsername(@PathVariable String username) {
        return userService.getUserByUsername(username);
    }

    @PostMapping
    public void addUser(@RequestBody User user) {
        userService.addUser(user);
    }
} 