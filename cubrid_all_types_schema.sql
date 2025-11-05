-- CUBRID 11.4 모든 데이터 타입을 포함한 테스트 스키마
-- 메인 테이블: 모든 데이터 타입 포함
-- 외래키 테이블: 메인 테이블을 참조

CREATE CLASS [dba].[all_types_main] REUSE_OID, COLLATE utf8_bin;

CREATE CLASS [dba].[all_types_ref] REUSE_OID, COLLATE utf8_bin;

-- 메인 테이블: 모든 CUBRID 11.4 데이터 타입 포함
ALTER CLASS [dba].[all_types_main] ADD ATTRIBUTE
       [id] integer AUTO_INCREMENT(1, 1) NOT NULL,
       [col_smallint] smallint,
       [col_integer] integer,
       [col_bigint] bigint,
       [col_numeric] numeric(10, 2),
       [col_decimal] decimal(15, 5),
       [col_float] float,
       [col_double] double,
       [col_char] character(50) COLLATE utf8_bin,
       [col_varchar] character varying(100) COLLATE utf8_bin,
       [col_string] string,
       [col_date] date,
       [col_time] time,
       [col_datetime] datetime,
       [col_timestamp] timestamp,
       [col_boolean] boolean,
       [col_bit] bit(8),
       [col_bit_varying] bit varying(16),
       [col_text] string;

ALTER CLASS [dba].[all_types_main] ADD ATTRIBUTE
       CONSTRAINT [pk_all_types_main_id] PRIMARY KEY([id]);

-- 외래키 테이블: 메인 테이블 참조
ALTER CLASS [dba].[all_types_ref] ADD ATTRIBUTE
       [ref_id] integer AUTO_INCREMENT(1, 1) NOT NULL,
       [main_id] integer NOT NULL,
       [ref_name] character varying(100) COLLATE utf8_bin,
       [ref_description] string,
       [ref_created] datetime;

ALTER CLASS [dba].[all_types_ref] ADD ATTRIBUTE
       CONSTRAINT [pk_all_types_ref_ref_id] PRIMARY KEY([ref_id]);

ALTER CLASS [dba].[all_types_ref] ADD CONSTRAINT [fk_all_types_ref_main] 
       FOREIGN KEY([main_id]) WITH DEDUPLICATE=0 
       REFERENCES [dba].[all_types_main]([id]) 
       ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER SERIAL [dba.all_types_main_ai_id] START WITH 1;
ALTER SERIAL [dba.all_types_ref_ai_ref_id] START WITH 1;

COMMIT WORK;

