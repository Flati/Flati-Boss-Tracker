package is.flati.bosstracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("flatibosstracker")
public interface BossTrackerConfig extends Config
{
	String THIRD_PARTY_WARNING =
		"This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers.";

	@ConfigItem(
		keyName = "enableExternalSync",
		name = "Enable external sync",
		description = "Send boss kills and KC to the configured backend",
		warning = THIRD_PARTY_WARNING
	)
	default boolean enableExternalSync()
	{
		return false;
	}

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
		description = "Shared group API key from your backend administrator",
		secret = true
	)
	default String apiKey()
	{
		return "";
	}

	@ConfigItem(
		keyName = "syncKcFromChat",
		name = "Sync KC from chat",
		description = "Parse kill count chat messages and send kill events",
		warning = THIRD_PARTY_WARNING
	)
	default boolean syncKcFromChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "syncKcFromCollectionLog",
		name = "Sync KC from Collection Log",
		description = "Sync KC when viewing Collection Log boss pages",
		warning = THIRD_PARTY_WARNING
	)
	default boolean syncKcFromCollectionLog()
	{
		return false;
	}

	@ConfigItem(
		keyName = "syncKcFromGraceLaps",
		name = "Sync KC from Grace View Laps",
		description = "Bulk sync agility course lap counts when viewing laps at Grace (Rogue's Den)",
		warning = THIRD_PARTY_WARNING
	)
	default boolean syncKcFromGraceLaps()
	{
		return false;
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
