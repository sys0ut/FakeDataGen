package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.model.Column;
import com.example.fakedatagen.parser.TestSchemaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ColumnExtractorTest {
    
    private ColumnExtractor extractor;
    
    private String schemaText = TestSchemaConstants.FULL_SCHEMA;
    
    @BeforeEach
    void setUp() {
        extractor = new ColumnExtractor();
    }
    
    @Test
    @DisplayName("컬럼 추출 - 기본 테스트")
    void testExtractColumns() {
        Map<String, List<Column>> columnsMap = extractor.extract(schemaText, true);

        for (String tableKey : columnsMap.keySet()) {
            System.out.println("테이블: " + tableKey);
            for (Column col : columnsMap.get(tableKey)) {
                System.out.println("  컬럼명: " + col.getName() + ", 타입: " + col.getDataType() + ", AUTO_INCREMENT: " + col.isAutoIncrement());
            }
        }

        assertNotNull(columnsMap, "컬럼 맵이 null이 아니어야 함");
        assertTrue(columnsMap.containsKey("dba.a"), "dba.a 테이블이 있어야 함");
        assertTrue(columnsMap.containsKey("dba.parent"), "dba.parent 테이블이 있어야 함");
        assertTrue(columnsMap.containsKey("dba.child"), "dba.child 테이블이 있어야 함");
    }
    
    @Test
    @DisplayName("테이블 a의 컬럼 확인")
    void testTableAColumns() {
        Map<String, List<Column>> columnsMap = extractor.extract(schemaText, true);
        
        List<Column> aColumns = columnsMap.get("dba.a");
        assertNotNull(aColumns, "a 테이블의 컬럼이 있어야 함");
        assertEquals(1, aColumns.size(), "a 테이블은 1개 컬럼만 가져야 함");
        
        Column idColumn = aColumns.get(0);
        assertEquals("id", idColumn.getName(), "컬럼명은 id");
        assertTrue(idColumn.isAutoIncrement(), "id는 AUTO_INCREMENT여야 함");
    }
    
    @Test
    @DisplayName("테이블 parent의 컬럼 확인")
    void testTableParentColumns() {
        Map<String, List<Column>> columnsMap = extractor.extract(schemaText, true);
        
        List<Column> parentColumns = columnsMap.get("dba.parent");
        assertNotNull(parentColumns, "parent 테이블의 컬럼이 있어야 함");
        assertEquals(3, parentColumns.size(), "parent 테이블은 3개 컬럼");
        
        Column parentIdColumn = parentColumns.stream()
            .filter(c -> c.getName().equals("parent_id"))
            .findFirst()
            .orElse(null);
        assertNotNull(parentIdColumn, "parent_id 컬럼이 있어야 함");
        assertTrue(parentIdColumn.getDataType().toLowerCase().contains("character varying"), 
                   "데이터 타입은 character varying");
    }
    
    @Test
    @DisplayName("테이블 child의 컬럼 확인 - datetime 타입")
    void testTableChildColumns() {
        Map<String, List<Column>> columnsMap = extractor.extract(schemaText, true);
        
        List<Column> childColumns = columnsMap.get("dba.child");
        assertNotNull(childColumns, "child 테이블의 컬럼이 있어야 함");
        assertEquals(4, childColumns.size(), "child 테이블은 4개 컬럼");
        
        Column createdDateColumn = childColumns.stream()
            .filter(c -> c.getName().equals("created_date"))
            .findFirst()
            .orElse(null);
        assertNotNull(createdDateColumn, "created_date 컬럼이 있어야 함");
        assertTrue(createdDateColumn.getDataType().toLowerCase().contains("datetime"), 
                   "데이터 타입은 datetime");
    }
}

