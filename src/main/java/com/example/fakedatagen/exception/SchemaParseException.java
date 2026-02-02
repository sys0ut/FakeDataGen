package com.example.fakedatagen.exception;

/**
 * 스키마 파싱 관련 예외를 나타내는 커스텀 예외 클래스
 */
public class SchemaParseException extends RuntimeException {
    
    public SchemaParseException(String message) {
        super(message);
    }
    
    public SchemaParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
