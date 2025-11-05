package com.example.fakedatagen.generator;

import com.example.fakedatagen.model.*;
import net.datafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RelationshipValueGenerator {
    
    @Autowired
    private BasicValueGenerator basicValueGenerator;
    
    private Faker faker = new Faker();

    /**
     * 실제 생성된 키 값과 데이터를 사용하여 Relationship 값을 생성
     */
    public Object generateFromData(Relationship relationship, String columnName, int index,
                                   Map<String, List<Long>> generatedKeysMap,
                                   Map<String, List<Map<String, Object>>> generatedDataMap) {
        boolean isCompositeKeyTable = isCompositePrimaryKeyTable(relationship.getSourceTable());

        switch (relationship.getType()) {
            case ONE_TO_ONE:
                return generateOneToOneValueWithGeneratedData(relationship, index, generatedKeysMap, generatedDataMap);
            case ONE_TO_MANY:
                return generateOneToManyValueWithGeneratedData(relationship, columnName, index, generatedKeysMap, generatedDataMap);
            case MANY_TO_ONE:
                boolean hasUnique = basicValueGenerator.hasUniqueConstraint(relationship.getSourceTable(), columnName);
                if (hasUnique) {
                    return generateOneToOneValueWithGeneratedData(relationship, index, generatedKeysMap, generatedDataMap);
                }
                return generateManyToOneValueWithGeneratedData(relationship, generatedKeysMap, generatedDataMap, index);
            case MANY_TO_MANY:
                if (isCompositeKeyTable) {
                    return generateCompositeKeyValueWithGeneratedData(relationship, columnName, index, generatedKeysMap, generatedDataMap);
                }
                return generateManyToManyValueWithGeneratedData(relationship, index, generatedKeysMap, generatedDataMap);
            default:
                return 1;
        }
    }

    public Object generateFromFakeData(Relationship relationship, String columnName, int index, 
                                       Map<String, List<Map<String, Object>>> fakeData) {
        boolean isCompositeKeyTable = isCompositePrimaryKeyTable(relationship.getSourceTable());

        switch (relationship.getType()) {
            case ONE_TO_ONE:
                return generateOneToOneValue(relationship, index, fakeData);
            case ONE_TO_MANY:
                return generateOneToManyValue(relationship, columnName, index, fakeData);
            case MANY_TO_ONE:
                boolean hasUnique = basicValueGenerator.hasUniqueConstraint(relationship.getSourceTable(), columnName);
                if (hasUnique) {
                    return generateOneToOneValue(relationship, index, fakeData);
                }
                return generateManyToOneValue(relationship, fakeData, index);
            case MANY_TO_MANY:
                if (isCompositeKeyTable) {
                    return generateCompositeKeyValue(relationship, columnName, index, fakeData);
                }
                return generateManyToManyValue(relationship, index, fakeData);
            default:
                return 1;
        }
    }

    public Object generateFromKeys(Relationship relationship, String columnName, int index, 
                                   Map<String, List<Long>> generatedKeysMap) {
        boolean isCompositeKeyTable = isCompositePrimaryKeyTable(relationship.getSourceTable());

        switch (relationship.getType()) {
            case ONE_TO_ONE:
                return generateOneToOneValueWithGeneratedKeys(relationship, index, generatedKeysMap);
            case ONE_TO_MANY:
                return generateOneToManyValueWithGeneratedKeys(relationship, columnName, index, generatedKeysMap);
            case MANY_TO_ONE:
                boolean hasUnique = basicValueGenerator.hasUniqueConstraint(relationship.getSourceTable(), columnName);
                if (hasUnique) {
                    return generateOneToOneValueWithGeneratedKeys(relationship, index, generatedKeysMap);
                }
                return generateManyToOneValueWithGeneratedKeys(relationship, generatedKeysMap, index);
            case MANY_TO_MANY:
                if (isCompositeKeyTable) {
                    return generateCompositeKeyValueWithGeneratedKeys(relationship, columnName, index, generatedKeysMap);
                }
                return generateManyToManyValueWithGeneratedKeys(relationship, index, generatedKeysMap);
            default:
                return 1;
        }
    }

    // ==================== Private Helper Methods ====================
    
    private boolean isCompositePrimaryKeyTable(Table table) {
        for (Constraint constraint : table.getConstraints()) {
            if (constraint.getType() == Constraint.ConstraintType.PRIMARY_KEY &&
                constraint.getColumns().size() > 1) {
                return true;
            }
        }
        return false;
    }

    // ==================== FakeData 기반 메서드들 ====================
    
    private Object generateCompositeKeyValue(Relationship relationship, String columnName, int index, 
                                             Map<String, List<Map<String, Object>>> fakeData) {
        String sourceTable = relationship.getSourceTable().getName();
        String targetTable = relationship.getTargetTable().getName();

        if (relationship.getSourceColumns().contains(columnName) && fakeData.containsKey(sourceTable)) {
            List<Map<String, Object>> sourceRecords = fakeData.get(sourceTable);
            if (!sourceRecords.isEmpty()) {
                int sourceIndex = index % sourceRecords.size();
                return sourceRecords.get(sourceIndex).get(relationship.getSourceColumns().get(0));
            }
        } else if (relationship.getTargetColumns().contains(columnName) && fakeData.containsKey(targetTable)) {
            List<Map<String, Object>> targetRecords = fakeData.get(targetTable);
            if (!targetRecords.isEmpty()) {
                int targetIndex = index % targetRecords.size();
                return targetRecords.get(targetIndex).get(relationship.getTargetColumns().get(0));
            }
        }
        return 1;
    }

    private Object generateOneToOneValue(Relationship relationship, int currentIndex, 
                                         Map<String, List<Map<String, Object>>> fakeData) {
        String targetTable = relationship.getTargetTable().getName();

        if (fakeData.containsKey(targetTable) && !fakeData.get(targetTable).isEmpty()) {
            List<Map<String, Object>> targetRecords = fakeData.get(targetTable);
            if (currentIndex < targetRecords.size()) {
                Map<String, Object> targetRecord = targetRecords.get(currentIndex);
                return targetRecord.get(relationship.getTargetColumns().get(0));
            }
        }
        return currentIndex + 1;
    }

    private Object generateOneToManyValue(Relationship relationship, String columnName, int index, 
                                          Map<String, List<Map<String, Object>>> fakeData) {
        String sourceTable = relationship.getSourceTable().getName();

        if (fakeData.containsKey(sourceTable) && !fakeData.get(sourceTable).isEmpty()) {
            List<Map<String, Object>> sourceRecords = fakeData.get(sourceTable);
            int childrenPerParent = faker.number().numberBetween(2, 6);
            int parentIndex = index / childrenPerParent;

            if (parentIndex < sourceRecords.size()) {
                Map<String, Object> parentRecord = sourceRecords.get(parentIndex);
                return parentRecord.get(relationship.getSourceColumns().get(0));
            }
        }
        return 1;
    }

    private Object generateManyToManyValue(Relationship relationship, int index, 
                                           Map<String, List<Map<String, Object>>> fakeData) {
        String sourceTable = relationship.getSourceTable().getName();
        String targetTable = relationship.getTargetTable().getName();

        if (fakeData.containsKey(sourceTable) && fakeData.containsKey(targetTable)) {
            List<Map<String, Object>> sourceRecords = fakeData.get(sourceTable);
            List<Map<String, Object>> targetRecords = fakeData.get(targetTable);

            if (!sourceRecords.isEmpty() && !targetRecords.isEmpty()) {
                int targetIndex = index % targetRecords.size();
                return targetRecords.get(targetIndex).get(relationship.getTargetColumns().get(0));
            }
        }
        return 1;
    }

    private Object generateManyToOneValue(Relationship relationship, Map<String, List<Map<String, Object>>> fakeData, 
                                          int currentIndex) {
        String targetTable = relationship.getTargetTable().getName();
        if (fakeData.containsKey(targetTable) && !fakeData.get(targetTable).isEmpty()) {
            List<Map<String, Object>> targetRecords = fakeData.get(targetTable);
            int selectedIndex = currentIndex % targetRecords.size();
            Map<String, Object> selectedRecord = targetRecords.get(selectedIndex);
            return selectedRecord.get(relationship.getTargetColumns().get(0));
        }
        return currentIndex + 1;
    }

    // ==================== GeneratedKeys 기반 메서드들 ====================
    
    private Object generateOneToOneValueWithGeneratedKeys(Relationship relationship, int currentIndex, 
                                                          Map<String, List<Long>> generatedKeysMap) {
        String targetTable = relationship.getTargetTable().getName();
        String targetTableOnly = targetTable;
        if (targetTable.contains(".")) {
            targetTableOnly = targetTable.substring(targetTable.lastIndexOf(".") + 1);
        }

        if (generatedKeysMap.containsKey(targetTableOnly) && !generatedKeysMap.get(targetTableOnly).isEmpty()) {
            List<Long> targetKeys = generatedKeysMap.get(targetTableOnly);
            if (currentIndex < targetKeys.size()) {
                return targetKeys.get(currentIndex);
            }
        }
        return currentIndex + 1;
    }

    private Object generateOneToManyValueWithGeneratedKeys(Relationship relationship, String columnName, int index, 
                                                           Map<String, List<Long>> generatedKeysMap) {
        String sourceTable = relationship.getSourceTable().getName();
        String sourceTableOnly = sourceTable;
        if (sourceTable.contains(".")) {
            sourceTableOnly = sourceTable.substring(sourceTable.lastIndexOf(".") + 1);
        }

        if (generatedKeysMap.containsKey(sourceTableOnly) && !generatedKeysMap.get(sourceTableOnly).isEmpty()) {
            List<Long> sourceKeys = generatedKeysMap.get(sourceTableOnly);
            int childrenPerParent = 3;
            int parentIndex = index / childrenPerParent;

            if (parentIndex < sourceKeys.size()) {
                return sourceKeys.get(parentIndex);
            }
        }
        return index + 1;
    }

    private Object generateManyToOneValueWithGeneratedKeys(Relationship relationship, 
                                                           Map<String, List<Long>> generatedKeysMap, int currentIndex) {
        String targetTable = relationship.getTargetTable().getName();
        String targetTableOnly = targetTable;
        if (targetTable.contains(".")) {
            targetTableOnly = targetTable.substring(targetTable.lastIndexOf(".") + 1);
        }

        if (generatedKeysMap.containsKey(targetTableOnly) && !generatedKeysMap.get(targetTableOnly).isEmpty()) {
            List<Long> targetKeys = generatedKeysMap.get(targetTableOnly);
            int selectedIndex = currentIndex % targetKeys.size();
            return targetKeys.get(selectedIndex);
        }
        return currentIndex + 1;
    }

    private Object generateManyToManyValueWithGeneratedKeys(Relationship relationship, int index, 
                                                            Map<String, List<Long>> generatedKeysMap) {
        String sourceTable = relationship.getSourceTable().getName();
        String targetTable = relationship.getTargetTable().getName();
        
        String sourceTableOnly = sourceTable;
        String targetTableOnly = targetTable;
        if (sourceTable.contains(".")) {
            sourceTableOnly = sourceTable.substring(sourceTable.lastIndexOf(".") + 1);
        }
        if (targetTable.contains(".")) {
            targetTableOnly = targetTable.substring(targetTable.lastIndexOf(".") + 1);
        }

        if (generatedKeysMap.containsKey(sourceTableOnly) && generatedKeysMap.containsKey(targetTableOnly)) {
            List<Long> sourceKeys = generatedKeysMap.get(sourceTableOnly);
            List<Long> targetKeys = generatedKeysMap.get(targetTableOnly);

            if (!sourceKeys.isEmpty() && !targetKeys.isEmpty()) {
                int sourceIndex = index % sourceKeys.size();
                return sourceKeys.get(sourceIndex);
            }
        }
        return index + 1;
    }

    private Object generateCompositeKeyValueWithGeneratedKeys(Relationship relationship, String columnName, int index, 
                                                              Map<String, List<Long>> generatedKeysMap) {
        String sourceTable = relationship.getSourceTable().getName();
        String targetTable = relationship.getTargetTable().getName();
        
        String sourceTableOnly = sourceTable;
        String targetTableOnly = targetTable;
        if (sourceTable.contains(".")) {
            sourceTableOnly = sourceTable.substring(sourceTable.lastIndexOf(".") + 1);
        }
        if (targetTable.contains(".")) {
            targetTableOnly = targetTable.substring(targetTable.lastIndexOf(".") + 1);
        }

        if (relationship.getSourceColumns().contains(columnName) && generatedKeysMap.containsKey(sourceTableOnly)) {
            List<Long> sourceKeys = generatedKeysMap.get(sourceTableOnly);
            if (!sourceKeys.isEmpty()) {
                int selectedIndex = index % sourceKeys.size();
                return sourceKeys.get(selectedIndex);
            }
        } else if (relationship.getTargetColumns().contains(columnName) && generatedKeysMap.containsKey(targetTableOnly)) {
            List<Long> targetKeys = generatedKeysMap.get(targetTableOnly);
            if (!targetKeys.isEmpty()) {
                int selectedIndex = index % targetKeys.size();
                return targetKeys.get(selectedIndex);
            }
        }
        return index + 1;
    }

    // ==================== GeneratedData 기반 메서드들 ====================
    
    private Object generateOneToOneValueWithGeneratedData(Relationship relationship, int currentIndex, 
                                                         Map<String, List<Long>> generatedKeysMap, 
                                                         Map<String, List<Map<String, Object>>> generatedDataMap) {
        String targetTable = relationship.getTargetTable().getName();
        String targetTableOnly = targetTable;
        if (targetTable.contains(".")) {
            targetTableOnly = targetTable.substring(targetTable.lastIndexOf(".") + 1);
        }
        
        if (generatedDataMap.containsKey(targetTableOnly) && !generatedDataMap.get(targetTableOnly).isEmpty()) {
            List<Map<String, Object>> targetData = generatedDataMap.get(targetTableOnly);
            if (currentIndex < targetData.size()) {
                Map<String, Object> targetRecord = targetData.get(currentIndex);
                return targetRecord.get(relationship.getTargetColumns().get(0));
            }
        }
        
        if (generatedKeysMap.containsKey(targetTableOnly) && !generatedKeysMap.get(targetTableOnly).isEmpty()) {
            List<Long> targetKeys = generatedKeysMap.get(targetTableOnly);
            if (currentIndex < targetKeys.size()) {
                return targetKeys.get(currentIndex);
            }
        }
        
        return currentIndex + 1;
    }
    
    private Object generateOneToManyValueWithGeneratedData(Relationship relationship, String columnName, int index, 
                                                          Map<String, List<Long>> generatedKeysMap, 
                                                          Map<String, List<Map<String, Object>>> generatedDataMap) {
        String sourceTable = relationship.getSourceTable().getName();
        String sourceTableOnly = sourceTable;
        if (sourceTable.contains(".")) {
            sourceTableOnly = sourceTable.substring(sourceTable.lastIndexOf(".") + 1);
        }
        
        if (generatedDataMap.containsKey(sourceTableOnly) && !generatedDataMap.get(sourceTableOnly).isEmpty()) {
            List<Map<String, Object>> sourceData = generatedDataMap.get(sourceTableOnly);
            int childrenPerParent = 3;
            int parentIndex = index / childrenPerParent;
            
            if (parentIndex < sourceData.size()) {
                Map<String, Object> parentRecord = sourceData.get(parentIndex);
                return parentRecord.get(relationship.getSourceColumns().get(0));
            }
        }
        
        if (generatedKeysMap.containsKey(sourceTableOnly) && !generatedKeysMap.get(sourceTableOnly).isEmpty()) {
            List<Long> sourceKeys = generatedKeysMap.get(sourceTableOnly);
            int childrenPerParent = 3;
            int parentIndex = index / childrenPerParent;
            
            if (parentIndex < sourceKeys.size()) {
                return sourceKeys.get(parentIndex);
            }
        }
        
        return index + 1;
    }
    
    private Object generateManyToOneValueWithGeneratedData(Relationship relationship, 
                                                          Map<String, List<Long>> generatedKeysMap, 
                                                          Map<String, List<Map<String, Object>>> generatedDataMap, 
                                                          int currentIndex) {
        String targetTable = relationship.getTargetTable().getName();
        String targetTableOnly = targetTable;
        if (targetTable.contains(".")) {
            targetTableOnly = targetTable.substring(targetTable.lastIndexOf(".") + 1);
        }
        
        if (generatedDataMap.containsKey(targetTableOnly) && !generatedDataMap.get(targetTableOnly).isEmpty()) {
            List<Map<String, Object>> targetData = generatedDataMap.get(targetTableOnly);
            int selectedIndex = currentIndex % targetData.size();
            Map<String, Object> selectedRecord = targetData.get(selectedIndex);
            return selectedRecord.get(relationship.getTargetColumns().get(0));
        }
        
        if (generatedKeysMap.containsKey(targetTableOnly) && !generatedKeysMap.get(targetTableOnly).isEmpty()) {
            List<Long> targetKeys = generatedKeysMap.get(targetTableOnly);
            int selectedIndex = currentIndex % targetKeys.size();
            return targetKeys.get(selectedIndex);
        }
        
        return currentIndex + 1;
    }
    
    private Object generateManyToManyValueWithGeneratedData(Relationship relationship, int index, 
                                                           Map<String, List<Long>> generatedKeysMap, 
                                                           Map<String, List<Map<String, Object>>> generatedDataMap) {
        String sourceTable = relationship.getSourceTable().getName();
        String targetTable = relationship.getTargetTable().getName();
        
        String sourceTableOnly = sourceTable;
        String targetTableOnly = targetTable;
        if (sourceTable.contains(".")) {
            sourceTableOnly = sourceTable.substring(sourceTable.lastIndexOf(".") + 1);
        }
        if (targetTable.contains(".")) {
            targetTableOnly = targetTable.substring(targetTable.lastIndexOf(".") + 1);
        }
        
        if (generatedDataMap.containsKey(sourceTableOnly) && generatedDataMap.containsKey(targetTableOnly)) {
            List<Map<String, Object>> sourceData = generatedDataMap.get(sourceTableOnly);
            List<Map<String, Object>> targetData = generatedDataMap.get(targetTableOnly);
            
            if (!sourceData.isEmpty() && !targetData.isEmpty()) {
                int sourceIndex = index % sourceData.size();
                return sourceData.get(sourceIndex).get(relationship.getSourceColumns().get(0));
            }
        }
        
        if (generatedKeysMap.containsKey(sourceTableOnly) && generatedKeysMap.containsKey(targetTableOnly)) {
            List<Long> sourceKeys = generatedKeysMap.get(sourceTableOnly);
            List<Long> targetKeys = generatedKeysMap.get(targetTableOnly);
            
            if (!sourceKeys.isEmpty() && !targetKeys.isEmpty()) {
                int sourceIndex = index % sourceKeys.size();
                return sourceKeys.get(sourceIndex);
            }
        }
        
        return index + 1;
    }
    
    private Object generateCompositeKeyValueWithGeneratedData(Relationship relationship, String columnName, int index, 
                                                            Map<String, List<Long>> generatedKeysMap, 
                                                            Map<String, List<Map<String, Object>>> generatedDataMap) {
        String sourceTable = relationship.getSourceTable().getName();
        String targetTable = relationship.getTargetTable().getName();
        
        String sourceTableOnly = sourceTable;
        String targetTableOnly = targetTable;
        if (sourceTable.contains(".")) {
            sourceTableOnly = sourceTable.substring(sourceTable.lastIndexOf(".") + 1);
        }
        if (targetTable.contains(".")) {
            targetTableOnly = targetTable.substring(targetTable.lastIndexOf(".") + 1);
        }
        
        if (relationship.getSourceColumns().contains(columnName) && generatedDataMap.containsKey(sourceTableOnly)) {
            List<Map<String, Object>> sourceData = generatedDataMap.get(sourceTableOnly);
            if (!sourceData.isEmpty()) {
                int selectedIndex = index % sourceData.size();
                return sourceData.get(selectedIndex).get(relationship.getSourceColumns().get(0));
            }
        } else if (relationship.getTargetColumns().contains(columnName) && generatedDataMap.containsKey(targetTableOnly)) {
            List<Map<String, Object>> targetData = generatedDataMap.get(targetTableOnly);
            if (!targetData.isEmpty()) {
                int selectedIndex = index % targetData.size();
                return targetData.get(selectedIndex).get(relationship.getTargetColumns().get(0));
            }
        }
        
        if (relationship.getSourceColumns().contains(columnName) && generatedKeysMap.containsKey(sourceTableOnly)) {
            List<Long> sourceKeys = generatedKeysMap.get(sourceTableOnly);
            if (!sourceKeys.isEmpty()) {
                int selectedIndex = index % sourceKeys.size();
                return sourceKeys.get(selectedIndex);
            }
        } else if (relationship.getTargetColumns().contains(columnName) && generatedKeysMap.containsKey(targetTableOnly)) {
            List<Long> targetKeys = generatedKeysMap.get(targetTableOnly);
            if (!targetKeys.isEmpty()) {
                int selectedIndex = index % targetKeys.size();
                return targetKeys.get(selectedIndex);
            }
        }
        
        return index + 1;
    }
}


