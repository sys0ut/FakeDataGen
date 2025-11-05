package com.example.fakedatagen.parser.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(UniqueConstraintExtractor.class);
    
    /**
     * 스키마 텍스트에서 UNIQUE 제약조건 추출
     * 
     * @param schemaText 스키마 정의 텍스트
     * @param keepSchemaName 스키마명 유지 여부
     * @return 테이블별 UNIQUE 컬럼 맵 (key: schema.table, value: UNIQUE 컬럼 리스트)
     */
    public Map<String, List<String>> extract(String schemaText, boolean keepSchemaName) {
        Map<String, List<String>> uniqueMap = new HashMap<>();
        int pattern1Count = 0;
        int pattern2Count = 0;
        
        // 패턴 1: ADD ATTRIBUTE CONSTRAINT 형태 (한 줄)
        // 예: ALTER CLASS [dba].[category] ADD ATTRIBUTE CONSTRAINT [uq_category_slug] UNIQUE([slug]);
        Pattern uniquePattern1 = Pattern.compile(
                "ALTER\\s+CLASS\\s+\\[([^\\]]+)\\]\\.\\[([^\\]]+)\\]\\s+ADD\\s+ATTRIBUTE\\s+CONSTRAINT\\s+\\[([^\\]]+)\\]\\s+UNIQUE\\s*\\(([^)]+)\\)",
                Pattern.CASE_INSENSITIVE);
        Matcher uniqueMatcher1 = uniquePattern1.matcher(schemaText);
        
        while (uniqueMatcher1.find()) {
            pattern1Count++;
            String schemaName = uniqueMatcher1.group(1).toLowerCase();
            String tableName = uniqueMatcher1.group(2).toLowerCase();
            String constraintName = uniqueMatcher1.group(3);
            String columnsStr = uniqueMatcher1.group(4);
            
            String key;
            if (keepSchemaName) {
                key = schemaName + "." + tableName;
            } else {
                key = tableName;
            }
            
            // 컬럼명 추출: [column1], [column2] 형태에서 대괄호 안의 내용만 추출
            List<String> columnList = extractColumnNames(columnsStr);
            if (!columnList.isEmpty()) {
                for (String colName : columnList) {
                    uniqueMap.computeIfAbsent(key, k -> new ArrayList<>()).add(colName);
                }
                log.info("[UNIQUE 추출 - 패턴1] 테이블={}, 제약조건={}, 컬럼={}", key, constraintName, columnList);
            }
        }
        
        // 패턴 2: ADD CONSTRAINT 형태 (ATTRIBUTE 없이, 여러 줄 가능)
        // 예: ALTER CLASS [dba].[category] ADD CONSTRAINT\n    [uq_category_slug] UNIQUE([slug]);
        // 더 정확한 매칭: UNIQUE( 다음에 [column] 형태만 허용하고, ) 전까지만
        Pattern uniquePattern2 = Pattern.compile(
                "ALTER\\s+CLASS\\s+\\[([^\\]]+)\\]\\.\\[([^\\]]+)\\]\\s+ADD\\s+CONSTRAINT\\s+\\[([^\\]]+)\\]\\s+UNIQUE\\s*\\(([^)]+)\\)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher uniqueMatcher2 = uniquePattern2.matcher(schemaText);
        
        while (uniqueMatcher2.find()) {
            String schemaName = uniqueMatcher2.group(1).toLowerCase();
            String tableName = uniqueMatcher2.group(2).toLowerCase();
            String constraintName = uniqueMatcher2.group(3);
            String columnsStr = uniqueMatcher2.group(4);
            
            // 컬럼 문자열이 너무 길면 잘못된 매칭 (다음 줄까지 포함됨)
            // 실제 컬럼명은 보통 짧으므로 200자 이상이면 스킵
            if (columnsStr.length() > 200) {
                log.warn("[UNIQUE 추출 - 패턴2] 잘못된 매칭 스킵: 테이블={}, 컬럼문자열길이={}, 컬럼문자열={}", 
                        tableName, columnsStr.length(), columnsStr.substring(0, Math.min(100, columnsStr.length())));
                continue;
            }
            
            pattern2Count++;
            String key;
            if (keepSchemaName) {
                key = schemaName + "." + tableName;
            } else {
                key = tableName;
            }
            
            // 컬럼명 추출: [column1], [column2] 형태에서 대괄호 안의 내용만 추출
            List<String> columnList = extractColumnNames(columnsStr);
            if (!columnList.isEmpty()) {
                for (String colName : columnList) {
                    uniqueMap.computeIfAbsent(key, k -> new ArrayList<>()).add(colName);
                }
                log.info("[UNIQUE 추출 - 패턴2] 테이블={}, 제약조건={}, 컬럼={}", key, constraintName, columnList);
            } else {
                log.warn("[UNIQUE 추출 - 패턴2] 컬럼 추출 실패: 테이블={}, 제약조건={}, 컬럼문자열={}", 
                        key, constraintName, columnsStr);
            }
        }
        
        log.info("[UNIQUE 추출 완료] 패턴1={}개, 패턴2={}개, 총 테이블={}개", pattern1Count, pattern2Count, uniqueMap.size());
        uniqueMap.forEach((table, columns) -> {
            log.info("[UNIQUE 최종] 테이블={}, UNIQUE 컬럼={}", table, columns);
        });
        
        return uniqueMap;
    }
    
    /**
     * UNIQUE 제약조건의 컬럼 문자열에서 컬럼명 추출
     * 예: "[slug]" 또는 "[col1], [col2]" 형태에서 컬럼명만 추출
     */
    private List<String> extractColumnNames(String columnsStr) {
        List<String> columnList = new ArrayList<>();
        if (columnsStr == null || columnsStr.trim().isEmpty()) {
            return columnList;
        }
        
        // [column1], [column2] 형태에서 대괄호 안의 내용만 추출
        Pattern columnPattern = Pattern.compile("\\[([^\\]]+)\\]");
        Matcher columnMatcher = columnPattern.matcher(columnsStr);
        while (columnMatcher.find()) {
            String colName = columnMatcher.group(1).trim().toLowerCase();
            if (!colName.isEmpty()) {
                columnList.add(colName);
            }
        }
        
        return columnList;
    }
}

