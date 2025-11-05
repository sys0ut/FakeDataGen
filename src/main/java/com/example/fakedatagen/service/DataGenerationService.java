package com.example.fakedatagen.service;

import com.example.fakedatagen.model.DatabaseSchema;
import com.example.fakedatagen.model.DatabaseConnectionInfo;
import com.example.fakedatagen.generator.RelationshipAwareGenerator;
import com.example.fakedatagen.repository.DatabaseInsertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.context.MessageSource;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.zaxxer.hikari.HikariDataSource;

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

    public DataGenerationService(RelationshipAwareGenerator relationshipAwareGenerator,
                                 DatabaseInsertRepository databaseInsertRepository,
                                 MessageSource messageSource) {
        this.relationshipAwareGenerator = relationshipAwareGenerator;
        this.databaseInsertRepository = databaseInsertRepository;
        this.messageSource = messageSource;
    }
    
    /**
     * 가짜 데이터를 생성하고 데이터베이스에 INSERT합니다.
     * 
     * @param schema 데이터베이스 스키마
     * @param recordCount 생성할 레코드 수
     * @param insertToDatabase 데이터베이스에 INSERT할지 여부
     * @param dbInfo 데이터베이스 연결 정보
     * @return 생성된 데이터와 INSERT 결과
     */
    public DataGenerationResult generateAndInsertData(DatabaseSchema schema, int recordCount, boolean insertToDatabase, DatabaseConnectionInfo dbInfo) {
        if (schema == null) {
            throw new IllegalArgumentException("schema must not be null");
        }
        if (recordCount <= 0) {
            throw new IllegalArgumentException("recordCount must be positive");
        }

        Map<String, List<Map<String, Object>>> allFakeData;
        int totalInserted = 0;
        String insertMessage = "";

        if (!insertToDatabase || dbInfo == null) {
            allFakeData = generateData(schema, recordCount);
            insertMessage = (insertToDatabase && dbInfo == null)
                    ? msg("insert.skipped.no_dbinfo", ServiceMessages.INSERT_SKIPPED_NO_DBINFO)
                    : msg("generate.only", ServiceMessages.GENERATE_ONLY);
            return new DataGenerationResult(allFakeData, totalInserted, insertMessage);
        }

        // DB 삽입 경로
        allFakeData = new HashMap<>();
        try {
            DataSource dynamicDataSource = createDynamicDataSource(dbInfo);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dynamicDataSource);

            PlatformTransactionManager txManager = new DataSourceTransactionManager(dynamicDataSource);
            TransactionTemplate txTemplate = new TransactionTemplate(txManager);

            List<String> orderedTableNames = relationshipAwareGenerator.getOrderedTableNames(schema);

            final Map<String, List<Long>> generatedKeysMap = new HashMap<>();
            final Map<String, List<Map<String, Object>>> generatedDataMap = new HashMap<>();
            final Map<String, Integer> tableInsertCounts = new HashMap<>();
            final java.util.List<String> warnings = new java.util.ArrayList<>();

            totalInserted = txTemplate.execute(status -> {
                // 삭제 전략 유지
                for (int i = orderedTableNames.size() - 1; i >= 0; i--) {
                    String tableName = orderedTableNames.get(i);
                    try {
                        String deleteSql = "DELETE FROM " + tableName;
                        jdbcTemplate.update(deleteSql);
                    } catch (Exception ignored) { }
                }

                int inserted = 0;
                for (String tableName : orderedTableNames) {
                    try {
                        List<Map<String, Object>> tableData = relationshipAwareGenerator.generateTableDataWithGeneratedData(
                                schema, tableName, recordCount, generatedKeysMap, generatedDataMap);
                        if (tableData != null && !tableData.isEmpty()) {
                            List<Long> generatedKeys = insertRecordsWithDynamicConnection(jdbcTemplate, tableName, tableData, schema);
                            generatedKeysMap.put(tableName, generatedKeys);
                            generatedDataMap.put(tableName, tableData);
                            allFakeData.put(tableName, tableData);
                            int count = generatedKeys.size();
                            tableInsertCounts.put(tableName, count);
                            inserted += count;
                        }
                    } catch (Exception e) {
                        log.warn("테이블 삽입 중 오류 table={}, message={}", tableName, e.getMessage());
                        warnings.add("insert failed: " + tableName + " - " + e.getMessage());
                    }
                }
                return inserted;
            });

            // 결과 확장 필드 포함해 반환 메시지 업데이트(경고 존재 시 접미사 부여)
            if (!warnings.isEmpty()) {
                insertMessage = (insertMessage == null ? "" : insertMessage) + " (" + warnings.size() + " warnings)";
            }
        } catch (Exception e) {
            insertMessage = msg("db.insert.error.prefix", ServiceMessages.DB_INSERT_ERROR_PREFIX) + e.getMessage();
            log.error("데이터 삽입 트랜잭션 실패: {}", e.getMessage(), e);
        }

        return new DataGenerationResult(allFakeData, totalInserted, insertMessage);
    }

    private String msg(String code, String defaultMsg) {
        try {
            return messageSource.getMessage(code, null, Locale.getDefault());
        } catch (Exception ignore) {
            return defaultMsg;
        }
    }

    // 생성 책임 분리
    public Map<String, List<Map<String, Object>>> generateData(DatabaseSchema schema, int recordCount) {
        return relationshipAwareGenerator.generateFakeData(schema, recordCount);
    }

    // 삽입 책임 분리 (필요 시 외부에서 직접 호출하도록 확장 가능)
    public int insertData(JdbcTemplate jdbcTemplate, DatabaseSchema schema, Map<String, List<Map<String, Object>>> data) {
        int total = 0;
        for (Map.Entry<String, List<Map<String, Object>>> entry : data.entrySet()) {
            total += insertRecordsWithDynamicConnection(jdbcTemplate, entry.getKey(), entry.getValue(), schema).size();
        }
        return total;
    }
    
    /**
     * 동적 데이터소스 생성
     */
    private DataSource createDynamicDataSource(DatabaseConnectionInfo dbInfo) {
        HikariDataSource dataSource = new HikariDataSource();
        // JDBC 4 드라이버 자동 로딩 사용
        dataSource.setJdbcUrl(dbInfo.getJdbcUrl());
        dataSource.setUsername(dbInfo.getUsername());
        dataSource.setPassword(dbInfo.getPassword());
        dataSource.setMaximumPoolSize(5);
        return dataSource;
    }
    
    /**
     * 동적 연결을 사용한 레코드 INSERT
     */
    private List<Long> insertRecordsWithDynamicConnection(JdbcTemplate jdbcTemplate, String tableName, 
                                                        List<Map<String, Object>> records, DatabaseSchema schema) {
        return databaseInsertRepository.insertRecordsWithJdbcTemplate(jdbcTemplate, tableName, records, schema);
    }
    
    /**
     * 데이터베이스 연결 테스트
     */
    public boolean testConnection(DatabaseConnectionInfo dbInfo) {
        try (var connection = DriverManager.getConnection(
                dbInfo.getJdbcUrl(),
                dbInfo.getUsername(),
                dbInfo.getPassword())) {
            return connection.isValid(5);
        } catch (SQLException e) {
            log.debug("DB 연결 테스트 실패: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 데이터 생성 및 INSERT 결과를 담는 클래스
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
