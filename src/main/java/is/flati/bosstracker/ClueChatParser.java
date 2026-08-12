package is.flati.bosstracker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ClueChatParser
{
	static final class ClueCompletion
	{
		private final String bossName;
		private final int count;

		ClueCompletion(String bossName, int count)
		{
			this.bossName = bossName;
			this.count = count;
		}

		String getBossName()
		{
			return bossName;
		}

		int getCount()
		{
			return count;
		}
	}

	private static final Map<String, String> TIER_TO_BOSS_NAME = new HashMap<>();

	static
	{
		TIER_TO_BOSS_NAME.put("beginner", "Beginner clues completed");
		TIER_TO_BOSS_NAME.put("easy", "Easy clues completed");
		TIER_TO_BOSS_NAME.put("medium", "Medium clues completed");
		TIER_TO_BOSS_NAME.put("hard", "Hard clues completed");
		TIER_TO_BOSS_NAME.put("elite", "Elite clues completed");
		TIER_TO_BOSS_NAME.put("master", "Master clues completed");
	}

	private static final String COLORED_NUMBER = "(?:<col=[0-9a-f]{6}>)?(?<count>[0-9,]+)(?:</col>)?";

	static final Pattern COMPLETION_PATTERN = Pattern.compile(
		"You have completed " + COLORED_NUMBER
			+ " (?<tier>beginner|easy|medium|hard|elite|master) Treasure Trails?\\.",
		Pattern.CASE_INSENSITIVE);

	private ClueChatParser()
	{
	}

	static Optional<ClueCompletion> parse(String message)
	{
		if (message == null || message.isEmpty())
		{
			return Optional.empty();
		}

		Matcher matcher = COMPLETION_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return Optional.empty();
		}

		String tier = matcher.group("tier").toLowerCase();
		String bossName = TIER_TO_BOSS_NAME.get(tier);
		if (bossName == null)
		{
			return Optional.empty();
		}

		int count = Integer.parseInt(matcher.group("count").replace(",", ""));
		return Optional.of(new ClueCompletion(bossName, count));
	}
}
