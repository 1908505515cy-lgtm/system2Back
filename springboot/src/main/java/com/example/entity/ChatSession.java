package com.example.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.common.annotations.FieldMeta;
import com.example.common.annotations.ModuleMeta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_session")
@ModuleMeta(name = "chatSession", title = "对话会话", icon = "ChatDotRound", features = "")
public class ChatSession {

    @TableId(type = IdType.AUTO)
    @FieldMeta(label = "ID", showInForm = false)
    private Long id;

    @FieldMeta(label = "标题", searchable = true)
    private String title;

    @FieldMeta(label = "用户ID", showInTable = false, showInForm = false)
    private Long userId;

    @FieldMeta(label = "创建时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @FieldMeta(label = "更新时间", type = "date", showInForm = false)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    @TableLogic
    private Integer deleted = 0;
}
