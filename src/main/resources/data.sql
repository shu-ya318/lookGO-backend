-- membership_tiers
IF NOT EXISTS (SELECT 1 FROM [dbo].[membership_tiers] WHERE [id] = 1)
    INSERT INTO [dbo].[membership_tiers]
        ([id], [name], [max_station_bookmarks], [max_trip_plans], [max_chats], [updated_at])
    VALUES
        (1, N'BASIC',   20,  10, 50,  SYSDATETIME());

IF NOT EXISTS (SELECT 1 FROM [dbo].[membership_tiers] WHERE [id] = 2)
    INSERT INTO [dbo].[membership_tiers]
        ([id], [name], [max_station_bookmarks], [max_trip_plans], [max_chats], [updated_at])
    VALUES
        (2,  N'PREMIUM', 100, 20, 500, SYSDATETIME());

-- roles
IF NOT EXISTS (SELECT 1 FROM [dbo].[roles] WHERE [id] = 1)
    INSERT INTO [dbo].[roles] ([id], [name], [updated_at])
    VALUES (1, N'USER',  SYSDATETIME());

IF NOT EXISTS (SELECT 1 FROM [dbo].[roles] WHERE [id] = 2)
    INSERT INTO [dbo].[roles] ([id], [name], [updated_at])
    VALUES (2, N'ADMIN', SYSDATETIME());
