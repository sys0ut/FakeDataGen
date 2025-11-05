package com.example.fakedatagen.parser;

/**
 * 테스트용 스키마 상수 클래스
 * 모든 파서 테스트에서 공통으로 사용할 CUBRID 스키마 정의
 */
public class TestSchemaConstants {
    
    /**
     * 테스트용 전체 CUBRID 스키마
     * 6개 테이블 (a, b, c, parent, child, grandchild)과 관계를 포함
     */
    public static final String FULL_SCHEMA = """
CREATE CLASS [dba].[a] REUSE_OID, COLLATE utf8_bin;

CREATE CLASS [dba].[b] REUSE_OID, COLLATE utf8_bin;

CREATE CLASS [dba].[c] REUSE_OID, COLLATE utf8_bin;

CREATE CLASS [dba].[parent] REUSE_OID, COLLATE utf8_bin;

CREATE CLASS [dba].[child] REUSE_OID, COLLATE utf8_bin;

CREATE CLASS [dba].[grandchild] REUSE_OID, COLLATE utf8_bin;

ALTER CLASS [dba].[a] ADD ATTRIBUTE
       [id] integer AUTO_INCREMENT(1, 1) NOT NULL;
ALTER CLASS [dba].[a] ADD ATTRIBUTE
       CONSTRAINT [pk_a_id] PRIMARY KEY([id]);

ALTER CLASS [dba].[b] ADD ATTRIBUTE
       [id] integer AUTO_INCREMENT(1, 1) NOT NULL,
       [a_id] integer NOT NULL;
ALTER CLASS [dba].[b] ADD ATTRIBUTE
       CONSTRAINT [pk_b_id] PRIMARY KEY([id]);

ALTER CLASS [dba].[c] ADD ATTRIBUTE
       [id] integer AUTO_INCREMENT(1, 1) NOT NULL,
       [b_id] integer NOT NULL;
ALTER CLASS [dba].[c] ADD ATTRIBUTE
       CONSTRAINT [pk_c_id] PRIMARY KEY([id]);

ALTER CLASS [dba].[parent] ADD ATTRIBUTE
       [parent_id] character varying(10) COLLATE utf8_bin NOT NULL,
       [name] character varying(50) COLLATE utf8_bin NOT NULL,
       [description] character varying(200) COLLATE utf8_bin;
ALTER CLASS [dba].[parent] ADD ATTRIBUTE
       CONSTRAINT [pk_parent_parent_id] PRIMARY KEY([parent_id]);

ALTER CLASS [dba].[child] ADD ATTRIBUTE
       [child_id] integer AUTO_INCREMENT(1, 1) NOT NULL,
       [child_name] character varying(50) COLLATE utf8_bin NOT NULL,
       [parent_id] character varying(10) COLLATE utf8_bin NOT NULL,
       [created_date] datetime;
ALTER CLASS [dba].[child] ADD ATTRIBUTE
       CONSTRAINT [pk_child_child_id] PRIMARY KEY([child_id]);

ALTER CLASS [dba].[grandchild] ADD ATTRIBUTE
       [grandchild_id] integer AUTO_INCREMENT(1, 1) NOT NULL,
       [grandchild_name] character varying(50) COLLATE utf8_bin NOT NULL,
       [parent_id] character varying(10) COLLATE utf8_bin NOT NULL,
       [child_id] integer NOT NULL;
ALTER CLASS [dba].[grandchild] ADD ATTRIBUTE
       CONSTRAINT [pk_grandchild_grandchild_id] PRIMARY KEY([grandchild_id]);

ALTER SERIAL [dba.a_ai_id] START WITH 2600;
SELECT [dba.a_ai_id].NEXT_VALUE;
 
ALTER SERIAL [dba.b_ai_id] START WITH 1009;
SELECT [dba.b_ai_id].NEXT_VALUE;
 
ALTER SERIAL [dba.c_ai_id] START WITH 1000;
SELECT [dba.c_ai_id].NEXT_VALUE;
 
ALTER SERIAL [dba.child_ai_child_id] START WITH 1;

ALTER SERIAL [dba.grandchild_ai_grandchild_id] START WITH 1;

ALTER CLASS [dba].[b] ADD CONSTRAINT [fk_b_a] FOREIGN KEY([a_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[a] ON DELETE RESTRICT ON UPDATE RESTRICT ;

ALTER CLASS [dba].[c] ADD CONSTRAINT [fk_c_b] FOREIGN KEY([b_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[b] ON DELETE RESTRICT ON UPDATE RESTRICT ;

ALTER CLASS [dba].[child] ADD CONSTRAINT [fk_child_parent] FOREIGN KEY([parent_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[parent] ON DELETE RESTRICT ON UPDATE RESTRICT ;

ALTER CLASS [dba].[grandchild] ADD CONSTRAINT [fk_grandchild_parent] FOREIGN KEY([parent_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[parent] ON DELETE RESTRICT ON UPDATE RESTRICT ;

ALTER CLASS [dba].[grandchild] ADD CONSTRAINT [fk_grandchild_child] FOREIGN KEY([child_id]) WITH DEDUPLICATE=0 REFERENCES [dba].[child] ON DELETE RESTRICT ON UPDATE RESTRICT ;

COMMIT WORK;
""";
    
    /**
     * 테스트용 테이블만 포함하는 간단한 스키마
     */
    public static final String SIMPLE_SCHEMA = """
CREATE CLASS [dba].[a] REUSE_OID, COLLATE utf8_bin;
CREATE CLASS [dba].[b] REUSE_OID, COLLATE utf8_bin;
CREATE CLASS [dba].[c] REUSE_OID, COLLATE utf8_bin;
""";
}

