-- SQL Server migration script, schema version 2 to 3 as of release 2.5.0
-- set to your database name
USE [MY_DB]
GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

alter table [dbo].[COLLECTOR] add [BROKER_TYPE] [nvarchar](16) NULL
GO

alter table [dbo].[PLANT_ENTITY] drop column WS_KEY
GO

alter table [dbo].[OEE_EVENT] add [COLLECTOR] [nvarchar](64) NULL
GO

/****** Plant Entity Work Schedule table ******/
IF OBJECT_ID('dbo.ENTITY_SCHEDULE', 'U') IS NOT NULL 
  DROP TABLE dbo.ENTITY_SCHEDULE
GO

CREATE TABLE [dbo].[ENTITY_SCHEDULE](
	[ES_KEY] [bigint] IDENTITY(1,1) NOT NULL,
	[ENT_KEY] [bigint] NULL,
	[WS_KEY] [bigint] NULL,
	[START_DATE_TIME] [datetime] NULL,
	[END_DATE_TIME] [datetime] NULL
	CONSTRAINT [PK_ENT_WS] PRIMARY KEY CLUSTERED 
(
	[ES_KEY] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO