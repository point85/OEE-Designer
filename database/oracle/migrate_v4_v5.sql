-- Oracle migration script file, schema version 4 to 5
-- set to your schema and tablespace names

--------------------------------------------------------
--  DDL for Table BREAK
--------------------------------------------------------
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE "SYSTEM"."BREAK"';
EXCEPTION
   WHEN OTHERS THEN
      IF SQLCODE != -942 THEN
         RAISE;
      END IF;
END;
/
CREATE TABLE "SYSTEM"."BREAK" 
   (	
    "BREAK_KEY" NUMBER(19,0) GENERATED ALWAYS AS IDENTITY,   
	"NAME" NVARCHAR2(64) NULL, 
	"DESCRIPTION" NVARCHAR2(128) NULL, 
	"START_TIME" TIMESTAMP(3) NULL, 
	"DURATION" NUMBER(19,0) NULL, 
	"SHIFT_KEY" NUMBER(19,0) NULL, 
	"LOSS" NVARCHAR2(32) NULL,
	PRIMARY KEY (BREAK_KEY)
   ) TABLESPACE "SYSTEM" ;
/