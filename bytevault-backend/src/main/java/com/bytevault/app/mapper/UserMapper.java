package com.bytevault.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bytevault.app.model.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承BaseMapper后，可以直接使用内置的CRUD方法
    // 也可以在这里添加自定义的SQL方法
} 