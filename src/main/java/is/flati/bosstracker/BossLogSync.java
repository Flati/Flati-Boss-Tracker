package is.flati.bosstracker;

import is.flati.bosstracker.model.KcSyncPayload;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

@Slf4j
@Singleton
public class BossLogSync
{
	private final Client client;
	private final BossTrackerConfig config;
	private final ApiClient apiClient;

	private boolean bossLogLoaded;

	@Inject
	public BossLogSync(Client client, BossTrackerConfig config, ApiClient apiClient)
	{
		this.client = client;
		this.config = config;
		this.apiClient = apiClient;
	}

	public void markBossLogLoaded()
	{
		bossLogLoaded = true;
	}

	public void onGameTick()
	{
		if (!config.enableExternalSync() || !bossLogLoaded)
		{
			return;
		}
		bossLogLoaded = false;
		syncFromOpenBossLog(null);
	}

	public boolean syncFromOpenBossLog(Runnable onSuccess)
	{
		Widget title = client.getWidget(InterfaceID.KillLog.INTERFACE_TITLE);
		Widget bossMonster = client.getWidget(InterfaceID.KillLog.NAME);
		Widget bossKills = client.getWidget(InterfaceID.KillLog.KILL);

		if (title == null || bossMonster == null || bossKills == null
			|| !"Boss Kill Log".equals(title.getText()))
		{
			return false;
		}

		Widget[] bossChildren = bossMonster.getChildren();
		Widget[] killsChildren = bossKills.getChildren();
		if (bossChildren == null || killsChildren == null)
		{
			return false;
		}

		List<KcSyncPayload.KcEntry> entries = new ArrayList<>();
		for (int i = 0; i < bossChildren.length && i < killsChildren.length; i++)
		{
			Widget boss = bossChildren[i];
			Widget kill = killsChildren[i];
			if (boss == null || kill == null)
			{
				continue;
			}

			String bossName = BossRegistry.normalizeBossName(boss.getText());
			int kc;
			try
			{
				kc = Integer.parseInt(kill.getText().replace(",", ""));
			}
			catch (NumberFormatException e)
			{
				continue;
			}

			if (kc <= 0 || bossName.isEmpty())
			{
				continue;
			}

			entries.add(KcSyncPayload.KcEntry.builder()
				.bossName(bossName)
				.killCount(kc)
				.build());
		}

		if (entries.isEmpty())
		{
			return false;
		}

		String playerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
		if (playerName == null)
		{
			return false;
		}

		KcSyncPayload payload = KcSyncPayload.builder()
			.playerName(playerName)
			.source("boss_log")
			.updatedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString())
			.entries(entries)
			.build();

		apiClient.sendKcSync(payload, onSuccess);

		if (config.debugLogging())
		{
			log.debug("Boss log sync: {} entries", entries.size());
		}

		return true;
	}
}
