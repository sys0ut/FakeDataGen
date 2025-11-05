package com.example.fakedatagen.parser.builder;

import com.example.fakedatagen.model.*;
import com.example.fakedatagen.parser.TestSchemaConstants;
import com.example.fakedatagen.parser.extractor.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TableBuilderTest {
    
    private TableBuilder builder;
    private TableExtractor tableExtractor;
    private ColumnExtractor columnExtractor;
    private PrimaryKeyExtractor pkExtractor;
    private ForeignKeyExtractor fkExtractor;
    private UniqueConstraintExtractor uniqueExtractor;
    
    private String schemaText = TestSchemaConstants.FULL_SCHEMA;
    
    @BeforeEach
    void setUp() {
        builder = new TableBuilder();
        tableExtractor = new TableExtractor();
        columnExtractor = new ColumnExtractor();
        pkExtractor = new PrimaryKeyExtractor();
        fkExtractor = new ForeignKeyExtractor();
        uniqueExtractor = new UniqueConstraintExtractor();
    }
    
    @Test
    @DisplayName("테이블 빌드 - 전체 프로세스 테스트")
    void testBuildTables() {
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 1. 추출
        List<Table> tables = tableExtractor.extract(schemaText, true);
        Map<String, List<Column>> columnsMap = columnExtractor.extract(schemaText, true);
        Map<String, List<String>> pkMap = pkExtractor.extract(schemaText, true);
        Map<String, List<String>> uniqueMap = uniqueExtractor.extract(schemaText, true);
        Map<String, List<ForeignKey>> fkMap = fkExtractor.extract(schemaText, true, pkMap);
        
        // 2. 빌드
        builder.buildTablesAndAddToSchema(schema, tables, columnsMap, pkMap, uniqueMap, fkMap, true);
        
        // 3. 검증
        assertEquals(6, schema.getTables().size(), "테이블이 6개 있어야 함");
        
        // a 테이블 확인
        Table tableA = schema.getTableByName("a");
        assertNotNull(tableA, "a 테이블이 있어야 함");
        assertEquals(1, tableA.getColumns().size(), "컬럼 1개");
        assertEquals(1, tableA.getConstraints().size(), "제약조건 1개 (PK)");
        
        Column idColumn = tableA.getColumnByName("id");
        assertTrue(idColumn.isPrimaryKey(), "id는 기본키");
        assertTrue(idColumn.isAutoIncrement(), "id는 AUTO_INCREMENT");

        for (Table table : schema.getTables()) {
            System.out.println("Table: " + table.getName());

            // 컬럼 출력
            for (Column column : table.getColumns()) {
                System.out.println("  Column: " + column.getName() +
                        ", PK=" + column.isPrimaryKey() +
                        ", AUTO_INCREMENT=" + column.isAutoIncrement());
            }

            // 제약조건 출력
            for (Constraint constraint : table.getConstraints()) {
                System.out.println("  Constraint: " + constraint.getName() +
                        ", Type=" + constraint.getType() +
                        ", Columns=" + constraint.getColumns());
            }

            // 외래키 출력
            for (ForeignKey fk : table.getForeignKeys()) {
                System.out.println("  ForeignKey: " + fk.getConstraintName() +
                        ", References=" + fk.getReferencedTableName() +
                        "(" + fk.getReferencedColumnName() + ")");
            }
        }
    }
    
    @Test
    @DisplayName("외래키가 있는 테이블 빌드 테스트")
    void testBuildTableWithForeignKey() {
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 추출
        List<Table> tables = tableExtractor.extract(schemaText, true);
        Map<String, List<Column>> columnsMap = columnExtractor.extract(schemaText, true);
        Map<String, List<String>> pkMap = pkExtractor.extract(schemaText, true);
        Map<String, List<String>> uniqueMap = uniqueExtractor.extract(schemaText, true);
        Map<String, List<ForeignKey>> fkMap = fkExtractor.extract(schemaText, true, pkMap);
        
        // 빌드
        builder.buildTablesAndAddToSchema(schema, tables, columnsMap, pkMap, uniqueMap, fkMap, true);
        
        // b 테이블 확인
        Table tableB = schema.getTableByName("b");
        assertNotNull(tableB, "b 테이블이 있어야 함");
        assertEquals(2, tableB.getColumns().size(), "컬럼 2개");
        assertEquals(1, tableB.getForeignKeys().size(), "외래키 1개");
        
        ForeignKey fk = tableB.getForeignKeys().get(0);
        assertEquals("a_id", fk.getColumnName(), "외래키 컬럼은 a_id");
        assertEquals("dba.a", fk.getReferencedTableName(), "참조 테이블은 dba.a");
    }
    
    @Test
    @DisplayName("복합 외래키 테이블 빌드 테스트")
    void testBuildTableWithMultipleForeignKeys() {
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 추출
        List<Table> tables = tableExtractor.extract(schemaText, true);
        Map<String, List<Column>> columnsMap = columnExtractor.extract(schemaText, true);
        Map<String, List<String>> pkMap = pkExtractor.extract(schemaText, true);
        Map<String, List<String>> uniqueMap = uniqueExtractor.extract(schemaText, true);
        Map<String, List<ForeignKey>> fkMap = fkExtractor.extract(schemaText, true, pkMap);
        
        // 빌드
        builder.buildTablesAndAddToSchema(schema, tables, columnsMap, pkMap, uniqueMap, fkMap, true);
        
        // grandchild 테이블 확인
        Table grandchild = schema.getTableByName("grandchild");
        assertNotNull(grandchild, "grandchild 테이블이 있어야 함");
        assertEquals(4, grandchild.getColumns().size(), "컬럼 4개");
        assertEquals(2, grandchild.getForeignKeys().size(), "외래키 2개");
        
        // parent_id 외래키
        ForeignKey fkParent = grandchild.getForeignKeys().stream()
            .filter(fk -> fk.getColumnName().equals("parent_id"))
            .findFirst()
            .orElse(null);
        assertNotNull(fkParent, "parent_id 외래키가 있어야 함");
        assertEquals("dba.parent", fkParent.getReferencedTableName(), "참조 테이블은 dba.parent");
        
        // child_id 외래키
        ForeignKey fkChild = grandchild.getForeignKeys().stream()
            .filter(fk -> fk.getColumnName().equals("child_id"))
            .findFirst()
            .orElse(null);
        assertNotNull(fkChild, "child_id 외래키가 있어야 함");
        assertEquals("dba.child", fkChild.getReferencedTableName(), "참조 테이블은 dba.child");
    }
    
    @Test
    @DisplayName("PRIMARY KEY 제약조건 추가 테스트")
    void testPrimaryKeyConstraints() {
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 추출
        List<Table> tables = tableExtractor.extract(schemaText, true);
        Map<String, List<Column>> columnsMap = columnExtractor.extract(schemaText, true);
        Map<String, List<String>> pkMap = pkExtractor.extract(schemaText, true);
        Map<String, List<String>> uniqueMap = uniqueExtractor.extract(schemaText, true);
        Map<String, List<ForeignKey>> fkMap = fkExtractor.extract(schemaText, true, pkMap);
        
        // 빌드
        builder.buildTablesAndAddToSchema(schema, tables, columnsMap, pkMap, uniqueMap, fkMap, true);
        
        // 각 테이블의 기본키 확인
        for (Table table : schema.getTables()) {
            List<Column> pkColumns = table.getPrimaryKeyColumns();
            assertFalse(pkColumns.isEmpty(), table.getName() + " 테이블은 기본키가 있어야 함");
            
            // 제약조건도 확인
            boolean hasPKConstraint = table.getConstraints().stream()
                .anyMatch(c -> c.getType() == Constraint.ConstraintType.PRIMARY_KEY);
            assertTrue(hasPKConstraint, table.getName() + " 테이블은 PRIMARY KEY 제약조건이 있어야 함");
        }
    }
}

