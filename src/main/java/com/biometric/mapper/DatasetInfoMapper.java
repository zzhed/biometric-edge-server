package com.biometric.mapper;

import com.biometric.entity.DatasetInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DatasetInfoMapper {

    @Select("SELECT * FROM dataset_info ORDER BY created_at DESC")
    List<DatasetInfo> selectAll();

    @Select("SELECT * FROM dataset_info WHERE id = #{id}")
    DatasetInfo selectById(Long id);

    @Insert("INSERT INTO dataset_info (name, modality, sample_count, description, created_at) " +
            "VALUES (#{name}, #{modality}, #{sampleCount}, #{description}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(DatasetInfo dataset);

    @Update("UPDATE dataset_info SET name=#{name}, modality=#{modality}, " +
            "sample_count=#{sampleCount}, description=#{description} WHERE id=#{id}")
    int update(DatasetInfo dataset);

    @Delete("DELETE FROM dataset_info WHERE id = #{id}")
    int deleteById(Long id);
}
