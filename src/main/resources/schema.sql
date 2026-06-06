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
    [max_chats]             SMALLINT        NOT NULL,
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
    [created_at]         DATETIME2(0)    NOT NULL,
    [updated_at]         DATETIME2(0)    NOT NULL,
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
    [id]         SMALLINT        NOT NULL,
    [letter]     NVARCHAR(10)    NOT NULL,
    [name_zh_tw] NVARCHAR(50)    NOT NULL,
    [name_en]    NVARCHAR(100)   NOT NULL,
    [color]      NVARCHAR(20)    NOT NULL,
    [status]     TINYINT         NOT NULL,
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
    [id]              INT             NOT NULL,
    [name_zh_tw]      NVARCHAR(100)   NOT NULL,
    [name_en]         NVARCHAR(200)   NOT NULL,
    [status]          TINYINT         NOT NULL,
    [atm]             NVARCHAR(100)   NULL,
    [nursing_room]    NVARCHAR(100)   NULL,
    [diaper_table]    NVARCHAR(100)   NULL,
    [charging_station] NVARCHAR(100)  NULL,
    [ticket_machine]  NVARCHAR(100)   NULL,
    [locker]          NVARCHAR(100)   NULL,
    [drinking_water]  NVARCHAR(100)   NULL,
    [restroom]        NVARCHAR(100)   NULL,
    [created_at]      DATETIME2(0)    NOT NULL,
    [updated_at]      DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_stations] PRIMARY KEY ([id])
);

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

-- station_exits
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'station_exits' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[station_exits] (
    [id]         INT             NOT NULL IDENTITY(1, 1),
    [station_id] INT             NOT NULL,
    [name_zh_tw] NVARCHAR(100)   NOT NULL,
    [name_en]    NVARCHAR(200)   NOT NULL,
    [elevator]   NVARCHAR(100)   NULL,
    [escalator]  NVARCHAR(100)   NULL,
    [status]     TINYINT         NOT NULL,
    [updated_at] DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_station_exits] PRIMARY KEY ([id]),
    CONSTRAINT [FK_station_exits_station_id]
        FOREIGN KEY ([station_id]) REFERENCES [dbo].[stations] ([id])
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
    [cumulative_distance] DECIMAL(8, 2)   NOT NULL,
    [cumulative_time]     SMALLINT        NOT NULL,
    [updated_at]          DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_lines_stations] PRIMARY KEY ([id]),
    CONSTRAINT [UK_lines_stations_line_sequence] UNIQUE ([line_id], [station_sequence]),
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
    [user_id]    INT             NOT NULL,
    [station_id] INT             NOT NULL,
    [created_at] DATETIME2(0)    NOT NULL,
    [deleted_at] DATETIME2(0)    NULL,
    CONSTRAINT [PK_user_station_bookmarks] PRIMARY KEY ([id]),
    CONSTRAINT [UK_user_station_bookmarks_user_station] UNIQUE ([user_id], [station_id]),
    CONSTRAINT [FK_user_station_bookmarks_user_id]
        FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id]),
    CONSTRAINT [FK_user_station_bookmarks_station_id]
        FOREIGN KEY ([station_id]) REFERENCES [dbo].[stations] ([id])
);

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

-- user_chat_messages
IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE [name] = 'user_chat_messages' AND [schema_id] = SCHEMA_ID('dbo')
)
CREATE TABLE [dbo].[user_chat_messages] (
    [id]          INT             NOT NULL IDENTITY(1, 1),
    [user_id]     INT             NOT NULL,
    [chat_type]   TINYINT         NOT NULL,
    [sender_type] TINYINT         NOT NULL,
    [content]     NVARCHAR(2000)  NOT NULL,
    [created_at]  DATETIME2(0)    NOT NULL,
    CONSTRAINT [PK_user_chat_messages] PRIMARY KEY ([id]),
    CONSTRAINT [FK_user_chat_messages_user_id]
        FOREIGN KEY ([user_id]) REFERENCES [dbo].[users] ([id])
);
