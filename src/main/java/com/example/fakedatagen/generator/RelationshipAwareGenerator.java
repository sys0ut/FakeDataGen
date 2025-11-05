package com.example.fakedatagen.generator;

import com.example.fakedatagen.model.*;
import net.datafaker.Faker;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RelationshipAwareGenerator {

    private final TopologicalSorter topologicalSorter;
    private final BasicValueGenerator basicValueGenerator;
    private final ForeignKeyValueGenerator foreignKeyValueGenerator;
    private final RelationshipValueGenerator relationshipValueGenerator;

    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(Faker::new);

    // Accessor for thread-safe Faker (reserved for future use by value generators)
    public Faker faker() { return FAKER.get(); }

    public RelationshipAwareGenerator(TopologicalSorter topologicalSorter,
                                      BasicValueGenerator basicValueGenerator,
                                      ForeignKeyValueGenerator foreignKeyValueGenerator,
                                      RelationshipValueGenerator relationshipValueGenerator) {
        this.topologicalSorter = topologicalSorter;
        this.basicValueGenerator = basicValueGenerator;
        this.foreignKeyValueGenerator = foreignKeyValueGenerator;
        this.relationshipValueGenerator = relationshipValueGenerator;
    }
    
    public Map<String, List<Map<String, Object>>> generateFakeData(DatabaseSchema schema, int recordCount) {
        Map<String, List<Map<String, Object>>> fakeData = new HashMap<>();
        
        // 1단계: 의존성 순서에 따라 테이블 생성
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        
        for (Table table : orderedTables) {
            List<Map<String, Object>> records = new ArrayList<>();
            
            for (int i = 0; i < recordCount; i++) {
                Map<String, Object> record = new HashMap<>();
                
                for (Column column : table.getColumns()) {
                    Object value = generateValueForColumn(column, i, table, fakeData, schema);
                    record.put(column.getName(), value);
                }
                
                records.add(record);
            }
            
            fakeData.put(table.getName(), records);
        }
        
        return fakeData;
    }
    
    /**
     * 특정 테이블의 데이터를 생성 (실제 생성된 키 값을 사용)
     * 
     * @param schema 데이터베이스 스키마
     * @param tableName 생성할 테이블명
     * @param recordCount 생성할 레코드 수
     * @param generatedKeysMap 이전 테이블들에서 생성된 키 값들
     * @return 생성된 테이블 데이터
     */
    public List<Map<String, Object>> generateTableData(DatabaseSchema schema, String tableName, int recordCount, Map<String, List<Long>> generatedKeysMap) {
        // 전체 테이블명에서 스키마명 제거하여 테이블 찾기
        String tableNameOnly = tableName.contains(".") ? 
            tableName.substring(tableName.lastIndexOf(".") + 1) : tableName;
        
        Table table = schema.getTableByName(tableNameOnly);
        if (table == null) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> records = new ArrayList<>();
        
        for (int i = 0; i < recordCount; i++) {
            Map<String, Object> record = new HashMap<>();
            
            for (Column column : table.getColumns()) {
                Object value = generateValueForColumnWithGeneratedKeys(column, i, table, generatedKeysMap, schema);
                record.put(column.getName(), value);
            }
            
            records.add(record);
        }
        
        return records;
    }
    
    /**
     * 특정 테이블의 데이터를 생성합니다. (실제 생성된 키 값과 데이터를 모두 사용)
     * 
     * @param schema 데이터베이스 스키마
     * @param tableName 생성할 테이블명
     * @param recordCount 생성할 레코드 수
     * @param generatedKeysMap 이전 테이블들에서 생성된 키 값들
     * @param generatedDataMap 이전 테이블들에서 생성된 데이터들
     * @return 생성된 테이블 데이터
     */
    public List<Map<String, Object>> generateTableDataWithGeneratedData(DatabaseSchema schema, String tableName, int recordCount, 
                                                                       Map<String, List<Long>> generatedKeysMap, 
                                                                       Map<String, List<Map<String, Object>>> generatedDataMap) {
        // 전체 테이블명에서 스키마명 제거하여 테이블 찾기
        // 굳이 스키마명 제거 할 필요가 없음 -> 추후 수정
        String tableNameOnly = tableName.contains(".") ? 
            tableName.substring(tableName.lastIndexOf(".") + 1) : tableName;
        
        Table table = schema.getTableByName(tableNameOnly);
        if (table == null) {
            return new ArrayList<>();
        }
        
        List<Map<String, Object>> records = new ArrayList<>();
        
        for (int i = 0; i < recordCount; i++) {
            Map<String, Object> record = new HashMap<>();
            
            for (Column column : table.getColumns()) {
                Object value = generateValueForColumnWithGeneratedData(column, i, table, generatedKeysMap, generatedDataMap, schema);
                record.put(column.getName(), value);
            }
            
            records.add(record);
        }
        
        return records;
    }
    
    private Object generateValueForColumn(Column column, int index, Table table, Map<String, List<Map<String, Object>>> fakeData, DatabaseSchema schema) {

        // 해당 기준 수정 필요

        // 1. Relationship 기반 처리 (우선순위 높음)
        for (Relationship relationship : getRelationshipsForTable(table.getName(), schema)) {
            if (relationship.getSourceColumns().contains(column.getName())) {
                return relationshipValueGenerator.generateFromFakeData(relationship, column.getName(), index, fakeData);
            }
        }
        
        // 2. Foreign Key 기반 처리
        for (ForeignKey fk : table.getForeignKeys()) {
            if (fk.getColumnName().equals(column.getName())) {
                return foreignKeyValueGenerator.generateFromFakeData(fk, fakeData, index);
            }
        }
        
        // 3. 기본 데이터 타입 처리
        return basicValueGenerator.generate(column, index, table);
    }
    
    /**
     * 실제 생성된 키 값과 데이터를 사용하여 컬럼 값을 생성
     */
    private Object generateValueForColumnWithGeneratedData(Column column, int index, Table table, 
                                                          Map<String, List<Long>> generatedKeysMap, 
                                                          Map<String, List<Map<String, Object>>> generatedDataMap, 
                                                          DatabaseSchema schema) {
        
        // 1. Relationship 기반 처리 (우선순위 높음)
        for (Relationship relationship : getRelationshipsForTable(table.getName(), schema)) {
            if (relationship.getSourceColumns().contains(column.getName())) {
                return relationshipValueGenerator.generateFromData(relationship, column.getName(), index, generatedKeysMap, generatedDataMap);
            }
        }
        
        // 2. Foreign Key 기반 처리
        for (ForeignKey fk : table.getForeignKeys()) {
            if (fk.getColumnName().equals(column.getName())) {
                Object fkValue = foreignKeyValueGenerator.generateFromData(fk, generatedKeysMap, generatedDataMap, index);
                return fkValue;
            }
        }
        
        // 3. 기본 데이터 타입 처리
        return basicValueGenerator.generate(column, index, table);
    }
    
    /**
     * 실제 생성된 키 값을 사용하여 컬럼 값을 생성
     */
    private Object generateValueForColumnWithGeneratedKeys(Column column, int index, Table table, Map<String, List<Long>> generatedKeysMap, DatabaseSchema schema) {
        
        // 1. Relationship 기반 처리 (우선순위 높음)
        for (Relationship relationship : getRelationshipsForTable(table.getName(), schema)) {
            if (relationship.getSourceColumns().contains(column.getName())) {
                return relationshipValueGenerator.generateFromKeys(relationship, column.getName(), index, generatedKeysMap);
            }
        }
        
        // 2. Foreign Key 기반 처리
        for (ForeignKey fk : table.getForeignKeys()) {
            if (fk.getColumnName().equals(column.getName())) {
                Object fkValue = foreignKeyValueGenerator.generateFromKeys(fk, generatedKeysMap, index);
                return fkValue;
            }
        }
        
        // 3. 기본 데이터 타입 처리
        return basicValueGenerator.generate(column, index, table);
    }

    private List<Relationship> getRelationshipsForTable(String tableName, DatabaseSchema schema) {
        List<Relationship> relationships = new ArrayList<>();
        
        // 테이블명에서 스키마명 제거
        String tableNameOnly = tableName;
        if (tableName.contains(".")) {
            tableNameOnly = tableName.substring(tableName.lastIndexOf(".") + 1);
        }
        
        // DatabaseSchema에서 해당 테이블과 관련된 관계들을 찾아서 반환
        for (Relationship relationship : schema.getRelationships()) {
            if (relationship.getSourceTable().getName().equals(tableNameOnly)) {
                relationships.add(relationship);
            }
        }
        
        return relationships;
    }

    /**
     * 위상 정렬된 테이블 순서를 반환
     * 이 순서대로 INSERT해야 외래키 제약조건을 만족
     */
    public List<String> getOrderedTableNames(DatabaseSchema schema) {
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> orderedTableNames = new ArrayList<>();
        
        for (Table table : orderedTables) {
            // 스키마명이 있으면 스키마명.테이블명 형태로, 없으면 테이블명만 사용
            String fullTableName = table.getSchemaName() != null && !table.getSchemaName().isEmpty() 
                ? table.getSchemaName() + "." + table.getName() 
                : table.getName();
            orderedTableNames.add(fullTableName);
        }
        
        return orderedTableNames;
    }
}