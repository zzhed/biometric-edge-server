package com.biometric.controller;

import com.biometric.dto.Result;
import com.biometric.entity.EdgeNode;
import com.biometric.mapper.EdgeNodeMapper;
import com.biometric.service.EdgeSimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/edge-nodes")
public class EdgeNodeController {

    @Autowired
    private EdgeNodeMapper edgeNodeMapper;
    @Autowired
    private EdgeSimulationService edgeSimulationService;

    @GetMapping
    public Result<List<EdgeNode>> list() {
        return Result.success(edgeNodeMapper.selectAll());
    }

    @PostMapping("/{id}/health")
    public Result<Map<String, Object>> healthCheck(@PathVariable Long id) {
        return Result.success(edgeSimulationService.healthCheck(id));
    }

    @PostMapping("/pipeline")
    public Result<Map<String, Object>> runPipeline(@RequestBody Map<String, Object> params) {
        Long modelId = parseLong(params.get("modelId"));
        Long datasetId = parseLong(params.get("datasetId"));
        Long imageMethodId = parseOptionalLong(params.get("imageMethodId"));
        Long templateMethodId = parseOptionalLong(params.get("templateMethodId"));
        Long cloudNodeId = parseLong(params.get("cloudNodeId"));
        Long edgeNodeId = parseLong(params.get("edgeNodeId"));
        Long deviceNodeId = parseLong(params.get("deviceNodeId"));
        int sampleCount = params.get("sampleCount") instanceof Number n ? n.intValue() : 100;
        return Result.success(edgeSimulationService.runPipeline(
                modelId, datasetId, imageMethodId, templateMethodId,
                cloudNodeId, edgeNodeId, deviceNodeId, sampleCount));
    }

    /**
     * 将对象转换为Long类型
     * 支持Number类型直接转换和String类型解析
     *
     * @param v 待转换的对象，可以是Number、String或null
     * @return 转换后的Long值，如果输入为null或无法转换则返回null
     */
    private Long parseLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s) return Long.parseLong(s);
        return null;
    }
    private Long parseOptionalLong(Object v) {
        if (v == null || v.toString().isEmpty()) return null;
        return parseLong(v);
    }
}
