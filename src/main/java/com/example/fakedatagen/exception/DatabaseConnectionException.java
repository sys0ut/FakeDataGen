package com.example.fakedatagen.exception;

/**
 * 데이터베이스 연결 관련 예외를 나타내는 커스텀 예외 클래스
 */
public class DatabaseConnectionException extends RuntimeException {
    
    public DatabaseConnectionException(String message) {
        super(message);
    }
    
    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
