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
	description = "Tracks GIM boss kills and KC for flati.is",
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

	@Override
	protected void startUp()
	{
		apiClient.flushRetryQueue();
		log.info("Flati Boss Tracker started");
	}

	@Override
	protected void shutDown()
	{
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
			apiClient.flushRetryQueue();

			if (config.remindOnLogin() && apiClient.isSyncStale())
			{
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
					"Flati Boss Tracker: Open Boss Kill Log or browse Collection Log to sync KC to flati.is",
					null);
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		bossLogSync.onGameTick();
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.KILL_LOG)
		{
			bossLogSync.markBossLogLoaded();
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
