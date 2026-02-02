package com.example.fakedatagen.model;

public class Column {
    private String name;
    private String dataType;
    private boolean isPrimaryKey = false;
    private boolean isAutoIncrement = false;
    private boolean isNullable = true;
    private String defaultValue;
    private int maxLength = -1;

    public Column(String name, String dataType) {
        this.name = name;
        this.dataType = dataType;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public boolean isAutoIncrement() {
        return isAutoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        isAutoIncrement = autoIncrement;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public int getMaxLength() {
        return maxLength;
    }
}
