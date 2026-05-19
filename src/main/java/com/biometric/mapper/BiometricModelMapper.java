package com.biometric.mapper;

import com.biometric.entity.BiometricModel;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BiometricModelMapper {

    @Select("SELECT * FROM biometric_model ORDER BY updated_at DESC")
    List<BiometricModel> selectAll();

    @Select("SELECT * FROM biometric_model WHERE id = #{id}")
    BiometricModel selectById(Long id);

    @Insert("INSERT INTO biometric_model (name, modality, version, status, description, created_at, updated_at) " +
            "VALUES (#{name}, #{modality}, #{version}, #{status}, #{description}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(BiometricModel model);

    @Update("UPDATE biometric_model SET name=#{name}, modality=#{modality}, version=#{version}, " +
            "status=#{status}, description=#{description}, updated_at=NOW() WHERE id=#{id}")
    int update(BiometricModel model);

    @Delete("DELETE FROM biometric_model WHERE id = #{id}")
    int deleteById(Long id);
}
