package com.biometric.service.impl;

import com.biometric.entity.Workflow;
import com.biometric.exception.BizException;
import com.biometric.mapper.WorkflowMapper;
import com.biometric.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowMapper mapper;

    @Override
    public List<Workflow> list() {
        return mapper.selectAll();
    }

    @Override
    public Workflow getById(Long id) {
        Workflow wf = mapper.selectById(id);
        if (wf == null) throw new BizException(404, "工作流不存在");
        return wf;
    }

    @Override
    public void create(Workflow workflow) {
        mapper.insert(workflow);
    }

    @Override
    public void update(Long id, Workflow workflow) {
        getById(id);
        workflow.setId(id);
        mapper.update(workflow);
    }

    @Override
    public void delete(Long id) {
        getById(id);
        mapper.deleteById(id);
    }
}
