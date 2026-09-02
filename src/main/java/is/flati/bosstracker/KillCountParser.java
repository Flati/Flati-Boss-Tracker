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
	static final Pattern KILLCOUNT_PATTERN = Pattern.compile(
		"Your (?<pre>completion count for |subdued |completed )?" + ChatMarkup.COLOR_OPEN + "(?<boss>.+?)"
			+ ChatMarkup.COLOR_CLOSE + " "
			+ "(?<post>(?:(?:kill|harvest|lap|completion|success|Total Ticket) )?(?:count )?)is: ?"
			+ ChatMarkup.coloredCount("kc"));

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
		String killedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();

		if (handleDelveMessage(message, killedAt))
		{
			return;
		}

		if (handleSepulchreMessage(message, killedAt))
		{
			return;
		}

		if (handleClueMessage(message, killedAt))
		{
			return;
		}

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
		if (post != null && post.contains("lap"))
		{
			boss = AgilityCourses.normalizeCourseName(boss);
		}
		int kc = Integer.parseInt(matcher.group("kc").replace(",", ""));

		sendKcUpdate(boss, kc, killedAt);
		killEventSender.sendKill(boss, kc, killedAt);
	}

	private boolean handleDelveMessage(String message, String killedAt)
	{
		java.util.OptionalInt floor = DelveChatParser.parseFloor(message);
		if (floor.isPresent())
		{
			int floorNumber = floor.getAsInt();
			if (floorNumber > 0)
			{
				killEventSender.sendKill(DelveChatParser.BOSS_NAME, floorNumber, killedAt);
				sendKcUpdate(DelveChatParser.DEEPEST_DELVE, floorNumber, killedAt);

				if (config.debugLogging())
				{
					log.debug("Delve floor cleared: {}", floorNumber);
				}
			}
			return true;
		}

		java.util.OptionalInt deepDelves = DelveChatParser.parseDeepDelvesCompleted(message);
		if (deepDelves.isPresent())
		{
			sendKcUpdate(DelveChatParser.DEEP_DELVES, deepDelves.getAsInt(), killedAt);

			if (config.debugLogging())
			{
				log.debug("Deep delves completed: {}", deepDelves.getAsInt());
			}
			return true;
		}

		return false;
	}

	private boolean handleSepulchreMessage(String message, String killedAt)
	{
		java.util.OptionalInt coffinsOpened = SepulchreChatParser.parseCoffinsOpened(message);
		if (coffinsOpened.isPresent())
		{
			int totalCoffins = coffinsOpened.getAsInt();
			killEventSender.sendKill(
				SepulchreChatParser.COFFIN_BOSS_NAME, totalCoffins, killedAt);
			sendKcUpdate(SepulchreChatParser.COFFINS_OPENED, totalCoffins, killedAt);

			if (config.debugLogging())
			{
				log.debug("Grand Hallowed Coffin opened: {}", totalCoffins);
			}
			return true;
		}

		java.util.OptionalInt floor = SepulchreChatParser.parseFloorCompletion(message);
		if (floor.isPresent())
		{
			int floorNumber = floor.getAsInt();
			killEventSender.sendKill(SepulchreChatParser.TIMELINE_BOSS_NAME, floorNumber, killedAt);

			if (config.debugLogging())
			{
				log.debug("Hallowed Sepulchre floor completed: {}", floorNumber);
			}
			return true;
		}

		return false;
	}

	private boolean handleClueMessage(String message, String killedAt)
	{
		java.util.Optional<ClueChatParser.ClueCompletion> completion = ClueChatParser.parse(message);
		if (!completion.isPresent())
		{
			return false;
		}

		ClueChatParser.ClueCompletion clue = completion.get();
		sendKcUpdate(clue.getBossName(), clue.getCount(), killedAt);

		if (config.debugLogging())
		{
			log.debug("Clue completed: {} = {}", clue.getBossName(), clue.getCount());
		}

		return true;
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
