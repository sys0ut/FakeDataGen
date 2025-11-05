package com.example.fakedatagen.generator;

import com.example.fakedatagen.model.*;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BasicValueGenerator {
    private static final Logger log = LoggerFactory.getLogger(BasicValueGenerator.class);
    private Faker faker = new Faker();
    
    // 테이블별 컬럼-Faker 타입 매핑 저장 (테이블명 -> (컬럼명 -> Faker타입))
    private final Map<String, Map<String, String>> tableColumnFakerMap = new ConcurrentHashMap<>();

    public Object generate(Column column, int index, Table table) {
        return generateBasicValue(column, index, table);
    }

    private int extractMaxLength(String dataType) {
        try {
            String pattern = "\\((\\d+)\\)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(dataType);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {
            // 파싱 실패시 기본값 반환
        }
        return 0;
    }

    /**
     * NUMERIC/DECIMAL 타입에서 정밀도와 스케일을 추출
     * 예: numeric(10,2) -> [10, 2]
     */
    private int[] extractNumericPrecisionScale(String dataType) {
        try {
            String pattern = "\\((\\d+),\\s*(\\d+)\\)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(dataType);
            if (m.find()) {
                int precision = Integer.parseInt(m.group(1));
                int scale = Integer.parseInt(m.group(2));
                return new int[]{precision, scale};
            }

            // 정밀도만 있는 경우 (예: numeric(10))
            pattern = "\\((\\d+)\\)";
            p = java.util.regex.Pattern.compile(pattern);
            m = p.matcher(dataType);
            if (m.find()) {
                int precision = Integer.parseInt(m.group(1));
                return new int[]{precision, 0};
            }
        } catch (Exception e) {
            // 파싱 실패시 기본값 반환
        }
        return new int[]{10, 0}; // 기본값: 정밀도 10, 스케일 0
    }

    /**
     * PRIMARY KEY 또는 UNIQUE 제약이 있는 컬럼에 대해 고유값을 생성
     */
    private Object generateUniqueValue(Column column, int index, String dataType, String columnName) {
        // 컬럼명의 해시코드를 사용하여 컬럼별로 다른 시드 생성 (복합 키 대응)
        int columnSeed = Math.abs(columnName.hashCode() % 1000);

        // 데이터 타입에 따라 고유값 생성
        if (dataType.contains("varchar") || dataType.contains("character varying")) {
            int maxLength = extractMaxLength(dataType);
            // 컬럼명 해시 + index로 고유 문자열 생성
            String uniqueStr = columnSeed + "_" + index;
            if (maxLength > 0 && uniqueStr.length() > maxLength) {
                // 길이 초과 시 해시값으로 축약
                long uniqueNum = (columnSeed * 1000000L + index) % (long)Math.pow(10, maxLength);
                uniqueStr = String.format("%0" + maxLength + "d", uniqueNum);
            }
            // 최대 길이 제한 적용
            if (maxLength > 0 && uniqueStr.length() > maxLength) {
                uniqueStr = uniqueStr.substring(0, maxLength);
            }
            return uniqueStr;
        } else if (dataType.contains("char")) {
            int maxLength = extractMaxLength(dataType);
            if (maxLength > 0) {
                // 고정 길이에 맞춘 고유값 (컬럼 시드 포함)
                long uniqueNum = (columnSeed * 1000000L + index) % (long)Math.pow(10, maxLength);
                String uniqueStr = String.format("%0" + maxLength + "d", uniqueNum);
                if (uniqueStr.length() > maxLength) {
                    uniqueStr = uniqueStr.substring(uniqueStr.length() - maxLength);
                }
                return uniqueStr;
            }
            return String.valueOf(columnSeed + index);
        } else if (dataType.contains("integer") || dataType.contains("int")) {
            // AUTO_INCREMENT가 아닌 경우에만
            if (!column.isAutoIncrement()) {
                // 컬럼별 다른 시작값 사용
                long uniqueInt = (columnSeed * 1000000L) + index;
                return String.valueOf(uniqueInt);
            }
            return String.valueOf(index + 1);
        } else if (dataType.contains("bigint") || dataType.contains("long")) {
            long uniqueLong = (columnSeed * 1000000000L) + index;
            return String.valueOf(uniqueLong);
        } else if (dataType.contains("decimal") || dataType.contains("numeric")) {
            // numeric 타입도 고유값 생성
            int uniqueNumeric = (columnSeed * 1000) + index;
            return uniqueNumeric;
        } else {
            // 기타 타입은 문자열 기반 고유값
            String uniqueStr = columnSeed + "_" + index;
            return uniqueStr;
        }
    }

    protected boolean hasUniqueConstraint(Table table, String columnName) {
        // 테이블의 제약조건 중에서 해당 컬럼에 UNIQUE 제약조건이 있는지 확인
        String columnNameLower = columnName.toLowerCase();
        for (Constraint constraint : table.getConstraints()) {
            // UNIQUE 제약조건만 확인 (PRIMARY KEY는 별도로 확인)
            if (constraint.getType() == Constraint.ConstraintType.UNIQUE) {
                for (String constraintColumn : constraint.getColumns()) {
                    // 대소문자 구분 없이 비교
                    if (constraintColumn.equalsIgnoreCase(columnName) || 
                        constraintColumn.toLowerCase().equals(columnNameLower)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 컬럼이 PRIMARY KEY인지 확인
     */
    private boolean isPrimaryKeyColumn(Table table, String columnName) {
        for (Constraint constraint : table.getConstraints()) {
            if (constraint.getType() == Constraint.ConstraintType.PRIMARY_KEY &&
                    constraint.getColumns().contains(columnName)) {
                return true;
            }
        }
        return false;
    }

    protected Object generateBasicValue(Column column, int index, Table table) {
        String dataType = column.getDataType().toLowerCase();
        String tableName = table.getName();
        String columnName = column.getName();
        String fakerType = null;

        // ⭐ PRIMARY KEY 또는 UNIQUE 제약이 있는 컬럼은 고유값 생성
        boolean isPrimaryKey = isPrimaryKeyColumn(table, column.getName());
        boolean isUnique = hasUniqueConstraint(table, column.getName());

        if (isPrimaryKey || isUnique) {
            fakerType = "UNIQUE_VALUE";
            recordFakerMapping(tableName, columnName, fakerType);
            return generateUniqueValue(column, index, dataType, column.getName());
        }

        // CUBRID 11.4 데이터 타입별 처리
        if (dataType.contains("varchar") || dataType.contains("char varying")) {
            int maxLength = extractMaxLength(dataType);
            if (maxLength > 0) {
                // VARCHAR 최대 길이: 1,073,741,823 (CUBRID 11.4)
                int actualLength = Math.min(maxLength, 1000); // 성능을 위해 1000자로 제한
                fakerType = "faker.lorem().characters(1, " + actualLength + ")";
                recordFakerMapping(tableName, columnName, fakerType);
                return faker.lorem().characters(1, actualLength);
            }
            fakerType = "faker.lorem().word()";
            recordFakerMapping(tableName, columnName, fakerType);
            return faker.lorem().word();
        } else if (dataType.contains("char(")) {
            int maxLength = extractMaxLength(dataType);
            if (maxLength > 0) {
                // CHAR 최대 길이: 2,048 (CUBRID 11.4에서 변경됨)
                int actualLength = Math.min(maxLength, 2048);
                fakerType = "CHAR_RANDOM(" + actualLength + ")";
                recordFakerMapping(tableName, columnName, fakerType);
                String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
                StringBuilder result = new StringBuilder();
                for (int i = 0; i < actualLength; i++) {
                    result.append(chars.charAt(faker.number().numberBetween(0, chars.length())));
                }
                return result.toString();
            }
            fakerType = "CHAR_FIXED('A')";
            recordFakerMapping(tableName, columnName, fakerType);
            return "A";
        } else if (dataType.contains("string")) {
            // STRING 타입 (CUBRID 11.4)
            fakerType = "faker.lorem().sentence()";
            recordFakerMapping(tableName, columnName, fakerType);
            return faker.lorem().sentence();
        } else if (dataType.contains("text")) {
            fakerType = "faker.lorem().paragraph()";
            recordFakerMapping(tableName, columnName, fakerType);
            return faker.lorem().paragraph();
        } else if (dataType.contains("timestamp")) {
            // TIMESTAMP 타입 - 현재 시간 기준으로 랜덤 생성
            fakerType = "LocalDateTime.random()";
            recordFakerMapping(tableName, columnName, fakerType);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime randomDateTime = now.minusDays(faker.number().numberBetween(1, 365))
                    .withHour(faker.number().numberBetween(0, 24))
                    .withMinute(faker.number().numberBetween(0, 60))
                    .withSecond(faker.number().numberBetween(0, 60));
            return randomDateTime.toString();
        } else if (dataType.contains("datetime")) {
            // DATETIME 타입 (CUBRID 11.4) - 현재 시간 기준으로 랜덤 생성
            fakerType = "LocalDateTime.random()";
            recordFakerMapping(tableName, columnName, fakerType);
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime randomDateTime = now.minusDays(faker.number().numberBetween(1, 365))
                    .withHour(faker.number().numberBetween(0, 24))
                    .withMinute(faker.number().numberBetween(0, 60))
                    .withSecond(faker.number().numberBetween(0, 60));
            return randomDateTime.toString();
        } else if (dataType.contains("date") && !dataType.contains("datetime") && !dataType.contains("timestamp")) {
            // DATE 타입 - 현재 날짜 기준으로 랜덤 생성 (datetime, timestamp가 아닌 경우만)
            fakerType = "LocalDate.random()";
            recordFakerMapping(tableName, columnName, fakerType);
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate randomDate = today.minusDays(faker.number().numberBetween(1, 365));
            return randomDate.toString();
        } else if (dataType.contains("time") && !dataType.contains("datetime") && !dataType.contains("timestamp")) {
            // TIME 타입 - 랜덤 시간 생성 (datetime, timestamp가 아닌 경우만)
            fakerType = "LocalTime.random()";
            recordFakerMapping(tableName, columnName, fakerType);
            java.time.LocalTime randomTime = java.time.LocalTime.of(
                    faker.number().numberBetween(0, 24),
                    faker.number().numberBetween(0, 60),
                    faker.number().numberBetween(0, 60)
            );
            return randomTime.toString();
        } else if (dataType.contains("integer") || dataType.contains("int")) {
            // INT/INTEGER 타입
            if (column.isAutoIncrement()) {
                fakerType = "AUTO_INCREMENT";
                recordFakerMapping(tableName, columnName, fakerType);
                return String.valueOf(index + 1);
            }
            fakerType = "faker.number().numberBetween(-2147483648, 2147483647)";
            recordFakerMapping(tableName, columnName, fakerType);
            return String.valueOf(faker.number().numberBetween(-2147483648, 2147483647));
        } else if (dataType.contains("short") || dataType.contains("smallint")) {
            // SHORT/SMALLINT 타입
            fakerType = "faker.number().numberBetween(-32768, 32767)";
            recordFakerMapping(tableName, columnName, fakerType);
            return String.valueOf(faker.number().numberBetween(-32768, 32767));
        } else if (dataType.contains("bigint")) {
            // BIGINT 타입
            fakerType = "faker.number().numberBetween(BIGINT_MIN, BIGINT_MAX)";
            recordFakerMapping(tableName, columnName, fakerType);
            return String.valueOf(faker.number().numberBetween(-9223372036854775808L, 9223372036854775807L));
        } else if (dataType.contains("numeric") || dataType.contains("decimal")) {
            // NUMERIC/DECIMAL 타입 - 정밀도와 스케일 파싱
            int[] precisionScale = extractNumericPrecisionScale(dataType);
            int precision = precisionScale[0];
            int scale = precisionScale[1];

            if (scale > 0) {
                // 소수점이 있는 경우
                double maxValue = Math.pow(10, precision - scale) - 1;
                double minValue = -maxValue;
                fakerType = "faker.number().randomDouble(" + scale + ", " + (int)minValue + ", " + (int)maxValue + ")";
                recordFakerMapping(tableName, columnName, fakerType);
                return String.valueOf(faker.number().randomDouble(scale, (int)minValue, (int)maxValue));
            } else {
                // 정수인 경우
                long maxValue = (long) Math.pow(10, precision) - 1;
                long minValue = -maxValue;
                fakerType = "faker.number().numberBetween(" + minValue + ", " + maxValue + ")";
                recordFakerMapping(tableName, columnName, fakerType);
                return String.valueOf(faker.number().numberBetween(minValue, maxValue));
            }
        } else if (dataType.contains("float") || dataType.contains("real")) {
            // FLOAT/REAL 타입
            fakerType = "faker.number().randomDouble(7, -340000000, 340000000)";
            recordFakerMapping(tableName, columnName, fakerType);
            return String.valueOf(faker.number().randomDouble(7, -340000000, 340000000));
        } else if (dataType.contains("double")) {
            // DOUBLE/DOUBLE PRECISION 타입
            fakerType = "faker.number().randomDouble(15, -1700000000, 1700000000)";
            recordFakerMapping(tableName, columnName, fakerType);
            return String.valueOf(faker.number().randomDouble(15, -1700000000, 1700000000));
        } else if (dataType.contains("boolean") || dataType.contains("bool")) {
            // BOOLEAN 타입
            fakerType = "faker.bool().bool()";
            recordFakerMapping(tableName, columnName, fakerType);
            return String.valueOf(faker.bool().bool());
        } else if (dataType.contains("bit(")) {
            // BIT(n) 타입
            int bitLength = extractMaxLength(dataType);
            if (bitLength > 0) {
                fakerType = "faker.bool().bool() * " + bitLength;
                recordFakerMapping(tableName, columnName, fakerType);
                StringBuilder bitString = new StringBuilder();
                for (int i = 0; i < bitLength; i++) {
                    bitString.append(faker.bool().bool() ? "1" : "0");
                }
                return bitString.toString();
            }
            fakerType = "faker.bool().bool()";
            recordFakerMapping(tableName, columnName, fakerType);
            return faker.bool().bool() ? "1" : "0";
        } else if (dataType.contains("bit varying")) {
            // BIT VARYING(n) 타입
            int maxBitLength = extractMaxLength(dataType);
            if (maxBitLength > 0) {
                fakerType = "faker.bool().bool() * random(1-" + maxBitLength + ")";
                recordFakerMapping(tableName, columnName, fakerType);
                int actualLength = faker.number().numberBetween(1, maxBitLength);
                StringBuilder bitString = new StringBuilder();
                for (int i = 0; i < actualLength; i++) {
                    bitString.append(faker.bool().bool() ? "1" : "0");
                }
                return bitString.toString();
            }
            fakerType = "faker.bool().bool()";
            recordFakerMapping(tableName, columnName, fakerType);
            return faker.bool().bool() ? "1" : "0";
        } else {
            // 알 수 없는 타입은 문자열로 처리
            fakerType = "faker.lorem().word()";
            recordFakerMapping(tableName, columnName, fakerType);
            return faker.lorem().word();
        }
    }
    
    /**
     * 컬럼-Faker 타입 매핑을 기록
     */
    private void recordFakerMapping(String tableName, String columnName, String fakerType) {
        tableColumnFakerMap.computeIfAbsent(tableName, k -> new ConcurrentHashMap<>())
            .putIfAbsent(columnName, fakerType);
    }
    
    /**
     * 테이블의 컬럼-Faker 타입 매핑 정보를 반환하고 로그 출력
     */
    public void logTableFakerMappings(String tableName) {
        Map<String, String> columnMappings = tableColumnFakerMap.get(tableName);
        if (columnMappings != null && !columnMappings.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Faker 매핑] ").append(tableName).append(": ");
            columnMappings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
                });
            // 마지막 쉼표 제거
            if (sb.length() > 2) {
                sb.setLength(sb.length() - 2);
            }
            log.info(sb.toString());
        }
    }
    
}