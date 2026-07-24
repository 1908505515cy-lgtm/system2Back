package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.WfInstanceNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfInstanceNodeMapper extends BaseMapper<WfInstanceNode> {

    @Select("SELECT * FROM wf_instance_node WHERE instance_id = #{instanceId} ORDER BY create_time ASC")
    List<WfInstanceNode> selectByInstanceId(@Param("instanceId") Long instanceId);

    @Select("SELECT * FROM wf_instance_node WHERE instance_id = #{instanceId} AND node_id = #{nodeId} LIMIT 1")
    WfInstanceNode selectByInstanceAndNode(@Param("instanceId") Long instanceId, @Param("nodeId") String nodeId);
}
