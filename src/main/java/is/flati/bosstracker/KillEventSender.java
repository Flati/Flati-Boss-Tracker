package is.flati.bosstracker;

import is.flati.bosstracker.model.KillPayload;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;

@Singleton
public class KillEventSender
{
	private final Client client;
	private final ApiClient apiClient;

	@Inject
	public KillEventSender(Client client, ApiClient apiClient)
	{
		this.client = client;
		this.apiClient = apiClient;
	}

	public void sendKill(String bossName, int killCount, String killedAt)
	{
		String playerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
		if (playerName == null)
		{
			return;
		}

		KillPayload payload = KillPayload.builder()
			.playerName(playerName)
			.bossName(bossName)
			.killCount(killCount)
			.killedAt(killedAt != null ? killedAt : Instant.now().truncatedTo(ChronoUnit.SECONDS).toString())
			.build();

		apiClient.sendKill(payload);
	}
}
