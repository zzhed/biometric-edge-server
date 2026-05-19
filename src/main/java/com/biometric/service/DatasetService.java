package com.biometric.service;

import com.biometric.entity.DatasetInfo;

import java.util.List;

public interface DatasetService {
    List<DatasetInfo> list();
    DatasetInfo getById(Long id);
    void create(DatasetInfo dataset);
    void update(DatasetInfo dataset);
    void delete(Long id);
}
