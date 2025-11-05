package com.example.fakedatagen.model;

import java.util.ArrayList;
import java.util.List;

public class Relationship {
    private Table sourceTable;
    private Table targetTable;
    private List<String> sourceColumns = new ArrayList<>();
    private List<String> targetColumns = new ArrayList<>();
    private RelationshipType type;
    private boolean isNullable = true;

    public enum RelationshipType {
        ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY
    }

    public Relationship(Table sourceTable, Table targetTable, RelationshipType type) {
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.type = type;
    }

    // getter, setter
    public Table getSourceTable() { 
        return sourceTable; 
    }
    
    public void setSourceTable(Table sourceTable) { 
        this.sourceTable = sourceTable; 
    }
    
    public Table getTargetTable() { 
        return targetTable; 
    }
    
    public void setTargetTable(Table targetTable) { 
        this.targetTable = targetTable; 
    }
    
    public List<String> getSourceColumns() { 
        return sourceColumns; 
    }
    
    public void setSourceColumns(List<String> sourceColumns) { 
        this.sourceColumns = sourceColumns; 
    }
    
    public List<String> getTargetColumns() { 
        return targetColumns; 
    }
    
    public void setTargetColumns(List<String> targetColumns) { 
        this.targetColumns = targetColumns; 
    }
    
    public RelationshipType getType() { 
        return type; 
    }
    
    public void setType(RelationshipType type) { 
        this.type = type; 
    }
    
    public boolean isNullable() { 
        return isNullable; 
    }
    
    public void setNullable(boolean nullable) { 
        isNullable = nullable; 
    }
    
    public void addSourceColumn(String column) { 
        sourceColumns.add(column); 
    }
    
    public void addTargetColumn(String column) { 
        targetColumns.add(column); 
    }
}
