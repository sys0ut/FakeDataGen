package com.example.fakedatagen.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Table {
    private String name;
    private String schemaName;
    private List<Column> columns = new ArrayList<>();
    private List<ForeignKey> foreignKeys = new ArrayList<>();
    private List<Constraint> constraints = new ArrayList<>();

    public Table(String name) {
        this.name = name;
    }

    public Table(String schemaName, String name) {
        this.schemaName = schemaName;
        this.name = name;
    }

    public String getName() {
        return name;
    }


    public String getSchemaName() {
        return schemaName;
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void setColumns(List<Column> columns) {
        this.columns = columns;
    }

    public List<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void addColumn(Column column) {
        columns.add(column);
    }

    public void addForeignKey(ForeignKey fk) {
        foreignKeys.add(fk);
    }

    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    public Column getColumnByName(String columnName) {
        return columns.stream()
                .filter(col -> col.getName().equals(columnName))
                .findFirst()
                .orElse(null);
    }

    public List<Column> getPrimaryKeyColumns() {
        return columns.stream()
                .filter(Column::isPrimaryKey)
                .collect(Collectors.toList());
    }
}
