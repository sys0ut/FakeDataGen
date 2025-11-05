# CUBRID Fake Data Generator - 클래스 설계서 (리팩토링 반영판)

## 프로젝트 개요

CUBRID DDL을 파싱해 스키마 모델을 만들고, 그 관계를 만족하는 가짜 데이터를 생성한 뒤 선택적으로 실제 DB에 INSERT하는 Spring Boot 애플리케이션.

핵심 변경점(2025-10)
- 생성자 주입으로 전환, 의존성 불변화
- TransactionTemplate 기반 트랜잭션 처리
- AUTO_INCREMENT의 실제 생성 키를 수집해 FK 무결성 보장
- IdentifierValidator로 테이블/컬럼 화이트리스트 검증
- MessageSource+i18n, ServiceMessages 폴백, SLF4J 로깅
- Faker ThreadLocal, 동시성 안전
- DataGenerationResult 확장(tableInsertCounts, warnings)
- UI 다크·미니멀, 연결 설정 기본값/placeholder 제거

---

## 패키지 구조

```
com.example.fakedatagen
├── controller/         # 웹 요청 처리(Thymeleaf)
├── model/             # 스키마/관계 도메인
├── parser/            # 스키마 파서(추출/분석/조립)
│   ├── extractor/
│   ├── analyzer/
│   └── builder/
├── generator/         # 값/관계/위상정렬 기반 데이터 생성
├── repository/        # DB 접근(INSERT/조회/검증 유틸)
└── service/           # 오케스트레이션/애플리케이션 로직
```

---

## Controller Layer

### SchemaController
책임: HTTP 요청 처리 및 화면 렌더링

주요 메서드:
- `index()` 메인 페이지
- `parseSchema(...)` 스키마 파싱 + 데이터 생성/삽입 실행
- `testConnection(...)` DB 연결 테스트

의존성(생성자 주입):
- `SchemaAnalysisService`, `DataGenerationService`

View Model: `tables`, `fakeData`, `totalInserted`, `insertMessage`, `tableInsertCounts`, `warnings`, `schemaText`, `recordCount`, `insertToDatabase`

---

## Model Layer

### DatabaseSchema
책임: 스키마 전체 상태 저장(테이블/관계/의존성)

핵심 메서드: `getTableByName`, `getRootTables`, `addTable`, `addDependency`, `addRelationship`

### Table / Column / ForeignKey / Constraint / Relationship
각 객체는 스키마 구성요소를 표현. Column은 `isPrimaryKey`, `isAutoIncrement`, `isNullable` 등 메타 포함. ForeignKey는 참조 테이블/컬럼과 ON DELETE/UPDATE 동작 보유.

### DatabaseConnectionInfo
책임: 연결 정보 값 객체. `getJdbcUrl()`로 CUBRID JDBC URL 생성.

---

## Parser Layer

### CubridSchemaParser
역할: 추출기/조립기/분석기를 조합해 스키마를 생성.
순서: Table → Column → PK → FK → UNIQUE → Build → Analyze.

하위 구성요소
- Extractor들(Table/Column/PK/FK/Unique)
- TableBuilder: 추출 결과를 `Table`로 조립
- RelationshipAnalyzer: FK/Unique/복합PK로 관계·의존성 도출

---

## Generator Layer

### RelationshipAwareGenerator
역할: 위상 정렬 순서대로 테이블 데이터를 생성(관계 준수).

의존성(생성자 주입): `TopologicalSorter`, `BasicValueGenerator`, `ForeignKeyValueGenerator`, `RelationshipValueGenerator`

특징
- ThreadLocal<Faker> 사용으로 멀티스레드 안전
- `generateFakeData(schema, count)` 전체 데이터 생성
- `generateTableDataWithGeneratedData(...)` 이미 생성된 키/데이터를 활용한 테이블 단위 생성

### TopologicalSorter
역할: FK 의존성 기반 테이블 정렬. 순환 감지.

### BasicValueGenerator / ForeignKeyValueGenerator / RelationshipValueGenerator
역할: 기본 타입/외래키/관계 기반 값 생성. UNIQUE, NULL 허용 등 제약 반영.

---

## Repository Layer

### IdentifierValidator
역할: 스키마 기반 식별자 화이트리스트 검증.
- `isAllowedTable(tableName)`, `isAllowedColumn(tableName, columnName)`

### DatabaseInsertRepository
역할: INSERT 수행 및(필요 시) 실제 생성 키 수집.

주요 메서드:
- `insertRecords(tableName, records, schema)` 고정 템플릿용
- `insertRecordsWithJdbcTemplate(jdbcTemplate, tableName, records, schema)` 동적 연결 경로
- `insertAllRecords(fakeData, orderedTableNames, schema)`

정책
- IdentifierValidator로 테이블/컬럼 화이트리스트 검증
- AUTO_INCREMENT: `PreparedStatement.RETURN_GENERATED_KEYS`로 개별 INSERT, 생성 키 수집(FK 연결에 사용)
- 비AUTO_INCREMENT: `JdbcTemplate#batchUpdate`로 배치 성능 최적화
- 유틸: 실제/전체 데이터 조회 메서드(디버깅)

---

## Service Layer

### SchemaAnalysisService
책임: DDL 문자열 → `DatabaseSchema` 반환

### DataGenerationService
책임: 데이터 생성/삽입 오케스트레이션, 연결 테스트

의존성(생성자 주입): `RelationshipAwareGenerator`, `DatabaseInsertRepository`, `MessageSource`

핵심 메서드/로직
- `generateAndInsertData(schema, recordCount, insertToDatabase, dbInfo)`
  - 비삽입: `generateFakeData` 실행, i18n 메시지(`ServiceMessages` 폴백) 세팅
  - 삽입: HikariDataSource → JdbcTemplate → DataSourceTransactionManager → TransactionTemplate 구성
    1) 위상 정렬 역순으로 DELETE(전략 유지)
    2) 테이블별 데이터 생성 → Repository INSERT → 생성 키/건수 기록
    3) 테이블 단위 예외는 `warnings`에 누적, 전체 실패는 트랜잭션 예외 처리
- `generateData(schema, count)`, `insertData(jdbcTemplate, schema, data)` 보조 분리
- `testConnection(dbInfo)` 간이 연결 검사

반환 모델(DataGenerationResult)
- `fakeData: Map<String, List<Map<String,Object>>>`
- `totalInserted: int`
- `insertMessage: String`
- `tableInsertCounts: Map<String,Integer>`
- `warnings: List<String>`

국제화/로깅
- `MessageSource`로 code 조회, 실패 시 `ServiceMessages` 폴백
- SLF4J 로깅: 테이블 단위 warn, 트랜잭션 error, 연결 테스트 debug

---

## 데이터 흐름

```
1. 사용자 입력(DDL) → SchemaController
2. SchemaAnalysisService → CubridSchemaParser(Extractor/Builder/Analyzer)
3. DatabaseSchema 완성
4. DataGenerationService → RelationshipAwareGenerator(TopologicalSorter/ValueGenerators)
5. Repository INSERT(키 수집) [선택]
6. DataGenerationResult 반환 → View 렌더링
```

---

## Test Layer

- TopologicalSorterTest: 위상 정렬/의존성 순서 검증
- Table/Column/PK/FK/Unique Extractor 테스트: 파싱 정확성
- DataGenerationServiceTest: 비삽입/DB정보 없음/i18n/외래키 일관성(목) 검증

---

## 설계 원칙

- SRP: 추출/조립/분석/생성/삽입 각자 책임 분리
- DI: 생성자 주입 + `final` 필드로 불변성/테스트 용이성
- Strategy: 관계/외래키/기본값 생성 전략 분리
- 트랜잭션 경계 명확화: TransactionTemplate
- 안전한 SQL: 식별자 화이트리스트 + 값 바인딩

---

## 주요 정규식(요약)

| 항목 | 패턴 |
|---|---|
| 테이블 | `CREATE\s+CLASS\s+\[([^\]]+)\]\.\[([^\]]+)\]` |
| 컬럼 | `ALTER\s+CLASS\s+\[(.*?)\]\.\[(.*?)\]\s+ADD\s+ATTRIBUTE` |
| 기본키 | `CONSTRAINT\s+\[(.*?)\]\s+PRIMARY\s+KEY\((.*?)\)` |
| 외래키 | `FOREIGN\s+KEY\s*\(([^)]*)\).*?REFERENCES\s+\[(.*?)\]\.\[(.*?)\]` |
| UNIQUE | `CONSTRAINT\s+\[(.*?)\]\s+UNIQUE\(([^)]*)\)` |

---

## 테이블 생성 순서(예)

```
a → b → c
parent → child → grandchild
```
이 순서를 준수해야 FK 무결성을 깨지지 않고 데이터 생성/삽입 가능.

---

## UI Layer(참고)
- `templates/index.html`: 미니멀 다크, 연결설정 기본값 없음, 테스트 버튼.
- `templates/result.html`: 요약/경고 수, 테이블 구조·데이터, 스키마 원문.
- `static/styles.css`: 패널/그리드/버튼/테이블 공통 스타일, 반응형.

---

## 🎯 Controller Layer

### SchemaController
**책임**: HTTP 요청 처리 및 화면 응답

**주요 메서드**:
- `index()` - 메인 페이지
- `parseSchema()` - 스키마 파싱 및 데이터 생성
- `testConnection()` - 데이터베이스 연결 테스트

**의존성**:
- SchemaAnalysisService
- DataGenerationService

---

## 📊 Model Layer

### DatabaseSchema
**책임**: 전체 데이터베이스 스키마 정보 관리

**주요 속성**:
- `schemaName` - 스키마명
- `tables` - 테이블 리스트
- `dependencies` - 테이블 의존성 그래프
- `relationships` - 테이블 간 관계

**주요 메서드**:
- `addTable()` - 테이블 추가
- `addDependency()` - 의존성 추가
- `getTableByName()` - 테이블 조회
- `getRootTables()` - 루트 테이블 조회

---

### Table
**책임**: 개별 테이블 정보 관리

**주요 속성**:
- `name` - 테이블명
- `schemaName` - 스키마명
- `columns` - 컬럼 리스트
- `foreignKeys` - 외래키 리스트
- `constraints` - 제약조건 리스트

**주요 메서드**:
- `addColumn()` - 컬럼 추가
- `addForeignKey()` - 외래키 추가
- `getPrimaryKeyColumns()` - 기본키 컬럼 조회
- `hasForeignKeyTo()` - 외래키 존재 여부

---

### Column
**책임**: 컬럼 정보 관리

**주요 속성**:
- `name` - 컬럼명
- `dataType` - 데이터 타입
- `isPrimaryKey` - 기본키 여부
- `isAutoIncrement` - AUTO_INCREMENT 여부
- `isNullable` - NULL 허용 여부
- `maxLength` - 최대 길이
- `defaultValue` - 기본값

---

### ForeignKey
**책임**: 외래키 정보 관리

**주요 속성**:
- `columnName` - 외래키 컬럼명
- `referencedTableName` - 참조 테이블명
- `referencedColumnName` - 참조 컬럼명
- `onDeleteAction` - DELETE 시 동작
- `onUpdateAction` - UPDATE 시 동작

---

### Constraint
**책임**: 제약조건 정보 관리

**주요 속성**:
- `name` - 제약조건명
- `type` - 제약조건 타입 (PRIMARY_KEY, FOREIGN_KEY, UNIQUE, CHECK, NOT_NULL)
- `columns` - 관련 컬럼 리스트

---

### Relationship
**책임**: 테이블 간 관계 정보 관리

**주요 속성**:
- `sourceTable` - 소스 테이블
- `targetTable` - 타겟 테이블
- `type` - 관계 타입 (ONE_TO_ONE, ONE_TO_MANY, MANY_TO_ONE, MANY_TO_MANY)
- `sourceColumns` - 소스 컬럼
- `targetColumns` - 타겟 컬럼

---

### DatabaseConnectionInfo
**책임**: 데이터베이스 연결 정보 관리

**주요 속성**:
- `host` - 호스트
- `port` - 포트
- `databaseName` - 데이터베이스명
- `username` - 사용자명
- `password` - 비밀번호

**주요 메서드**:
- `getJdbcUrl()` - JDBC URL 생성

---

## 🔧 Parser Layer

### CubridSchemaParser (메인 파서)
**책임**: 전체 파싱 프로세스 오케스트레이션

**주요 메서드**:
- `parseSchema(schemaText, keepSchemaName)` - 스키마 파싱

**의존성**:
- TableExtractor
- ColumnExtractor
- PrimaryKeyExtractor
- ForeignKeyExtractor
- UniqueConstraintExtractor
- RelationshipAnalyzer
- TableBuilder

**처리 순서**:
1. 테이블 추출
2. 컬럼 추출
3. 기본키 추출
4. 외래키 추출
5. UNIQUE 제약조건 추출
6. 테이블 객체 조립
7. 관계 분석

---

### TableExtractor
**책임**: 테이블 정의 추출

**주요 메서드**:
- `extract(schemaText, keepSchemaName)` - CREATE CLASS 문에서 테이블 추출

**정규식**: `CREATE\\s+CLASS\\s+\\[([^\\]]+)\\]\\.\\[([^\\]]+)\\]`

**출력**: `List<Table>`

<pre>
==== Extracted Tables ====
Table: a
Table: b
Table: c
Table: parent
Table: child
Table: grandchild
==== End of Tables ====
</pre>

---

### ColumnExtractor
**책임**: 컬럼 정의 추출

**주요 메서드**:
- `extract(schemaText, keepSchemaName)` - ALTER CLASS ADD ATTRIBUTE 문에서 컬럼 추출

**정규식**: `ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+ATTRIBUTE\\s+...`

**출력**: `Map<String, List<Column>>` (key: schema.table)

**특별 처리**:
- AUTO_INCREMENT 감지
- 컬럼 타입 파싱

<pre>
테이블: dba.c
  컬럼명: id, 타입: integer, AUTO_INCREMENT: true
  컬럼명: b_id, 타입: integer, AUTO_INCREMENT: false

테이블: dba.b
  컬럼명: id, 타입: integer, AUTO_INCREMENT: true
  컬럼명: a_id, 타입: integer, AUTO_INCREMENT: false

테이블: dba.a
  컬럼명: id, 타입: integer, AUTO_INCREMENT: true

테이블: dba.grandchild
  컬럼명: grandchild_id, 타입: integer, AUTO_INCREMENT: true
  컬럼명: grandchild_name, 타입: character varying(50), AUTO_INCREMENT: false
  컬럼명: parent_id, 타입: character varying(10), AUTO_INCREMENT: false
  컬럼명: child_id, 타입: integer, AUTO_INCREMENT: false

테이블: dba.parent
  컬럼명: parent_id, 타입: character varying(10), AUTO_INCREMENT: false
  컬럼명: name, 타입: character varying(50), AUTO_INCREMENT: false
  컬럼명: description, 타입: character varying(200), AUTO_INCREMENT: false

테이블: dba.child
  컬럼명: child_id, 타입: integer, AUTO_INCREMENT: true
  컬럼명: child_name, 타입: character varying(50), AUTO_INCREMENT: false
  컬럼명: parent_id, 타입: character varying(10), AUTO_INCREMENT: false
  컬럼명: created_date, 타입: datetime, AUTO_INCREMENT: false
</pre>

---

### PrimaryKeyExtractor
**책임**: PRIMARY KEY 제약조건 추출

**주요 메서드**:
- `extract(schemaText, keepSchemaName)` - PRIMARY KEY 제약조건 추출

**정규식**: `ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+ATTRIBUTE\\s+CONSTRAINT\\s+\\[(.*?)\\]\\s+PRIMARY\\s+KEY\\((.*?)\\)`

**출력**: `Map<String, List<String>>` (key: schema.table, value: PK 컬럼 리스트)

<pre>
==== Extracted Tables ====
Table: dba.cPK 컬럼: [id]
Table: dba.bPK 컬럼: [id]
Table: dba.aPK 컬럼: [id]
Table: dba.grandchildPK 컬럼: [grandchild_id]
Table: dba.parentPK 컬럼: [parent_id]
Table: dba.childPK 컬럼: [child_id]
==== End of Tables ====
</pre>

---

### ForeignKeyExtractor
**책임**: FOREIGN KEY 제약조건 추출

**주요 메서드**:
- `extract(schemaText, keepSchemaName, pkMap)` - FOREIGN KEY 제약조건 추출

**정규식**: `ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+CONSTRAINT\\s+\\[(.*?)\\]\\s+FOREIGN\\s+KEY\\s*\\(([^)]*)\\).*?REFERENCES\\s+\\[(.*?)\\]\\.\\[(.*?)\\]`

**출력**: `Map<String, List<ForeignKey>>`

**특별 처리**:
- 참조 컬럼명 자동 추정 (pkMap 사용)
- ON DELETE/UPDATE 액션 파싱

<pre>
==== Extracted Tables ====
테이블: dba.c
  FK 컬럼: b_id, 참조 테이블: dba.b, 참조 컬럼: id
--------------------
테이블: dba.b
  FK 컬럼: a_id, 참조 테이블: dba.a, 참조 컬럼: id
--------------------
테이블: dba.grandchild
  FK 컬럼: parent_id, 참조 테이블: dba.parent, 참조 컬럼: parent_id
  FK 컬럼: child_id, 참조 테이블: dba.child, 참조 컬럼: child_id
--------------------
테이블: dba.child
  FK 컬럼: parent_id, 참조 테이블: dba.parent, 참조 컬럼: parent_id
==== End of Tables ====
</pre>

---

### UniqueConstraintExtractor
**책임**: UNIQUE 제약조건 추출

**주요 메서드**:
- `extract(schemaText, keepSchemaName)` - UNIQUE 제약조건 추출

**정규식**: `ALTER\\s+CLASS\\s+\\[(.*?)\\]\\.\\[(.*?)\\]\\s+ADD\\s+ATTRIBUTE\\s+CONSTRAINT\\s+\\[(.*?)\\]\\s+UNIQUE\\(([^)]*)\\)`

**출력**: `Map<String, List<String>>`

---

### RelationshipAnalyzer
**책임**: 테이블 간 관계 분석 및 의존성 그래프 생성

**주요 메서드**:
- `analyze(schema)` - 관계 분석 및 의존성 그래프 생성

**관계 타입 결정 로직**:
1. UNIQUE 제약조건 존재 → ONE_TO_ONE
2. 복합 기본키 테이블 → MANY_TO_MANY (중간 테이블)
3. 기본값 → MANY_TO_ONE

**의존성 그래프**: 외래키를 기반으로 테이블 간 의존성 관계 구축

RelationshipAnalyzer 클래스는 데이터베이스 스키마를 분석해 테이블 간 외래키 기반 관계와 의존성을 자동으로 파악하는 역할을 함. 
외래키가 참조하는 테이블을 찾고, UNIQUE나 복합 PK 여부를 기준으로 1:1, N:1, N:N 관계를 결정한 뒤, 관계 정보를 스키마에 등록

<pre>
&lt; 1:1 관계&gt;

소스 테이블의 외래키 컬럼이 UNIQUE 제약조건을 가지고 있을 때

의미: 한 레코드가 참조 테이블의 한 레코드에만 대응됨

&lt; N:N 관계&gt;

소스 테이블이 복합 기본키(PK)를 가진 테이블일 때

보통 중간 조인 테이블(join table)을 의미

&lt; N:1 관계&gt;

위 두 조건에 해당하지 않을 때

의미: 여러 소스 레코드가 타겟 테이블의 한 레코드를 참조
</pre>


---

### TableBuilder
**책임**: 추출된 정보를 조합하여 Table 객체 생성

**주요 메서드**:
- `buildTablesAndAddToSchema(...)` - 테이블 객체 생성 및 스키마에 추가

**처리 내용**:
1. 컬럼 추가 및 기본키 설정
2. PRIMARY KEY 제약조건 추가
3. UNIQUE 제약조건 추가
4. 외래키 추가
5. 스키마에 테이블 추가

<pre>
Table: a
  Column: id, PK=true, AUTO_INCREMENT=true
  Constraint: pk_a_id, Type=PRIMARY_KEY, Columns=[id]
Table: b

  Column: id, PK=true, AUTO_INCREMENT=true
  Column: a_id, PK=false, AUTO_INCREMENT=false
  Constraint: pk_b_id, Type=PRIMARY_KEY, Columns=[id]
  ForeignKey: null, References=dba.a(id)
Table: c

  Column: id, PK=true, AUTO_INCREMENT=true
  Column: b_id, PK=false, AUTO_INCREMENT=false
  Constraint: pk_c_id, Type=PRIMARY_KEY, Columns=[id]
  ForeignKey: null, References=dba.b(id)

Table: parent
  Column: parent_id, PK=true, AUTO_INCREMENT=false
  Column: name, PK=false, AUTO_INCREMENT=false
  Column: description, PK=false, AUTO_INCREMENT=false
  Constraint: pk_parent_parent_id, Type=PRIMARY_KEY, Columns=[parent_id]

Table: child
  Column: child_id, PK=true, AUTO_INCREMENT=true
  Column: child_name, PK=false, AUTO_INCREMENT=false
  Column: parent_id, PK=false, AUTO_INCREMENT=false
  Column: created_date, PK=false, AUTO_INCREMENT=false
  Constraint: pk_child_child_id, Type=PRIMARY_KEY, Columns=[child_id]
  ForeignKey: null, References=dba.parent(parent_id)

Table: grandchild
  Column: grandchild_id, PK=true, AUTO_INCREMENT=true
  Column: grandchild_name, PK=false, AUTO_INCREMENT=false
  Column: parent_id, PK=false, AUTO_INCREMENT=false
  Column: child_id, PK=false, AUTO_INCREMENT=false
  Constraint: pk_grandchild_grandchild_id, Type=PRIMARY_KEY, Columns=[grandchild_id]
  ForeignKey: null, References=dba.parent(parent_id)
  ForeignKey: null, References=dba.child(child_id)
</pre>

---

## 🎲 Generator Layer

### RelationshipAwareGenerator
**책임**: 데이터 생성 프로세스 조율 (Orchestrator)

**주요 메서드**:
- `generateFakeData(schema, recordCount)` - 전체 데이터 생성
- `generateTableData(...)` - 특정 테이블 데이터 생성
- `generateTableDataWithGeneratedData(...)` - 생성된 키 값을 사용한 데이터 생성
- `getOrderedTableNames(schema)` - 의존성을 고려한 테이블 순서 반환

**의존성**:
- TopologicalSorter
- BasicValueGenerator
- ForeignKeyValueGenerator
- RelationshipValueGenerator

**특징**:
- 데이터 생성 프로세스 전체를 조율하는 역할
- 각 생성기 클래스로 작업 위임
- 외래키 관계를 만족하는 데이터 생성

**데이터 생성 우선순위**:
1. Relationship 기반
2. Foreign Key 기반
3. 기본 데이터 타입

---

### TopologicalSorter
**책임**: 테이블 위상 정렬

**주요 메서드**:
- `getOrderedTables(schema)` - 의존성을 고려한 테이블 순서 반환
- `topologicalSort(...)` - 위상 정렬 수행

**특징**:
- 외래키 의존성을 기반으로 테이블 생성 순서 결정
- 순환 참조 감지 및 처리

---

### BasicValueGenerator
**책임**: 기본 데이터 타입 값 생성

**주요 메서드**:
- `generate(column, index, table)` - 컬럼 타입에 맞는 데이터 생성
- `generateBasicValue(...)` - 기본 데이터 타입 처리
- `generateUniqueValue(...)` - UNIQUE 제약조건이 있는 컬럼 처리

**생성 가능한 타입**:
- INTEGER, BIGINT, NUMERIC, DECIMAL
- CHARACTER, VARCHAR, STRING
- DATE, DATETIME, TIMESTAMP
- BOOLEAN

**특징**:
- 컬럼의 데이터 타입, 길이, 제약조건을 고려한 데이터 생성
- UNIQUE 제약조건이 있는 컬럼은 중복 방지

---

### ForeignKeyValueGenerator
**책임**: 외래키 값 생성

**주요 메서드**:
- `generateFromFakeData(fk, fakeData, index)` - 가짜 데이터에서 참조
- `generateFromKeys(fk, generatedKeysMap, index)` - 생성된 키에서 참조
- `generateFromData(...)` - 키와 데이터 모두에서 참조

**특징**:
- 참조 테이블의 키 값을 사용하여 외래키 값 생성
- NULL 허용 컬럼 처리

---

### RelationshipValueGenerator
**책임**: 테이블 간 관계 기반 값 생성

**주요 메서드**:
- `generateFromFakeData(...)` - 가짜 데이터에서 관계 처리
- `generateFromKeys(...)` - 생성된 키에서 관계 처리
- `generateFromData(...)` - 키와 데이터 모두에서 관계 처리

**지원하는 관계 타입**:
- `ONE_TO_ONE` - 1:1 관계
- `ONE_TO_MANY` - 1:N 관계
- `MANY_TO_ONE` - N:1 관계
- `MANY_TO_MANY` - N:N 관계 (복합 기본키)

**특징**:
- 관계 타입에 따라 다른 값 생성 전략 적용
- 복합 기본키 테이블 특별 처리

---

## 💾 Repository Layer

### DatabaseInsertRepository
**책임**: 데이터베이스에 데이터 INSERT

**주요 메서드**:
- `insertRecords(tableName, records, schema)` - 레코드 INSERT
- `insertRecordsWithJdbcTemplate(...)` - 동적 JdbcTemplate 사용한 INSERT
- `insertAllRecords(...)` - 전체 테이블 데이터 INSERT

**특징**:
- AUTO_INCREMENT 컬럼 자동 처리
- getGeneratedKeys()로 실제 생성된 키 값 반환
- 배치 INSERT 지원 (성능 최적화)
- 트랜잭션 관리

---

## 🚀 Service Layer

### SchemaAnalysisService
**책임**: 스키마 분석 및 파싱 서비스

**주요 메서드**:
- `parseSchema(schemaText, keepSchemaName)` - 스키마 파싱

**의존성**:
- CubridSchemaParser

---

### DataGenerationService
**책임**: 가짜 데이터 생성 및 데이터베이스 INSERT 조율

**주요 메서드**:
- `generateAndInsertData(...)` - 데이터 생성 및 INSERT
- `testConnection(dbInfo)` - 연결 테스트
- `createDynamicDataSource(dbInfo)` - 동적 데이터소스 생성

**의존성**:
- RelationshipAwareGenerator
- DatabaseInsertRepository

**처리 흐름**:
1. 동적 데이터베이스 연결
2. 위상 정렬된 테이블 순서로 데이터 생성
3. 실제 생성된 키 값 추적
4. 트랜잭션 관리 (커밋/롤백)
5. 결과 반환

---

## 📈 데이터 흐름

```
1. 사용자 입력 (스키마 텍스트)
   ↓
2. SchemaController
   ↓
3. SchemaAnalysisService
   ↓
4. CubridSchemaParser
   ├─ TableExtractor
   ├─ ColumnExtractor
   ├─ PrimaryKeyExtractor
   ├─ ForeignKeyExtractor
   ├─ UniqueConstraintExtractor
   ├─ TableBuilder
   └─ RelationshipAnalyzer
   ↓
5. DatabaseSchema (완성)
   ↓
6. DataGenerationService
   ├─ RelationshipAwareGenerator (데이터 생성 조율)
   │   ├─ TopologicalSorter (위상 정렬)
   │   ├─ BasicValueGenerator (기본 데이터 생성)
   │   ├─ ForeignKeyValueGenerator (외래키 값 생성)
   │   └─ RelationshipValueGenerator (관계 값 생성)
   └─ DatabaseInsertRepository (INSERT)
   ↓
7. 결과 반환
```

---

## 🧪 Test Layer

### TestSchemaConstants
**책임**: 테스트용 스키마 상수 정의

**상수**:
- `FULL_SCHEMA` - 전체 CUBRID 스키마 (6개 테이블)
- `SIMPLE_SCHEMA` - 간단한 스키마

### Extractor Tests
- `TableExtractorTest`
- `ColumnExtractorTest`
- `PrimaryKeyExtractorTest`
- `ForeignKeyExtractorTest`
- `UniqueConstraintExtractorTest`

모든 테스트는 `TestSchemaConstants.FULL_SCHEMA`를 사용하여 일관성 유지

---

## 🎨 설계 원칙

### Single Responsibility Principle (SRP)
각 클래스는 하나의 책임만 가짐:
- Extractor: 정보 추출만
- Analyzer: 분석만
- Builder: 조립만
- Generator: 생성만

### Dependency Injection
Spring의 `@Autowired`를 통한 의존성 주입

### Strategy Pattern
Relationship 타입별 다른 데이터 생성 전략

### Builder Pattern
TableBuilder를 통한 복잡한 객체 조립

---

## 📝 주요 정규식 패턴

| 항목 | 패턴 |
|------|------|
| 테이블 | `CREATE\s+CLASS\s+\[([^\]]+)\]\.\[([^\]]+)\]` |
| 컬럼 | `ALTER\s+CLASS\s+\[(.*?)\]\.\[(.*?)\]\s+ADD\s+ATTRIBUTE` |
| 기본키 | `CONSTRAINT\s+\[(.*?)\]\s+PRIMARY\s+KEY\((.*?)\)` |
| 외래키 | `FOREIGN\s+KEY\s*\(([^)]*)\).*?REFERENCES\s+\[(.*?)\]\.\[(.*?)\]` |
| UNIQUE | `CONSTRAINT\s+\[(.*?)\]\s+UNIQUE\(([^)]*)\)` |

---

## 🔄 테이블 생성 순서 (위상 정렬)

```
a (독립)
  ↓
b (a에 의존)
  ↓
c (b에 의존)

parent (독립)
  ↓
child (parent에 의존)
  ↓
grandchild (parent, child에 의존)
```

이 순서를 지켜야 외래키 제약조건을 만족하면서 데이터를 생성할 수 있습니다.

