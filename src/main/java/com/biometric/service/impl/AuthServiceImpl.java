package com.biometric.service.impl;

import com.biometric.dto.LoginRequestDTO;
import com.biometric.dto.RegisterRequestDTO;
import com.biometric.entity.User;
import com.biometric.exception.BizException;
import com.biometric.mapper.UserMapper;
import com.biometric.service.AuthService;
import com.biometric.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public void register(RegisterRequestDTO request) {
        if (userMapper.selectByUsername(request.getUsername()) != null) {
            throw new BizException(400, "用户名已存在");
        }
        if (userMapper.selectByEmail(request.getEmail()) != null) {
            throw new BizException(400, "该邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Override
    public Map<String, Object> login(LoginRequestDTO request) {
        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return Map.of("token", token, "username", user.getUsername());
    }
}
