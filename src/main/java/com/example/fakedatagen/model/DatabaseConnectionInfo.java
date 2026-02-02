package com.example.fakedatagen.model;

import java.util.Objects;

/**
 * 데이터베이스 연결 정보를 담는 값 객체
 * Builder 패턴을 지원하여 유연한 객체 생성 가능
 */
public class DatabaseConnectionInfo {
    private final String host;
    private final int port;
    private final String databaseName;
    private final String username;
    private final String password;
    
    public DatabaseConnectionInfo(String host, int port, String databaseName, String username, String password) {
        this.host = validateHost(host);
        this.port = validatePort(port);
        this.databaseName = validateDatabaseName(databaseName);
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.password = Objects.requireNonNull(password, "password must not be null");
    }
    
    private DatabaseConnectionInfo(Builder builder) {
        this.host = validateHost(builder.host);
        this.port = validatePort(builder.port);
        this.databaseName = validateDatabaseName(builder.databaseName);
        this.username = Objects.requireNonNull(builder.username, "username must not be null");
        this.password = Objects.requireNonNull(builder.password, "password must not be null");
    }
    
    private String validateHost(String host) {
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("host must not be null or empty");
        }
        return host.trim();
    }
    
    private int validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        return port;
    }
    
    private String validateDatabaseName(String databaseName) {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("databaseName must not be null or empty");
        }
        return databaseName.trim();
    }
    
    /**
     * CUBRID JDBC URL 생성
     * 형식: jdbc:cubrid:host:port:databaseName:::
     */
    public String getJdbcUrl() {
        return String.format("jdbc:cubrid:%s:%d:%s:::", host, port, databaseName);
    }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    
    /**
     * Builder 패턴을 위한 내부 클래스
     */
    public static class Builder {
        private String host;
        private int port;
        private String databaseName;
        private String username;
        private String password;
        
        public Builder host(String host) {
            this.host = host;
            return this;
        }
        
        public Builder port(int port) {
            this.port = port;
            return this;
        }
        
        public Builder databaseName(String databaseName) {
            this.databaseName = databaseName;
            return this;
        }
        
        public Builder username(String username) {
            this.username = username;
            return this;
        }
        
        public Builder password(String password) {
            this.password = password;
            return this;
        }
        
        public DatabaseConnectionInfo build() {
            return new DatabaseConnectionInfo(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseConnectionInfo that = (DatabaseConnectionInfo) o;
        return port == that.port &&
               Objects.equals(host, that.host) &&
               Objects.equals(databaseName, that.databaseName) &&
               Objects.equals(username, that.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(host, port, databaseName, username);
    }
    
    @Override
    public String toString() {
        return String.format("DatabaseConnectionInfo{host='%s', port=%d, databaseName='%s', username='%s'}", 
                           host, port, databaseName, username);
    }
}
