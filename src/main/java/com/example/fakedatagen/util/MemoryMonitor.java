package com.example.fakedatagen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

/**
 * 메모리 사용량을 모니터링하는 유틸리티 클래스
 */
public class MemoryMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(MemoryMonitor.class);
    
    private MemoryMonitor() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * 메모리 정보를 담는 DTO 클래스
     */
    public static class MemoryInfo {
        private final long jvmUsedMB;
        private final long jvmMaxMB;
        private final double jvmUsedPercent;
        private final long systemTotalMB;
        private final long systemFreeMB;
        private final long systemUsedMB;
        private final double systemUsedPercent;
        
        public MemoryInfo(long jvmUsedMB, long jvmMaxMB, double jvmUsedPercent,
                         long systemTotalMB, long systemFreeMB, long systemUsedMB, double systemUsedPercent) {
            this.jvmUsedMB = jvmUsedMB;
            this.jvmMaxMB = jvmMaxMB;
            this.jvmUsedPercent = jvmUsedPercent;
            this.systemTotalMB = systemTotalMB;
            this.systemFreeMB = systemFreeMB;
            this.systemUsedMB = systemUsedMB;
            this.systemUsedPercent = systemUsedPercent;
        }
        
        public long getJvmUsedMB() { return jvmUsedMB; }
        public long getJvmMaxMB() { return jvmMaxMB; }
        public double getJvmUsedPercent() { return jvmUsedPercent; }
        public long getSystemTotalMB() { return systemTotalMB; }
        public long getSystemFreeMB() { return systemFreeMB; }
        public long getSystemUsedMB() { return systemUsedMB; }
        public double getSystemUsedPercent() { return systemUsedPercent; }
    }
    
    /**
     * JVM 및 시스템 메모리 정보를 가져옵니다.
     * 
     * @return MemoryInfo 객체
     */
    public static MemoryInfo getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        // JVM 메모리 정보
        long jvmTotalMemory = runtime.totalMemory();
        long jvmFreeMemory = runtime.freeMemory();
        long jvmUsedMemory = jvmTotalMemory - jvmFreeMemory;
        long jvmMaxMemory = runtime.maxMemory();
        double jvmUsedPercent = jvmMaxMemory > 0 ? (double) jvmUsedMemory / jvmMaxMemory * 100 : 0;
        
        // 시스템 메모리 정보
        long systemTotalMB = 0;
        long systemFreeMB = 0;
        long systemUsedMB = 0;
        double systemUsedPercent = 0;
        
        try {
            OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
            long systemTotalMemory = osBean.getTotalMemorySize();
            long systemFreeMemory = osBean.getFreeMemorySize();
            long systemUsedMemory = systemTotalMemory - systemFreeMemory;
            
            systemTotalMB = systemTotalMemory / (1024 * 1024);
            systemFreeMB = systemFreeMemory / (1024 * 1024);
            systemUsedMB = systemUsedMemory / (1024 * 1024);
            systemUsedPercent = systemTotalMemory > 0 ? (double) systemUsedMemory / systemTotalMemory * 100 : 0;
        } catch (Exception e) {
            log.debug("Failed to get system memory info: {}", e.getMessage());
        }
        
        return new MemoryInfo(
            jvmUsedMemory / (1024 * 1024),
            jvmMaxMemory / (1024 * 1024),
            jvmUsedPercent,
            systemTotalMB,
            systemFreeMB,
            systemUsedMB,
            systemUsedPercent
        );
    }
    
    /**
     * 현재 메모리 사용량을 로깅합니다.
     * 
     * @param context 컨텍스트 정보 (예: "데이터 생성 중")
     */
    public static void logMemoryUsage(String context) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double usedPercentage = (double) usedMemory / maxMemory * 100;
        
        log.info("JVM Memory usage [{}] - Used: {} MB / Max: {} MB ({}%)", 
                context,
                usedMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                String.format("%.2f", usedPercentage));
    }
    
    /**
     * 메모리 사용량이 임계값을 초과하는지 확인합니다.
     * 
     * @param warningThreshold 경고 임계값 (0.0 ~ 1.0)
     * @param criticalThreshold 위험 임계값 (0.0 ~ 1.0)
     * @return MemoryStatus 상태
     */
    public static MemoryStatus checkMemoryStatus(double warningThreshold, double criticalThreshold) {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        
        double usageRatio = (double) usedMemory / maxMemory;
        
        if (usageRatio >= criticalThreshold) {
            log.warn("Critical JVM memory usage detected: {}% (threshold: {}%)", 
                    String.format("%.2f", usageRatio * 100),
                    String.format("%.2f", criticalThreshold * 100));
            return MemoryStatus.CRITICAL;
        } else if (usageRatio >= warningThreshold) {
            log.warn("High JVM memory usage detected: {}% (threshold: {}%)", 
                    String.format("%.2f", usageRatio * 100),
                    String.format("%.2f", warningThreshold * 100));
            return MemoryStatus.WARNING;
        }
        
        return MemoryStatus.NORMAL;
    }
    
    /**
     * 메모리 상태를 나타내는 enum
     */
    public enum MemoryStatus {
        NORMAL,
        WARNING,
        CRITICAL
    }
}
