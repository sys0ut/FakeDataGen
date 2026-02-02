package com.example.fakedatagen.service;

import com.example.fakedatagen.model.DatabaseSchema;
import com.example.fakedatagen.parser.CubridSchemaParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SchemaAnalysisService {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaAnalysisService.class);
    
    @Autowired
    private CubridSchemaParser cubridSchemaParser;
    
    public DatabaseSchema parseSchema(String schemaText, boolean keepSchemaName) {
        log.debug("Parsing schema text - keepSchemaName={}", keepSchemaName);
        return cubridSchemaParser.parseSchema(schemaText, keepSchemaName);
    }
}
