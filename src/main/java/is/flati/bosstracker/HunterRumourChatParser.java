package is.flati.bosstracker;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class HunterRumourChatParser
{
	static final String BOSS_NAME = "Hunter rumours";

	static final class RumourCompletion
	{
		private final int count;

		RumourCompletion(int count)
		{
			this.count = count;
		}

		int getCount()
		{
			return count;
		}
	}

	static final Pattern COMPLETION_PATTERN = Pattern.compile(
		"You have completed " + ChatMarkup.coloredCount("count")
			+ " rumours for the Hunter Guild\\.",
		Pattern.CASE_INSENSITIVE);

	private HunterRumourChatParser()
	{
	}

	static Optional<RumourCompletion> parse(String message)
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

		int count = Integer.parseInt(matcher.group("count").replace(",", ""));
		return Optional.of(new RumourCompletion(count));
	}
}
