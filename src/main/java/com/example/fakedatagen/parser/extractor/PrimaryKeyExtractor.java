package com.example.fakedatagen.parser.extractor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CUBRID 스키마에서 PRIMARY KEY 제약조건을 추출하는 클래스
 */
@Component
public class PrimaryKeyExtractor {
    
    /**
     * 스키마 텍스트에서 PRIMARY KEY 제약조건 추출
     * 
     * @param schemaText 스키마 정의 텍스트
     * @param keepSchemaName 스키마명 유지 여부
     * @return 테이블별 PRIMARY KEY 컬럼 맵 (key: schema.table, value: PK 컬럼 리스트)
     */
    public Map<String, List<String>> extract(String schemaText, boolean keepSchemaName) {
        Pattern pkPattern = Pattern.compile(
                "ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+ATTRIBUTE\\s+CONSTRAINT\\s+\\[(.*?)\\]\\s+PRIMARY\\s+KEY\\((.*?)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher pkMatcher = pkPattern.matcher(schemaText);
        
        Map<String, List<String>> pkMap = new HashMap<>();
        
        while (pkMatcher.find()) {
            String schemaName = pkMatcher.group(1).toLowerCase();
            String tableName = pkMatcher.group(2).toLowerCase();
            String columns = pkMatcher.group(4);
            
            String key;
            if (keepSchemaName) {
                key = schemaName + "." + tableName;
            } else {
                key = tableName;
            }
            
            String[] pkColumns = columns.replaceAll("\\[|\\]", "").split(",");
            for (String pkCol : pkColumns) {
                pkMap.computeIfAbsent(key, k -> new ArrayList<>()).add(pkCol.trim().toLowerCase());
            }
        }
        
        return pkMap;
    }
}

