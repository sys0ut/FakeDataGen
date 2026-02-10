package com.example.fakedatagen.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Map;
import com.example.fakedatagen.model.DatabaseSchema;
import com.example.fakedatagen.model.Table;
import com.example.fakedatagen.model.Column;
import com.example.fakedatagen.config.FakeDataGenProperties;

@Repository
public class DatabaseInsertRepository {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInsertRepository.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final FakeDataGenProperties properties;
    
    public DatabaseInsertRepository(JdbcTemplate jdbcTemplate, FakeDataGenProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }
    
    private int extractMaxLengthFromDataType(String dataType) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\((\\d+)\\)");
            java.util.regex.Matcher matcher = pattern.matcher(dataType);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
        }
        return 0;
    }
    private Object convertValueByColumnType(Object value, Column column) {
        if (value == null) {
            return null;
        }
        
        String dataType = column.getDataType().toLowerCase();
        String dataTypeNoSpaces = dataType.replaceAll("\\s+", "");
        
        try {
            if (dataType.contains("bigint") || dataType.contains("long")) {
                if (value instanceof Long) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).longValue();
                } else if (value instanceof String) {
                    return Long.parseLong((String) value);
                }
                return value;
            } else if (dataType.contains("smallint") || dataType.contains("short")) {
                if (value instanceof Short) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).shortValue();
                } else if (value instanceof String) {
                    return Short.parseShort((String) value);
                }
                return value;
            } else if (dataType.contains("integer") || dataType.contains("int")) {
                if (value instanceof Integer) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).intValue();
                } else if (value instanceof String) {
                    return Integer.parseInt((String) value);
                }
                return value;
            } else if (dataType.contains("numeric") || dataType.contains("decimal")) {
                if (value instanceof BigDecimal) {
                    return value;
                } else if (value instanceof Number) {
                    return new BigDecimal(((Number) value).toString());
                } else if (value instanceof String) {
                    return new BigDecimal((String) value);
                }
                return value;
            } else if (dataType.contains("float") || dataType.contains("real")) {
                if (value instanceof Float) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).floatValue();
                } else if (value instanceof String) {
                    return Float.parseFloat((String) value);
                }
                return value;
            } else if (dataType.contains("double")) {
                if (value instanceof Double) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                } else if (value instanceof String) {
                    return Double.parseDouble((String) value);
                }
                return value;
            } else if (dataType.contains("date") && !dataType.contains("datetime") && !dataType.contains("timestamp")) {
                if (value instanceof Date) {
                    return value;
                } else if (value instanceof LocalDate) {
                    return Date.valueOf((LocalDate) value);
                } else if (value instanceof String) {
                    String strValue = ((String) value).trim();
                    try {
                        LocalDate localDate = LocalDate.parse(strValue);
                        return Date.valueOf(localDate);
                    } catch (Exception e) {
                        strValue = strValue.replace('/', '-');
                        if (strValue.length() >= 10) {
                            strValue = strValue.substring(0, 10);
                            LocalDate localDate = LocalDate.parse(strValue);
                            return Date.valueOf(localDate);
                        }
                        throw new IllegalArgumentException("날짜 형식 변환 실패: " + strValue);
                    }
                }
                return value;
            } else if (dataType.contains("time") && !dataType.contains("datetime") && !dataType.contains("timestamp")) {
                if (value instanceof Time) {
                    return value;
                } else if (value instanceof LocalTime) {
                    return Time.valueOf((LocalTime) value);
                } else if (value instanceof String) {
                    String strValue = ((String) value).trim();
                    try {
                        LocalTime localTime = LocalTime.parse(strValue);
                        return Time.valueOf(localTime);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("시간 형식 변환 실패: " + strValue);
                    }
                }
                return value;
            } else if (dataTypeNoSpaces.contains("timestamptz") || dataTypeNoSpaces.contains("timestampltz")
                    || dataTypeNoSpaces.contains("datetimetz") || dataTypeNoSpaces.contains("datetimeltz")) {
                // TZ/LTZ 계열은 Timestamp로 변환하지 말고 "타임존 포함 문자열" 그대로 바인딩한다.
                // (Generator에서 TZ 문자열을 만들어주며, 이미 Timestamp/LocalDateTime 객체면 toString()으로 내려보낸다)
                return value.toString();
            } else if (dataType.contains("datetime") || dataType.contains("timestamp")) {
                if (value instanceof Timestamp) {
                    return value;
                } else if (value instanceof LocalDateTime) {
                    return Timestamp.valueOf((LocalDateTime) value);
                } else if (value instanceof String) {
                    String strValue = ((String) value).trim();
                    try {
                        // ISO 형식: "2023-11-04T17:29:31" 또는 "2023-11-04 17:29:31"
                        strValue = strValue.replace('T', ' ');
                        // 밀리초가 있으면 제거 (최대 19자: "YYYY-MM-DD HH:MM:SS")
                        if (strValue.length() > 19) {
                            strValue = strValue.substring(0, 19);
                        }
                        // 공백을 'T'로 변경하여 LocalDateTime.parse()가 인식하도록
                        strValue = strValue.replace(' ', 'T');
                        LocalDateTime localDateTime = LocalDateTime.parse(strValue);
                        return Timestamp.valueOf(localDateTime);
                    } catch (Exception e) {
                        // 다른 형식 시도
                        strValue = strValue.replace('/', '-');
                        if (strValue.length() >= 19) {
                            strValue = strValue.substring(0, 19).replace(' ', 'T');
                            LocalDateTime localDateTime = LocalDateTime.parse(strValue);
                            return Timestamp.valueOf(localDateTime);
                        }
                        throw new IllegalArgumentException("날짜시간 형식 변환 실패: " + strValue);
                    }
                }
                return value;
            } else if (dataType.contains("bit")) {
                if (value instanceof String) {
                    String bitStr = (String) value;
                    if (bitStr.matches("[01]+")) {
                        if (!dataType.contains("varying") && bitStr.length() == 1) {
                            return Integer.parseInt(bitStr);
                        }
                        return bitStr;
                    }
                } else if (value instanceof Number) {
                    return value;
                } else if (value instanceof Boolean) {
                    return ((Boolean) value) ? 1 : 0;
                }
                return value.toString();
            } else if (dataType.contains("boolean") || dataType.contains("bool")) {
                if (value instanceof String) {
                    String str = ((String) value).toLowerCase().trim();
                    if ("true".equals(str) || "1".equals(str) || "yes".equals(str)) {
                        return true;
                    } else if ("false".equals(str) || "0".equals(str) || "no".equals(str)) {
                        return false;
                    }
                    return Boolean.parseBoolean(str);
                } else if (value instanceof Boolean) {
                    return value;
                } else if (value instanceof Number) {
                    return ((Number) value).intValue() != 0;
                }
                return value;
            } else {
                String strValue = value.toString();
                int maxLength = column.getMaxLength();
                if (maxLength <= 0) {
                    maxLength = extractMaxLengthFromDataType(dataType);
                }
                if (maxLength > 0 && strValue.length() > maxLength) {
                    return strValue.substring(0, maxLength);
                }
                return strValue;
            }
        } catch (Exception e) {
            log.error("타입 변환 실패 - 컬럼={}, 타입={}, 값={}", column.getName(), dataType, value, e);
            return value;
        }
    }

    public boolean tableExists(String tableName) {
        try {
            String sanitizedTableName = sanitizeTableName(tableName, true);
            String sql = "SELECT COUNT(*) FROM " + sanitizedTableName + " WHERE 1=0";
            jdbcTemplate.queryForObject(sql, Integer.class);
            return true;
        } catch (Exception e) {
            // schema-qualified가 실패하면 unqualified로 한 번 더 확인
            if (tableName != null && tableName.contains(".")) {
                try {
                    String fallbackTableName = sanitizeTableName(tableName, false);
                    String fallbackSql = "SELECT COUNT(*) FROM " + fallbackTableName + " WHERE 1=0";
                    jdbcTemplate.queryForObject(fallbackSql, Integer.class);
                    return true;
                } catch (Exception ignored) {
                    // ignore
                }
            }
            return false;
        }
    }
    
    private String sanitizeTableName(String tableName, boolean useSchemaQualifiedTableName) {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is blank");
        }
        // UI의 "11.2 이상" 토글이 켜진 경우 schema.table 형태를 사용한다.
        if (useSchemaQualifiedTableName && tableName.contains(".")) {
            String schemaName = tableName.substring(0, tableName.lastIndexOf(".")).trim();
            String tableNameOnly = tableName.substring(tableName.lastIndexOf(".") + 1).trim();
            return schemaName + "." + tableNameOnly;
        }
        return tableName.contains(".") ? tableName.substring(tableName.lastIndexOf(".") + 1).trim() : tableName.trim();
    }
    public List<Long> insertRecordsWithJdbcTemplate(JdbcTemplate jdbcTemplate, String tableName, 
                                                   List<Map<String, Object>> records, DatabaseSchema schema, boolean useSchemaQualifiedTableName) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        
        IdentifierValidator validator = new IdentifierValidator(schema);
        if (!validator.isAllowedTable(tableName)) {
            throw new IllegalArgumentException("Unknown table (not in schema): " + tableName);
        }
        
        String tableNameOnly = tableName.contains(".") ? 
            tableName.substring(tableName.lastIndexOf(".") + 1) : tableName;
        
        Table table = schema.getTableByName(tableNameOnly);
        if (table == null) {
            throw new IllegalArgumentException("Table not found: " + tableNameOnly + " (전체명: " + tableName + ")");
        }
        
        Map<String, Object> firstRecord = records.get(0);
        
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
        
        List<String> existingColumns = new ArrayList<>();
        for (String columnName : columnNames) {
            if (firstRecord.containsKey(columnName)) {
                Column column = table.getColumnByName(columnName);
                boolean isNotNullColumn = (column != null && !column.isNullable());
                
                if (firstRecord.get(columnName) != null || isNotNullColumn) {
                    if (!validator.isAllowedColumn(tableNameOnly, columnName)) {
                        throw new IllegalArgumentException("Unknown column (not in schema): " + tableNameOnly + "." + columnName);
                    }
                    existingColumns.add(columnName);
                }
            }
        }
        
        final List<String> finalColumnNames = existingColumns;
        String sanitizedTableName = sanitizeTableName(tableName, useSchemaQualifiedTableName);
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO ").append(sanitizedTableName);
        
        if (columnNames.isEmpty()) {
            if (!autoIncrementColumns.isEmpty()) {
                sqlBuilder.append(" (");
                for (int i = 0; i < autoIncrementColumns.size(); i++) {
                    if (i > 0) sqlBuilder.append(", ");
                    sqlBuilder.append(autoIncrementColumns.get(i));
                }
                sqlBuilder.append(") VALUES (");
                for (int i = 0; i < autoIncrementColumns.size(); i++) {
                    if (i > 0) sqlBuilder.append(", ");
                    sqlBuilder.append("NULL");
                }
                sqlBuilder.append(")");
            } else {
                throw new IllegalArgumentException("테이블 " + tableName + "에 INSERT할 컬럼이 없습니다.");
            }
        } else {
            sqlBuilder.append(" (");
            for (int i = 0; i < finalColumnNames.size(); i++) {
                if (i > 0) sqlBuilder.append(", ");
                sqlBuilder.append(finalColumnNames.get(i));
            }
            sqlBuilder.append(") VALUES (");
            for (int i = 0; i < finalColumnNames.size(); i++) {
                if (i > 0) sqlBuilder.append(", ");
                sqlBuilder.append("?");
            }
            sqlBuilder.append(")");
        }
        
        String sql = sqlBuilder.toString();
        List<Long> generatedKeys = new ArrayList<>();
        if (!autoIncrementColumns.isEmpty()) {
            int batchSize = properties.getBatchSize();
            int totalRecords = records.size();
            try {
                doAutoIncrementInsert(jdbcTemplate, sql, sanitizedTableName, records, finalColumnNames, autoIncrementColumns,
                        batchSize, totalRecords, tableName, table, generatedKeys);
            } catch (Exception e) {
                if (useSchemaQualifiedTableName && tableName != null && tableName.contains(".")) {
                    String fallbackSanitizedTableName = sanitizeTableName(tableName, false);
                    String fallbackSql = sql.replace("INSERT INTO " + sanitizedTableName, "INSERT INTO " + fallbackSanitizedTableName);
                    log.warn("AUTO_INCREMENT INSERT 실패로 unqualified로 재시도 - table={}, sql={}, fallbackSql={}, reason={}",
                            tableName, sql, fallbackSql, e.getMessage());
                    doAutoIncrementInsert(jdbcTemplate, fallbackSql, fallbackSanitizedTableName, records, finalColumnNames, autoIncrementColumns,
                            batchSize, totalRecords, tableName, table, generatedKeys);
                } else {
                    throw e;
                }
            }
        } else {
            int batchSize = properties.getBatchSize();
            int totalRecords = records.size();
            
            for (int batchStart = 0; batchStart < totalRecords; batchStart += batchSize) {
                int batchEnd = Math.min(batchStart + batchSize, totalRecords);
                List<Map<String, Object>> batch = records.subList(batchStart, batchEnd);
                
                try {
                    jdbcTemplate.batchUpdate(sql, batch, batch.size(), (ps, record) -> {
                        for (int i = 0; i < finalColumnNames.size(); i++) {
                            String columnName = finalColumnNames.get(i);
                            Column column = table.getColumnByName(columnName);
                            Object value = record.get(columnName);
                            Object convertedValue = (column != null) ? convertValueByColumnType(value, column) : value;
                            ps.setObject(i + 1, convertedValue);
                        }
                    });
                    for (int i = 0; i < batch.size(); i++) {
                        generatedKeys.add(0L);
                    }
                } catch (Exception e) {
                    if (useSchemaQualifiedTableName && tableName != null && tableName.contains(".")) {
                        String fallbackSanitizedTableName = sanitizeTableName(tableName, false);
                        String fallbackSql = sql.replace("INSERT INTO " + sanitizedTableName, "INSERT INTO " + fallbackSanitizedTableName);
                        try {
                            log.warn("배치 INSERT 실패로 unqualified로 재시도 - table={}, batch={}~{}, sql={}, fallbackSql={}, reason={}",
                                    tableName, batchStart, batchEnd, sql, fallbackSql, e.getMessage());
                            jdbcTemplate.batchUpdate(fallbackSql, batch, batch.size(), (ps, record) -> {
                                for (int i = 0; i < finalColumnNames.size(); i++) {
                                    String columnName = finalColumnNames.get(i);
                                    Column column = table.getColumnByName(columnName);
                                    Object value = record.get(columnName);
                                    Object convertedValue = (column != null) ? convertValueByColumnType(value, column) : value;
                                    ps.setObject(i + 1, convertedValue);
                                }
                            });
                            for (int i = 0; i < batch.size(); i++) {
                                generatedKeys.add(0L);
                            }
                        } catch (Exception fallbackEx) {
                            log.error("배치 INSERT 실패 - table={}, batch={}~{}", tableName, batchStart, batchEnd, fallbackEx);
                            throw new RuntimeException("배치 INSERT 실패: " + tableName + " (배치 " + batchStart + "~" + batchEnd + ") - " + fallbackEx.getMessage(), fallbackEx);
                        }
                    } else {
                        log.error("배치 INSERT 실패 - table={}, batch={}~{}", tableName, batchStart, batchEnd, e);
                        throw new RuntimeException("배치 INSERT 실패: " + tableName + " (배치 " + batchStart + "~" + batchEnd + ") - " + e.getMessage(), e);
                    }
                }
            }
        }
        
        return generatedKeys;
    }

    private void doAutoIncrementInsert(JdbcTemplate jdbcTemplate, String sql, String sanitizedTableName,
                                      List<Map<String, Object>> records, List<String> finalColumnNames,
                                      List<String> autoIncrementColumns, int batchSize, int totalRecords,
                                      String tableName, Table table, List<Long> generatedKeys) {
        jdbcTemplate.execute((Connection conn) -> {
            try (PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                for (int recordIdx = 0; recordIdx < totalRecords; recordIdx++) {
                    Map<String, Object> record = records.get(recordIdx);

                    if (!finalColumnNames.isEmpty()) {
                        for (int j = 0; j < finalColumnNames.size(); j++) {
                            String columnName = finalColumnNames.get(j);
                            Column column = table.getColumnByName(columnName);
                            Object value = record.get(columnName);
                            Object convertedValue = (column != null) ? convertValueByColumnType(value, column) : value;
                            ps.setObject(j + 1, convertedValue);
                        }
                    }
                    ps.addBatch();

                    if ((recordIdx + 1) % batchSize == 0 || recordIdx == totalRecords - 1) {
                        int[] updateCounts = ps.executeBatch();
                        try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                            int keyCount = 0;
                            while (rs.next() && keyCount < updateCounts.length) {
                                generatedKeys.add(rs.getLong(1));
                                keyCount++;
                            }
                            if (keyCount < updateCounts.length) {
                                try {
                                    String maxIdSql = "SELECT COALESCE(MAX(" + autoIncrementColumns.get(0) + "), 0) FROM " + sanitizedTableName;
                                    Long currentMax = jdbcTemplate.queryForObject(maxIdSql, Long.class);
                                    for (int i = keyCount; i < updateCounts.length; i++) {
                                        generatedKeys.add(currentMax - (updateCounts.length - i - 1));
                                    }
                                } catch (Exception ex) {
                                    for (int i = keyCount; i < updateCounts.length; i++) {
                                        generatedKeys.add(0L);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("AUTO_INCREMENT INSERT 실패 - table={}", tableName, e);
                throw e;
            }
            return null;
        });
    }
    
}

