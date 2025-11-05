package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.model.Column;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CUBRID 스키마에서 컬럼 정의를 추출하는 클래스
 */
@Component
public class ColumnExtractor {
    
    /**
     * 스키마 텍스트에서 컬럼 정의 추출
     * 
     * @param schemaText 스키마 정의 텍스트
     * @param keepSchemaName 스키마명 유지 여부
     * @return 테이블별 컬럼 맵 (key: schema.table, value: 컬럼 리스트)
     */
    public Map<String, List<Column>> extract(String schemaText, boolean keepSchemaName) {
        // ALTER CLASS [schema].[table] ADD ATTRIBUTE 패턴 매칭
        Pattern columnPattern = Pattern.compile(
                "ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+ATTRIBUTE\\s+([\\s\\S]*?)(?=ALTER\\s+CLASS|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher columnMatcher = columnPattern.matcher(schemaText);
        
        Map<String, List<Column>> columnsMap = new HashMap<>();
        
        while (columnMatcher.find()) {
            String schemaName = columnMatcher.group(1).toLowerCase();
            String tableName = columnMatcher.group(2).toLowerCase();
            String attributesBlock = columnMatcher.group(3);
            
            String key;
            if (keepSchemaName) {
                key = schemaName + "." + tableName;
            } else {
                key = tableName;
            }
            
            // 컬럼 파싱
            parseColumnsFromBlock(attributesBlock, key, columnsMap);
        }

        return columnsMap;
    }
    
    /**
     * 컬럼 정의 블록을 파싱하여 컬럼 정보 추출
     * 
     * @param attributesBlock 컬럼 정의 문자열 블록
     * @param key 테이블 키 (schema.table)
     * @param columnsMap 컬럼 맵
     */
    private void parseColumnsFromBlock(String attributesBlock, String key, Map<String, List<Column>> columnsMap) {
        List<Column> columns = columnsMap.computeIfAbsent(key, k -> new ArrayList<>());
        
        // 컬럼 패턴: [column_name] type [constraints]
        Pattern singleColumnPattern = Pattern.compile(
                "\\[([\\w]+)]\\s+([a-zA-Z0-9(), ]+?)(?:\\s+NOT\\s+NULL|\\s+DEFAULT|\\s+COLLATE|\\s+AUTO_INCREMENT\\([^)]+\\)|\\s+AUTO_INCREMENT|;|$)", 
                Pattern.CASE_INSENSITIVE);
        Matcher singleColumnMatcher = singleColumnPattern.matcher(attributesBlock);
        
        while (singleColumnMatcher.find()) {
            String columnName = singleColumnMatcher.group(1).toLowerCase();
            String type = singleColumnMatcher.group(2).trim().replace(";", "");
            
            // AUTO_INCREMENT 감지
            boolean isAutoIncrement = attributesBlock.toLowerCase()
                    .contains("[" + columnName + "] integer auto_increment");
            
            Column column = new Column(columnName, type);
            column.setAutoIncrement(isAutoIncrement);
            columns.add(column);
        }
        
        columnsMap.put(key, columns);
    }
}

