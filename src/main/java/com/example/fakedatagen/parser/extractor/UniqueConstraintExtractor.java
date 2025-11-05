package com.example.fakedatagen.parser.extractor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CUBRID 스키마에서 UNIQUE 제약조건을 추출하는 클래스
 */
@Component
public class UniqueConstraintExtractor {
    
    /**
     * 스키마 텍스트에서 UNIQUE 제약조건 추출
     * 
     * @param schemaText 스키마 정의 텍스트
     * @param keepSchemaName 스키마명 유지 여부
     * @return 테이블별 UNIQUE 컬럼 맵 (key: schema.table, value: UNIQUE 컬럼 리스트)
     */
    public Map<String, List<String>> extract(String schemaText, boolean keepSchemaName) {
        Pattern uniquePattern = Pattern.compile(
                "ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+ATTRIBUTE\\s+CONSTRAINT\\s+\\[(.*?)\\]\\s+UNIQUE\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher uniqueMatcher = uniquePattern.matcher(schemaText);
        
        Map<String, List<String>> uniqueMap = new HashMap<>();
        
        while (uniqueMatcher.find()) {
            String schemaName = uniqueMatcher.group(1).toLowerCase();
            String tableName = uniqueMatcher.group(2).toLowerCase();
            String columns = uniqueMatcher.group(4);
            
            String key;
            if (keepSchemaName) {
                key = schemaName + "." + tableName;
            } else {
                key = tableName;
            }
            
            String[] uniqueColumns = columns.replaceAll("\\[|\\]", "").split(",");
            for (String uniqueCol : uniqueColumns) {
                uniqueMap.computeIfAbsent(key, k -> new ArrayList<>()).add(uniqueCol.trim().toLowerCase());
            }
        }
        
        return uniqueMap;
    }
}

