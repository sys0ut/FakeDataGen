package com.example.fakedatagen.service;

import com.example.fakedatagen.model.DatabaseSchema;
import com.example.fakedatagen.model.DatabaseConnectionInfo;
import com.example.fakedatagen.generator.RelationshipAwareGenerator;
import com.example.fakedatagen.repository.DatabaseInsertRepository;
import com.example.fakedatagen.config.DataSourceConfig;
import com.example.fakedatagen.config.FakeDataGenProperties;
import com.example.fakedatagen.exception.DataGenerationException;
import com.example.fakedatagen.exception.DatabaseConnectionException;
import com.example.fakedatagen.util.MemoryMonitor;
import com.example.fakedatagen.util.PerformanceMetrics;
import com.example.fakedatagen.util.RetryHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.MessageSource;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class DataGenerationService {
    
    private static final Logger log = LoggerFactory.getLogger(DataGenerationService.class);

    private final RelationshipAwareGenerator relationshipAwareGenerator;
    private final DatabaseInsertRepository databaseInsertRepository;
    private final MessageSource messageSource;
    private final FakeDataGenProperties properties;

    public DataGenerationService(RelationshipAwareGenerator relationshipAwareGenerator,
                                 DatabaseInsertRepository databaseInsertRepository,
                                 MessageSource messageSource,
                                 FakeDataGenProperties properties) {
        this.relationshipAwareGenerator = relationshipAwareGenerator;
        this.databaseInsertRepository = databaseInsertRepository;
        this.messageSource = messageSource;
        this.properties = properties;
    }
    
    /**
     * Generates fake data and optionally inserts it into the database.
     * 
     * @param schema database schema
     * @param recordCount number of records to generate
     * @param insertToDatabase whether to insert data into database
     * @param dbInfo database connection information
     * @return generated data and insertion results
     */
    public DataGenerationResult generateAndInsertData(DatabaseSchema schema, int recordCount, boolean insertToDatabase, DatabaseConnectionInfo dbInfo) {
        PerformanceMetrics metrics = PerformanceMetrics.start("Data Generation");
        validateInput(schema, recordCount, insertToDatabase, dbInfo);

        if (properties.getMemoryMonitoring().isEnabled()) {
            MemoryMonitor.logMemoryUsage("데이터 생성 시작");
        }

        Map<String, List<Map<String, Object>>> allFakeData;
        int totalInserted = 0;
        String insertMessage;

        if (!insertToDatabase) {
            log.info("Generating data only (no database insertion)");
            allFakeData = generateData(schema, recordCount);
            insertMessage = msg("generate.only", ServiceMessages.GENERATE_ONLY);
            log.info("Data generation completed - {} tables", allFakeData.size());
            
            int totalRecords = allFakeData.values().stream()
                    .mapToInt(List::size)
                    .sum();
            metrics.withRecordCount(totalRecords).logAndComplete();
            
            if (properties.getMemoryMonitoring().isEnabled()) {
                MemoryMonitor.logMemoryUsage("데이터 생성 완료");
                MemoryMonitor.checkMemoryStatus(
                        properties.getMemoryMonitoring().getWarningThreshold(),
                        properties.getMemoryMonitoring().getCriticalThreshold()
                );
            }
            
            return new DataGenerationResult(allFakeData, totalInserted, insertMessage);
        }

        allFakeData = new HashMap<>(properties.getInitialCapacity().getMedium());
        DataSource dynamicDataSource = null;
        try {
            log.info("Creating database connection for data insertion");
            dynamicDataSource = createDynamicDataSource(dbInfo, recordCount);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dynamicDataSource);
            log.debug("Database connection established successfully");

            PlatformTransactionManager txManager = new DataSourceTransactionManager(dynamicDataSource);
            TransactionTemplate txTemplate = new TransactionTemplate(txManager);

            List<String> orderedTableNames = relationshipAwareGenerator.getOrderedTableNames(schema);

            final Map<String, List<Long>> generatedKeysMap = new HashMap<>(properties.getInitialCapacity().getMedium());
            final Map<String, List<Map<String, Object>>> generatedDataMap = new HashMap<>(properties.getInitialCapacity().getMedium());
            final Map<String, Integer> tableInsertCounts = new HashMap<>(properties.getInitialCapacity().getMedium());
            final java.util.List<String> warnings = new java.util.ArrayList<>();

            totalInserted = txTemplate.execute(status -> {
                deleteExistingData(jdbcTemplate, orderedTableNames);
                
                int inserted = 0;
                for (String tableName : orderedTableNames) {
                    try {
                        if (properties.getMemoryMonitoring().isEnabled()) {
                            MemoryMonitor.checkMemoryStatus(
                                    properties.getMemoryMonitoring().getWarningThreshold(),
                                    properties.getMemoryMonitoring().getCriticalThreshold()
                            );
                        }
                        
                        List<Map<String, Object>> tableData = relationshipAwareGenerator.generateTableDataWithGeneratedData(
                                schema, tableName, recordCount, generatedKeysMap, generatedDataMap);
                        if (tableData != null && !tableData.isEmpty()) {
                            log.debug("Inserting data into table: {} ({} records)", tableName, tableData.size());
                            
                            // 재시도 로직 적용
                            List<Long> generatedKeys;
                            if (properties.getRetry().isEnabled()) {
                                generatedKeys = RetryHelper.executeWithRetry(
                                        () -> insertRecordsWithDynamicConnection(jdbcTemplate, tableName, tableData, schema),
                                        properties.getRetry().getMaxAttempts(),
                                        properties.getRetry().getDelay(),
                                        properties.getRetry().getBackoffMultiplier()
                                );
                            } else {
                                generatedKeys = insertRecordsWithDynamicConnection(jdbcTemplate, tableName, tableData, schema);
                            }
                            
                            generatedKeysMap.put(tableName, generatedKeys);
                            generatedDataMap.put(tableName, tableData);
                            allFakeData.put(tableName, tableData);
                            int count = generatedKeys.size();
                            tableInsertCounts.put(tableName, count);
                            inserted += count;
                            log.debug("Successfully inserted {} records into table: {}", count, tableName);
                        }
                    } catch (Exception e) {
                        log.error("Failed to insert data into table: {} - {}", tableName, e.getMessage(), e);
                        // 트랜잭션 내에서 실패 시 전체 롤백을 위해 예외를 다시 던짐
                        // 부분 실패를 허용하려면 이 부분을 주석 처리하고 warnings에만 추가
                        throw new DataGenerationException("테이블 '" + tableName + "' 삽입 실패: " + e.getMessage(), e);
                    }
                }
                return inserted;
            });

            insertMessage = buildInsertMessage(warnings);
            
            metrics.withRecordCount(totalInserted).logAndComplete();
            
            if (properties.getMemoryMonitoring().isEnabled()) {
                MemoryMonitor.logMemoryUsage("데이터 삽입 완료");
            }
            
            return new DataGenerationResult(allFakeData, totalInserted, insertMessage, tableInsertCounts, warnings);
        } catch (DatabaseConnectionException e) {
            log.error("Database connection failed during data insertion", e);
            throw e;
        } catch (Exception e) {
            log.error("Transaction failed during data insertion", e);
            throw new DataGenerationException("데이터 생성 및 삽입 중 오류가 발생했습니다: " + e.getMessage(), e);
        } finally {
            closeDataSource(dynamicDataSource);
        }
    }
    
    private void closeDataSource(DataSource dataSource) {
        if (dataSource == null) {
            return;
        }
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
                log.debug("DataSource closed successfully");
            } catch (Exception e) {
                log.warn("Failed to close DataSource", e);
            }
        } else if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
            try {
                ((com.zaxxer.hikari.HikariDataSource) dataSource).close();
                log.debug("HikariDataSource closed successfully");
            } catch (Exception e) {
                log.warn("Failed to close HikariDataSource", e);
            }
        }
    }
    
    private void deleteExistingData(JdbcTemplate jdbcTemplate, List<String> orderedTableNames) {
        log.info("기존 데이터 삭제 시작 - {}개 테이블", orderedTableNames.size());
        int deletedCount = 0;
        int totalRowsDeleted = 0;
        for (int i = orderedTableNames.size() - 1; i >= 0; i--) {
            String tableName = orderedTableNames.get(i);
            try {
                String sanitizedTableName = sanitizeTableName(tableName);
                String sql = "DELETE FROM " + sanitizedTableName;
                int rowsDeleted = jdbcTemplate.update(sql);
                if (rowsDeleted > 0) {
                    log.info("테이블 '{}'에서 {}개 행 삭제됨", tableName, rowsDeleted);
                    totalRowsDeleted += rowsDeleted;
                }
                deletedCount++;
            } catch (Exception e) {
                throw new DataGenerationException("기존 데이터 삭제 중 오류 발생: " + tableName + " - " + e.getMessage(), e);
            }
        }
        log.info("기존 데이터 삭제 완료 - {}개 테이블, 총 {}개 행 삭제됨", deletedCount, totalRowsDeleted);
    }
    
    private String sanitizeTableName(String tableName) {
        if (tableName.contains(".")) {
            String schemaName = tableName.substring(0, tableName.lastIndexOf("."));
            String tableNameOnly = tableName.substring(tableName.lastIndexOf(".") + 1);
            return "[" + schemaName.replace("]", "]]") + "].[" + tableNameOnly.replace("]", "]]") + "]";
        } else {
            return "[" + tableName.replace("]", "]]") + "]";
        }
    }
    
    private String buildInsertMessage(List<String> warnings) {
        String message = msg("db.insert.success", "데이터 삽입 완료");
        if (!warnings.isEmpty()) {
            message += " (경고 " + warnings.size() + "개)";
        }
        return message;
    }

    private void validateInput(DatabaseSchema schema, int recordCount, boolean insertToDatabase, DatabaseConnectionInfo dbInfo) {
        if (schema == null) {
            throw new IllegalArgumentException("스키마가 null일 수 없습니다");
        }
        if (recordCount < properties.getMinRecordCount()) {
            throw new IllegalArgumentException(
                    String.format("레코드 수는 최소 %d개 이상이어야 합니다. 입력값: %d", 
                            properties.getMinRecordCount(), recordCount));
        }
        if (recordCount > properties.getMaxRecordCount()) {
            throw new IllegalArgumentException(
                    String.format("레코드 수는 최대 %d개를 초과할 수 없습니다. 입력값: %d", 
                            properties.getMaxRecordCount(), recordCount));
        }
        if (insertToDatabase && dbInfo == null) {
            throw new IllegalArgumentException("DB INSERT를 선택하셨지만 데이터베이스 연결 정보가 제공되지 않았습니다");
        }
        if (schema.getTables().isEmpty()) {
            throw new IllegalArgumentException("스키마에 최소 하나의 테이블이 포함되어야 합니다");
        }
    }
    
    private String msg(String code, String defaultMsg) {
        try {
            return messageSource.getMessage(code, null, Locale.getDefault());
        } catch (Exception e) {
            log.debug("Failed to get message for code: {}, using default", code, e);
            return defaultMsg;
        }
    }

    public Map<String, List<Map<String, Object>>> generateData(DatabaseSchema schema, int recordCount) {
        return relationshipAwareGenerator.generateFakeData(schema, recordCount);
    }
    
    private DataSource createDynamicDataSource(DatabaseConnectionInfo dbInfo, int recordCount) {
        try {
            int poolSize = calculateOptimalPoolSize(recordCount);
            log.debug("Creating data source with pool size: {}", poolSize);
            return DataSourceConfig.createDataSourceForBulkInsert(
                    dbInfo.getJdbcUrl(),
                    dbInfo.getUsername(),
                    dbInfo.getPassword(),
                    poolSize
            );
        } catch (Exception e) {
            log.error("Failed to create data source", e);
            throw new DatabaseConnectionException("데이터소스 생성에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    private int calculateOptimalPoolSize(int recordCount) {
        FakeDataGenProperties.PoolThreshold threshold = properties.getPoolThreshold();
        FakeDataGenProperties.PoolSize poolSize = properties.getPoolSize();
        
        if (recordCount < threshold.getThreshold1()) {
            return poolSize.getSmall();
        } else if (recordCount < threshold.getThreshold2()) {
            return poolSize.getMedium();
        } else if (recordCount < threshold.getThreshold3()) {
            return poolSize.getLarge();
        } else {
            return poolSize.getXlarge();
        }
    }
    
    private List<Long> insertRecordsWithDynamicConnection(JdbcTemplate jdbcTemplate, String tableName, 
                                                        List<Map<String, Object>> records, DatabaseSchema schema) {
        return databaseInsertRepository.insertRecordsWithJdbcTemplate(jdbcTemplate, tableName, records, schema);
    }
    
    public boolean testConnection(DatabaseConnectionInfo dbInfo) {
        try (var connection = DriverManager.getConnection(
                dbInfo.getJdbcUrl(),
                dbInfo.getUsername(),
                dbInfo.getPassword())) {
            boolean isValid = connection.isValid(5);
            if (!isValid) {
                throw new DatabaseConnectionException("연결 유효성 검사 실패");
            }
            log.debug("Database connection test successful: {}", dbInfo);
            return true;
        } catch (SQLException e) {
            log.error("Database connection test failed", e);
            throw new DatabaseConnectionException("데이터베이스 연결에 실패했습니다: " + e.getMessage(), e);
        }
    }
    
    /**
     * Result class containing generated data and insertion results.
     */
    public static class DataGenerationResult {
        private final Map<String, List<Map<String, Object>>> fakeData;
        private final int totalInserted;
        private final String insertMessage;
        private final Map<String, Integer> tableInsertCounts;
        private final java.util.List<String> warnings;
        
        public DataGenerationResult(Map<String, List<Map<String, Object>>> fakeData, int totalInserted, String insertMessage) {
            this(fakeData, totalInserted, insertMessage, java.util.Collections.emptyMap(), java.util.Collections.emptyList());
        }

        public DataGenerationResult(Map<String, List<Map<String, Object>>> fakeData,
                                    int totalInserted,
                                    String insertMessage,
                                    Map<String, Integer> tableInsertCounts,
                                    java.util.List<String> warnings) {
            this.fakeData = fakeData;
            this.totalInserted = totalInserted;
            this.insertMessage = insertMessage;
            this.tableInsertCounts = tableInsertCounts;
            this.warnings = warnings;
        }
        
        public Map<String, List<Map<String, Object>>> getFakeData() {
            return fakeData;
        }
        
        public int getTotalInserted() {
            return totalInserted;
        }
        
        public String getInsertMessage() {
            return insertMessage;
        }

        public Map<String, Integer> getTableInsertCounts() { return tableInsertCounts; }

        public java.util.List<String> getWarnings() { return warnings; }
    }
}
