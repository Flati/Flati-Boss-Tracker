package is.flati.bosstracker;

/**
 * Chat colour wrappers used in game messages.
 * Numbers may be wrapped in {@code <col=rrggbb>} or in the newer {@code @macro@…</col>} form.
 */
final class ChatMarkup
{
	static final String COLOR_OPEN = "(?:<col=[0-9a-f]{6}>|@.+?@)?";
	static final String COLOR_CLOSE = "(?:</col>)?";

	private ChatMarkup()
	{
	}

	static String coloredDigits(String groupName)
	{
		return COLOR_OPEN + "(?<" + groupName + ">\\d+)" + COLOR_CLOSE;
	}

	static String coloredCount(String groupName)
	{
		return COLOR_OPEN + "(?<" + groupName + ">[0-9,]+)" + COLOR_CLOSE;
	}
}
