package com.example.fakedatagen.service;

import com.example.fakedatagen.generator.RelationshipAwareGenerator;
import com.example.fakedatagen.model.DatabaseConnectionInfo;
import com.example.fakedatagen.model.DatabaseSchema;
import com.example.fakedatagen.repository.DatabaseInsertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataGenerationServiceTest {

    @Mock
    private RelationshipAwareGenerator relationshipAwareGenerator;

    @Mock
    private DatabaseInsertRepository databaseInsertRepository;

    @InjectMocks
    private DataGenerationService dataGenerationService;

    private DatabaseSchema schema;

    @BeforeEach
    void setUp() {
        schema = new DatabaseSchema("test");
    }

    @Test
    void generateAndInsertData_whenInsertFalse_onlyGeneratesData() {
        // given
        Map<String, List<Map<String, Object>>> fakeData = new HashMap<>();
        fakeData.put("a", List.of(Map.of("id", 1)));
        when(relationshipAwareGenerator.generateFakeData(schema, 2)).thenReturn(fakeData);

        // when
        DataGenerationService.DataGenerationResult result =
                dataGenerationService.generateAndInsertData(schema, 2, false, null);

        // then
        assertNotNull(result);
        assertEquals(fakeData, result.getFakeData());
        assertEquals(0, result.getTotalInserted());
        assertEquals("데이터 생성만 완료되었습니다. (INSERT하지 않음)", result.getInsertMessage());
        verify(relationshipAwareGenerator, times(1)).generateFakeData(schema, 2);
        verifyNoInteractions(databaseInsertRepository);

        System.out.println("Fake Data: " + result.getFakeData());
        System.out.println("Total Inserted: " + result.getTotalInserted());
        System.out.println("Insert Message: " + result.getInsertMessage());
    }

    @Test
    void generateAndInsertData_whenInsertTrueButNoDbInfo_generatesOnlyAndWarns() {
        // given
        Map<String, List<Map<String, Object>>> fakeData = new HashMap<>();
        fakeData.put("a", List.of(Map.of("id", 1)));
        when(relationshipAwareGenerator.generateFakeData(schema, 3)).thenReturn(fakeData);

        // when
        DataGenerationService.DataGenerationResult result =
                dataGenerationService.generateAndInsertData(schema, 3, true, null);

        // then
        assertEquals(fakeData, result.getFakeData());
        assertEquals(0, result.getTotalInserted());
        assertEquals("데이터베이스 연결 정보가 제공되지 않아 INSERT하지 않았습니다. 데이터 생성만 완료되었습니다.",
                result.getInsertMessage());
        verify(relationshipAwareGenerator, times(1)).generateFakeData(schema, 3);
        verifyNoInteractions(databaseInsertRepository);
    }

     @Test
     void generateAndInsertData_generatesFkConsistentData_withoutDbInsert() {
         // given: a(id PK), b(id PK, a_id FK->a.id)
         DatabaseSchema fkSchema = new DatabaseSchema("test");

         // 가짜 데이터(map) 구성: a의 id 1,2 / b의 a_id가 1 또는 2를 참조
         Map<String, List<Map<String, Object>>> fakeData = new HashMap<>();
         fakeData.put("a", List.of(
                 Map.of("id", 1),
                 Map.of("id", 2)
         ));
         fakeData.put("b", List.of(
                 Map.of("id", 10, "a_id", 1),
                 Map.of("id", 11, "a_id", 2)
         ));

         when(relationshipAwareGenerator.generateFakeData(fkSchema, 2)).thenReturn(fakeData);

         // when
         DataGenerationService.DataGenerationResult result =
                 dataGenerationService.generateAndInsertData(fkSchema, 2, false, null);

         // then: 참조 무결성(외래키 일치) 검증
         Map<String, List<Map<String, Object>>> out = result.getFakeData();
         assertNotNull(out);
         assertTrue(out.containsKey("a") && out.containsKey("b"));

         Set<Object> aIds = new HashSet<>();
         out.get("a").forEach(r -> aIds.add(r.get("id")));
         assertFalse(aIds.isEmpty());

         out.get("b").forEach(r -> assertTrue(aIds.contains(r.get("a_id")), "b.a_id는 a.id를 참조해야 함"));

         assertEquals(0, result.getTotalInserted());
         verify(relationshipAwareGenerator, times(1)).generateFakeData(fkSchema, 2);
         verifyNoInteractions(databaseInsertRepository);
     }

    @Test
    void testConnection_withInvalidInfo_returnsFalse() {
        // given: 존재하지 않는 DB에 대한 연결 정보
        DatabaseConnectionInfo dbInfo = new DatabaseConnectionInfo("127.0.0.1", 33000, "no_such_db", "user", "pw");

        // when
        boolean valid = dataGenerationService.testConnection(dbInfo);

        // then
        assertFalse(valid);
    }
}


