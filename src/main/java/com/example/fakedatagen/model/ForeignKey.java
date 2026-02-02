package com.example.fakedatagen.model;

public class ForeignKey {
    private String columnName;
    private String referencedTableName;
    private String referencedColumnName;
    private String constraintName;
    private String onDeleteAction = "RESTRICT";
    private String onUpdateAction = "RESTRICT";

    public ForeignKey(String columnName, String referencedTableName, String referencedColumnName) {
        this.columnName = columnName;
        this.referencedTableName = referencedTableName;
        this.referencedColumnName = referencedColumnName;
    }

    public ForeignKey(String constraintName, String columnName, String referencedTableName, String referencedColumnName) {
        this.constraintName = constraintName;
        this.columnName = columnName;
        this.referencedTableName = referencedTableName;
        this.referencedColumnName = referencedColumnName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getReferencedTableName() {
        return referencedTableName;
    }

    public String getReferencedColumnName() {
        return referencedColumnName;
    }

    public String getConstraintName() {
        return constraintName;
    }
}
