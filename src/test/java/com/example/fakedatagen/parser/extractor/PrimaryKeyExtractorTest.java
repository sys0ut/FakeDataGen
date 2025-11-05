package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.parser.TestSchemaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PrimaryKeyExtractorTest {
    
    private PrimaryKeyExtractor extractor;
    
    private String schemaText = TestSchemaConstants.FULL_SCHEMA;
    
    @BeforeEach
    void setUp() {
        extractor = new PrimaryKeyExtractor();
    }
    
    @Test
    @DisplayName("기본키 추출 테스트")
    void testExtractPrimaryKeys() {
        Map<String, List<String>> pkMap = extractor.extract(schemaText, true);
        
        assertNotNull(pkMap, "PK 맵이 null이 아니어야 함");
        assertEquals(6, pkMap.size(), "6개 테이블의 PK가 추출되어야 함");

        pkMap.forEach((table, pkList) -> {
            System.out.println("Table: " + table + "PK 컬럼: " + pkList);
        });
    }
    
    @Test
    @DisplayName("테이블 a의 기본키 확인")
    void testTableAPrimaryKey() {
        Map<String, List<String>> pkMap = extractor.extract(schemaText, true);
        
        List<String> aPKs = pkMap.get("dba.a");
        assertNotNull(aPKs, "a 테이블의 PK가 있어야 함");
        assertEquals(1, aPKs.size(), "a 테이블은 1개 PK");
        assertEquals("id", aPKs.get(0), "PK는 id");
    }
    
    @Test
    @DisplayName("테이블 parent의 기본키 확인")
    void testTableParentPrimaryKey() {
        Map<String, List<String>> pkMap = extractor.extract(schemaText, true);
        
        List<String> parentPKs = pkMap.get("dba.parent");
        assertNotNull(parentPKs, "parent 테이블의 PK가 있어야 함");
        assertEquals(1, parentPKs.size(), "parent 테이블은 1개 PK");
        assertEquals("parent_id", parentPKs.get(0), "PK는 parent_id");
    }
}

