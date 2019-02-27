-- SQL Server script file for creating the database event interface table, schema version 3
-- set to your database name
USE [OEE]
GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

/****** Database event table ******/
IF OBJECT_ID('dbo.DB_EVENT', 'U') IS NOT NULL 
  DROP TABLE dbo.DB_EVENT
GO

CREATE TABLE [dbo].[DB_EVENT](
	[EVENT_KEY] [bigint] IDENTITY(1,1) NOT NULL,
	[SOURCE_ID] [nvarchar](128) NOT NULL,
	[IN_VALUE] [nvarchar](64) NOT NULL,
	[EVENT_TIME] [datetime2](3) NULL,
	[EVENT_TIME_OFFSET] int NULL,
	[STATUS] [nvarchar](16) NOT NULL,
	[ERROR] [nvarchar](512) NULL,
	[REASON] [nvarchar](64) NULL
) ON [PRIMARY]
GO
CREATE NONCLUSTERED INDEX [IDX_EVT_STATUS] ON [dbo].[DB_EVENT]
(
	[STATUS] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
GO