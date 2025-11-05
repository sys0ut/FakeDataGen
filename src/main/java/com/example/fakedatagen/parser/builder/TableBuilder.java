package com.example.fakedatagen.parser.builder;

import com.example.fakedatagen.model.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 테이블 정보를 조합하여 Table 객체를 생성하는 빌더 클래스
 */
@Component
public class TableBuilder {
    
    public void buildTablesAndAddToSchema(
            DatabaseSchema schema,
            List<Table> tables,
            Map<String, List<Column>> columnsMap,
            Map<String, List<String>> pkMap,
            Map<String, List<String>> uniqueMap,
            Map<String, List<ForeignKey>> fkMap,
            boolean keepSchemaName) {
        
        for (Table table : tables) {
            String key;
            if (keepSchemaName) {
                key = table.getSchemaName().isEmpty() ? 
                    table.getName() : table.getSchemaName() + "." + table.getName();
            } else {
                key = table.getName();
            }
            
            addColumnsToTable(table, columnsMap.getOrDefault(key, Collections.emptyList()), 
                            pkMap.getOrDefault(key, Collections.emptyList()));
            addPrimaryKeyConstraints(table, pkMap.getOrDefault(key, Collections.emptyList()));
            addUniqueConstraints(table, uniqueMap.getOrDefault(key, Collections.emptyList()));
            addForeignKeys(table, fkMap.getOrDefault(key, Collections.emptyList()));
            
            schema.addTable(table);
        }
    }
    
    private void addColumnsToTable(Table table, List<Column> columns, List<String> pkColumns) {
        for (Column column : columns) {
            if (pkColumns.contains(column.getName())) {
                column.setPrimaryKey(true);
            }
            table.addColumn(column);
        }
    }
    
    private void addPrimaryKeyConstraints(Table table, List<String> pkColumns) {
        if (!pkColumns.isEmpty()) {
            Constraint pkConstraint = new Constraint(
                    "pk_" + table.getName() + "_" + String.join("_", pkColumns), 
                    Constraint.ConstraintType.PRIMARY_KEY);
            for (String pkCol : pkColumns) {
                pkConstraint.addColumn(pkCol);
            }
            table.addConstraint(pkConstraint);
        }
    }
    
    private void addUniqueConstraints(Table table, List<String> uniqueColumns) {
        for (String uniqueCol : uniqueColumns) {
            Constraint uniqueConstraint = new Constraint(
                    "u_" + table.getName() + "_" + uniqueCol, 
                    Constraint.ConstraintType.UNIQUE);
            uniqueConstraint.addColumn(uniqueCol);
            table.addConstraint(uniqueConstraint);
        }
    }
    
    private void addForeignKeys(Table table, List<ForeignKey> foreignKeys) {
        for (ForeignKey fk : foreignKeys) {
            table.addForeignKey(fk);
        }
    }
}

