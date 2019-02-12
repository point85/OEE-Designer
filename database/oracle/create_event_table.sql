-- Oracle script file for creating the database event interface table, schema version 2
-- set to your schema and tablespace names
--------------------------------------------------------
--  DDL for Table DB_EVENT
--------------------------------------------------------
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE "SYSTEM"."DB_EVENT"';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/
CREATE TABLE "SYSTEM"."DB_EVENT" 
   (	
    "EVENT_KEY" NUMBER(19,0) GENERATED ALWAYS AS IDENTITY, 
	"SOURCE_ID" NVARCHAR2(128) NOT NULL,
	"IN_VALUE" NVARCHAR2(64) NOT NULL,
	"TIME" TIMESTAMP(3) NULL, 
	"EVENT_TIME_OFFSET" NUMBER(10,0) NULL,
	"STATUS" NVARCHAR2(16) NOT NULL,
	"ERROR" NVARCHAR2(512) NULL,
	PRIMARY KEY (EVENT_KEY)
   ) TABLESPACE "SYSTEM" ;   
/
CREATE INDEX "SYSTEM"."IDX_EVT_STATUS" ON "SYSTEM"."DB_EVENT" ("STATUS") TABLESPACE "SYSTEM" ;
/  