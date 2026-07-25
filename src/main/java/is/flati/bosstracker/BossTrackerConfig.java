package is.flati.bosstracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("flatibosstracker")
public interface BossTrackerConfig extends Config
{
	@ConfigItem(
		keyName = "bossKillEndpoint",
		name = "Boss kill endpoint",
		description = "URL for POST /api/osrs/boss-kill"
	)
	default String bossKillEndpoint()
	{
		return "https://flati.is/api/osrs/boss-kill";
	}

	@ConfigItem(
		keyName = "kcUpdateEndpoint",
		name = "KC update endpoint",
		description = "URL for POST /api/osrs/kc-update"
	)
	default String kcUpdateEndpoint()
	{
		return "https://flati.is/api/osrs/kc-update";
	}

	@ConfigItem(
		keyName = "kcSyncEndpoint",
		name = "KC sync endpoint",
		description = "URL for POST /api/osrs/kc-sync"
	)
	default String kcSyncEndpoint()
	{
		return "https://flati.is/api/osrs/kc-sync";
	}

	@ConfigItem(
		keyName = "apiKey",
		name = "API key",
		description = "Shared GIM group API key",
		secret = true
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "syncKcFromChat",
		name = "Sync KC from chat",
		description = "Parse kill count chat messages"
	)
	default boolean syncKcFromChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "syncKcFromCollectionLog",
		name = "Sync KC from Collection Log",
		description = "Sync KC when viewing Collection Log boss pages"
	)
	default boolean syncKcFromCollectionLog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "remindOnLogin",
		name = "Remind on login",
		description = "Remind to sync KC when stale or never synced"
	)
	default boolean remindOnLogin()
	{
		return true;
	}

	@ConfigItem(
		keyName = "staleSyncDays",
		name = "Stale sync days",
		description = "Days before showing sync reminder again"
	)
	default int staleSyncDays()
	{
		return 7;
	}

	@ConfigItem(
		keyName = "debugLogging",
		name = "Debug logging",
		description = "Log debug messages"
	)
	default boolean debugLogging()
	{
		return false;
	}
}
