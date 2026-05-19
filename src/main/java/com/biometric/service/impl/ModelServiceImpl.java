package com.biometric.service.impl;

import com.biometric.entity.BiometricModel;
import com.biometric.exception.BizException;
import com.biometric.mapper.BiometricModelMapper;
import com.biometric.service.ModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModelServiceImpl implements ModelService {

    private final BiometricModelMapper mapper;

    @Override
    public List<BiometricModel> list() {
        return mapper.selectAll();
    }

    @Override
    public BiometricModel getById(Long id) {
        BiometricModel model = mapper.selectById(id);
        if (model == null) throw new BizException(404, "模型不存在");
        return model;
    }

    @Override
    public void create(BiometricModel model) {
        model.setStatus("active");
        mapper.insert(model);
    }

    @Override
    public void update(BiometricModel model) {
        getById(model.getId());
        mapper.update(model);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }
}
