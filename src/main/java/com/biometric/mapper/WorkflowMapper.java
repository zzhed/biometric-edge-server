package com.biometric.mapper;

import com.biometric.entity.Workflow;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WorkflowMapper {

    @Select("SELECT * FROM workflow ORDER BY updated_at DESC")
    List<Workflow> selectAll();

    @Select("SELECT * FROM workflow WHERE id = #{id}")
    Workflow selectById(Long id);

    @Insert("INSERT INTO workflow (name, nodes, edges, created_at, updated_at) " +
            "VALUES (#{name}, #{nodes}, #{edges}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Workflow workflow);

    @Update("UPDATE workflow SET name=#{name}, nodes=#{nodes}, edges=#{edges}, updated_at=NOW() WHERE id=#{id}")
    int update(Workflow workflow);

    @Delete("DELETE FROM workflow WHERE id = #{id}")
    int deleteById(Long id);
}
