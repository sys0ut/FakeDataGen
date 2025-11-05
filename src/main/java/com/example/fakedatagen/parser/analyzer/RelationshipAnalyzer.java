package com.example.fakedatagen.parser.analyzer;

import com.example.fakedatagen.model.*;
import org.springframework.stereotype.Component;

/**
 * 데이터베이스 스키마의 관계를 분석하는 클래스
 */
@Component
public class RelationshipAnalyzer {
    
    /**
     * 스키마의 관계와 의존성을 분석
     * 
     * @param schema 데이터베이스 스키마
     */
    public void analyze(DatabaseSchema schema) {
        for (Table table : schema.getTables()) {
            for (ForeignKey fk : table.getForeignKeys()) {
                // 외래키의 참조 테이블 찾기
                Table referencedTable = schema.getTables().stream()
                        .filter(t -> t.getName().equalsIgnoreCase(fk.getReferencedTableName()))
                        .findFirst()
                        .orElse(null);

                if (referencedTable != null) {
                    // 의존성 추가
                    schema.addDependency(table.getName(), referencedTable.getName());

                    // 관계 타입 자동 감지
                    Relationship.RelationshipType relationshipType = determineRelationshipType(table, referencedTable, fk, schema);
                    
                    // 관계 생성
                    Relationship relationship = new Relationship(
                            table, referencedTable, relationshipType
                    );
                    relationship.addSourceColumn(fk.getColumnName());
                    relationship.addTargetColumn(fk.getReferencedColumnName());
                    schema.addRelationship(relationship);
                }
            }
        }
    }
    
    /**
     * 관계 타입 결정
     * 
     * @param sourceTable 소스 테이블
     * @param targetTable 타겟 테이블
     * @param fk 외래키
     * @param schema 스키마
     * @return 관계 타입
     */
    private Relationship.RelationshipType determineRelationshipType(
            Table sourceTable, Table targetTable, ForeignKey fk, DatabaseSchema schema) {
        
        // 1. UNIQUE 제약조건 확인 → 1:1 관계
        if (hasUniqueConstraint(sourceTable, fk.getColumnName())) {
            return Relationship.RelationshipType.ONE_TO_ONE;
        }
        
        // 2. 복합 기본키 확인 → N:N 관계 (중간 테이블)
        if (isCompositePrimaryKeyTable(sourceTable)) {
            return Relationship.RelationshipType.MANY_TO_MANY;
        }
        
        // 3. 기본값 → N:1 관계
        return Relationship.RelationshipType.MANY_TO_ONE;
    }
    
    /**
     * 테이블의 특정 컬럼에 UNIQUE 제약조건이 있는지 확인
     */
    private boolean hasUniqueConstraint(Table table, String columnName) {
        for (Constraint constraint : table.getConstraints()) {
            if (constraint.getType() == Constraint.ConstraintType.UNIQUE && 
                constraint.getColumns().contains(columnName)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 복합 기본키 테이블인지 확인
     */
    private boolean isCompositePrimaryKeyTable(Table table) {
        for (Constraint constraint : table.getConstraints()) {
            if (constraint.getType() == Constraint.ConstraintType.PRIMARY_KEY && 
                constraint.getColumns().size() > 1) {
                return true;
            }
        }
        return false;
    }
}

