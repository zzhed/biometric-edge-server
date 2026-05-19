package com.biometric.mapper;

import com.biometric.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user_info WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM user_info WHERE email = #{email}")
    User selectByEmail(String email);

    @Insert("INSERT INTO user_info (username, password, email, status, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{email}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
}
