package com.biometric.service;

import com.biometric.entity.ProtectionMethod;

import java.util.List;

public interface ProtectionMethodService {
    List<ProtectionMethod> list();
    ProtectionMethod getById(Long id);
    void create(ProtectionMethod method);
    void update(ProtectionMethod method);
    void delete(Long id);
}
