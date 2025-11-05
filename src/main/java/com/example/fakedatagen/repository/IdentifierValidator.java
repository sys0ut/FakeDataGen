package com.example.fakedatagen.repository;

import com.example.fakedatagen.model.DatabaseSchema;
import com.example.fakedatagen.model.Table;
import com.example.fakedatagen.model.Column;

import java.util.HashSet;
import java.util.Set;

/**
 * 테이블/컬럼 식별자 화이트리스트 검증 유틸리티
 * 스키마에 존재하는 테이블/컬럼만 허용
 */
public final class IdentifierValidator {
    private final Set<String> allowedTables;
    private final Set<String> allowedTableAndColumn; // "table.column" 형태

    public IdentifierValidator(DatabaseSchema schema) {
        this.allowedTables = new HashSet<>();
        this.allowedTableAndColumn = new HashSet<>();
        for (Table t : schema.getTables()) {
            allowedTables.add(t.getName());
            for (Column c : t.getColumns()) {
                allowedTableAndColumn.add(t.getName() + "." + c.getName());
            }
        }
    }

    public boolean isAllowedTable(String tableName) {
        String nameOnly = tableName.contains(".") ? tableName.substring(tableName.lastIndexOf('.') + 1) : tableName;
        return allowedTables.contains(nameOnly);
    }

    public boolean isAllowedColumn(String tableName, String columnName) {
        String nameOnly = tableName.contains(".") ? tableName.substring(tableName.lastIndexOf('.') + 1) : tableName;
        return allowedTableAndColumn.contains(nameOnly + "." + columnName);
    }
}


