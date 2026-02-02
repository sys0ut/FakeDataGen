package com.example.fakedatagen.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * CUBRID 데이터소스 설정을 관리하는 Configuration 클래스
 */
@Configuration
public class DataSourceConfig {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);
    
    /**
     * CUBRID에 최적화된 HikariCP 설정
     */
    private static final int DEFAULT_MAX_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final long DEFAULT_CONNECTION_TIMEOUT = 30000; // 30초
    private static final long DEFAULT_IDLE_TIMEOUT = 600000; // 10분
    private static final long DEFAULT_MAX_LIFETIME = 1800000; // 30분
    private static final String DEFAULT_POOL_NAME = "FakeDataGen-Pool";
    
    /**
     * 동적 데이터소스를 생성합니다.
     * CUBRID 특성에 맞춘 최적화된 설정을 적용합니다.
     * 
     * @param jdbcUrl JDBC URL
     * @param username 사용자명
     * @param password 비밀번호
     * @return HikariDataSource
     */
    public static DataSource createDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);

        config.setDriverClassName("cubrid.jdbc.driver.CUBRIDDriver");

        config.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZE);
        config.setMinimumIdle(DEFAULT_MIN_IDLE);
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME);
        config.setPoolName(DEFAULT_POOL_NAME);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        log.debug("Creating data source - url={}, poolSize={}", jdbcUrl, DEFAULT_MAX_POOL_SIZE);
        
        return new HikariDataSource(config);
    }
    
    public static DataSource createDataSourceForBulkInsert(String jdbcUrl, String username, String password, int maxPoolSize) {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("cubrid.jdbc.driver.CUBRIDDriver");
        
        config.setMaximumPoolSize(Math.max(maxPoolSize, DEFAULT_MAX_POOL_SIZE));
        config.setMinimumIdle(Math.max(maxPoolSize / 2, DEFAULT_MIN_IDLE));
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME);
        config.setPoolName(DEFAULT_POOL_NAME + "-Bulk");
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "500");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        
        log.debug("Creating bulk insert data source - url={}, poolSize={}", jdbcUrl, maxPoolSize);
        
        return new HikariDataSource(config);
    }
}
