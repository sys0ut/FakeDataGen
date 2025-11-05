package com.example.fakedatagen.model;

import java.util.*;
import java.util.stream.Collectors;

public class DatabaseSchema {
    private String schemaName;
    private List<Table> tables = new ArrayList<>();
    private Map<String, List<String>> dependencies = new HashMap<>();
    private List<Relationship> relationships = new ArrayList<>();

    public DatabaseSchema(String schemaName) {
        this.schemaName = schemaName;
    }

    // getter, setter
    public String getSchemaName() { 
        return schemaName; 
    }
    
    public void setSchemaName(String schemaName) { 
        this.schemaName = schemaName; 
    }
    
    public List<Table> getTables() { 
        return tables; 
    }
    
    public void setTables(List<Table> tables) { 
        this.tables = tables; 
    }
    
    public Map<String, List<String>> getDependencies() { 
        return dependencies; 
    }
    
    public void setDependencies(Map<String, List<String>> dependencies) { 
        this.dependencies = dependencies; 
    }
    
    public List<Relationship> getRelationships() { 
        return relationships; 
    }
    
    public void setRelationships(List<Relationship> relationships) { 
        this.relationships = relationships; 
    }
    
    public void addTable(Table table) { 
        tables.add(table); 
    }
    
    public void addDependency(String table, String dependsOn) { 
        dependencies.computeIfAbsent(table, k -> new ArrayList<>()).add(dependsOn); 
    }
    
    public void addRelationship(Relationship relationship) { 
        relationships.add(relationship); 
    }
    
    public Table getTableByName(String tableName) {
        // 스키마명 제거하고 테이블명만 사용
        final String tableNameOnly = tableName.contains(".") ? 
            tableName.substring(tableName.lastIndexOf(".") + 1) : tableName;
        
        return tables.stream()
                .filter(t -> t.getName().equals(tableNameOnly))
                .findFirst()
                .orElse(null);
    }
    
    public List<Table> getRootTables() {
        return tables.stream()
                .filter(t -> !dependencies.containsKey(t.getName()))
                .collect(Collectors.toList());
    }

    public Table getTableBySchemaAndName(String schemaName, String tableName) {
        return tables.stream()
                .filter(t -> t.getSchemaName().equals(schemaName) && t.getName().equals(tableName))
                .findFirst()
                .orElse(null);
    }
}
