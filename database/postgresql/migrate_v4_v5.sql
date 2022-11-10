-- PostgresQL schema migration script file, schema version 4 to 5

/****** BREAK table ******/
DROP TABLE IF EXISTS BREAK;

CREATE TABLE BREAK (
	BREAK_KEY bigint GENERATED ALWAYS AS IDENTITY,
	NAME varchar(64) NULL,
	DESCRIPTION varchar(128) NULL,
	START_TIME time(3) NULL,
	DURATION bigint NULL,
	SHIFT_KEY bigint NULL,
	LOSS varchar(32) NULL,
	CONSTRAINT PK_BREAK_KEY PRIMARY KEY(BREAK_KEY)
);