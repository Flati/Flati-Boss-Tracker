package is.flati.bosstracker;

import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SepulchreChatParser
{
	static final String TIMELINE_BOSS_NAME = "Hallowed Sepulchre";
	static final String COFFIN_BOSS_NAME = "Grand Hallowed Coffin";
	static final String COFFINS_OPENED = "Grand Hallowed Coffins opened";

	static final Pattern FLOOR_COMPLETION_PATTERN = Pattern.compile(
		"You have completed Floor (?<floor>[1-5]) of the Hallowed Sepulchre! Total completions: "
			+ ChatMarkup.coloredCount("completions") + "\\.");

	static final Pattern COFFIN_PATTERN = Pattern.compile(
		"You have opened the Grand Hallowed Coffin " + ChatMarkup.coloredCount("kc") + " times?!");

	private SepulchreChatParser()
	{
	}

	static OptionalInt parseFloorCompletion(String message)
	{
		if (message == null || message.isEmpty())
		{
			return OptionalInt.empty();
		}

		Matcher matcher = FLOOR_COMPLETION_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return OptionalInt.empty();
		}

		return OptionalInt.of(Integer.parseInt(matcher.group("floor")));
	}

	static OptionalInt parseCoffinsOpened(String message)
	{
		if (message == null || message.isEmpty())
		{
			return OptionalInt.empty();
		}

		Matcher matcher = COFFIN_PATTERN.matcher(message);
		if (!matcher.find())
		{
			return OptionalInt.empty();
		}

		return OptionalInt.of(Integer.parseInt(matcher.group("kc").replace(",", "")));
	}
}
