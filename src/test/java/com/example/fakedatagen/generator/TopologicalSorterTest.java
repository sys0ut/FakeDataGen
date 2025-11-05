package com.example.fakedatagen.generator;

import com.example.fakedatagen.model.*;
import com.example.fakedatagen.parser.CubridSchemaParser;
import com.example.fakedatagen.parser.TestSchemaConstants;
import com.example.fakedatagen.parser.extractor.*;
import com.example.fakedatagen.parser.builder.*;
import com.example.fakedatagen.parser.analyzer.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TopologicalSorter 테스트 클래스
 * 위상 정렬이 올바른 순서로 테이블을 반환하는지 검증
 */
class TopologicalSorterTest {

    private TopologicalSorter topologicalSorter;
    private DatabaseSchema schema;
    
    // 필요한 Extractor들
    private TableExtractor tableExtractor;
    private ColumnExtractor columnExtractor;
    private PrimaryKeyExtractor pkExtractor;
    private ForeignKeyExtractor fkExtractor;
    private UniqueConstraintExtractor uniqueExtractor;
    private TableBuilder tableBuilder;
    private RelationshipAnalyzer relationshipAnalyzer;

    @BeforeEach
    void setUp() {
        topologicalSorter = new TopologicalSorter();
        
        // Extractor와 Builder, Analyzer 생성
        tableExtractor = new TableExtractor();
        columnExtractor = new ColumnExtractor();
        pkExtractor = new PrimaryKeyExtractor();
        fkExtractor = new ForeignKeyExtractor();
        uniqueExtractor = new UniqueConstraintExtractor();
        tableBuilder = new TableBuilder();
        relationshipAnalyzer = new RelationshipAnalyzer();
        
        // Parser 생성 및 의존성 주입 (setter 방식)
        CubridSchemaParser parser = new CubridSchemaParser();
        parser.setTableExtractor(tableExtractor);
        parser.setColumnExtractor(columnExtractor);
        parser.setPkExtractor(pkExtractor);
        parser.setFkExtractor(fkExtractor);
        parser.setUniqueExtractor(uniqueExtractor);
        parser.setTableBuilder(tableBuilder);
        parser.setRelationshipAnalyzer(relationshipAnalyzer);
        
        schema = parser.parseSchema(TestSchemaConstants.FULL_SCHEMA, true);
    }

    @Test
    void testGetOrderedTables_ReturnsAllTables() {
        // Given & When
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);

        // Then
        assertNotNull(orderedTables);
        assertEquals(6, orderedTables.size(), "스키마에는 6개의 테이블이 있어야 함");
    }

    @Test
    void testGetOrderedTables_CorrectDependencyOrder() {
        // Given & When
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> tableNames = orderedTables.stream()
                .map(Table::getName)
                .collect(Collectors.toList());

        // Then - a는 b보다 앞에 있어야 함 (b가 a_id를 참조)
        int indexA = tableNames.indexOf("a");
        int indexB = tableNames.indexOf("b");
        assertTrue(indexA < indexB, "a는 b보다 앞에 있어야 함");

        // b는 c보다 앞에 있어야 함 (c가 b_id를 참조)
        int indexC = tableNames.indexOf("c");
        assertTrue(indexB < indexC, "b는 c보다 앞에 있어야 함");
    }

    @Test
    void testGetOrderedTables_ParentChildGrandchildOrder() {
        // Given & When
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> tableNames = orderedTables.stream()
                .map(Table::getName)
                .collect(Collectors.toList());

        // Then
        int indexParent = tableNames.indexOf("parent");
        int indexChild = tableNames.indexOf("child");
        int indexGrandchild = tableNames.indexOf("grandchild");

        // parent는 child보다 앞에 있어야 함
        assertTrue(indexParent < indexChild, "parent는 child보다 앞에 있어야 함");
        
        // child는 grandchild보다 앞에 있어야 함
        assertTrue(indexChild < indexGrandchild, "child는 grandchild보다 앞에 있어야 함");
    }

    @Test
    void testGetOrderedTables_HandlesMultipleDependencies() {
        // Given - grandchild는 parent와 child 모두에 의존
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> tableNames = orderedTables.stream()
                .map(Table::getName)
                .collect(Collectors.toList());

        // When
        int indexParent = tableNames.indexOf("parent");
        int indexChild = tableNames.indexOf("child");
        int indexGrandchild = tableNames.indexOf("grandchild");

        // Then - grandchild는 parent와 child 모두보다 뒤에 있어야 함
        assertTrue(indexParent < indexGrandchild, "parent는 grandchild보다 앞에 있어야 함");
        assertTrue(indexChild < indexGrandchild, "child는 grandchild보다 앞에 있어야 함");
    }

    @Test
    void testGetOrderedTables_IndependentTablesCanBeAnyOrder() {
        // Given - a와 parent는 독립적이므로 순서는 상관없음
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> tableNames = orderedTables.stream()
                .map(Table::getName)
                .collect(Collectors.toList());

        // Then - 둘 다 리스트에 존재해야 함
        assertTrue(tableNames.contains("a"), "a 테이블이 있어야 함");
        assertTrue(tableNames.contains("parent"), "parent 테이블이 있어야 함");
    }

    @Test
    void testGetOrderedTables_NoDuplicates() {
        // Given & When
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);

        // Then
        long uniqueCount = orderedTables.stream()
                .map(Table::getName)
                .distinct()
                .count();
        
        assertEquals(orderedTables.size(), uniqueCount, "중복된 테이블이 없어야 함");
    }

    @Test
    void testGetOrderedTableNames_ReturnsSchemaQualifiedNames() {
        // Given & When
        List<String> orderedTableNames = topologicalSorter.getOrderedTableNames(schema);

        // Then
        assertNotNull(orderedTableNames);
        assertEquals(6, orderedTableNames.size());
        
        // 스키마명이 포함되어야 함
        assertTrue(orderedTableNames.get(0).contains("."), "스키마명이 포함되어야 함");
        assertTrue(orderedTableNames.stream().allMatch(name -> name.contains(".")), 
                "모든 테이블명에 스키마명이 포함되어야 함");
    }

    @Test
    void testGetOrderedTableNames_MaintainsOrder() {
        // Given
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> expectedNames = orderedTables.stream()
                .map(t -> t.getSchemaName() + "." + t.getName())
                .collect(Collectors.toList());

        // When
        List<String> actualNames = topologicalSorter.getOrderedTableNames(schema);

        // Then
        assertEquals(expectedNames, actualNames, "테이블 순서가 유지되어야 함");
    }

    @Test
    void testGetOrderedTables_EmptySchemaReturnsEmptyList() {
        // Given
        DatabaseSchema emptySchema = new DatabaseSchema("test");
        
        // When
        List<Table> result = topologicalSorter.getOrderedTables(emptySchema);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty(), "빈 스키마는 빈 리스트를 반환해야 함");
    }

    @Test
    void testGetOrderedTables_SingleTable() {
        // Given
        DatabaseSchema singleTableSchema = new DatabaseSchema("test");
        Table table = new Table("test", "single_table"); // schemaName, tableName 순서
        singleTableSchema.addTable(table);
        
        // When
        List<Table> result = topologicalSorter.getOrderedTables(singleTableSchema);

        // Then
        assertEquals(1, result.size());
        assertEquals("single_table", result.get(0).getName());
    }

    @Test
    void testGetOrderedTables_HandlesCircularDependency() {
        // Given - 순환 참조가 있는 스키마 생성
        DatabaseSchema circularSchema = new DatabaseSchema("test");
        
        Table table1 = new Table("test", "table1"); // schemaName, tableName 순서
        Table table2 = new Table("test", "table2");
        
        ForeignKey fk1 = new ForeignKey("table2_id", "table2", "id");
        ForeignKey fk2 = new ForeignKey("table1_id", "table1", "id");
        
        table1.addForeignKey(fk1);
        table2.addForeignKey(fk2);
        
        circularSchema.addTable(table1);
        circularSchema.addTable(table2);
        circularSchema.addDependency(table1.getName(), table2.getName());
        circularSchema.addDependency(table2.getName(), table1.getName());
        
        // When
        List<Table> result = topologicalSorter.getOrderedTables(circularSchema);

        // Then - 순환 참조가 있어도 예외가 발생하지 않아야 함
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetOrderedTableNames_WithoutSchemaName() {
        // Given
        DatabaseSchema schemaWithoutSchema = new DatabaseSchema(null);
        Table table = new Table(null, "test_table"); // schemaName, tableName 순서
        schemaWithoutSchema.addTable(table);
        
        // When
        List<String> result = topologicalSorter.getOrderedTableNames(schemaWithoutSchema);

        // Then
        assertEquals(1, result.size());
        assertEquals("test_table", result.get(0), "스키마명이 없으면 테이블명만 반환");
    }

    @Test
    void testGetOrderedTables_ComplexDependencyChain() {
        // Given - a -> b -> c 체인
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> tableNames = orderedTables.stream()
                .map(Table::getName)
                .collect(Collectors.toList());

        // When
        int indexA = tableNames.indexOf("a");
        int indexB = tableNames.indexOf("b");
        int indexC = tableNames.indexOf("c");

        // Then - 의존성 체인이 올바르게 정렬되어야 함
        assertTrue(indexA < indexB && indexB < indexC, "a -> b -> c 순서가 올바르게 정렬되어야 함");
    }

    @Test
    void testGetOrderedTables_RelationshipDependencies() {
        // Given & When
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> tableNames = orderedTables.stream()
                .map(Table::getName)
                .collect(Collectors.toList());

        // Then - Relationship 기반 의존성도 올바르게 처리되어야 함
        int indexParent = tableNames.indexOf("parent");
        int indexChild = tableNames.indexOf("child");
        
        assertTrue(indexParent >= 0 && indexChild >= 0, "parent와 child가 모두 리스트에 있어야 함");
        assertTrue(indexParent < indexChild, "parent는 child보다 앞에 있어야 함");
    }
}

