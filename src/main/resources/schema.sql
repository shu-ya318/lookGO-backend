-- membership_tiers
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'membership_tiers' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[membership_tiers] (
    [id]                    TINYINT         NOT NULL,
    [name]                  NVARCHAR(50)    NOT NULL,
    [max_station_bookmarks] SMALLINT        NOT NULL,
    [max_trip_plans]        SMALLINT        NOT NULL,
    [max_daily_chats]     SMALLINT        NOT NULL,  -- 用"每日"計算訊息總量上限
    [updated_at]            DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_membership_tiers] PRIMARY KEY ([id]),
    CONSTRAINT [UK_membership_tiers_name] UNIQUE ([name])
);

-- roles
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'roles' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[roles] (
    [id]         TINYINT         NOT NULL,
    [name]       NVARCHAR(50)    NOT NULL,
    [updated_at] DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_roles] PRIMARY KEY ([id]),
    CONSTRAINT [UK_roles_name] UNIQUE ([name])
);

-- users
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'users' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[users] (
    [id]                 INT             NOT NULL IDENTITY(1, 1),
    [membership_tier_id] TINYINT         NOT NULL,
    [role_id]            TINYINT         NOT NULL,
    [email]              NVARCHAR(254)   NOT NULL,
    [password]           NVARCHAR(255)   NOT NULL,
    [username]           NVARCHAR(100)   NOT NULL,
    [birth_date]         DATE            NULL,
    [cellphone]          NVARCHAR(20)    NOT NULL,
    [status]             TINYINT         NOT NULL,
    [created_at]         DATETIME2(0)    NOT NULL,
    [updated_at]         DATETIME2(0)    NOT NULL,
    [last_login_at]      DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_users] PRIMARY KEY ([id]),
    CONSTRAINT [UK_users_email] UNIQUE ([email]),
    CONSTRAINT [FK_users_membership_tier_id]
        FOREIGN KEY ([membership_tier_id]) REFERENCES [dbo].[membership_tiers] ([id]),
    CONSTRAINT [FK_users_role_id]
        FOREIGN KEY ([role_id]) REFERENCES [dbo].[roles] ([id])
);

-- lines
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'lines' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[lines] (
    [id]         SMALLINT        NOT NULL IDENTITY(1, 1),
    [letter]     NVARCHAR(10)    NOT NULL,
    [name_zh_tw] NVARCHAR(50)    NOT NULL,
    [name_en]    NVARCHAR(100)   NOT NULL,
    [color]      NVARCHAR(20)    NOT NULL,
    [updated_at] DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_lines] PRIMARY KEY ([id]),
    CONSTRAINT [UK_lines_letter] UNIQUE ([letter])
);

-- stations
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'stations' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[stations] (
    [id]              INT             NOT NULL IDENTITY(1, 1),
    [name_zh_tw]      NVARCHAR(100)   NOT NULL,
    [name_en]         NVARCHAR(200)   NOT NULL,
    [atm]             NVARCHAR(1000)  NULL,
    [nursing_room]    NVARCHAR(1000)  NULL,
    [diaper_table]    NVARCHAR(1000)  NULL,
    [charging_station] NVARCHAR(1000) NULL,
    [ticket_machine]  NVARCHAR(1000)  NULL,
    [locker]          NVARCHAR(1000)  NULL,
    [drinking_water]  NVARCHAR(1000)  NULL,
    [restroom]        NVARCHAR(1000)  NULL,
    [elevator]        NVARCHAR(1000)  NULL,
    [escalator]       NVARCHAR(1000)  NULL,
    [updated_at]      DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_stations] PRIMARY KEY ([id])
);

-- stations.original_name_zh_tw：僅供同步比對，任何管理端 API 皆不可寫入此欄位
-- 註：拆成多個單一陳述式（而非 BEGIN...END 區塊），因 Spring 的 schema.sql 執行器僅以「;」切割陳述式，
-- 不解析 T-SQL 的 BEGIN...END，區塊寫法會被攔腰切斷造成語法錯誤
IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.stations') AND [name] = 'original_name_zh_tw'
)
ALTER TABLE [dbo].[stations] ADD [original_name_zh_tw] NVARCHAR(100) NULL;

UPDATE [dbo].[stations] SET [original_name_zh_tw] = [name_zh_tw] WHERE [original_name_zh_tw] IS NULL;

IF EXISTS (
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID('dbo.stations') AND [name] = 'original_name_zh_tw' AND [is_nullable] = 1
)
ALTER TABLE [dbo].[stations] ALTER COLUMN [original_name_zh_tw] NVARCHAR(100) NOT NULL;

-- station_fares
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'station_fares' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[station_fares] (
    [id]              INT             NOT NULL IDENTITY(1, 1),
    [from_station_id] INT             NOT NULL,
    [to_station_id]   INT             NOT NULL,
    [fare_type]       TINYINT         NOT NULL,
    [price]           NUMERIC(5, 2)   NOT NULL,
    [updated_at]      DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_station_fares] PRIMARY KEY ([id]),
    CONSTRAINT [FK_station_fares_from_station_id]
        FOREIGN KEY ([from_station_id]) REFERENCES [dbo].[stations] ([id]),
    CONSTRAINT [FK_station_fares_to_station_id]
        FOREIGN KEY ([to_station_id]) REFERENCES [dbo].[stations] ([id])
);

-- lines_stations
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'lines_stations' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[lines_stations] (
    [id]                  INT             NOT NULL IDENTITY(1, 1),
    [line_id]             SMALLINT        NOT NULL,
    [station_id]          INT             NOT NULL,
    [station_sequence]    SMALLINT        NOT NULL,
    [station_code]        NVARCHAR(20)    NOT NULL,
    [cumulative_distance] DECIMAL(8, 2)   NULL,
    [cumulative_time]     SMALLINT        NULL,
    [updated_at]          DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_lines_stations] PRIMARY KEY ([id]),
    CONSTRAINT [UK_lines_stations_line_sequence] UNIQUE ([line_id], [station_sequence]),
    CONSTRAINT [UK_lines_stations_station_code] UNIQUE ([station_code]),
    CONSTRAINT [FK_lines_stations_line_id]
        FOREIGN KEY ([line_id]) REFERENCES [dbo].[lines] ([id]),
    CONSTRAINT [FK_lines_stations_station_id]
        FOREIGN KEY ([station_id]) REFERENCES [dbo].[stations] ([id])
);

-- lines_transfers
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'lines_transfers' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[lines_transfers] (
    [id]                   INT             NOT NULL IDENTITY(1, 1),
    [from_line_station_id] INT             NOT NULL,
    [to_line_station_id]   INT             NOT NULL,
    [transfer_time]        SMALLINT        NOT NULL,
    [updated_at]           DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_lines_transfers] PRIMARY KEY ([id]),
    CONSTRAINT [FK_lines_transfers_from_line_station_id]
        FOREIGN KEY ([from_line_station_id]) REFERENCES [dbo].[lines_stations] ([id]),
    CONSTRAINT [FK_lines_transfers_to_line_station_id]
        FOREIGN KEY ([to_line_station_id]) REFERENCES [dbo].[lines_stations] ([id])
);

-- user_station_bookmarks
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'user_station_bookmarks' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[user_station_bookmarks] (
    [id]         INT             NOT NULL IDENTITY(1, 1),
    [station_id] INT             NOT NULL,
    [user_id]    INT             NOT NULL,
    [created_at] DATETIME2(0)    NOT NULL,
    [deleted_at] DATETIME2(0)    NULL,
    CONSTRAINT [PK_user_station_bookmarks] PRIMARY KEY ([id]),
    CONSTRAINT [FK_user_station_bookmarks_user_id]
        FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_user_station_bookmarks_station_id]
        FOREIGN KEY ([station_id]) REFERENCES [dbo].[stations] ([id])
);

-- Create Filtered Index for unique active bookmarks (handling soft deletes)
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'UK_user_station_bookmarks_active' AND object_id = OBJECT_ID('dbo.user_station_bookmarks')
)
CREATE UNIQUE NONCLUSTERED INDEX [UK_user_station_bookmarks_active]
    ON [dbo].[user_station_bookmarks] ([user_id], [station_id])
    WHERE [deleted_at] IS NULL;


-- user_trip_plans
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'user_trip_plans' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[user_trip_plans] (
    [id]               INT             NOT NULL IDENTITY(1, 1),
    [user_id]          INT             NOT NULL,
    [from_station_id]  INT             NOT NULL,
    [to_station_id]    INT             NOT NULL,
    [fare_type]        TINYINT         NOT NULL,
    [fare_price]       NUMERIC(5, 2)   NOT NULL,
    [transfer_count]   SMALLINT        NOT NULL,
    [routing_strategy] TINYINT         NOT NULL,
    [notes]            NVARCHAR(2000)  NULL,
    [created_at]       DATETIME2(0)    NOT NULL,
    [deleted_at]       DATETIME2(0)    NULL,
    CONSTRAINT [PK_user_trip_plans] PRIMARY KEY ([id]),
    CONSTRAINT [FK_user_trip_plans_user_id]
        FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_user_trip_plans_from_station_id]
        FOREIGN KEY ([from_station_id]) REFERENCES [dbo].[stations] ([id]),
    CONSTRAINT [FK_user_trip_plans_to_station_id]
        FOREIGN KEY ([to_station_id]) REFERENCES [dbo].[stations] ([id])
);

-- station_chat_messages
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'station_chat_messages' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[station_chat_messages] (
    [id]          INT             NOT NULL IDENTITY(1, 1),
    [station_id]  INT             NOT NULL,
    [user_id]     INT             NOT NULL,
    [trip_plan_id] INT             NULL,   -- 旅程分享時才關聯
    [chat_type]   TINYINT         NOT NULL, -- 分成1.手動輸入文字 2.旅程分享
    [content]     NVARCHAR(1000)  NULL, -- 只儲存手動輸入文字  -- 旅程分享直接關聯該表，允 NULL
    [created_at]  DATETIME2(0)    NOT NULL,
    [deleted_at]  DATETIME2(0)    NULL,
    CONSTRAINT [PK_station_chat_messages] PRIMARY KEY ([id]),
    CONSTRAINT [FK_station_chat_messages_station_id]
        FOREIGN KEY ([station_id]) REFERENCES [dbo].[stations] ([id]),
    CONSTRAINT [FK_station_chat_messages_user_id]
        FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_station_chat_messages_trip_plan_id]
        FOREIGN KEY ([trip_plan_id]) REFERENCES [dbo].[user_trip_plans] ([id])
);

-- station_chat_announcements
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'station_chat_announcements' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[station_chat_announcements] (
    [id]          INT             NOT NULL IDENTITY(1, 1),
    [station_id]  INT             NOT NULL,
    [content]     NVARCHAR(1000)  NOT NULL,
    [created_by]  INT             NOT NULL,  -- 建立的 ADMIN user_id -- 不命名成 admin_id
    [created_at]  DATETIME2(0)    NOT NULL,
    [updated_at]  DATETIME2(0)    NOT NULL,
    [deleted_at]  DATETIME2(0)    NULL,
    CONSTRAINT [PK_station_chat_announcements] PRIMARY KEY ([id]),
    CONSTRAINT [FK_station_chat_announcements_station_id]
        FOREIGN KEY ([station_id]) REFERENCES [dbo].[stations] ([id]),
    CONSTRAINT [FK_station_chat_announcements_created_by]
        FOREIGN KEY ([created_by]) REFERENCES [dbo].[users] ([id])
);

-- Create composite index for paginated announcement queries by station (handling soft deletes)
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_station_chat_announcements_station_created' AND object_id = OBJECT_ID('dbo.station_chat_announcements')
)
CREATE NONCLUSTERED INDEX [IX_station_chat_announcements_station_created]
    ON [dbo].[station_chat_announcements] ([station_id], [created_at] DESC)
    WHERE [deleted_at] IS NULL;
