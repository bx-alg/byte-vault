package com.bytevault.app.controller;

import com.bytevault.app.model.User;
import com.bytevault.app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    public void testGetAllUsers() throws Exception {
        // 准备测试数据
        User user1 = new User("user1", "user1@bytevault.com");
        User user2 = new User("user2", "user2@bytevault.com");
        List<User> allUsers = Arrays.asList(user1, user2);

        // 模拟服务行为
        when(userService.getAllUsers()).thenReturn(allUsers);

        // 执行并验证
        mockMvc.perform(get("/api/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("user1")))
                .andExpect(jsonPath("$[1].username", is("user2")));
    }

    @Test
    public void testGetUserByUsername() throws Exception {
        // 准备测试数据
        User user = new User("testuser", "test@bytevault.com");

        // 模拟服务行为
        when(userService.getUserByUsername("testuser")).thenReturn(user);

        // 执行并验证
        mockMvc.perform(get("/api/users/testuser")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("testuser")))
                .andExpect(jsonPath("$.email", is("test@bytevault.com")));
    }
} 