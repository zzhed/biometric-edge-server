package com.biometric.service.impl;

import com.biometric.entity.ProtectionMethod;
import com.biometric.exception.BizException;
import com.biometric.mapper.ProtectionMethodMapper;
import com.biometric.service.ProtectionMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProtectionMethodServiceImpl implements ProtectionMethodService {

    private final ProtectionMethodMapper mapper;

    @Override
    public List<ProtectionMethod> list() {
        return mapper.selectAll();
    }

    @Override
    public ProtectionMethod getById(Long id) {
        ProtectionMethod pm = mapper.selectById(id);
        if (pm == null) throw new BizException(404, "保护方法不存在");
        return pm;
    }

    @Override
    public void create(ProtectionMethod method) {
        mapper.insert(method);
    }

    @Override
    public void update(ProtectionMethod method) {
        getById(method.getId());
        mapper.update(method);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }
}
