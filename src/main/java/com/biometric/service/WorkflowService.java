package com.biometric.service;

import com.biometric.entity.Workflow;

import java.util.List;

public interface WorkflowService {
    List<Workflow> list();
    Workflow getById(Long id);
    void create(Workflow workflow);
    void update(Long id, Workflow workflow);
    void delete(Long id);
}
