package com.example.fakedatagen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 성능 메트릭을 수집하고 로깅하는 유틸리티 클래스
 */
public class PerformanceMetrics {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceMetrics.class);
    
    private final long startTime;
    private final String operationName;
    private int recordCount;
    
    private PerformanceMetrics(String operationName) {
        this.operationName = operationName;
        this.startTime = System.currentTimeMillis();
        this.recordCount = 0;
    }
    
    /**
     * 새로운 성능 메트릭 수집을 시작합니다.
     * 
     * @param operationName 작업 이름
     * @return PerformanceMetrics 인스턴스
     */
    public static PerformanceMetrics start(String operationName) {
        return new PerformanceMetrics(operationName);
    }
    
    /**
     * 처리된 레코드 수를 설정합니다.
     * 
     * @param recordCount 레코드 수
     * @return this
     */
    public PerformanceMetrics withRecordCount(int recordCount) {
        this.recordCount = recordCount;
        return this;
    }
    
    /**
     * 성능 메트릭을 로깅하고 완료합니다.
     */
    public void logAndComplete() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        double recordsPerSecond = recordCount > 0 && elapsedTime > 0 
                ? (double) recordCount / elapsedTime * 1000 
                : 0;
        
        log.info("Performance [{}] - Records: {}, Elapsed: {} ms, Throughput: {:.2f} records/sec", 
                operationName,
                recordCount,
                elapsedTime,
                String.format("%.2f", recordsPerSecond));
    }
    
    /**
     * 경과 시간을 반환합니다 (밀리초).
     * 
     * @return 경과 시간 (밀리초)
     */
    public long getElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }
    
    /**
     * 처리량을 계산합니다 (레코드/초).
     * 
     * @return 처리량 (레코드/초)
     */
    public double getThroughput() {
        long elapsedTime = getElapsedTime();
        return elapsedTime > 0 ? (double) recordCount / elapsedTime * 1000 : 0;
    }
}
