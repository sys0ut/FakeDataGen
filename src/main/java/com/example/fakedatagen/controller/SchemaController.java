package com.example.fakedatagen.controller;

import com.example.fakedatagen.service.SchemaAnalysisService;
import com.example.fakedatagen.service.DataGenerationService;
import com.example.fakedatagen.model.*;
import com.example.fakedatagen.config.FakeDataGenProperties;
import com.example.fakedatagen.exception.DataGenerationException;
import com.example.fakedatagen.exception.DatabaseConnectionException;
import com.example.fakedatagen.exception.SchemaParseException;
import com.example.fakedatagen.util.MemoryMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@Controller
public class SchemaController {
    
    private static final Logger log = LoggerFactory.getLogger(SchemaController.class);
    
    private final SchemaAnalysisService schemaAnalysisService;
    private final DataGenerationService dataGenerationService;
    private final FakeDataGenProperties properties;
    
    public SchemaController(SchemaAnalysisService schemaAnalysisService, 
                           DataGenerationService dataGenerationService,
                           FakeDataGenProperties properties) {
        this.schemaAnalysisService = schemaAnalysisService;
        this.dataGenerationService = dataGenerationService;
        this.properties = properties;
    }
    
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @PostMapping("/parse")
    public String parseSchema(@RequestParam("schemaText") String schemaText, 
                             @RequestParam(value = "recordCount", defaultValue = "100000") int recordCount,
                             @RequestParam(value = "insertToDatabase", defaultValue = "false") boolean insertToDatabase,
                             @RequestParam(value = "cubridVersion112", defaultValue = "false") boolean cubridVersion112,
                             @RequestParam(value = "dbHost", required = false) String dbHost,
                             @RequestParam(value = "dbPort", required = false) Integer dbPort,
                             @RequestParam(value = "dbName", required = false) String dbName,
                             @RequestParam(value = "dbUsername", required = false) String dbUsername,
                             @RequestParam(value = "dbPassword", required = false) String dbPassword,
                             Model model) {
        validateRequestParameters(schemaText, recordCount);
        try {
            log.info("Starting data generation process - recordCount={}, insertToDatabase={}, cubridVersion={}", 
                    recordCount, insertToDatabase, cubridVersion112 ? "11.2+" : "11.1");
            
            DatabaseConnectionInfo dbInfo = buildDatabaseConnectionInfo(dbHost, dbPort, dbName, dbUsername, dbPassword);
            
            if (insertToDatabase && dbInfo == null) {
                throw new IllegalArgumentException("DB INSERT를 선택하셨지만 데이터베이스 연결 정보가 제공되지 않았습니다. 연결 설정을 입력해주세요.");
            }
            
            DatabaseSchema schema = schemaAnalysisService.parseSchema(schemaText, cubridVersion112);
            log.info("Schema parsing completed - found {} tables", schema.getTables().size());
            
            DataGenerationService.DataGenerationResult result = 
                    dataGenerationService.generateAndInsertData(schema, recordCount, insertToDatabase, dbInfo);
            
            log.info("Data generation process completed - totalInserted={}", result.getTotalInserted());
            
            model.addAttribute("schema", schema);
            model.addAttribute("tables", schema.getTables());
            model.addAttribute("fakeData", result.getFakeData());
            model.addAttribute("recordCount", recordCount);
            model.addAttribute("schemaText", schemaText);
            model.addAttribute("insertToDatabase", insertToDatabase);
            model.addAttribute("totalInserted", result.getTotalInserted());
            model.addAttribute("insertMessage", result.getInsertMessage());
            model.addAttribute("tableInsertCounts", result.getTableInsertCounts());
            model.addAttribute("warnings", result.getWarnings() != null ? result.getWarnings() : java.util.Collections.emptyList());
            model.addAttribute("memoryInfo", MemoryMonitor.getMemoryInfo());
            
        } catch (SchemaParseException e) {
            log.error("Schema parsing failed", e);
            model.addAttribute("error", "스키마 파싱 오류: " + e.getMessage());
        } catch (DataGenerationException e) {
            log.error("Data generation failed", e);
            model.addAttribute("error", "데이터 생성 오류: " + e.getMessage());
        } catch (DatabaseConnectionException e) {
            log.error("Database connection failed", e);
            model.addAttribute("error", "데이터베이스 연결 오류: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Invalid input parameter", e);
            model.addAttribute("error", "입력 오류: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error occurred during data generation", e);
            model.addAttribute("error", "처리 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "result";
    }
    
    @GetMapping("/api/memory")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMemoryInfo() {
        Map<String, Object> response = new HashMap<>();
        try {
            MemoryMonitor.MemoryInfo memoryInfo = MemoryMonitor.getMemoryInfo();
            Map<String, Object> data = new HashMap<>();
            data.put("jvmUsedMB", memoryInfo.getJvmUsedMB());
            data.put("jvmMaxMB", memoryInfo.getJvmMaxMB());
            data.put("jvmUsedPercent", memoryInfo.getJvmUsedPercent());
            data.put("systemTotalMB", memoryInfo.getSystemTotalMB());
            data.put("systemFreeMB", memoryInfo.getSystemFreeMB());
            data.put("systemUsedMB", memoryInfo.getSystemUsedMB());
            data.put("systemUsedPercent", memoryInfo.getSystemUsedPercent());
            
            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get memory info", e);
            response.put("success", false);
            response.put("message", "메모리 정보를 가져오는데 실패했습니다");
            return ResponseEntity.ok(response);
        }
    }
    
    @PostMapping("/test-connection")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testConnection(@RequestParam("dbHost") String dbHost,
                                                              @RequestParam("dbPort") int dbPort,
                                                              @RequestParam("dbName") String dbName,
                                                              @RequestParam("dbUsername") String dbUsername,
                                                              @RequestParam("dbPassword") String dbPassword) {
        Map<String, Object> response = new HashMap<>();
        try {
            DatabaseConnectionInfo dbInfo = DatabaseConnectionInfo.builder()
                    .host(dbHost)
                    .port(dbPort)
                    .databaseName(dbName)
                    .username(dbUsername)
                    .password(dbPassword)
                    .build();
            
            boolean isConnected = dataGenerationService.testConnection(dbInfo);
            response.put("success", isConnected);
            response.put("message", isConnected ? "연결 성공" : "연결 실패");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid connection parameters", e);
            response.put("success", false);
            response.put("message", "입력 오류: " + e.getMessage());
            return ResponseEntity.ok(response);
        } catch (DatabaseConnectionException e) {
            log.error("Database connection test failed", e);
            response.put("success", false);
            response.put("message", "연결 실패: " + e.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Unexpected error during connection test", e);
            response.put("success", false);
            response.put("message", "연결 오류: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
    
    private void validateRequestParameters(String schemaText, int recordCount) {
        if (schemaText == null || schemaText.trim().isEmpty()) {
            throw new IllegalArgumentException("스키마 텍스트가 비어있습니다");
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
        if (schemaText.length() > properties.getMaxSchemaTextSize()) {
            throw new IllegalArgumentException(
                    String.format("스키마 텍스트가 너무 큽니다 (최대 %d 바이트)", 
                            properties.getMaxSchemaTextSize()));
        }
    }
    
    private DatabaseConnectionInfo buildDatabaseConnectionInfo(String dbHost, Integer dbPort, String dbName,
                                                               String dbUsername, String dbPassword) {
        if (dbHost != null && dbPort != null && dbName != null && 
            dbUsername != null && dbPassword != null) {
            log.debug("Database connection info provided - host={}, port={}, database={}", dbHost, dbPort, dbName);
            return new DatabaseConnectionInfo(dbHost, dbPort, dbName, dbUsername, dbPassword);
        } else {
            log.debug("Database connection info not provided - data generation only");
            return null;
        }
    }
}
