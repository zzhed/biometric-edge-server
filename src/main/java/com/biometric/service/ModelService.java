package com.biometric.service;

import com.biometric.entity.BiometricModel;

import java.util.List;

public interface ModelService {
    List<BiometricModel> list();
    BiometricModel getById(Long id);
    void create(BiometricModel model);
    void update(BiometricModel model);
    void delete(Long id);
}
