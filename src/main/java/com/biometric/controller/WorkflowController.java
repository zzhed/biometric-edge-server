package com.biometric.controller;

import com.biometric.dto.Result;
import com.biometric.entity.Workflow;
import com.biometric.service.WorkflowService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        List<Map<String, Object>> summaries = workflowService.list().stream().map(wf -> {
            int nodeCount = 0;
            String nodesJson = wf.getNodes();
            if (nodesJson != null && !nodesJson.isEmpty()) {
                try {
                    JsonNode arr = objectMapper.readTree(nodesJson);
                    nodeCount = arr.isArray() ? arr.size() : 0;
                } catch (Exception ignored) {
                }
            }
            return Map.<String, Object>of(
                    "id", wf.getId().toString(),
                    "name", wf.getName(),
                    "nodeCount", nodeCount,
                    "updatedAt", wf.getUpdatedAt() != null ? wf.getUpdatedAt().toString() : ""
            );
        }).toList();
        return Result.success(summaries);
    }

    @GetMapping("/{id}")
    public Result<Workflow> get(@PathVariable Long id) {
        return Result.success(workflowService.getById(id));
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Workflow workflow) {
        workflowService.create(workflow);
        return Result.success(Map.of("id", workflow.getId().toString()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Workflow workflow) {
        workflowService.update(id, workflow);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workflowService.delete(id);
        return Result.success();
    }
}
