package com.example.fakedatagen.service;

/**
 * 서비스 계층에서 사용하는 사용자 메시지 상수 모음
 */
public final class ServiceMessages {
    private ServiceMessages() {}

    public static final String GENERATE_ONLY = "데이터 생성만 완료되었습니다. (INSERT하지 않음)";
    public static final String INSERT_SKIPPED_NO_DBINFO = "데이터베이스 연결 정보가 제공되지 않아 INSERT하지 않았습니다. 데이터 생성만 완료되었습니다.";
    public static final String DB_INSERT_ERROR_PREFIX = "데이터베이스 INSERT 중 오류가 발생했습니다: ";
}


