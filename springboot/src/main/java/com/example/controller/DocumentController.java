package com.example.controller;

import com.example.common.GenericController;
import com.example.entity.Document;
import com.example.service.DocumentService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/document")
public class DocumentController extends GenericController<Document, Document, Document> {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    protected DocumentService getService() {
        return documentService;
    }
}
