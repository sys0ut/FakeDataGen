package com.example.fakedatagen.parser.extractor;

import com.example.fakedatagen.model.ForeignKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ForeignKeyExtractorNewSchemaTest {
    
    private ForeignKeyExtractor extractor;
    
    private String schemaText = """
        CREATE CLASS [dba].[a] REUSE_OID, COLLATE utf8_bin;
        
        CREATE CLASS [dba].[b] REUSE_OID, COLLATE utf8_bin;
        
        CREATE CLASS [dba].[c] REUSE_OID, COLLATE utf8_bin;
        
        CREATE CLASS [dba].[parent] REUSE_OID, COLLATE utf8_bin;
        
        CREATE CLASS [dba].[child] REUSE_OID, COLLATE utf8_bin;
        
        CREATE CLASS [dba].[grandchild] REUSE_OID, COLLATE utf8_bin;
        
        ALTER CLASS [dba].[a] ADD ATTRIBUTE
               [id] integer AUTO_INCREMENT(1, 1) NOT NULL;
        ALTER CLASS [dba].[a] ADD ATTRIBUTE
               CONSTRAINT [pk_a_id] PRIMARY KEY([id]);
        
        ALTER CLASS [dba].[b] ADD ATTRIBUTE
               [id] integer AUTO_INCREMENT(1, 1) NOT NULL,
               [a_id] integer NOT NULL;
        ALTER CLASS [dba].[b] ADD ATTRIBUTE
               CONSTRAINT [pk_b_id] PRIMARY KEY([id]);
        
        ALTER CLASS [dba].[c] ADD ATTRIBUTE
               [id] integer AUTO_INCREMENT(1, 1) NOT NULL,
               [b_id] integer NOT NULL;
        ALTER CLASS [dba].[c] ADD ATTRIBUTE
               CONSTRAINT [pk_c_id] PRIMARY KEY([id]);
        
        ALTER CLASS [dba].[parent] ADD ATTRIBUTE
               [parent_id] character varying(10) COLLATE utf8_bin NOT NULL,
               [name] character varying(50) COLLATE utf8_bin NOT NULL,
               [description] character varying(200) COLLATE utf8_bin;
        ALTER CLASS [dba].[parent] ADD ATTRIBUTE
               CONSTRAINT [pk_parent_parent_id] PRIMARY KEY([parent_id]);
        
        ALTER CLASS [dba].[child] ADD ATTRIBUTE
               [child_id] integer AUTO_INCREMENT(1, 1) NOT NULL,
               [child_name] character varying(50) COLLATE utf8_bin NOT NULL,
               [parent_id] character varying(10) COLLATE utf8_bin NOT NULL,
               [created_date] datetime;
        ALTER CLASS [dba].[child] ADD ATTRIBUTE
               CONSTRAINT [pk_child_child_id] PRIMARY KEY([child_id]);
        
        ALTER CLASS [dba].[grandchild] ADD ATTRIBUTE
               [grandchild_id] integer AUTO_INCREMENT(1, 1) NOT NULL,
               [grandchild_name] character varying(50) COLLATE utf8_bin NOT NULL,
               [parent_id] character varying(10) COLLATE utf8_bin NOT NULL,
               [child_id] integer NOT NULL;
        ALTER CLASS [dba].[grandchild] ADD ATTRIBUTE
               CONSTRAINT [pk_grandchild_grandchild_id] PRIMARY KEY([grandchild_id]);
        
        ALTER CLASS [dba].[b] ADD CONSTRAINT [fk_b_a] FOREIGN KEY([a_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[a] ON DELETE RESTRICT ON UPDATE RESTRICT ;
        
        ALTER CLASS [dba].[c] ADD CONSTRAINT [fk_c_b] FOREIGN KEY([b_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[b] ON DELETE RESTRICT ON UPDATE RESTRICT ;
        
        ALTER CLASS [dba].[child] ADD CONSTRAINT [fk_child_parent] FOREIGN KEY([parent_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[parent] ON DELETE RESTRICT ON UPDATE RESTRICT ;
        
        ALTER CLASS [dba].[grandchild] ADD CONSTRAINT [fk_grandchild_parent] FOREIGN KEY([parent_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[parent] ON DELETE RESTRICT ON UPDATE RESTRICT ;
        
        ALTER CLASS [dba].[grandchild] ADD CONSTRAINT [fk_grandchild_child] FOREIGN KEY([child_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[child] ON DELETE RESTRICT ON UPDATE RESTRICT ;
        """;
    
    @BeforeEach
    void setUp() {
        extractor = new ForeignKeyExtractor();
    }
    
    @Test
    @DisplayName("새 스키마 외래키 추출 테스트 - 한 줄 정의")
    void testExtractForeignKeysNewSchema() {
        Map<String, List<String>> pkMap = Map.of(
            "dba.a", List.of("id"),
            "dba.b", List.of("id"),
            "dba.c", List.of("id"),
            "dba.parent", List.of("parent_id"),
            "dba.child", List.of("child_id"),
            "dba.grandchild", List.of("grandchild_id")
        );
        
        Map<String, List<ForeignKey>> fkMap = extractor.extract(schemaText, true, pkMap);
        
        // 기대되는 외래키:
        // dba.b -> a_id -> dba.a.id
        // dba.c -> b_id -> dba.b.id
        // dba.child -> parent_id -> dba.parent.parent_id
        // dba.grandchild -> parent_id -> dba.parent.parent_id
        // dba.grandchild -> child_id -> dba.child.child_id
        
        assertNotNull(fkMap, "FK 맵이 null이 아니어야 함");
        assertEquals(4, fkMap.size(), "4개의 테이블에 외래키가 있어야 함");
        
        // dba.b 테이블 확인
        assertTrue(fkMap.containsKey("dba.b"), "dba.b 테이블이 있어야 함");
        List<ForeignKey> bFks = fkMap.get("dba.b");
        assertEquals(1, bFks.size(), "dba.b는 1개의 외래키를 가져야 함");
        ForeignKey bFk = bFks.get(0);
        assertEquals("a_id", bFk.getColumnName(), "외래키 컬럼명은 a_id");
        assertEquals("dba.a", bFk.getReferencedTableName(), "참조 테이블은 dba.a");
        assertEquals("id", bFk.getReferencedColumnName(), "참조 컬럼은 id");
        
        // dba.c 테이블 확인
        assertTrue(fkMap.containsKey("dba.c"), "dba.c 테이블이 있어야 함");
        List<ForeignKey> cFks = fkMap.get("dba.c");
        assertEquals(1, cFks.size(), "dba.c는 1개의 외래키를 가져야 함");
        ForeignKey cFk = cFks.get(0);
        assertEquals("b_id", cFk.getColumnName(), "외래키 컬럼명은 b_id");
        assertEquals("dba.b", cFk.getReferencedTableName(), "참조 테이블은 dba.b");
        assertEquals("id", cFk.getReferencedColumnName(), "참조 컬럼은 id");
        
        // dba.child 테이블 확인
        assertTrue(fkMap.containsKey("dba.child"), "dba.child 테이블이 있어야 함");
        List<ForeignKey> childFks = fkMap.get("dba.child");
        assertEquals(1, childFks.size(), "dba.child는 1개의 외래키를 가져야 함");
        ForeignKey childFk = childFks.get(0);
        assertEquals("parent_id", childFk.getColumnName(), "외래키 컬럼명은 parent_id");
        assertEquals("dba.parent", childFk.getReferencedTableName(), "참조 테이블은 dba.parent");
        assertEquals("parent_id", childFk.getReferencedColumnName(), "참조 컬럼은 parent_id");
        
        // dba.grandchild 테이블 확인 (2개의 외래키)
        assertTrue(fkMap.containsKey("dba.grandchild"), "dba.grandchild 테이블이 있어야 함");
        List<ForeignKey> grandchildFks = fkMap.get("dba.grandchild");
        assertEquals(2, grandchildFks.size(), "dba.grandchild는 2개의 외래키를 가져야 함");
        
        // 모든 외래키 출력
        System.out.println("\n=== 추출된 외래키 ===");
        fkMap.forEach((table, fkList) -> {
            System.out.println("테이블: " + table);
            fkList.forEach(fk -> System.out.println("  FK 컬럼: " + fk.getColumnName() + 
                    ", 참조 테이블: " + fk.getReferencedTableName() + 
                    ", 참조 컬럼: " + fk.getReferencedColumnName()));
            System.out.println("--------------------");
        });
    }
}
