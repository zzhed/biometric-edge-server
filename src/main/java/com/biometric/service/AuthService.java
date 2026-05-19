package com.biometric.service;

import com.biometric.dto.LoginRequestDTO;
import com.biometric.dto.RegisterRequestDTO;

import java.util.Map;

public interface AuthService {
    void register(RegisterRequestDTO request);
    Map<String, Object> login(LoginRequestDTO request);
}
