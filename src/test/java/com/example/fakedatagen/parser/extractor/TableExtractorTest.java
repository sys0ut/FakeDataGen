package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.model.Table;
import com.example.fakedatagen.parser.TestSchemaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TableExtractorTest {
    
    private TableExtractor extractor;
    
    private String schemaText = TestSchemaConstants.FULL_SCHEMA;
    
    @BeforeEach
    void setUp() {
        extractor = new TableExtractor();
    }
    
    @Test
    @DisplayName("테이블 추출 - 스키마명 포함")
    void testExtractTablesWithSchema() {
        List<Table> tables = extractor.extract(schemaText, true);

        System.out.println("==== Extracted Tables ====");
        for (Table table : tables) {
            System.out.println("Table: " + table.getName()); // Table 클래스에 getName() 또는 toString() 필요
        }
        System.out.println("==== End of Tables ====");


        assertEquals(6, tables.size(), "테이블이 6개 추출되어야 함");
        
        // a 테이블 확인
        Table tableA = tables.stream()
            .filter(t -> t.getName().equals("a"))
            .findFirst()
            .orElse(null);
        assertNotNull(tableA, "a 테이블이 있어야 함");
        assertEquals("dba", tableA.getSchemaName(), "스키마명은 dba");
        
        // parent 테이블 확인
        Table parentTable = tables.stream()
            .filter(t -> t.getName().equals("parent"))
            .findFirst()
            .orElse(null);
        assertNotNull(parentTable, "parent 테이블이 있어야 함");
        assertEquals("dba", parentTable.getSchemaName(), "스키마명은 dba");
    }
    
    @Test
    @DisplayName("테이블 추출 - 스키마명 제외")
    void testExtractTablesWithoutSchema() {
        List<Table> tables = extractor.extract(schemaText, false);
        
        assertEquals(6, tables.size(), "테이블이 6개 추출되어야 함");
        
        Table tableA = tables.stream()
            .filter(t -> t.getName().equals("a"))
            .findFirst()
            .orElse(null);
        assertNotNull(tableA, "a 테이블이 있어야 함");
        assertEquals("", tableA.getSchemaName(), "스키마명은 빈 문자열");
    }
    
    @Test
    @DisplayName("모든 테이블명 확인")
    void testAllTableNames() {
        List<Table> tables = extractor.extract(schemaText, true);
        
        String[] expectedTables = {"a", "b", "c", "parent", "child", "grandchild"};
        
        for (String expectedTable : expectedTables) {
            boolean found = tables.stream()
                .anyMatch(t -> t.getName().equals(expectedTable));
            assertTrue(found, expectedTable + " 테이블이 추출되어야 함");
        }
    }
}
