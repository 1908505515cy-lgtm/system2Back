package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.common.GenericServiceImpl;
import com.example.entity.Document;
import com.example.mapper.DocumentMapper;
import com.example.service.DocumentService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DocumentServiceImpl extends GenericServiceImpl<Document, Document, Document>
        implements DocumentService {

    private final DocumentMapper documentMapper;

    public DocumentServiceImpl(DocumentMapper mapper, JdbcTemplate jdbcTemplate) {
        super(mapper, jdbcTemplate);
        this.documentMapper = mapper;
    }

    @Override
    protected void buildKeywordCondition(QueryWrapper<Document> wrapper, String keyword) {
        wrapper.and(w -> w
                .like("title", keyword)
                .or().like("content", keyword)
        );
    }

    @Override
    protected void buildTrashKeywordCondition(StringBuilder whereClause,
                                               java.util.List<Object> params, String keyword) {
        whereClause.append(" AND (title LIKE ? OR content LIKE ?)");
        String pattern = "%" + keyword + "%";
        params.add(pattern);
        params.add(pattern);
    }
}
