package is.flati.bosstracker;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DelveChatParser
{
	static final String BOSS_NAME = "Delve";
	static final String DEEPEST_DELVE = "Deepest delve";
	static final String DEEP_DELVES = "Deep delves";
	static final String TOTAL_DELVES = "Total delves";

	private static final String COLORED_NUMBER = "(?:<col=[0-9a-f]{6}>)?(?<value>\\d+)(?:</col>)?";

	static final Pattern FLOOR_PATTERN = Pattern.compile(
		"Delve level: " + COLORED_NUMBER.replace("value", "floor") + " duration: "
			+ "(?:<col=[0-9a-f]{6}>)?(?<duration>\\d+:\\d{2})(?:</col>)?");

	static final Pattern DEEP_DELVES_PATTERN = Pattern.compile(
		"Deep delves completed: " + COLORED_NUMBER.replace("value", "kc"));

	private DelveChatParser()
	{
	}

	static OptionalInt parseFloor(String message)
	{
		if (message == null || message.isEmpty())
		{
			return OptionalInt.empty();
		}

		Matcher matcher = FLOOR_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return OptionalInt.empty();
		}

		return OptionalInt.of(Integer.parseInt(matcher.group("floor")));
	}

	static OptionalInt parseDeepDelvesCompleted(String message)
	{
		if (message == null || message.isEmpty())
		{
			return OptionalInt.empty();
		}

		Matcher matcher = DEEP_DELVES_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return OptionalInt.empty();
		}

		return OptionalInt.of(Integer.parseInt(matcher.group("kc")));
	}
}
