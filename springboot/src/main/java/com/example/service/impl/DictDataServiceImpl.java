package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.common.OptionItem;
import com.example.entity.DictData;
import com.example.mapper.DictDataMapper;
import com.example.service.DictDataService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DictDataServiceImpl extends GenericServiceImpl<DictData, DictData, DictData>
        implements DictDataService {

    private final DictDataMapper dictDataMapper;

    public DictDataServiceImpl(DictDataMapper mapper, JdbcTemplate jdbcTemplate) {
        super(mapper, jdbcTemplate);
        this.dictDataMapper = mapper;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<DictData> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("label", keyword)
                .or().like("dict_type_code", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause, java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (label LIKE ? OR dict_type_code LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
    }

    @Override
    public List<OptionItem> getOptionsByTypeCode(String dictTypeCode) {
        List<DictData> list = dictDataMapper.selectByTypeCode(dictTypeCode);
        return list.stream()
                .map(d -> new OptionItem(d.getLabel(), parseValue(d.getValue())))
                .collect(Collectors.toList());
    }

    private Object parseValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
