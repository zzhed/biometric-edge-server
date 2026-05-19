package com.biometric.service.impl;

import com.biometric.entity.DatasetInfo;
import com.biometric.exception.BizException;
import com.biometric.mapper.DatasetInfoMapper;
import com.biometric.service.DatasetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DatasetServiceImpl implements DatasetService {

    private final DatasetInfoMapper mapper;

    @Override
    public List<DatasetInfo> list() {
        return mapper.selectAll();
    }

    @Override
    public DatasetInfo getById(Long id) {
        DatasetInfo ds = mapper.selectById(id);
        if (ds == null) throw new BizException(404, "数据集不存在");
        return ds;
    }

    @Override
    public void create(DatasetInfo dataset) {
        mapper.insert(dataset);
    }

    @Override
    public void update(DatasetInfo dataset) {
        getById(dataset.getId());
        mapper.update(dataset);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }
}
