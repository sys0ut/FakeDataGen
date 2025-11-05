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

    // getter, setter
    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getReferencedTableName() {
        return referencedTableName;
    }

    public void setReferencedTableName(String referencedTableName) {
        this.referencedTableName = referencedTableName;
    }

    public String getReferencedColumnName() {
        return referencedColumnName;
    }

    public void setReferencedColumnName(String referencedColumnName) {
        this.referencedColumnName = referencedColumnName;
    }

    public String getConstraintName() {
        return constraintName;
    }

    public void setConstraintName(String constraintName) {
        this.constraintName = constraintName;
    }

    public String getOnDeleteAction() {
        return onDeleteAction;
    }

    public void setOnDeleteAction(String onDeleteAction) {
        this.onDeleteAction = onDeleteAction;
    }

    public String getOnUpdateAction() {
        return onUpdateAction;
    }

    public void setOnUpdateAction(String onUpdateAction) {
        this.onUpdateAction = onUpdateAction;
    }

    public String getFullReference() {
        return referencedTableName + "." + referencedColumnName;
    }
}
