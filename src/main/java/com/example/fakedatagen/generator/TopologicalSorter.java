package com.example.fakedatagen.generator;

import com.example.fakedatagen.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 데이터베이스 스키마의 테이블을 의존성 순서에 따라 위상 정렬하는 클래스
 */
@Component
public class TopologicalSorter {
    
    /**
     * 의존성 순서에 따라 정렬된 테이블 리스트를 반환
     * 
     * @param schema 데이터베이스 스키마
     * @return 위상 정렬된 테이블 리스트
     */
    public List<Table> getOrderedTables(DatabaseSchema schema) {
        List<Table> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        
        for (Table table : schema.getTables()) {
            String tableKey = table.getName();
            if (!visited.contains(tableKey)) {
                topologicalSort(table, schema, visited, visiting, result);
            }
        }
        return result;
    }
    
    /**
     * 테이블명 기준으로 위상 정렬된 테이블명 리스트를 반환
     * 
     * @param schema 데이터베이스 스키마
     * @return 위상 정렬된 테이블명 리스트 (스키마명.테이블명 형태)
     */
    public List<String> getOrderedTableNames(DatabaseSchema schema) {
        List<Table> orderedTables = getOrderedTables(schema);
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
    
    /**
     * DFS를 사용한 위상 정렬 재귀 메서드
     */
    private void topologicalSort(Table table, DatabaseSchema schema, Set<String> visited, 
                                Set<String> visiting, List<Table> result) {
        String tableKey = table.getName();
        
        if (visiting.contains(tableKey)) {
            // 순환 참조 감지
            return;
        }
        
        if (visited.contains(tableKey)) {
            return;
        }
        
        visiting.add(tableKey);
        
        // 1. 외래키 기반 의존성 처리
        String dependencyKey = table.getName();
        List<String> dependencies = schema.getDependencies().get(dependencyKey);
        if (dependencies != null) {
            for (String depTableName : dependencies) {
                Table depTable = schema.getTableByName(depTableName);
                if (depTable != null) {
                    topologicalSort(depTable, schema, visited, visiting, result);
                }
            }
        }
        
        // 2. Relationship 기반 의존성 처리
        for (Relationship relationship : schema.getRelationships()) {
            String sourceTableKey = relationship.getSourceTable().getName();
            if (sourceTableKey.equals(tableKey)) {
                // sourceTable을 처리하기 전에 targetTable을 먼저 처리해야 함
                String targetTableName = relationship.getTargetTable().getName();
                Table targetTable = schema.getTableByName(targetTableName);
                if (targetTable != null && !visited.contains(targetTableName)) {
                    topologicalSort(targetTable, schema, visited, visiting, result);
                }
            }
        }
        
        visiting.remove(tableKey);
        visited.add(tableKey);
        result.add(table);
    }
}

