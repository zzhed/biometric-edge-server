package com.biometric.controller;

import com.biometric.dto.Result;
import com.biometric.dto.LoginRequestDTO;
import com.biometric.dto.RegisterRequestDTO;
import com.biometric.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO){
        authService.register(registerRequestDTO);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<Object> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        return Result.success(authService.login(loginRequestDTO));
    }
}
