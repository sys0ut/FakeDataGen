package com.example.fakedatagen.parser.analyzer;

import com.example.fakedatagen.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

public class RelationshipAnalyzerTest {
    
    private RelationshipAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        analyzer = new RelationshipAnalyzer();
    }
    
    @Test
    @DisplayName("MANY_TO_ONE 관계 분석 테스트")
    void testAnalyzeManyToOneRelationship() {
        // Given
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 부모 테이블 (a)
        Table parentTable = new Table("a");
        Column parentId = new Column("id", "integer");
        parentId.setPrimaryKey(true);
        parentTable.addColumn(parentId);
        parentTable.addConstraint(new Constraint("pk_a_id", Constraint.ConstraintType.PRIMARY_KEY));
        parentTable.getConstraints().get(0).addColumn("id");
        
        // 자식 테이블 (b)
        Table childTable = new Table("b");
        Column childId = new Column("id", "integer");
        childId.setPrimaryKey(true);
        Column aIdColumn = new Column("a_id", "integer");
        childTable.addColumn(childId);
        childTable.addColumn(aIdColumn);
        childTable.addConstraint(new Constraint("pk_b_id", Constraint.ConstraintType.PRIMARY_KEY));
        childTable.getConstraints().get(0).addColumn("id");
        
        // 외래키 설정
        ForeignKey fk = new ForeignKey("fk_b_a", "a_id", "a", "id");
        childTable.addForeignKey(fk);
        
        schema.addTable(parentTable);
        schema.addTable(childTable);
        
        // When
        analyzer.analyze(schema);
        
        // Then
        assertEquals(1, schema.getRelationships().size(), "관계가 1개 있어야 함");
        
        Relationship relationship = schema.getRelationships().get(0);
        assertEquals(childTable, relationship.getSourceTable(), "소스 테이블은 b");
        assertEquals(parentTable, relationship.getTargetTable(), "타겟 테이블은 a");
        assertEquals(Relationship.RelationshipType.MANY_TO_ONE, relationship.getType(), "MANY_TO_ONE 관계");
        
        assertEquals(1, relationship.getSourceColumns().size(), "소스 컬럼 1개");
        assertEquals("a_id", relationship.getSourceColumns().get(0), "소스 컬럼은 a_id");
        
        assertEquals(1, relationship.getTargetColumns().size(), "타겟베럼 1개");
        assertEquals("id", relationship.getTargetColumns().get(0), "타겟 컬럼은 id");
        
        // 의존성 확인
        assertTrue(schema.getDependencies().containsKey("b"), "b 테이블의 의존성이 있어야 함");
        assertTrue(schema.getDependencies().get("b").contains("a"), "b는 a에 의존");

        for (Relationship rel : schema.getRelationships()) {
            System.out.println(
                    rel.getSourceTable().getName() + " -> " +
                            rel.getTargetTable().getName() + " : " +
                            rel.getType()
            );
            System.out.println("  Source columns: " + rel.getSourceColumns());
            System.out.println("  Target columns: " + rel.getTargetColumns());
        }
    }
    
    @Test
    @DisplayName("ONE_TO_ONE 관계 분석 테스트 - UNIQUE 제약조건")
    void testAnalyzeOneToOneRelationship() {
        // Given
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 부모 테이블 (user)
        Table userTable = new Table("user");
        Column userId = new Column("id", "integer");
        userId.setPrimaryKey(true);
        userTable.addColumn(userId);
        userTable.addConstraint(new Constraint("pk_user_id", Constraint.ConstraintType.PRIMARY_KEY));
        userTable.getConstraints().get(0).addColumn("id");
        
        // 자식 테이블 (profile) - UNIQUE 제약조건이 있는 외래키
        Table profileTable = new Table("profile");
        Column profileId = new Column("id", "integer");
        profileId.setPrimaryKey(true);
        Column userIdColumn = new Column("user_id", "integer");
        profileTable.addColumn(profileId);
        profileTable.addColumn(userIdColumn);
        profileTable.addConstraint(new Constraint("pk_profile_id", Constraint.ConstraintType.PRIMARY_KEY));
        profileTable.getConstraints().get(0).addColumn("id");
        
        // UNIQUE 제약조건 추가
        Constraint uniqueConstraint = new Constraint("uq_profile_user_id", Constraint.ConstraintType.UNIQUE);
        uniqueConstraint.addColumn("user_id");
        profileTable.addConstraint(uniqueConstraint);
        
        // 외래키 설정
        ForeignKey fk = new ForeignKey("fk_profile_user", "user_id", "user", "id");
        profileTable.addForeignKey(fk);
        
        schema.addTable(userTable);
        schema.addTable(profileTable);
        
        // When
        analyzer.analyze(schema);
        
        // Then
        assertEquals(1, schema.getRelationships().size(), "관계가 1개 있어야 함");
        
        Relationship relationship = schema.getRelationships().get(0);
        assertEquals(Relationship.RelationshipType.ONE_TO_ONE, relationship.getType(), "ONE_TO_ONE 관계");

        for (Relationship rel : schema.getRelationships()) {
            System.out.println(
                    rel.getSourceTable().getName() + " -> " +
                            rel.getTargetTable().getName() + " : " +
                            rel.getType()
            );
            System.out.println("  Source columns: " + rel.getSourceColumns());
            System.out.println("  Target columns: " + rel.getTargetColumns());
        }
    }
    
    @Test
    @DisplayName("MANY_TO_MANY 관계 분석 테스트 - 복합 기본키")
    void testAnalyzeManyToManyRelationship() {
        // Given
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 테이블 A
        Table tableA = new Table("student");
        Column studentId = new Column("id", "integer");
        studentId.setPrimaryKey(true);
        tableA.addColumn(studentId);
        tableA.addConstraint(new Constraint("pk_student_id", Constraint.ConstraintType.PRIMARY_KEY));
        tableA.getConstraints().get(0).addColumn("id");
        
        // 테이블 B
        Table tableB = new Table("course");
        Column courseId = new Column("id", "integer");
        courseId.setPrimaryKey(true);
        tableB.addColumn(courseId);
        tableB.addConstraint(new Constraint("pk_course_id", Constraint.ConstraintType.PRIMARY_KEY));
        tableB.getConstraints().get(0).addColumn("id");
        
        // 중간 테이블 (복합 기본키)
        Table junctionTable = new Table("enrollment");
        Column enrollmentId = new Column("id", "integer");
        enrollmentId.setPrimaryKey(true);
        Column studentIdColumn = new Column("student_id", "integer");
        Column courseIdColumn = new Column("course_id", "integer");
        junctionTable.addColumn(enrollmentId);
        junctionTable.addColumn(studentIdColumn);
        junctionTable.addColumn(courseIdColumn);
        
        // 복합 기본키 제약조건
        Constraint compositePk = new Constraint("pk_enrollment", Constraint.ConstraintType.PRIMARY_KEY);
        compositePk.addColumn("student_id");
        compositePk.addColumn("course_id");
        junctionTable.addConstraint(compositePk);
        
        // 외래키 설정
        ForeignKey fk1 = new ForeignKey("fk_enrollment_student", "student_id", "student", "id");
        junctionTable.addForeignKey(fk1);
        
        schema.addTable(tableA);
        schema.addTable(tableB);
        schema.addTable(junctionTable);
        
        // When
        analyzer.analyze(schema);
        
        // Then
        // junction 테이블의 student_id 외래키 → MANY_TO_MANY 관계
        assertEquals(1, schema.getRelationships().size(), "관계가 1개 있어야 함");
        
        Relationship relationship = schema.getRelationships().get(0);
        assertEquals(junctionTable, relationship.getSourceTable(), "소스 테이블은 junction");
        assertEquals(tableA, relationship.getTargetTable(), "타겟 테이블은 student");
        assertEquals(Relationship.RelationshipType.MANY_TO_MANY, relationship.getType(), "MANY_TO_MANY 관계");

        for (Relationship rel : schema.getRelationships()) {
            System.out.println(
                    rel.getSourceTable().getName() + " -> " +
                            rel.getTargetTable().getName() + " : " +
                            rel.getType()
            );
            System.out.println("  Source columns: " + rel.getSourceColumns());
            System.out.println("  Target columns: " + rel.getTargetColumns());
        }
    }
    
    @Test
    @DisplayName("여러 관계가 있는 스키마 분석 테스트")
    void testAnalyzeMultipleRelationships() {
        // Given - a → b → c 체인 구조
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 테이블 a
        Table tableA = createTableWithPK("a", "id");
        
        // 테이블 b
        Table tableB = createTableWithPK("b", "id");
        addForeignKey(tableB, "fk_b_a", "a_id", "a", "id");
        
        // 테이블 c
        Table tableC = createTableWithPK("c", "id");
        addForeignKey(tableC, "fk_c_b", "b_id", "b", "id");
        
        schema.addTable(tableA);
        schema.addTable(tableB);
        schema.addTable(tableC);
        
        // When
        analyzer.analyze(schema);
        
        // Then
        assertEquals(2, schema.getRelationships().size(), "관계가 2개 있어야 함");
        
        // b → a 관계
        Relationship relationship1 = schema.getRelationships().stream()
            .filter(r -> r.getSourceTable().getName().equals("b"))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship1, "b → a 관계가 있어야 함");
        assertEquals(Relationship.RelationshipType.MANY_TO_ONE, relationship1.getType());
        
        // c → b 관계
        Relationship relationship2 = schema.getRelationships().stream()
            .filter(r -> r.getSourceTable().getName().equals("c"))
            .findFirst()
            .orElse(null);
        assertNotNull(relationship2, "c → b 관계가 있어야 함");
        assertEquals(Relationship.RelationshipType.MANY_TO_ONE, relationship2.getType());
        
        // 의존성 확인
        assertTrue(schema.getDependencies().containsKey("b"), "b의 의존성이 있어야 함");
        assertTrue(schema.getDependencies().get("b").contains("a"), "b는 a에 의존");
        
        assertTrue(schema.getDependencies().containsKey("c"), "c의 의존성이 있어야 함");
        assertTrue(schema.getDependencies().get("c").contains("b"), "c는 b에 의존");

        for (Relationship rel : schema.getRelationships()) {
            System.out.println(
                    rel.getSourceTable().getName() + " -> " +
                            rel.getTargetTable().getName() + " : " +
                            rel.getType()
            );
            System.out.println("  Source columns: " + rel.getSourceColumns());
            System.out.println("  Target columns: " + rel.getTargetColumns());
        }
    }
    
    @Test
    @DisplayName("존재하지 않는 참조 테이블을 참조하는 경우")
    void testAnalyzeWithNonExistentReference() {
        // Given
        DatabaseSchema schema = new DatabaseSchema("test");
        
        Table tableB = createTableWithPK("b", "id");
        // 존재하지 않는 테이블 "nonexistent"를 참조
        addForeignKey(tableB, "fk_b_nonexistent", "nonexistent_id", "nonexistent", "id");
        
        schema.addTable(tableB);
        
        // When
        analyzer.analyze(schema);
        
        // Then
        assertEquals(0, schema.getRelationships().size(), "관계가 생성되지 않아야 함");
        assertTrue(schema.getDependencies().isEmpty(), "의존성도 추가되지 않아야 함");

        for (Relationship rel : schema.getRelationships()) {
            System.out.println(
                    rel.getSourceTable().getName() + " -> " +
                            rel.getTargetTable().getName() + " : " +
                            rel.getType()
            );
            System.out.println("  Source columns: " + rel.getSourceColumns());
            System.out.println("  Target columns: " + rel.getTargetColumns());
        }
    }
    
    @Test
    @DisplayName("외래키가 없는 테이블 분석")
    void testAnalyzeTableWithoutForeignKey() {
        // Given
        DatabaseSchema schema = new DatabaseSchema("test");
        
        Table tableA = createTableWithPK("a", "id");
        schema.addTable(tableA);
        
        // When
        analyzer.analyze(schema);
        
        // Then
        assertEquals(0, schema.getRelationships().size(), "관계가 없어야 함");
        assertTrue(schema.getDependencies().isEmpty(), "의존성이 없어야 함");

        for (Relationship rel : schema.getRelationships()) {
            System.out.println(
                    rel.getSourceTable().getName() + " -> " +
                            rel.getTargetTable().getName() + " : " +
                            rel.getType()
            );
            System.out.println("  Source columns: " + rel.getSourceColumns());
            System.out.println("  Target columns: " + rel.getTargetColumns());
        }
    }
    
    @Test
    @DisplayName("복합 외래키 관계 분석")
    void testAnalyzeWithCompositeForeignKey() {
        // Given
        DatabaseSchema schema = new DatabaseSchema("test");
        
        // 부모 example
        Table parentTable = new Table("parent");
        Column parentId1 = new Column("id1", "varchar");
        parentId1.setPrimaryKey(true);
        Column parentId2 = new Column("id2", "varchar");
        parentId2.setPrimaryKey(true);
        parentTable.addColumn(parentId1);
        parentTable.addColumn(parentId2);
        
        Constraint pk = new Constraint("pk_parent", Constraint.ConstraintType.PRIMARY_KEY);
        pk.addColumn("id1");
        pk.addColumn("id2");
        parentTable.addConstraint(pk);
        
        // 자식 테이블
        Table childTable = new Table("child");
        Column childId = new Column("id", "integer");
        childId.setPrimaryKey(true);
        Column parentId1Col = new Column("parent_id1", "varchar");
        Column parentId2Col = new Column("parent_id2", "varchar");
        childTable.addColumn(childId);
        childTable.addColumn(parentId1Col);
        childTable.addColumn(parentId2Col);
        
        childTable.addConstraint(new Constraint("pk_child_id", Constraint.ConstraintType.PRIMARY_KEY));
        childTable.getConstraints().get(0).addColumn("id");
        
        // 외래키
        ForeignKey fk1 = new ForeignKey("fk_child_parent1", "parent_id1", "parent", "id1");
        ForeignKey fk2 = new ForeignKey("fk_child_parent2", "parent_id2", "parent", "id2");
        childTable.addForeignKey(fk1);
        childTable.addForeignKey(fk2);
        
        schema.addTable(parentTable);
        schema.addTable(childTable);
        
        // When
        analyzer.analyze(schema);
        
        // Then
        assertEquals(2, schema.getRelationships().size(), "관계가 2개 있어야 함");

        for (Relationship rel : schema.getRelationships()) {
            System.out.println(
                    rel.getSourceTable().getName() + " -> " +
                            rel.getTargetTable().getName() + " : " +
                            rel.getType()
            );
            System.out.println("  Source columns: " + rel.getSourceColumns());
            System.out.println("  Target columns: " + rel.getTargetColumns());
        }
    }
    
    // Helper methods
    private Table createTableWithPK(String tableName, String pkColumnName) {
        Table table = new Table(tableName);
        Column pkColumn = new Column(pkColumnName, "integer");
        pkColumn.setPrimaryKey(true);
        table.addColumn(pkColumn);
        table.addConstraint(new Constraint("pk_" + tableName + "_" + pkColumnName, Constraint.ConstraintType.PRIMARY_KEY));
        table.getConstraints().get(0).addColumn(pkColumnName);
        return table;
    }
    
    private void addForeignKey(Table table, String constraintName, String columnName, String referencedTable, String referencedColumn) {
        ForeignKey fk = new ForeignKey(constraintName, columnName, referencedTable, referencedColumn);
        table.addForeignKey(fk);
    }
}

