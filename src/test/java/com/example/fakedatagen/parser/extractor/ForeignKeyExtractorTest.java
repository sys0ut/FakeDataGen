package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.model.ForeignKey;
import com.example.fakedatagen.parser.TestSchemaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ForeignKeyExtractorTest {
    
    private ForeignKeyExtractor extractor;
    
    private String schemaText = TestSchemaConstants.FULL_SCHEMA;
    
    @BeforeEach
    void setUp() {
        extractor = new ForeignKeyExtractor();
    }
    
    @Test
    @DisplayName("외래키 추출 테스트")
    void testExtractForeignKeys() {
        Map<String, List<String>> pkMap = Map.of(
            "dba.a", List.of("id"),
            "dba.b", List.of("id"),
            "dba.c", List.of("id"),
            "dba.parent", List.of("parent_id"),
            "dba.child", List.of("child_id"),
            "dba.grandchild", List.of("grandchild_id")
        );
        
        Map<String, List<ForeignKey>> fkMap = extractor.extract(schemaText, true, pkMap);
        
        assertNotNull(fkMap, "FK 맵이 null이 아니어야 함");
        assertTrue(fkMap.containsKey("dba.b"), "dba.b 테이블이 있어야 함");
        assertTrue(fkMap.containsKey("dba.c"), "dba.c 테이블이 있어야 함");
        assertTrue(fkMap.containsKey("dba.child"), "dba.child 테이블이 있어야 함");
        assertTrue(fkMap.containsKey("dba.grandchild"), "dba.grandchild 테이블이 있어야 함");

        fkMap.forEach((table, fkList) -> {
            System.out.println("테이블: " + table);
            fkList.forEach(fk -> System.out.println("  FK 컬럼: " + fk.getColumnName() + ", 참조 테이블: " + fk.getReferencedTableName() + ", 참조 컬럼: " + fk.getReferencedColumnName()));
            System.out.println("--------------------");
        });
    }
}

