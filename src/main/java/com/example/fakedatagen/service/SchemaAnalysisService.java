package com.example.fakedatagen.service;

import com.example.fakedatagen.model.DatabaseSchema;
import com.example.fakedatagen.parser.CubridSchemaParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SchemaAnalysisService {
    
    @Autowired
    private CubridSchemaParser cubridSchemaParser;
    
    public DatabaseSchema parseSchema(String schemaText, boolean keepSchemaName) {
        return cubridSchemaParser.parseSchema(schemaText, keepSchemaName);
    }
}
