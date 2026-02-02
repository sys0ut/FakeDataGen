package com.example.fakedatagen.generator;

import com.example.fakedatagen.model.*;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RelationshipAwareGenerator {

    private static final Logger log = LoggerFactory.getLogger(RelationshipAwareGenerator.class);

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
        int initialCapacity = Math.max(schema.getTables().size(), 16);
        Map<String, List<Map<String, Object>>> fakeData = new HashMap<>(initialCapacity);
        
        log.debug("Determining table generation order using topological sort");
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        log.debug("Table generation order: {}", orderedTables.stream().map(Table::getName).toList());
        
        for (Table table : orderedTables) {
            log.debug("Generating data for table: {} ({} records)", table.getName(), recordCount);
            List<Map<String, Object>> records = generateTableRecords(table, recordCount, fakeData, schema);
            fakeData.put(table.getName(), records);
            log.debug("Generated {} records for table: {}", records.size(), table.getName());
        }
        
        return fakeData;
    }
    
    private List<Map<String, Object>> generateTableRecords(Table table, int recordCount, 
                                                           Map<String, List<Map<String, Object>>> fakeData, 
                                                           DatabaseSchema schema) {
        // 관계 정보를 미리 계산
        List<Relationship> relationships = getRelationshipsForTable(table.getName(), schema);
        Map<String, Relationship> columnToRelationship = new HashMap<>();
        for (Relationship rel : relationships) {
            for (String col : rel.getSourceColumns()) {
                columnToRelationship.put(col, rel);
            }
        }
        
        Map<String, ForeignKey> columnToForeignKey = new HashMap<>();
        for (ForeignKey fk : table.getForeignKeys()) {
            columnToForeignKey.put(fk.getColumnName(), fk);
        }
        
        List<Map<String, Object>> records = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            Map<String, Object> record = new HashMap<>();
            for (Column column : table.getColumns()) {
                Object value = generateValueForColumnOptimized(column, i, table, fakeData, schema, 
                                                               columnToRelationship, columnToForeignKey);
                record.put(column.getName(), value);
            }
            records.add(record);
        }
        return records;
    }
    
    private Object generateValueForColumnOptimized(Column column, int index, Table table, 
                                                   Map<String, List<Map<String, Object>>> fakeData, 
                                                   DatabaseSchema schema,
                                                   Map<String, Relationship> columnToRelationship,
                                                   Map<String, ForeignKey> columnToForeignKey) {
        // 관계 체크 (캐시된 맵 사용)
        Relationship relationship = columnToRelationship.get(column.getName());
        if (relationship != null) {
            return relationshipValueGenerator.generateFromFakeData(relationship, column.getName(), index, fakeData);
        }
        
        // 외래키 체크 (캐시된 맵 사용)
        ForeignKey fk = columnToForeignKey.get(column.getName());
        if (fk != null) {
            return foreignKeyValueGenerator.generateFromFakeData(fk, fakeData, index);
        }
        
        return basicValueGenerator.generate(column, index, table);
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
        String tableNameOnly = extractTableNameOnly(tableName);
        Table table = schema.getTableByName(tableNameOnly);
        if (table == null) {
            log.warn("Table not found in schema: {}", tableNameOnly);
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
        String tableNameOnly = extractTableNameOnly(tableName);
        Table table = schema.getTableByName(tableNameOnly);
        if (table == null) {
            log.warn("Table not found in schema: {}", tableNameOnly);
            return new ArrayList<>();
        }
        
        log.debug("Generating data for table: {} ({} records)", tableName, recordCount);
        
        // 관계 정보를 미리 계산 (성능 최적화)
        List<Relationship> relationships = getRelationshipsForTable(table.getName(), schema);
        Map<String, Relationship> columnToRelationship = new HashMap<>();
        for (Relationship rel : relationships) {
            for (String col : rel.getSourceColumns()) {
                columnToRelationship.put(col, rel);
            }
        }
        
        Map<String, ForeignKey> columnToForeignKey = new HashMap<>();
        for (ForeignKey fk : table.getForeignKeys()) {
            columnToForeignKey.put(fk.getColumnName(), fk);
        }
        
        List<Map<String, Object>> records = new ArrayList<>(recordCount);
        
        for (int i = 0; i < recordCount; i++) {
            Map<String, Object> record = new HashMap<>();
            for (Column column : table.getColumns()) {
                Object value = generateValueForColumnWithGeneratedDataOptimized(column, i, table, generatedKeysMap, 
                                                                               generatedDataMap, schema,
                                                                               columnToRelationship, columnToForeignKey);
                record.put(column.getName(), value);
            }
            records.add(record);
        }
        
        basicValueGenerator.logTableFakerMappings(tableNameOnly);
        log.debug("Generated {} records for table: {}", records.size(), tableName);
        
        return records;
    }
    
    private Object generateValueForColumnWithGeneratedDataOptimized(Column column, int index, Table table, 
                                                                    Map<String, List<Long>> generatedKeysMap, 
                                                                    Map<String, List<Map<String, Object>>> generatedDataMap, 
                                                                    DatabaseSchema schema,
                                                                    Map<String, Relationship> columnToRelationship,
                                                                    Map<String, ForeignKey> columnToForeignKey) {
        // 관계 체크 (캐시된 맵 사용)
        Relationship relationship = columnToRelationship.get(column.getName());
        if (relationship != null) {
            return relationshipValueGenerator.generateFromData(relationship, column.getName(), index, generatedKeysMap, generatedDataMap);
        }
        
        // 외래키 체크 (캐시된 맵 사용)
        ForeignKey fk = columnToForeignKey.get(column.getName());
        if (fk != null) {
            return foreignKeyValueGenerator.generateFromData(fk, generatedKeysMap, generatedDataMap, index);
        }
        
        return basicValueGenerator.generate(column, index, table);
    }
    
    private String extractTableNameOnly(String tableName) {
        return tableName.contains(".") ? tableName.substring(tableName.lastIndexOf(".") + 1) : tableName;
    }
    
    private Object generateValueForColumn(Column column, int index, Table table, 
                                         Map<String, List<Map<String, Object>>> fakeData, DatabaseSchema schema) {
        List<Relationship> relationships = getRelationshipsForTable(table.getName(), schema);
        for (Relationship relationship : relationships) {
            if (relationship.getSourceColumns().contains(column.getName())) {
                return relationshipValueGenerator.generateFromFakeData(relationship, column.getName(), index, fakeData);
            }
        }
        
        for (ForeignKey fk : table.getForeignKeys()) {
            if (fk.getColumnName().equals(column.getName())) {
                return foreignKeyValueGenerator.generateFromFakeData(fk, fakeData, index);
            }
        }
        
        return basicValueGenerator.generate(column, index, table);
    }
    
    /**
     * 실제 생성된 키 값과 데이터를 사용하여 컬럼 값을 생성
     */
    private Object generateValueForColumnWithGeneratedData(Column column, int index, Table table, 
                                                          Map<String, List<Long>> generatedKeysMap, 
                                                          Map<String, List<Map<String, Object>>> generatedDataMap, 
                                                          DatabaseSchema schema) {
        List<Relationship> relationships = getRelationshipsForTable(table.getName(), schema);
        for (Relationship relationship : relationships) {
            if (relationship.getSourceColumns().contains(column.getName())) {
                return relationshipValueGenerator.generateFromData(relationship, column.getName(), index, generatedKeysMap, generatedDataMap);
            }
        }
        
        for (ForeignKey fk : table.getForeignKeys()) {
            if (fk.getColumnName().equals(column.getName())) {
                return foreignKeyValueGenerator.generateFromData(fk, generatedKeysMap, generatedDataMap, index);
            }
        }
        
        return basicValueGenerator.generate(column, index, table);
    }
    
    private Object generateValueForColumnWithGeneratedKeys(Column column, int index, Table table, 
                                                           Map<String, List<Long>> generatedKeysMap, DatabaseSchema schema) {
        List<Relationship> relationships = getRelationshipsForTable(table.getName(), schema);
        for (Relationship relationship : relationships) {
            if (relationship.getSourceColumns().contains(column.getName())) {
                return relationshipValueGenerator.generateFromKeys(relationship, column.getName(), index, generatedKeysMap);
            }
        }
        
        for (ForeignKey fk : table.getForeignKeys()) {
            if (fk.getColumnName().equals(column.getName())) {
                return foreignKeyValueGenerator.generateFromKeys(fk, generatedKeysMap, index);
            }
        }
        
        return basicValueGenerator.generate(column, index, table);
    }

    private List<Relationship> getRelationshipsForTable(String tableName, DatabaseSchema schema) {
        String tableNameOnly = extractTableNameOnly(tableName);
        List<Relationship> relationships = new ArrayList<>();
        
        for (Relationship relationship : schema.getRelationships()) {
            if (relationship.getSourceTable().getName().equals(tableNameOnly)) {
                relationships.add(relationship);
            }
        }
        
        return relationships;
    }

    public List<String> getOrderedTableNames(DatabaseSchema schema) {
        log.debug("Determining table insertion order using topological sort");
        List<Table> orderedTables = topologicalSorter.getOrderedTables(schema);
        List<String> orderedTableNames = new ArrayList<>();
        
        for (Table table : orderedTables) {
            String fullTableName = (table.getSchemaName() != null && !table.getSchemaName().isEmpty())
                    ? table.getSchemaName() + "." + table.getName()
                    : table.getName();
            orderedTableNames.add(fullTableName);
        }
        
        log.debug("Table insertion order: {}", orderedTableNames);
        return orderedTableNames;
    }
}