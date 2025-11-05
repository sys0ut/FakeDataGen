package com.example.fakedatagen.model;

public class DatabaseConnectionInfo {
    private String host;
    private int port;
    private String databaseName;
    private String username;
    private String password;
    
    public DatabaseConnectionInfo(String host, int port, String databaseName, String username, String password) {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.username = username;
        this.password = password;
    }
    
    public String getJdbcUrl() {
        return String.format("jdbc:cubrid:%s:%d:%s:::", host, port, databaseName);
    }
    
    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabaseName() { return databaseName; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
