-- SQL Server migration script, schema version 4 to 5
-- set to your database name
USE [MY_DB]
GO

SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

SET ANSI_PADDING ON
GO

/****** BREAK table ******/
IF OBJECT_ID('dbo.BREAK', 'U') IS NOT NULL 
  DROP TABLE dbo.BREAK; 
GO

CREATE TABLE [dbo].[BREAK](
	[BREAK_KEY] [bigint] IDENTITY(1,1) NOT NULL,
	[NAME] [nvarchar](64) NULL,
	[DESCRIPTION] [nvarchar](128) NULL,
	[START_TIME] [time](3) NULL,
	[DURATION] [bigint] NULL,
	[SHIFT_KEY] [bigint] NULL,
	[LOSS] [nvarchar](32) NULL
	CONSTRAINT [PK_BREAK_PERIOD] PRIMARY KEY CLUSTERED 
(
	[BREAK_KEY] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO