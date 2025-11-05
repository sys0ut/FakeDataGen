package com.example.fakedatagen.model;

import java.util.ArrayList;
import java.util.List;

public class Constraint {
    private String name;
    private ConstraintType type;
    private List<String> columns = new ArrayList<>();
    private String checkCondition;
    private String referencedTable;
    private String referencedColumn;

    public enum ConstraintType {
        PRIMARY_KEY, FOREIGN_KEY, UNIQUE, CHECK, NOT_NULL
    }

    public Constraint(String name, ConstraintType type) {
        this.name = name;
        this.type = type;
    }

    // getter, setter
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) { 
        this.name = name; 
    }
    
    public ConstraintType getType() { 
        return type; 
    }
    
    public void setType(ConstraintType type) { 
        this.type = type; 
    }
    
    public List<String> getColumns() { 
        return columns; 
    }
    
    public void setColumns(List<String> columns) { 
        this.columns = columns; 
    }
    
    public String getCheckCondition() { 
        return checkCondition; 
    }
    
    public void setCheckCondition(String checkCondition) { 
        this.checkCondition = checkCondition; 
    }
    
    public String getReferencedTable() { 
        return referencedTable; 
    }
    
    public void setReferencedTable(String referencedTable) { 
        this.referencedTable = referencedTable; 
    }
    
    public String getReferencedColumn() { 
        return referencedColumn; 
    }
    
    public void setReferencedColumn(String referencedColumn) { 
        this.referencedColumn = referencedColumn; 
    }
    
    public void addColumn(String column) { 
        columns.add(column); 
    }
}
