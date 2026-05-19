package com.biometric.mapper;

import com.biometric.entity.ProtectionMethod;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProtectionMethodMapper {

    @Select("SELECT * FROM protection_method ORDER BY updated_at DESC")
    List<ProtectionMethod> selectAll();

    @Select("SELECT * FROM protection_method WHERE id = #{id}")
    ProtectionMethod selectById(Long id);

    @Insert("INSERT INTO protection_method (name, level, type, description, parameters, created_at, updated_at) " +
            "VALUES (#{name}, #{level}, #{type}, #{description}, #{parameters}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ProtectionMethod method);

    @Update("UPDATE protection_method SET name=#{name}, level=#{level}, type=#{type}, " +
            "description=#{description}, parameters=#{parameters}, updated_at=NOW() WHERE id=#{id}")
    int update(ProtectionMethod method);

    @Delete("DELETE FROM protection_method WHERE id = #{id}")
    int deleteById(Long id);
}
