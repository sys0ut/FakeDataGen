package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.parser.TestSchemaConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class UniqueConstraintExtractorTest {
    
    private UniqueConstraintExtractor extractor;
    
    private String schemaText = TestSchemaConstants.FULL_SCHEMA;
    
    @BeforeEach
    void setUp() {
        extractor = new UniqueConstraintExtractor();
    }
    
    @Test
    @DisplayName("UNIQUE 제약조건 추출 - 없을 수 있음")
    void testExtractUniqueConstraints() {
        Map<String, List<String>> uniqueMap = extractor.extract(schemaText, true);

        uniqueMap.forEach((table, uniqueList) -> {
            System.out.println("테이블: " + table);
            System.out.println("UNIQUE 컬럼: " + uniqueList);
            System.out.println("-------------------------");
        });
        
        assertNotNull(uniqueMap, "UNIQUE 맵이 null이 아니어야 함");


    }
}

