package is.flati.bosstracker;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Flati Boss Tracker",
	description = "Tracks GIM boss kills and KC for flati.is or self-hosted backends",
	tags = {"boss", "gim", "ironman", "tracking"}
)
public class BossTrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private BossTrackerConfig config;

	@Inject
	private ApiClient apiClient;

	@Inject
	private KillCountParser killCountParser;

	@Inject
	private BossLogSync bossLogSync;

	@Inject
	private CollectionLogSync collectionLogSync;

	@Inject
	private GraceLapSync graceLapSync;

	@Override
	protected void startUp()
	{
		if (config.enableExternalSync())
		{
			apiClient.flushRetryQueue();
		}
		log.info("Flati Boss Tracker started");
	}

	@Override
	protected void shutDown()
	{
		apiClient.shutdown();
	}

	@Provides
	BossTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BossTrackerConfig.class);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			if (config.enableExternalSync())
			{
				apiClient.flushRetryQueue();
			}

			if (config.enableExternalSync() && config.remindOnLogin() && apiClient.isSyncStale())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"Flati Boss Tracker: Open Boss Kill Log, Grace View Laps, or Collection Log to sync KC to flati.is",
					null);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		bossLogSync.onGameTick();
		graceLapSync.onGameTick();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.KILL_LOG)
		{
			bossLogSync.markBossLogLoaded();
		}
		else if (event.getGroupId() == InterfaceID.LONGSCROLL)
		{
			if (config.debugLogging())
			{
				log.debug("WidgetLoaded: LONGSCROLL ({})", InterfaceID.LONGSCROLL);
			}
			graceLapSync.markGraceLapsLoaded();
		}
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.COLLECTION_DRAW_LIST)
		{
			collectionLogSync.syncCurrentPage();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		killCountParser.onChatMessage(event);
	}
}
