package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.WfInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfInstanceMapper extends BaseMapper<WfInstance> {

    @Select("SELECT * FROM wf_instance WHERE deleted = 0 AND initiator = #{username} ORDER BY create_time DESC")
    List<WfInstance> selectByInitiator(@Param("username") String username);
}
