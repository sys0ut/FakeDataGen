package com.example.fakedatagen.parser;

import com.example.fakedatagen.model.*;
import com.example.fakedatagen.parser.extractor.*;
import com.example.fakedatagen.parser.analyzer.RelationshipAnalyzer;
import com.example.fakedatagen.parser.builder.TableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * CUBRID 스키마를 파싱하는 메인 클래스
 * 각종 Extractor와 Analyzer를 조합하여 스키마를 파싱합니다.
 */
@Service
public class CubridSchemaParser {
    
    @Autowired
    private TableExtractor tableExtractor;
    
    @Autowired
    private ColumnExtractor columnExtractor;
    
    @Autowired
    private PrimaryKeyExtractor primaryKeyExtractor;
    
    @Autowired
    private ForeignKeyExtractor foreignKeyExtractor;
    
    @Autowired
    private UniqueConstraintExtractor uniqueConstraintExtractor;
    
    @Autowired
    private RelationshipAnalyzer relationshipAnalyzer;
    
    @Autowired
    private TableBuilder tableBuilder;
    
    // 테스트를 위한 setter 메서드들
    public void setTableExtractor(TableExtractor tableExtractor) {
        this.tableExtractor = tableExtractor;
    }
    
    public void setColumnExtractor(ColumnExtractor columnExtractor) {
        this.columnExtractor = columnExtractor;
    }
    
    public void setPkExtractor(PrimaryKeyExtractor pkExtractor) {
        this.primaryKeyExtractor = pkExtractor;
    }
    
    public void setFkExtractor(ForeignKeyExtractor fkExtractor) {
        this.foreignKeyExtractor = fkExtractor;
    }
    
    public void setUniqueExtractor(UniqueConstraintExtractor uniqueExtractor) {
        this.uniqueConstraintExtractor = uniqueExtractor;
    }
    
    public void setTableBuilder(TableBuilder tableBuilder) {
        this.tableBuilder = tableBuilder;
    }
    
    public void setRelationshipAnalyzer(RelationshipAnalyzer relationshipAnalyzer) {
        this.relationshipAnalyzer = relationshipAnalyzer;
    }
    
    /**
     * 스키마 텍스트를 파싱하여 DatabaseSchema 객체를 생성
     * 
     * @param schemaText 파싱할 스키마 텍스트
     * @param keepSchemaName 스키마명을 유지할지 여부
     * @return 파싱된 DatabaseSchema 객체
     */
    public DatabaseSchema parseSchema(String schemaText, boolean keepSchemaName) {
        DatabaseSchema schema = new DatabaseSchema("unknown");
        
        // 1. 테이블 정보 추출
        List<Table> tables = tableExtractor.extract(schemaText, keepSchemaName);
        
        // 2. 각 테이블별 정보 파싱
        Map<String, List<Column>> columnsMap = columnExtractor.extract(schemaText, keepSchemaName);
        Map<String, List<String>> pkMap = primaryKeyExtractor.extract(schemaText, keepSchemaName);
        Map<String, List<String>> uniqueMap = uniqueConstraintExtractor.extract(schemaText, keepSchemaName);
        Map<String, List<ForeignKey>> fkMap = foreignKeyExtractor.extract(schemaText, keepSchemaName, pkMap);
        
        // 3. 테이블 객체 구성 및 스키마에 추가
        tableBuilder.buildTablesAndAddToSchema(schema, tables, columnsMap, pkMap, uniqueMap, fkMap, keepSchemaName);
        
        // 4. 관계 분석 및 의존성 그래프 생성
        relationshipAnalyzer.analyze(schema);
        
        return schema;
    }
}
