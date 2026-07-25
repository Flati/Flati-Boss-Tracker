package is.flati.bosstracker;

import is.flati.bosstracker.model.KcUpdatePayload;
import is.flati.bosstracker.model.KillPayload;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.ChatMessage;

@Slf4j
@Singleton
public class KillCountParser
{
	private static final Pattern KILLCOUNT_PATTERN = Pattern.compile(
		"Your (?<pre>completion count for |subdued |completed )?(?:<col=[0-9a-f]{6}>)?(?<boss>.+?)(?:</col>)? "
			+ "(?<post>(?:(?:kill|harvest|lap|completion|success|Total Ticket) )?(?:count )?)is: ?"
			+ "<col=[0-9a-f]{6}>(?<kc>[0-9,]+)</col>");

	private final Client client;
	private final BossTrackerConfig config;
	private final ApiClient apiClient;
	private final KillEventSender killEventSender;

	@Inject
	public KillCountParser(Client client, BossTrackerConfig config, ApiClient apiClient,
		KillEventSender killEventSender)
	{
		this.client = client;
		this.config = config;
		this.apiClient = apiClient;
		this.killEventSender = killEventSender;
	}

	public void onChatMessage(ChatMessage chatMessage)
	{
		if (!config.enableExternalSync() || !config.syncKcFromChat())
		{
			return;
		}

		ChatMessageType type = chatMessage.getType();
		if (type != ChatMessageType.GAMEMESSAGE
			&& type != ChatMessageType.SPAM
			&& type != ChatMessageType.TRADE
			&& type != ChatMessageType.FRIENDSCHATNOTIFICATION)
		{
			return;
		}

		String message = chatMessage.getMessage();
		Matcher matcher = KILLCOUNT_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return;
		}

		String pre = matcher.group("pre");
		String post = matcher.group("post");
		if ((pre == null || pre.isEmpty()) && (post == null || post.isEmpty()))
		{
			return;
		}

		String boss = BossRegistry.normalizeBossName(matcher.group("boss"));
		int kc = Integer.parseInt(matcher.group("kc").replace(",", ""));

		String killedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
		sendKcUpdate(boss, kc, killedAt);
		killEventSender.sendKill(boss, kc, killedAt);
	}

	public void sendKcUpdate(String bossName, int killCount)
	{
		sendKcUpdate(bossName, killCount, Instant.now().truncatedTo(ChronoUnit.SECONDS).toString());
	}

	private void sendKcUpdate(String bossName, int killCount, String updatedAt)
	{
		String playerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
		if (playerName == null)
		{
			return;
		}

		KcUpdatePayload payload = KcUpdatePayload.builder()
			.playerName(playerName)
			.bossName(bossName)
			.killCount(killCount)
			.updatedAt(updatedAt)
			.build();

		apiClient.sendKcUpdate(payload);

		if (config.debugLogging())
		{
			log.debug("KC update: {} = {}", bossName, killCount);
		}
	}
}
