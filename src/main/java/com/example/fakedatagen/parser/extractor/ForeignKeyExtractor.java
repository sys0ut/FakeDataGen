package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.model.ForeignKey;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CUBRID 스키마에서 FOREIGN KEY 제약조건을 추출하는 클래스
 */
@Component
public class ForeignKeyExtractor {
    
    /**
     * 스키마 텍스트에서 FOREIGN KEY 제약조건 추출
     * 
     * @param schemaText 스키마 정의 텍스트
     * @param keepSchemaName 스키마명 유지 여부
     * @param pkMap PRIMARY KEY 맵 (참조 컬럼명 찾기용)
     * @return 테이블별 FOREIGN KEY 맵 (key: schema.table, value: FK 리스트)
     */
    public Map<String, List<ForeignKey>> extract(String schemaText, boolean keepSchemaName, Map<String, List<String>> pkMap) {
        Pattern fkPattern = Pattern.compile(
                "ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+CONSTRAINT\\s+\\[(.*?)\\]\\s+FOREIGN\\s+KEY\\s*\\(([^)]*)\\).*?REFERENCES\\s+\\[(.*?)\\]\\.\\[(.*?)\\]",
                Pattern.CASE_INSENSITIVE);
        Matcher fkMatcher = fkPattern.matcher(schemaText);
        
        Map<String, List<ForeignKey>> fkMap = new HashMap<>();
        
        while (fkMatcher.find()) {
            String schemaName = fkMatcher.group(1).toLowerCase();
            String tableName = fkMatcher.group(2).toLowerCase();
            String fkColumnsRaw = fkMatcher.group(4).replaceAll("\\[|\\]", "");
            String refSchemaName = fkMatcher.group(5).toLowerCase();
            String refTable = fkMatcher.group(6).toLowerCase();
            
            String key;
            String referencedTableName;
            if (keepSchemaName) {
                key = schemaName + "." + tableName;
                referencedTableName = refSchemaName + "." + refTable;
            } else {
                key = tableName;
                referencedTableName = refTable;
            }
            
            String fkColumnName = fkColumnsRaw.trim().toLowerCase();
            
            // 참조되는 테이블의 기본키 컬럼명 찾기
            String referencedColumnName = findReferencedPrimaryKeyColumn(refSchemaName, refTable, fkColumnName, pkMap, keepSchemaName);
            
            ForeignKey fk = new ForeignKey(fkColumnName, referencedTableName, referencedColumnName);
            fkMap.computeIfAbsent(key, k -> new ArrayList<>()).add(fk);
        }
        
        return fkMap;
    }
    
    /**
     * 참조되는 테이블의 기본키 컬럼명 찾기
     */
    private String findReferencedPrimaryKeyColumn(String refSchemaName, String refTableName, String fkColumnName, 
                                                 Map<String, List<String>> pkMap, boolean keepSchemaName) {
        String refTableKey;
        if (keepSchemaName) {
            refTableKey = refSchemaName + "." + refTableName;
        } else {
            refTableKey = refTableName;
        }
        
        // 실제 파싱된 기본키 정보에서 찾기
        if (pkMap.containsKey(refTableKey)) {
            List<String> primaryKeys = pkMap.get(refTableKey);
            if (!primaryKeys.isEmpty()) {
                return primaryKeys.get(0);
            }
        }
        
        // 기본키 정보가 없으면 일반적인 패턴들 시도
        String[] possibleColumnNames = {
            refTableName + "_id",
            "id",
            "pk_" + refTableName,
            fkColumnName.replace(refTableName + "_", ""),
            fkColumnName
        };
        
        return possibleColumnNames[0];
    }
}

