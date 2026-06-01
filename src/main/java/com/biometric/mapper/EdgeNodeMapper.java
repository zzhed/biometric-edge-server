package com.biometric.mapper;

import com.biometric.entity.EdgeNode;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EdgeNodeMapper {

    @Select("SELECT * FROM edge_node ORDER BY FIELD(tier, 'cloud', 'edge', 'device'), id")
    List<EdgeNode> selectAll();

    @Select("SELECT * FROM edge_node WHERE id = #{id}")
    EdgeNode selectById(Long id);

    @Update("UPDATE edge_node SET name=#{name}, tier=#{tier}, host=#{host}, port=#{port}, mips=#{mips}, " +
            "status=#{status}, cpu_usage=#{cpuUsage} WHERE id=#{id}")
    int update(EdgeNode node);

    @Insert("INSERT INTO edge_task_log (node_id, task_type, workload, latency_ms, energy_mj, status, created_at) " +
            "VALUES (#{nodeId}, #{taskType}, #{workload}, #{latencyMs}, #{energyMj}, #{status}, NOW())")
    int insertTaskLog(@Param("nodeId") Long nodeId, @Param("taskType") String taskType,
                      @Param("workload") int workload, @Param("latencyMs") long latencyMs,
                      @Param("energyMj") double energyMj, @Param("status") String status);
}
