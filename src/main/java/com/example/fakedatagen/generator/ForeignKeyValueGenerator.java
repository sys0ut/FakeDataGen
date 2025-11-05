package com.example.fakedatagen.generator;

import com.example.fakedatagen.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ForeignKey 값 생성을 담당하는 클래스
 */
@Component
public class ForeignKeyValueGenerator {
    
    /**
     * 메모리 데이터 기반 외래키 값 생성
     */
    public Object generateFromFakeData(ForeignKey fk, Map<String, List<Map<String, Object>>> fakeData, int currentIndex) {
        String referencedTable = fk.getReferencedTableName();
        String referencedColumn = fk.getReferencedColumnName();
        
        String tableNameOnly = referencedTable;
        if (referencedTable.contains(".")) {
            tableNameOnly = referencedTable.substring(referencedTable.lastIndexOf(".") + 1);
        }
        
        if (fakeData.containsKey(tableNameOnly)) {
            List<Map<String, Object>> referencedRecords = fakeData.get(tableNameOnly);
            if (!referencedRecords.isEmpty()) {
                int selectedIndex = currentIndex % referencedRecords.size();
                Map<String, Object> selectedRecord = referencedRecords.get(selectedIndex);
                Object value = selectedRecord.get(referencedColumn);
                return value;
            }
        }
        
        int defaultValue = currentIndex + 1;
        return defaultValue;
    }
    
    /**
     * 실제 생성된 키 값을 사용하여 외래키 값 생성
     */
    public Object generateFromKeys(ForeignKey fk, Map<String, List<Long>> generatedKeysMap, int currentIndex) {
        String referencedTable = fk.getReferencedTableName();
        
        String[] possibleTableNames = {
            referencedTable,
            referencedTable.contains(".") ? referencedTable.substring(referencedTable.lastIndexOf(".") + 1) : referencedTable,
            referencedTable.toUpperCase(),
            referencedTable.toLowerCase(),
            referencedTable.contains(".") ? referencedTable.substring(referencedTable.lastIndexOf(".") + 1).toUpperCase() : referencedTable.toUpperCase(),
            referencedTable.contains(".") ? referencedTable.substring(referencedTable.lastIndexOf(".") + 1).toLowerCase() : referencedTable.toLowerCase()
        };
        
        for (String tableName : possibleTableNames) {
            if (generatedKeysMap.containsKey(tableName)) {
                List<Long> generatedKeys = generatedKeysMap.get(tableName);
                if (!generatedKeys.isEmpty()) {
                    int selectedIndex = currentIndex % generatedKeys.size();
                    Long value = generatedKeys.get(selectedIndex);
                    return value;
                }
            }
        }
        
        if (isForeignKeyColumnNullable(fk)) {
            return null;
        } else {
            return 1L;
        }
    }
    
    /**
     * 실제 생성된 키 값과 데이터를 사용하여 외래키 값 생성
     */
    public Object generateFromData(ForeignKey fk, Map<String, List<Long>> generatedKeysMap, 
                                   Map<String, List<Map<String, Object>>> generatedDataMap, int currentIndex) {
        String referencedTable = fk.getReferencedTableName();
        String referencedColumn = fk.getReferencedColumnName();
        
        System.out.println("외래키 값 생성 (데이터 포함) - 참조 테이블: " + referencedTable + ", 컬럼: " + referencedColumn);
        
        String[] possibleTableNames = {
            referencedTable,
            referencedTable.contains(".") ? referencedTable.substring(referencedTable.lastIndexOf(".") + 1) : referencedTable,
            referencedTable.toUpperCase(),
            referencedTable.toLowerCase(),
            referencedTable.contains(".") ? referencedTable.substring(referencedTable.lastIndexOf(".") + 1).toUpperCase() : referencedTable.toUpperCase(),
            referencedTable.contains(".") ? referencedTable.substring(referencedTable.lastIndexOf(".") + 1).toLowerCase() : referencedTable.toLowerCase()
        };
        
        for (String tableName : possibleTableNames) {
            if (generatedKeysMap.containsKey(tableName)) {
                List<Long> generatedKeys = generatedKeysMap.get(tableName);
                if (!generatedKeys.isEmpty()) {
                    int selectedIndex = currentIndex % generatedKeys.size();
                    Long value = generatedKeys.get(selectedIndex);
                    if (value != 0) {
                        System.out.println("생성된 키에서 외래키 값 선택: " + value);
                        return value;
                    }
                }
            }
        }
        
        for (String tableName : possibleTableNames) {
            if (generatedDataMap.containsKey(tableName)) {
                List<Map<String, Object>> referencedData = generatedDataMap.get(tableName);
                if (!referencedData.isEmpty()) {
                    int selectedIndex = currentIndex % referencedData.size();
                    Map<String, Object> selectedRecord = referencedData.get(selectedIndex);
                    
                    Object value = selectedRecord.get(referencedColumn);
                    if (value == null) {
                        String[] possibleColumnNames = {
                            referencedColumn,
                            "id",
                            tableName.contains(".") ? tableName.substring(tableName.lastIndexOf(".") + 1) + "_id" : referencedColumn,
                            "pk_" + referencedColumn,
                            referencedColumn.toUpperCase(),
                            referencedColumn.toLowerCase()
                        };
                        
                        for (String columnName : possibleColumnNames) {
                            value = selectedRecord.get(columnName);
                            if (value != null) {
                                return value;
                            }
                        }
                        
                        if (!selectedRecord.isEmpty()) {
                            value = selectedRecord.values().iterator().next();
                            return value;
                        }
                    } else {
                        return value;
                    }
                }
            }
        }
        
        if (isForeignKeyColumnNullable(fk)) {
            return null;
        } else {
            return 1L;
        }
    }
    
    private boolean isForeignKeyColumnNullable(ForeignKey fk) {
        return true;
    }
}

