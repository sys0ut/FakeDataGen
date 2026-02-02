package com.example.fakedatagen.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FakeDataGen 애플리케이션 설정을 외부화하는 Properties 클래스
 * application.properties에서 설정값을 읽어옵니다.
 */
@Component
@ConfigurationProperties(prefix = "fakedatagen")
public class FakeDataGenProperties {
    
    /**
     * 배치 처리 크기
     */
    private int batchSize = 10_000;
    
    /**
     * 최대 레코드 수
     */
    private int maxRecordCount = 10_000_000;
    
    /**
     * 최소 레코드 수
     */
    private int minRecordCount = 1;
    
    /**
     * 기본 레코드 수
     */
    private int defaultRecordCount = 100_000;
    
    /**
     * 스키마 텍스트 최대 크기 (바이트)
     */
    private int maxSchemaTextSize = 10_000_000;
    
    /**
     * Connection Pool 크기 설정
     */
    private PoolSize poolSize = new PoolSize();
    
    /**
     * Connection Pool 임계값 설정
     */
    private PoolThreshold poolThreshold = new PoolThreshold();
    
    /**
     * 컬렉션 초기 용량 설정
     */
    private InitialCapacity initialCapacity = new InitialCapacity();
    
    /**
     * 재시도 설정
     */
    private Retry retry = new Retry();
    
    /**
     * 메모리 모니터링 설정
     */
    private MemoryMonitoring memoryMonitoring = new MemoryMonitoring();
    
    // Getters and Setters
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public int getMaxRecordCount() {
        return maxRecordCount;
    }
    
    public void setMaxRecordCount(int maxRecordCount) {
        this.maxRecordCount = maxRecordCount;
    }
    
    public int getMinRecordCount() {
        return minRecordCount;
    }
    
    public void setMinRecordCount(int minRecordCount) {
        this.minRecordCount = minRecordCount;
    }
    
    public int getDefaultRecordCount() {
        return defaultRecordCount;
    }
    
    public void setDefaultRecordCount(int defaultRecordCount) {
        this.defaultRecordCount = defaultRecordCount;
    }
    
    public int getMaxSchemaTextSize() {
        return maxSchemaTextSize;
    }
    
    public void setMaxSchemaTextSize(int maxSchemaTextSize) {
        this.maxSchemaTextSize = maxSchemaTextSize;
    }
    
    public PoolSize getPoolSize() {
        return poolSize;
    }
    
    public void setPoolSize(PoolSize poolSize) {
        this.poolSize = poolSize;
    }
    
    public PoolThreshold getPoolThreshold() {
        return poolThreshold;
    }
    
    public void setPoolThreshold(PoolThreshold poolThreshold) {
        this.poolThreshold = poolThreshold;
    }
    
    public InitialCapacity getInitialCapacity() {
        return initialCapacity;
    }
    
    public void setInitialCapacity(InitialCapacity initialCapacity) {
        this.initialCapacity = initialCapacity;
    }
    
    public Retry getRetry() {
        return retry;
    }
    
    public void setRetry(Retry retry) {
        this.retry = retry;
    }
    
    public MemoryMonitoring getMemoryMonitoring() {
        return memoryMonitoring;
    }
    
    public void setMemoryMonitoring(MemoryMonitoring memoryMonitoring) {
        this.memoryMonitoring = memoryMonitoring;
    }
    
    /**
     * Connection Pool 크기 설정
     */
    public static class PoolSize {
        private int small = 5;
        private int medium = 10;
        private int large = 15;
        private int xlarge = 20;
        
        public int getSmall() {
            return small;
        }
        
        public void setSmall(int small) {
            this.small = small;
        }
        
        public int getMedium() {
            return medium;
        }
        
        public void setMedium(int medium) {
            this.medium = medium;
        }
        
        public int getLarge() {
            return large;
        }
        
        public void setLarge(int large) {
            this.large = large;
        }
        
        public int getXlarge() {
            return xlarge;
        }
        
        public void setXlarge(int xlarge) {
            this.xlarge = xlarge;
        }
    }
    
    /**
     * Connection Pool 임계값 설정
     */
    public static class PoolThreshold {
        private int threshold1 = 1_000;
        private int threshold2 = 100_000;
        private int threshold3 = 1_000_000;
        
        public int getThreshold1() {
            return threshold1;
        }
        
        public void setThreshold1(int threshold1) {
            this.threshold1 = threshold1;
        }
        
        public int getThreshold2() {
            return threshold2;
        }
        
        public void setThreshold2(int threshold2) {
            this.threshold2 = threshold2;
        }
        
        public int getThreshold3() {
            return threshold3;
        }
        
        public void setThreshold3(int threshold3) {
            this.threshold3 = threshold3;
        }
    }
    
    /**
     * 컬렉션 초기 용량 설정
     */
    public static class InitialCapacity {
        private int small = 16;
        private int medium = 100;
        private int large = 1000;
        
        public int getSmall() {
            return small;
        }
        
        public void setSmall(int small) {
            this.small = small;
        }
        
        public int getMedium() {
            return medium;
        }
        
        public void setMedium(int medium) {
            this.medium = medium;
        }
        
        public int getLarge() {
            return large;
        }
        
        public void setLarge(int large) {
            this.large = large;
        }
    }
    
    /**
     * 재시도 설정
     */
    public static class Retry {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private long delay = 1000; // milliseconds
        private double backoffMultiplier = 2.0;
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public int getMaxAttempts() {
            return maxAttempts;
        }
        
        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
        
        public long getDelay() {
            return delay;
        }
        
        public void setDelay(long delay) {
            this.delay = delay;
        }
        
        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }
        
        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }
    }
    
    /**
     * 메모리 모니터링 설정
     */
    public static class MemoryMonitoring {
        private boolean enabled = true;
        private double warningThreshold = 0.8; // 80%
        private double criticalThreshold = 0.9; // 90%
        
        public boolean isEnabled() {
            return enabled;
        }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public double getWarningThreshold() {
            return warningThreshold;
        }
        
        public void setWarningThreshold(double warningThreshold) {
            this.warningThreshold = warningThreshold;
        }
        
        public double getCriticalThreshold() {
            return criticalThreshold;
        }
        
        public void setCriticalThreshold(double criticalThreshold) {
            this.criticalThreshold = criticalThreshold;
        }
    }
}
