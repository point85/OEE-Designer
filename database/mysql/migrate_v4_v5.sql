-- MySQL schema migration script file, schema version 4 to 5
-- set to your database name
USE oee;

/****** BREAK table ******/
DROP TABLE IF EXISTS BREAK;

CREATE TABLE BREAK (
	BREAK_KEY bigint AUTO_INCREMENT,
	NAME varchar(64) NULL,
	DESCRIPTION varchar(128) NULL,
	START_TIME time NULL,
	DURATION bigint NULL,
	SHIFT_KEY bigint NULL,
	LOSS varchar(32) NULL,
	PRIMARY KEY (BREAK_KEY)
)  ENGINE=INNODB;