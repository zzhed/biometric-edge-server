package com.biometric.service;

import java.util.List;
import java.util.Map;

public interface EdgeSimulationService {
    Map<String, Object> dispatchTask(Long nodeId, String taskType, int workload);

    /** 使用 CloudSim 模拟完整管道 + 生物特征评估指标 */
    Map<String, Object> runPipeline(Long modelId, Long datasetId,
                                    Long imageMethodId, Long templateMethodId,
                                    Long cloudNodeId, Long edgeNodeId, Long deviceNodeId,
                                    int sampleCount);

    Map<String, Object> healthCheck(Long nodeId);
    List<Map<String, Object>> getTaskLogs(Long nodeId);
}
