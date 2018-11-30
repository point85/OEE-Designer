-- Oracle script file for creating the database event interface table
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
    "EVENT_KEY" NUMBER(19,0), 
	"SOURCE_ID" NVARCHAR2(256),
	"IN_VALUE" NVARCHAR2(128),
	"TIME" TIMESTAMP(3) WITH TIME ZONE, 
	"STATUS" NVARCHAR2(32),
	"ERROR" NVARCHAR2(512)
   ) TABLESPACE "SYSTEM" ;
/
CREATE UNIQUE INDEX "SYSTEM"."EVENT_PK" ON "SYSTEM"."DB_EVENT" ("EVENT_KEY") TABLESPACE "SYSTEM" ;   
/
CREATE UNIQUE INDEX "SYSTEM"."IDX_STATUS" ON "SYSTEM"."DB_EVENT" ("STATUS") TABLESPACE "SYSTEM" ;
/  