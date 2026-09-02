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

	// Levels past 8 are "8+ (9)". colouredDigits already wraps each number (open + digits + close).
	// The extra COLOR_CLOSE after ")" consumes a tag that wrapped the whole "8+ (9)", whose
	// COLOR_OPEN was taken by the floor group: <col=ff0000>8+ (9)</col>
	static final Pattern FLOOR_PATTERN = Pattern.compile(
		"Delve level: " + ChatMarkup.coloredDigits("floor")
			+ "(?:" + ChatMarkup.COLOR_OPEN + "\\+ \\(" + ChatMarkup.coloredDigits("deepFloor") + "\\)"
			+ ChatMarkup.COLOR_CLOSE + ")?"
			+ " duration: " + ChatMarkup.COLOR_OPEN + "(?<duration>\\d+:\\d{2})" + ChatMarkup.COLOR_CLOSE);

	static final Pattern DEEP_DELVES_PATTERN = Pattern.compile(
		"Deep delves completed: " + ChatMarkup.coloredDigits("kc"));

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

		String deepFloor = matcher.group("deepFloor");
		return OptionalInt.of(Integer.parseInt(deepFloor != null ? deepFloor : matcher.group("floor")));
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
