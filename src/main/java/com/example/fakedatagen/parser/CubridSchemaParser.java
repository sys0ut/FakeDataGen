package com.example.fakedatagen.parser;

import com.example.fakedatagen.model.*;
import com.example.fakedatagen.parser.extractor.*;
import com.example.fakedatagen.parser.analyzer.RelationshipAnalyzer;
import com.example.fakedatagen.parser.builder.TableBuilder;
import com.example.fakedatagen.exception.SchemaParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    private static final Logger log = LoggerFactory.getLogger(CubridSchemaParser.class);
    
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
        if (schemaText == null || schemaText.trim().isEmpty()) {
            throw new SchemaParseException("스키마 텍스트가 비어있습니다");
        }
        
        try {
            DatabaseSchema schema = new DatabaseSchema("unknown");
            
            log.debug("Extracting table definitions");
            List<Table> tables = tableExtractor.extract(schemaText, keepSchemaName);
            if (tables.isEmpty()) {
                throw new SchemaParseException("테이블을 찾을 수 없습니다. CREATE CLASS 구문이 있는지 확인하세요.");
            }
            log.debug("Found {} tables", tables.size());
            
            log.debug("Extracting column information");
            Map<String, List<Column>> columnsMap = columnExtractor.extract(schemaText, keepSchemaName);
            
            log.debug("Extracting primary key constraints");
            Map<String, List<String>> pkMap = primaryKeyExtractor.extract(schemaText, keepSchemaName);
            
            log.debug("Extracting foreign key constraints");
            Map<String, List<ForeignKey>> fkMap = foreignKeyExtractor.extract(schemaText, keepSchemaName, pkMap);
            int totalFk = fkMap.values().stream().mapToInt(List::size).sum();
            log.debug("Found {} foreign key relationships", totalFk);
            
            log.debug("Extracting unique constraints");
            Map<String, List<String>> uniqueMap = uniqueConstraintExtractor.extract(schemaText, keepSchemaName);
            
            log.debug("Building table objects");
            tableBuilder.buildTablesAndAddToSchema(schema, tables, columnsMap, pkMap, uniqueMap, fkMap, keepSchemaName);
            
            log.debug("Analyzing table relationships");
            relationshipAnalyzer.analyze(schema);
            log.debug("Relationship analysis completed - found {} relationships", schema.getRelationships().size());
            
            return schema;
        } catch (SchemaParseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during schema parsing", e);
            throw new SchemaParseException("스키마 파싱 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}
