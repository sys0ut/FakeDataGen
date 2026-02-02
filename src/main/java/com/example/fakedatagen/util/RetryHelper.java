package com.example.fakedatagen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * 재시도 로직을 제공하는 유틸리티 클래스
 */
public class RetryHelper {
    
    private static final Logger log = LoggerFactory.getLogger(RetryHelper.class);
    
    private RetryHelper() {
        throw new AssertionError("Cannot instantiate utility class");
    }
    
    /**
     * 재시도 로직을 실행합니다.
     * 
     * @param operation 실행할 작업
     * @param maxAttempts 최대 시도 횟수
     * @param delay 초기 지연 시간 (밀리초)
     * @param backoffMultiplier 지연 시간 배수
     * @param <T> 반환 타입
     * @return 작업 결과
     * @throws RuntimeException 모든 시도가 실패한 경우
     */
    public static <T> T executeWithRetry(Supplier<T> operation, 
                                        int maxAttempts, 
                                        long delay, 
                                        double backoffMultiplier) {
        Exception lastException = null;
        long currentDelay = delay;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    log.warn("Operation failed (attempt {}/{}), retrying after {} ms: {}", 
                            attempt, maxAttempts, currentDelay, e.getMessage());
                    try {
                        Thread.sleep(currentDelay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                    currentDelay = (long) (currentDelay * backoffMultiplier);
                } else {
                    log.error("Operation failed after {} attempts", maxAttempts, e);
                }
            }
        }
        
        throw new RuntimeException("Operation failed after " + maxAttempts + " attempts", lastException);
    }
    
    /**
     * 재시도 로직을 실행합니다 (void 작업).
     * 
     * @param operation 실행할 작업
     * @param maxAttempts 최대 시도 횟수
     * @param delay 초기 지연 시간 (밀리초)
     * @param backoffMultiplier 지연 시간 배수
     * @throws RuntimeException 모든 시도가 실패한 경우
     */
    public static void executeWithRetry(Runnable operation, 
                                       int maxAttempts, 
                                       long delay, 
                                       double backoffMultiplier) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, maxAttempts, delay, backoffMultiplier);
    }
}
