package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.DictType;
import com.example.mapper.DictTypeMapper;
import com.example.service.DictTypeService;
import org.springframework.stereotype.Service;

@Service
public class DictTypeServiceImpl extends GenericServiceImpl<DictType, DictType, DictType>
        implements DictTypeService {

    public DictTypeServiceImpl(DictTypeMapper mapper) {
        super(mapper);
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<DictType> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("name", keyword)
                .or().like("code", keyword)
        );
    }
}
