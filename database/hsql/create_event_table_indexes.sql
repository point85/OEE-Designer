-- HSQLDB script file for creating the database event interface index, schema version 1
-- set to your schema
SET AUTOCOMMIT TRUE;

CREATE INDEX IDX_EVT_STATUS ON DB_EVENT (STATUS);	