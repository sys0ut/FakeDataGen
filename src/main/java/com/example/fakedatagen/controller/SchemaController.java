package com.example.fakedatagen.controller;

import com.example.fakedatagen.service.SchemaAnalysisService;
import com.example.fakedatagen.service.DataGenerationService;
import com.example.fakedatagen.model.*;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private SchemaAnalysisService schemaAnalysisService;
    
    @Autowired
    private DataGenerationService dataGenerationService;
    
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
        try {
            DatabaseConnectionInfo dbInfo = null;
            
            // 데이터베이스 연결 정보가 모두 제공된 경우에만 연결 정보 생성
            if (dbHost != null && dbPort != null && dbName != null && 
                dbUsername != null && dbPassword != null) {
                dbInfo = new DatabaseConnectionInfo(dbHost, dbPort, dbName, dbUsername, dbPassword);
                System.out.println("데이터베이스 연결 정보 생성됨: " + dbHost + ":" + dbPort + "/" + dbName);
            } else {
                System.out.println("데이터베이스 연결 정보가 불완전함 - dbHost:" + dbHost + ", dbPort:" + dbPort + ", dbName:" + dbName + ", dbUsername:" + dbUsername + ", dbPassword:" + (dbPassword != null ? "***" : "null"));
            }
            
            
            
            // 스키마 분석
            DatabaseSchema schema = schemaAnalysisService.parseSchema(schemaText, cubridVersion112);
            
            // 가짜 데이터 생성 및 INSERT
            DataGenerationService.DataGenerationResult result = dataGenerationService.generateAndInsertData(schema, recordCount, insertToDatabase, dbInfo);
            
            model.addAttribute("schema", schema);
            model.addAttribute("tables", schema.getTables());
            model.addAttribute("fakeData", result.getFakeData());
            model.addAttribute("recordCount", recordCount);
            model.addAttribute("schemaText", schemaText);
            model.addAttribute("insertToDatabase", insertToDatabase);
            model.addAttribute("totalInserted", result.getTotalInserted());
            model.addAttribute("insertMessage", result.getInsertMessage());
            model.addAttribute("tableInsertCounts", result.getTableInsertCounts());
            model.addAttribute("warnings", result.getWarnings());
            
        } catch (Exception e) {
            model.addAttribute("error", "스키마 파싱 중 오류가 발생했습니다: " + e.getMessage());
        }
        
        return "result";
    }
    
    // 연결 테스트 엔드포인트 추가
    @PostMapping("/test-connection")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testConnection(@RequestParam("dbHost") String dbHost,
                                                              @RequestParam("dbPort") int dbPort,
                                                              @RequestParam("dbName") String dbName,
                                                              @RequestParam("dbUsername") String dbUsername,
                                                              @RequestParam("dbPassword") String dbPassword) {
        try {
            // 연결 테스트 로직
            DatabaseConnectionInfo dbInfo = new DatabaseConnectionInfo(dbHost, dbPort, dbName, dbUsername, dbPassword);
            boolean isConnected = dataGenerationService.testConnection(dbInfo);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", isConnected);
            response.put("message", isConnected ? "연결 성공!" : "연결 실패");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "연결 오류: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
