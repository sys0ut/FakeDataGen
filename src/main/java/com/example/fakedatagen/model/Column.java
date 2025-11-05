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

    // getter, setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
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

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public String getFullDataType() {
        if (maxLength > 0 && dataType.toLowerCase().contains("varchar")) {
            return dataType + "(" + maxLength + ")";
        }
        return dataType;
    }
}
