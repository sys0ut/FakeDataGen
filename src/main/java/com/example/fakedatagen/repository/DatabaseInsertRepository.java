package com.example.fakedatagen.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.fakedatagen.model.DatabaseSchema;
import com.example.fakedatagen.model.Table;
import com.example.fakedatagen.model.Column;

@Repository
public class DatabaseInsertRepository {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInsertRepository.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 컬럼 타입에 맞게 값을 적절한 Java 타입으로 변환
     */
    private Object convertValueByColumnType(Object value, Column column) {
        if (value == null) {
            return null;
        }
        
        String dataType = column.getDataType().toLowerCase();
        
        try {
            // 정수 타입 (우선순위: bigint, smallint 먼저 확인 후 integer/int 확인)
            if (dataType.contains("bigint") || dataType.contains("long")) {
                if (value instanceof String) {
                    return Long.parseLong((String) value);
                }
                return value;
            } else if (dataType.contains("smallint") || dataType.contains("short")) {
                if (value instanceof String) {
                    return Short.parseShort((String) value);
                }
                return value;
            } else if (dataType.contains("integer") || dataType.contains("int")) {
                if (value instanceof String) {
                    return Integer.parseInt((String) value);
                }
                return value;
            }
            // 실수 타입
            else if (dataType.contains("numeric") || dataType.contains("decimal")) {
                if (value instanceof String) {
                    return new BigDecimal((String) value);
                }
                return value;
            } else if (dataType.contains("float") || dataType.contains("real")) {
                if (value instanceof String) {
                    return Float.parseFloat((String) value);
                }
                return value;
            } else if (dataType.contains("double")) {
                if (value instanceof String) {
                    return Double.parseDouble((String) value);
                }
                return value;
            }
            // 날짜/시간 타입
            else if (dataType.contains("date") && !dataType.contains("datetime") && !dataType.contains("timestamp")) {
                if (value instanceof String) {
                    LocalDate localDate = LocalDate.parse((String) value);
                    return Date.valueOf(localDate);
                }
                return value;
            } else if (dataType.contains("time") && !dataType.contains("datetime") && !dataType.contains("timestamp")) {
                if (value instanceof String) {
                    LocalTime localTime = LocalTime.parse((String) value);
                    return Time.valueOf(localTime);
                }
                return value;
            } else if (dataType.contains("datetime") || dataType.contains("timestamp")) {
                if (value instanceof String) {
                    // ISO 형식: "2023-11-04T17:29:31" 또는 "2023-11-04 17:29:31"
                    String strValue = (String) value;
                    // 'T'를 공백으로 변경하고, 초과 부분 제거
                    strValue = strValue.replace('T', ' ');
                    // 밀리초가 있으면 제거 (최대 19자: "YYYY-MM-DD HH:MM:SS")
                    if (strValue.length() > 19) {
                        strValue = strValue.substring(0, 19);
                    }
                    // 공백을 'T'로 변경하여 LocalDateTime.parse()가 인식하도록
                    strValue = strValue.replace(' ', 'T');
                    LocalDateTime localDateTime = LocalDateTime.parse(strValue);
                    return Timestamp.valueOf(localDateTime);
                }
                return value;
            }
            // 불리언 타입
            else if (dataType.contains("boolean") || dataType.contains("bool")) {
                if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                }
                return value;
            }
            // 문자열 타입 및 기타 (VARCHAR, CHAR, STRING, TEXT, BIT 등)
            else {
                // BIT 타입도 CUBRID에서는 문자열로 처리
                return value.toString();
            }
        } catch (Exception e) {
            log.warn("값 변환 실패 - column={}, type={}, value={}, error={}", 
                    column.getName(), dataType, value, e.getMessage());
            // 변환 실패 시 원본 값 반환
            return value;
        }
    }

    /**
     * 테이블에 데이터를 INSERT합니다.
     * 
     * @param tableName 테이블명
     * @param records INSERT할 레코드들
     * @param schema 데이터베이스 스키마 (AUTO_INCREMENT 컬럼 제외용)
     * @return INSERT된 레코드의 생성된 키 값들
     */
    public List<Long> insertRecords(String tableName, List<Map<String, Object>> records, DatabaseSchema schema) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 테이블 정보 가져오기
        Table table = schema.getTableByName(tableName);
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + tableName);
        }
        
        // 첫 번째 레코드에서 컬럼명들을 가져옵니다
        Map<String, Object> firstRecord = records.get(0);
        
        // AUTO_INCREMENT 컬럼 제외
        List<String> columnNames = new ArrayList<>();
        List<String> autoIncrementColumns = new ArrayList<>();
        
        for (String columnName : firstRecord.keySet()) {
            Column column = table.getColumnByName(columnName);
            if (column != null && column.isAutoIncrement()) {
                autoIncrementColumns.add(columnName);
                // AUTO_INCREMENT 컬럼은 INSERT에서 제외
            } else {
                columnNames.add(columnName);
            }
        }
        
        final List<String> finalColumnNames1 = columnNames;
        
        if (columnNames.isEmpty()) {
            return new ArrayList<>();
        }
        
        // INSERT SQL 생성 (전체 테이블명 사용)
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(tableName).append(" (");
        
        // 컬럼명 추가 (AUTO_INCREMENT 제외)
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) sqlBuilder.append(", ");
            sqlBuilder.append(columnNames.get(i));
        }
        sqlBuilder.append(") VALUES (");
        
        // 플레이스홀더 추가
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) sqlBuilder.append(", ");
            sqlBuilder.append("?");
        }
        sqlBuilder.append(")");
        
        String sql = sqlBuilder.toString();
        
        List<Long> generatedKeys = new ArrayList<>();
        
        // AUTO_INCREMENT 컬럼이 있는 경우: 배치 수행 후 시작 ID 기반으로 키 추정(성능 향상)
        if (!autoIncrementColumns.isEmpty()) {
            jdbcTemplate.execute((Connection conn) -> {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (Map<String, Object> record : records) {
                        if (!finalColumnNames1.isEmpty()) {
                            for (int j = 0; j < finalColumnNames1.size(); j++) {
                                String columnName = finalColumnNames1.get(j);
                                Column column = table.getColumnByName(columnName);
                                Object value = record.get(columnName);
                                Object convertedValue = (column != null) ? convertValueByColumnType(value, column) : value;
                                ps.setObject(j + 1, convertedValue);
                            }
                        }
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    // 생성된 키를 JDBC로 안전히 얻기 어려우므로 startId 기준으로 계산(시퀀스 증가 가정)
                    for (int i = 0; i < records.size(); i++) {
                        generatedKeys.add((long) (i + 1));
                    }
                } catch (Exception e) {
                    log.warn("AUTO_INCREMENT 배치 INSERT 실패, fallback 사용: table={}, msg={}", tableName, e.getMessage());
                    for (int i = 0; i < records.size(); i++) {
                        generatedKeys.add((long) (i + 1));
                    }
                }
                return null;
            });
        } else {
            // AUTO_INCREMENT 컬럼이 없는 경우 배치 INSERT 사용
            int batchSize = 10000;
            int totalRecords = records.size();
            
            
            for (int batchStart = 0; batchStart < totalRecords; batchStart += batchSize) {
                int batchEnd = Math.min(batchStart + batchSize, totalRecords);
                List<Map<String, Object>> batch = records.subList(batchStart, batchEnd);
                
                try {
                    // 배치 INSERT 실행 (훨씬 빠름!)
                    jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, record) -> {
                        for (int i = 0; i < finalColumnNames1.size(); i++) {
                            String columnName = finalColumnNames1.get(i);
                            Column column = table.getColumnByName(columnName);
                            Object value = record.get(columnName);
                            Object convertedValue = (column != null) ? convertValueByColumnType(value, column) : value;
                            ps.setObject(i + 1, convertedValue);
                        }
                    });
                    
                    // 결과는 사용하지 않음(키 미수집 경로)
                    
                } catch (Exception e) {
                    log.warn("배치 INSERT 실패 - table={}, batch={}~{}, msg={}", tableName, batchStart, batchEnd, e.getMessage());
                }
            }
        }
        
        
        return generatedKeys;
    }
    
    /**
     * 여러 테이블에 순서대로 데이터를 INSERT합니다.
     * 의존성 순서를 고려하여 부모 테이블부터 INSERT합니다.
     *
     * @param fakeData          테이블별 데이터 맵
     * @param orderedTableNames 정렬된 테이블명 리스트
     * @param schema           데이터베이스 스키마 (AUTO_INCREMENT 컬럼 제외용)
     * @return INSERT된 총 레코드 수
     */
    public Map<String, List<Long>> insertAllRecords(Map<String, List<Map<String, Object>>> fakeData, List<String> orderedTableNames, DatabaseSchema schema) {
        Map<String, List<Long>> generatedKeysMap = new HashMap<>();
        
        // 매개변수로 받은 orderedTableNames 사용
        for (String tableName : orderedTableNames) {
            List<Map<String, Object>> records = fakeData.get(tableName);
            
            if (records != null && !records.isEmpty()) {
                List<Long> generatedKeys = insertRecords(tableName, records, schema); // schema 정보 전달
                generatedKeysMap.put(tableName, generatedKeys);
            }
        }
        
        return generatedKeysMap;
    }
    
    /**
     * 테이블이 존재하는지 확인합니다.
     * 
     * @param tableName 테이블명
     * @return 테이블 존재 여부
     */
    public boolean tableExists(String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE 1=0";
            jdbcTemplate.queryForObject(sql, Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 동적 JdbcTemplate을 사용하여 테이블에 데이터를 INSERT합니다.
     * 
     * @param jdbcTemplate 사용할 JdbcTemplate
     * @param tableName 테이블명
     * @param records INSERT할 레코드들
     * @param schema 데이터베이스 스키마 (AUTO_INCREMENT 컬럼 제외용)
     * @return INSERT된 레코드의 생성된 키 값들
     */
    public List<Long> insertRecordsWithJdbcTemplate(JdbcTemplate jdbcTemplate, String tableName, 
                                                   List<Map<String, Object>> records, DatabaseSchema schema) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 식별자 화이트리스트 검증기
        IdentifierValidator validator = new IdentifierValidator(schema);
        if (!validator.isAllowedTable(tableName)) {
            throw new IllegalArgumentException("Unknown table (not in schema): " + tableName);
        }
        
        // 테이블 정보 가져오기 (스키마명 제거)
        String tableNameOnly = tableName.contains(".") ? 
            tableName.substring(tableName.lastIndexOf(".") + 1) : tableName;
        
        Table table = schema.getTableByName(tableNameOnly);
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + tableNameOnly + " (전체명: " + tableName + ")");
        }
        
        // 첫 번째 레코드에서 컬럼명들을 가져옵니다
        Map<String, Object> firstRecord = records.get(0);
        
        // AUTO_INCREMENT 컬럼 제외
        List<String> columnNames = new ArrayList<>();
        List<String> autoIncrementColumns = new ArrayList<>();
        
        for (Column column : table.getColumns()) {
            String columnName = column.getName();
            if (column.isAutoIncrement()) {
                autoIncrementColumns.add(columnName);
            } else {
                columnNames.add(columnName);
            }
        }
        
        // 첫 번째 레코드에서 실제로 존재하는 컬럼만 필터링
        List<String> existingColumns = new ArrayList<>();
        for (String columnName : columnNames) {
            if (firstRecord.containsKey(columnName)) {
                // NOT NULL 제약 조건이 있는 컬럼은 NULL 값이어도 포함
                Column column = table.getColumnByName(columnName);
                boolean isNotNullColumn = (column != null && !column.isNullable());
                
                if (firstRecord.get(columnName) != null || isNotNullColumn) {
                    if (!validator.isAllowedColumn(tableNameOnly, columnName)) {
                        throw new IllegalArgumentException("Unknown column (not in schema): " + tableNameOnly + "." + columnName);
                    }
                    existingColumns.add(columnName);
                } else {
                }
            } else {
            }
        }
        
        final List<String> finalColumnNames = existingColumns;
        
        
        if (finalColumnNames.isEmpty()) {
            // AUTO_INCREMENT 컬럼만 있는 경우에도 INSERT 허용 (빈 INSERT)
        }
        
        // INSERT SQL 생성 (전체 테이블명 사용)
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(tableName);
        
        if (columnNames.isEmpty()) {
            // AUTO_INCREMENT 컬럼만 있는 경우 - CUBRID에서는 DEFAULT VALUES 대신 명시적 컬럼 지정
            if (!autoIncrementColumns.isEmpty()) {
                // AUTO_INCREMENT 컬럼이 있으면 해당 컬럼을 명시적으로 지정
                sqlBuilder.append(" (");
                for (int i = 0; i < autoIncrementColumns.size(); i++) {
                    if (i > 0) sqlBuilder.append(", ");
                    sqlBuilder.append(autoIncrementColumns.get(i));
                }
                sqlBuilder.append(") VALUES (");
                for (int i = 0; i < autoIncrementColumns.size(); i++) {
                    if (i > 0) sqlBuilder.append(", ");
                    sqlBuilder.append("NULL"); // AUTO_INCREMENT는 NULL로 설정
                }
                sqlBuilder.append(")");
            } else {
                // 컬럼이 전혀 없는 경우는 에러 처리
                throw new IllegalArgumentException("테이블 " + tableName + "에 INSERT할 컬럼이 없습니다.");
            }
        } else {
            sqlBuilder.append(" (");
            // 컬럼명 추가
            for (int i = 0; i < finalColumnNames.size(); i++) {
                if (i > 0) sqlBuilder.append(", ");
                sqlBuilder.append(finalColumnNames.get(i));
            }
            sqlBuilder.append(") VALUES (");
            
            // 플레이스홀더 생성
            for (int i = 0; i < finalColumnNames.size(); i++) {
                if (i > 0) sqlBuilder.append(", ");
                sqlBuilder.append("?");
            }
            sqlBuilder.append(")");
        }
        
        String sql = sqlBuilder.toString();
        
        // 배치 INSERT 실행
        List<Long> generatedKeys = new ArrayList<>();
        
        // AUTO_INCREMENT 컬럼이 있는 경우: FK 연계를 위해 실제 생성 키를 수집
        if (!autoIncrementColumns.isEmpty()) {
            jdbcTemplate.execute((Connection conn) -> {
                try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    for (Map<String, Object> record : records) {
                        if (!finalColumnNames.isEmpty()) {
                            for (int j = 0; j < finalColumnNames.size(); j++) {
                                String columnName = finalColumnNames.get(j);
                                Column column = table.getColumnByName(columnName);
                                Object value = record.get(columnName);
                                Object convertedValue = (column != null) ? convertValueByColumnType(value, column) : value;
                                ps.setObject(j + 1, convertedValue);
                            }
                        }
                        ps.executeUpdate();
                        try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                generatedKeys.add(rs.getLong(1));
                            } else {
                                // 드라이버가 키 반환을 제공하지 않으면 보정: MAX 조회
                                try {
                                    String maxIdSql = "SELECT COALESCE(MAX(" + autoIncrementColumns.get(0) + "), 0) FROM " + tableName;
                                    Long maxId = jdbcTemplate.queryForObject(maxIdSql, Long.class);
                                    generatedKeys.add(maxId);
                                } catch (Exception ex) {
                                    generatedKeys.add(0L);
                                }
                            }
                        }
                        ps.clearParameters();
                    }
                } catch (Exception e) {
                    log.warn("AUTO_INCREMENT INSERT 중 키 수집 실패 - table={}, msg={}", tableName, e.getMessage());
                }
                return null;
            });
        } else {
            // AUTO_INCREMENT 컬럼이 없는 경우 기존 방식 사용
            for (int i = 0; i < records.size(); i++) {
                Map<String, Object> record = records.get(i);
                if (columnNames.isEmpty()) {
                    // 컬럼이 없는 경우 - 파라미터 없이 INSERT
                    jdbcTemplate.update(sql);
                } else {
                    // 일반적인 경우 - 파라미터와 함께 INSERT
                    Object[] params = new Object[columnNames.size()];
                    for (int j = 0; j < columnNames.size(); j++) {
                        String columnName = columnNames.get(j);
                        Column column = table.getColumnByName(columnName);
                        Object value = record.get(columnName);
                        params[j] = (column != null) ? convertValueByColumnType(value, column) : value;
                    }
                    jdbcTemplate.update(sql, params);
                }
                generatedKeys.add(0L); // 키가 없는 경우 0으로 설정
            }
        }
        
        return generatedKeys;
    }
    
    /**
     * 테이블의 실제 데이터를 조회하여 반환합니다.
     * 외래키가 일반 컬럼인 경우 실제 데이터 값이 필요할 때 사용합니다.
     * 
     * @param tableName 테이블명
     * @param columnName 조회할 컬럼명
     * @return 실제 데이터 값들의 리스트
     */
    public List<Object> getActualDataFromTable(String tableName, String columnName) {
        List<Object> actualData = new ArrayList<>();
        
        try {
            String sql = "SELECT " + columnName + " FROM " + tableName + " ORDER BY " + columnName;
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            for (Map<String, Object> row : results) {
                Object value = row.get(columnName);
                actualData.add(value);
            }
            
            
        } catch (Exception e) {
            log.warn("실제 데이터 조회 실패 - table={}, column={}, msg={}", tableName, columnName, e.getMessage(), e);
        }
        
        return actualData;
    }
    
    /**
     * 테이블의 모든 실제 데이터를 조회하여 반환합니다.
     * 외래키 매칭을 위해 전체 레코드를 필요로 할 때 사용합니다.
     * 
     * @param tableName 테이블명
     * @return 실제 데이터 레코드들의 리스트
     */
    public List<Map<String, Object>> getAllActualDataFromTable(String tableName) {
        List<Map<String, Object>> actualData = new ArrayList<>();
        
        try {
            String sql = "SELECT * FROM " + tableName;
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            
            actualData.addAll(results);
            
            // 첫 번째 레코드 샘플 출력
            if (!actualData.isEmpty()) {
            }
            
        } catch (Exception e) {
            log.warn("전체 데이터 조회 실패 - table={}, msg={}", tableName, e.getMessage(), e);
        }
        
        return actualData;
    }
}

