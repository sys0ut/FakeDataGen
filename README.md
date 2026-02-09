# FakeDataGen

CUBRID 데이터베이스용 가짜 데이터 생성기입니다. DDL 스키마를 입력하면 테이블 구조를 자동으로 파싱하여 현실적인 가짜 데이터를 생성하고, 선택적으로 실제 데이터베이스에 대량 삽입할 수 있습니다.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green)
![CUBRID](https://img.shields.io/badge/CUBRID-11.x-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 주요 기능

- **DDL 스키마 파싱** — `CREATE CLASS`, `ALTER CLASS ADD ATTRIBUTE` 등 CUBRID DDL 구문을 자동 분석
- **외래키 관계 인식** — 테이블 간 FK 관계를 분석하여 참조 무결성을 보장하는 데이터 생성
- **위상 정렬(Topological Sort)** — FK 의존관계에 따라 테이블 삽입 순서를 자동 결정
- **다양한 데이터 타입 지원** — INTEGER, VARCHAR, DATE, DATETIME, TIMESTAMP, NUMERIC, BOOLEAN, BIT 등 CUBRID의 모든 주요 타입 지원
- **AUTO_INCREMENT 처리** — 자동 증가 컬럼을 인식하고 생성된 키를 FK 참조에 활용
- **대량 배치 삽입** — 최대 1,000만 건까지 배치 INSERT로 고성능 삽입
- **CUBRID 버전 호환** — 11.2 이상(`[owner].[table]` 형식)과 11.1 이하(`[table]` 형식) 모두 지원
- **실시간 메모리 모니터링** — JVM 및 시스템 메모리 사용량을 결과 페이지에서 실시간 확인
- **재시도 메커니즘** — 일시적 DB 오류 발생 시 자동 재시도 (Exponential Backoff)
- **연결 테스트** — 데이터 생성 전 DB 연결 상태를 사전 확인

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 3.5.5 |
| Template Engine | Thymeleaf |
| Database | CUBRID (JDBC 11.3.2) |
| Data Generation | DataFaker 2.4.2 |
| Connection Pool | HikariCP |
| Build Tool | Gradle |

---

## 프로젝트 구조

```
src/main/java/com/example/fakedatagen/
├── config/                          # 설정
│   ├── DataSourceConfig.java        # DataSource 설정 (HikariCP)
│   └── FakeDataGenProperties.java   # 외부 설정 프로퍼티
├── controller/
│   └── SchemaController.java        # 웹 요청 처리 (파싱, 생성, 연결 테스트)
├── exception/                       # 커스텀 예외
│   ├── DatabaseConnectionException.java
│   ├── DataGenerationException.java
│   └── SchemaParseException.java
├── generator/                       # 데이터 생성
│   ├── BasicValueGenerator.java     # 기본 타입별 값 생성 (DataFaker 활용)
│   ├── ForeignKeyValueGenerator.java# FK 참조 값 생성
│   ├── RelationshipAwareGenerator.java # 관계 인식 데이터 생성 오케스트레이터
│   ├── RelationshipValueGenerator.java # 관계 기반 값 생성
│   └── TopologicalSorter.java       # FK 기반 테이블 정렬
├── model/                           # 도메인 모델
│   ├── Column.java
│   ├── Constraint.java
│   ├── DatabaseConnectionInfo.java
│   ├── DatabaseSchema.java
│   ├── ForeignKey.java
│   ├── Relationship.java
│   └── Table.java
├── parser/                          # DDL 파서
│   ├── CubridSchemaParser.java      # 메인 파서 (오케스트레이터)
│   ├── analyzer/
│   │   └── RelationshipAnalyzer.java# 테이블 간 관계 분석
│   ├── builder/
│   │   └── TableBuilder.java        # 파싱 결과로 Table 객체 조립
│   └── extractor/                   # 개별 요소 추출기
│       ├── ColumnExtractor.java
│       ├── ForeignKeyExtractor.java
│       ├── PrimaryKeyExtractor.java
│       ├── TableExtractor.java
│       └── UniqueConstraintExtractor.java
├── repository/
│   ├── DatabaseInsertRepository.java# DB INSERT 처리 (배치)
│   └── IdentifierValidator.java     # SQL 식별자 검증
├── service/
│   ├── DataGenerationService.java   # 데이터 생성/삽입 핵심 서비스
│   ├── SchemaAnalysisService.java   # 스키마 분석 서비스
│   └── ServiceMessages.java        # 서비스 메시지 상수
├── util/
│   ├── MemoryMonitor.java           # JVM/시스템 메모리 모니터링
│   ├── PerformanceMetrics.java      # 성능 측정 유틸
│   └── RetryHelper.java            # 재시도 유틸 (Exponential Backoff)
└── FakeDataGenApplication.java      # 메인 애플리케이션
```

---

## 시작하기

### 사전 요구사항

- **Java 21** 이상
- **Gradle** (Wrapper 포함)
- **CUBRID Database** (데이터 삽입 시)

### 빌드 및 실행

```bash
# 프로젝트 클론
git clone https://github.com/your-username/FakeDataGen.git
cd FakeDataGen

# 빌드
./gradlew build

# 실행
./gradlew bootRun
```

> Windows 환경에서는 `./gradlew` 대신 `gradlew.bat`을 사용하세요.

### 접속

브라우저에서 **http://localhost:9090** 으로 접속합니다.

---

## 사용 방법

### 1. 연결 설정 (선택)

실제 DB에 데이터를 삽입하려면 연결 정보를 입력합니다.

| 항목 | 예시 |
|------|------|
| 호스트 | `192.168.1.100` |
| 포트 | `33000` |
| DB 이름 | `demodb` |
| 사용자 | `dba` |
| 비밀번호 | (비밀번호) |

**연결 테스트** 버튼으로 연결 상태를 사전 확인할 수 있습니다.

### 2. 스키마 입력

CUBRID DDL 스키마를 텍스트 영역에 붙여넣습니다.

```sql
CREATE CLASS [dba].[users] REUSE_OID, COLLATE utf8_bin;

ALTER CLASS [dba].[users] ADD ATTRIBUTE
       [id] integer AUTO_INCREMENT(1, 1) NOT NULL,
       [name] character varying(100) COLLATE utf8_bin,
       [email] character varying(200) COLLATE utf8_bin,
       [created_at] datetime;

ALTER CLASS [dba].[users] ADD CONSTRAINT [pk_users_id]
       PRIMARY KEY([id]);
```

### 3. 옵션 설정

| 옵션 | 설명 |
|------|------|
| **레코드 수** | 생성할 레코드 수 (1 ~ 10,000,000) |
| **DB INSERT** | 체크 시 실제 DB에 데이터 삽입 |
| **CUBRID 11.2 이상** | 체크 시 `[owner].[table]` 형식으로 쿼리 실행 |

### 4. 결과 확인

- 테이블별 컬럼 구조 및 생성된 샘플 데이터 확인
- 외래키 관계 시각화
- DB INSERT 시 삽입 건수 및 경고 확인
- JVM / 시스템 메모리 사용량 실시간 모니터링

---

## 설정

`application.properties`에서 주요 설정을 변경할 수 있습니다.

```properties
# 서버 포트
server.port=9090

# 배치 크기 (한 번에 INSERT하는 레코드 수)
fakedatagen.batch-size=50000

# 레코드 수 제한
fakedatagen.max-record-count=10000000
fakedatagen.min-record-count=1
fakedatagen.default-record-count=100000

# 재시도 설정
fakedatagen.retry.enabled=true
fakedatagen.retry.max-attempts=3
fakedatagen.retry.delay=1000
fakedatagen.retry.backoff-multiplier=2.0

# 메모리 모니터링
fakedatagen.memory-monitoring.enabled=true
fakedatagen.memory-monitoring.warning-threshold=0.8
fakedatagen.memory-monitoring.critical-threshold=0.9
```

---

## 지원 데이터 타입

| 카테고리 | 타입 |
|----------|------|
| 정수 | `SMALLINT`, `INTEGER`, `BIGINT` |
| 실수 | `FLOAT`, `DOUBLE`, `NUMERIC`, `DECIMAL` |
| 문자열 | `CHAR`, `VARCHAR`, `STRING` |
| 날짜/시간 | `DATE`, `TIME`, `DATETIME`, `TIMESTAMP` |
| 논리 | `BOOLEAN` |
| 바이너리 | `BIT`, `BIT VARYING` |

---

## 처리 흐름

```
DDL 입력
  │
  ▼
스키마 파싱 (CubridSchemaParser)
  ├── 테이블 추출 (TableExtractor)
  ├── 컬럼 추출 (ColumnExtractor)
  ├── PK 추출 (PrimaryKeyExtractor)
  ├── FK 추출 (ForeignKeyExtractor)
  ├── UNIQUE 추출 (UniqueConstraintExtractor)
  └── 관계 분석 (RelationshipAnalyzer)
  │
  ▼
위상 정렬 (TopologicalSorter)
  │ FK 의존관계 기반 삽입 순서 결정
  │
  ▼
데이터 생성 (RelationshipAwareGenerator)
  ├── BasicValueGenerator (타입별 값 생성)
  ├── ForeignKeyValueGenerator (FK 참조 값)
  └── RelationshipValueGenerator (관계 기반 값)
  │
  ▼
DB 삽입 (DatabaseInsertRepository)
  ├── 기존 데이터 DELETE (역순)
  ├── 배치 INSERT (PreparedStatement + addBatch)
  └── AUTO_INCREMENT 키 수집 (getGeneratedKeys)
  │
  ▼
결과 표시 (result.html)
  ├── 테이블별 구조 / 샘플 데이터
  ├── 삽입 건수 / 경고
  └── 메모리 사용량 (실시간)
```

---

## 스크린샷

### 메인 페이지
- DDL 스키마 입력
- 연결 설정 및 테스트
- 레코드 수 / CUBRID 버전 선택

### 결과 페이지
- 테이블별 컬럼 구조
- 생성된 가짜 데이터 미리보기
- DB INSERT 결과 요약
- 실시간 메모리 모니터링 (JVM + 시스템)

---

## 라이선스

This project is licensed under the MIT License.
