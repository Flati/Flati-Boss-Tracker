package is.flati.bosstracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import org.junit.Test;

public class KillCountParserTest
{
	@Test
	public void killcountPattern_withColTags()
	{
		Matcher matcher = KillCountParser.KILLCOUNT_PATTERN.matcher(
			"Your Vorkath kill count is: <col=ff0000>4</col>.");
		assertTrue(matcher.find());
		assertEquals("Vorkath", matcher.group("boss"));
		assertEquals("4", matcher.group("kc"));
	}

	@Test
	public void killcountPattern_withColorMacro()
	{
		Matcher matcher = KillCountParser.KILLCOUNT_PATTERN.matcher(
			"Your Vorkath kill count is: @mes_hl_red@4</col>.");
		assertTrue(matcher.find());
		assertEquals("Vorkath", matcher.group("boss"));
		assertEquals("4", matcher.group("kc"));
	}

	@Test
	public void killcountPattern_doesNotMatchUnrelatedChat()
	{
		assertFalse(KillCountParser.KILLCOUNT_PATTERN.matcher(
			"Delve level: 7 duration: 1:35. Personal best: 1:22").find());
	}
}
