package com.example.fakedatagen.exception;

/**
 * 데이터 생성 관련 예외를 나타내는 커스텀 예외 클래스
 */
public class DataGenerationException extends RuntimeException {
    
    public DataGenerationException(String message) {
        super(message);
    }
    
    public DataGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
