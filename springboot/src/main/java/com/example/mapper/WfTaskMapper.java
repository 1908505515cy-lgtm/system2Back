package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.WfTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WfTaskMapper extends BaseMapper<WfTask> {

    @Select("SELECT * FROM wf_task WHERE deleted = 0 AND assignee = #{username} AND action = 'pending' ORDER BY create_time DESC")
    List<WfTask> selectPendingByAssignee(@Param("username") String username);

    @Select("SELECT * FROM wf_task WHERE deleted = 0 AND assignee = #{username} AND action != 'pending' ORDER BY update_time DESC")
    List<WfTask> selectDoneByAssignee(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM wf_task WHERE deleted = 0 AND assignee = #{username} AND action = 'pending'")
    int countPending(@Param("username") String username);
}
