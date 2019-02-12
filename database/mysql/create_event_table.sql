-- MySQL script file for creating the database event interface table, schema version 2
-- set to your database name
USE oee;

/****** Database event table ******/
DROP TABLE IF EXISTS DB_EVENT;

CREATE TABLE DB_EVENT (
	EVENT_KEY bigint AUTO_INCREMENT,
	SOURCE_ID varchar(128) NOT NULL,
	IN_VALUE varchar(64) NOT NULL,
	EVENT_TIME timestamp(3) NULL,
	EVENT_TIME_OFFSET int NULL,
	STATUS varchar(16) NOT NULL,
	ERROR varchar(512) NULL,
    PRIMARY KEY (EVENT_KEY)
)  ENGINE=INNODB;
CREATE INDEX IDX_EVT_STATUS ON DB_EVENT (STATUS);		