package com.biometric.service.impl;

import com.biometric.entity.BiometricModel;
import com.biometric.entity.DatasetInfo;
import com.biometric.entity.EdgeNode;
import com.biometric.entity.ProtectionMethod;
import com.biometric.exception.BizException;
import com.biometric.mapper.BiometricModelMapper;
import com.biometric.mapper.DatasetInfoMapper;
import com.biometric.mapper.EdgeNodeMapper;
import com.biometric.mapper.ProtectionMethodMapper;
import com.biometric.service.EdgeSimulationService;
import com.biometric.simulation.BiometricCloudSimEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class EdgeSimulationServiceImpl implements EdgeSimulationService {

    @Autowired private EdgeNodeMapper edgeNodeMapper;
    @Autowired private BiometricModelMapper modelMapper;
    @Autowired private DatasetInfoMapper datasetMapper;
    @Autowired private ProtectionMethodMapper protectionMethodMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public Map<String, Object> dispatchTask(Long nodeId, String taskType, int workload) {
        EdgeNode node = edgeNodeMapper.selectById(nodeId);
        if (node == null) throw new BizException(404, "边缘节点不存在");
        try {
            String url = "http://" + node.getHost() + ":" + node.getPort() + "/execute";
            @SuppressWarnings("unchecked")
            Map<String, Object> result = restTemplate.postForObject(url, Map.of("taskType", taskType, "workload", workload), Map.class);
            if (result != null) {
                long latencyMs = ((Number) result.getOrDefault("latencyMs", 0)).longValue();
                double energyMj = ((Number) result.getOrDefault("energyMj", 0)).doubleValue();
                edgeNodeMapper.insertTaskLog(nodeId, taskType, workload, latencyMs, energyMj, "success");
                return Map.of("latencyMs", latencyMs, "energyMj", energyMj, "nodeName", node.getName(), "tier", node.getTier());
            }
        } catch (Exception e) { log.debug("Docker 容器 {} 不可用，降级为 CloudSim 模拟", node.getName()); }
        int mips = node.getMips() != null ? node.getMips() : 1000;
        double baseCost = Map.of("preprocess", 0.3, "extract", 1.0, "protect", 0.15, "match", 0.5).getOrDefault(taskType, 1.0);
        long simLatency = (long) ((double) workload / mips * baseCost * 5000) + new Random().nextInt(50);
        double simEnergy = (node.getTier().equals("cloud") ? 200.0 : node.getTier().equals("edge") ? 30.0 : 5.0) * simLatency / 1000.0;
        edgeNodeMapper.insertTaskLog(nodeId, taskType, workload, simLatency, simEnergy, "simulated");
        return Map.of("latencyMs", simLatency, "energyMj", Math.round(simEnergy * 100) / 100.0,
                "nodeName", node.getName(), "tier", node.getTier(), "simulated", true);
    }

    @Override
    public Map<String, Object> runPipeline(Long modelId, Long datasetId,
                                            Long imageMethodId, Long templateMethodId,
                                            Long cloudNodeId, Long edgeNodeId, Long deviceNodeId,
                                            int sampleCount) {
        BiometricModel model = modelMapper.selectById(modelId);
        if (model == null) throw new BizException(404, "模型不存在");
        DatasetInfo dataset = datasetMapper.selectById(datasetId);
        if (dataset == null) throw new BizException(404, "数据集不存在");
        ProtectionMethod imageMethod = imageMethodId != null ? protectionMethodMapper.selectById(imageMethodId) : null;
        ProtectionMethod templateMethod = templateMethodId != null ? protectionMethodMapper.selectById(templateMethodId) : null;

        EdgeNode cloudNode = edgeNodeMapper.selectById(cloudNodeId);
        EdgeNode edgeNode = edgeNodeMapper.selectById(edgeNodeId);
        EdgeNode deviceNode = edgeNodeMapper.selectById(deviceNodeId);

        log.info("启动 CloudSim 管道模拟: model={}, dataset={}, samples={}", model.getName(), dataset.getName(), sampleCount);
        BiometricCloudSimEngine engine = new BiometricCloudSimEngine(cloudNode, edgeNode, deviceNode);
        return engine.runPipeline(model, dataset, imageMethod, templateMethod, sampleCount);
    }

    @Override
    public Map<String, Object> healthCheck(Long nodeId) {
        EdgeNode node = edgeNodeMapper.selectById(nodeId);
        if (node == null) throw new BizException(404, "边缘节点不存在");
        try {
            String url = "http://" + node.getHost() + ":" + node.getPort() + "/health";
            @SuppressWarnings("unchecked")
            Map<String, Object> health = restTemplate.getForObject(url, Map.class);
            if (health != null) { node.setStatus("online"); if (health.containsKey("cpuUsage")) node.setCpuUsage(((Number) health.get("cpuUsage")).doubleValue()); }
        } catch (Exception e) { node.setStatus("offline"); }
        edgeNodeMapper.update(node);
        return Map.of("id", node.getId(), "name", node.getName(), "status", node.getStatus(),
                "tier", node.getTier(), "cpuUsage", node.getCpuUsage() != null ? node.getCpuUsage() : 0);
    }

    @Override
    public List<Map<String, Object>> getTaskLogs(Long nodeId) { return List.of(); }
}
