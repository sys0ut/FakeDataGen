package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.model.Table;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CUBRID 스키마에서 테이블 정의를 추출하는 클래스
 */
@Component
public class TableExtractor {
    
    /**
     * 스키마 텍스트에서 테이블 정의 추출
     * 
     * @param schemaText 스키마 정의 텍스트
     * @param keepSchemaName 스키마명 유지 여부
     * @return 추출된 테이블 리스트
     */
    public List<Table> extract(String schemaText, boolean keepSchemaName) {
        // CREATE CLASS [schema].[table] 패턴 매칭
        Pattern tablePattern = Pattern.compile("CREATE\\s+CLASS\\s+\\[([^\\]]+)\\]\\.\\[([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher tableMatcher = tablePattern.matcher(schemaText);
        
        List<Table> tables = new ArrayList<>();
        
        // 첫 번째 패턴으로 시도
        while (tableMatcher.find()) {
            String schemaName = tableMatcher.group(1).toLowerCase();
            String tableName = tableMatcher.group(2).toLowerCase();
            
            if (keepSchemaName) {
                tables.add(new Table(schemaName, tableName));
            } else {
                tables.add(new Table("", tableName));
            }
        }
        
        return tables;
    }
}


