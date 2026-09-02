package is.flati.bosstracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.OptionalInt;
import org.junit.Test;

public class DelveChatParserTest
{
	@Test
	public void parseFloor_plainText()
	{
		OptionalInt floor = DelveChatParser.parseFloor(
			"Delve level: 2 duration: 1:01. Personal best: 0:41");
		assertTrue(floor.isPresent());
		assertEquals(2, floor.getAsInt());
	}

	@Test
	public void parseFloor_withColorTags()
	{
		OptionalInt floor = DelveChatParser.parseFloor(
			"Delve level: <col=ff0000>8</col> duration: <col=ff0000>2:33</col>. Personal best: <col=ff0000>1:46</col>");
		assertTrue(floor.isPresent());
		assertEquals(8, floor.getAsInt());
	}

	@Test
	public void parseFloor_deepDelveUsesParenthesisedLevel()
	{
		OptionalInt floor = DelveChatParser.parseFloor(
			"Delve level: 8+ (9) duration: 2:26. Personal best: 1:46");
		assertTrue(floor.isPresent());
		assertEquals(9, floor.getAsInt());
	}

	@Test
	public void parseFloor_deepDelveWithColorTags()
	{
		OptionalInt floor = DelveChatParser.parseFloor(
			"Delve level: <col=ff0000>8+ (24)</col> duration: <col=ff0000>2:26</col>. Personal best: <col=ff0000>1:46</col>");
		assertTrue(floor.isPresent());
		assertEquals(24, floor.getAsInt());
	}

	@Test
	public void parseFloor_withColorMacro()
	{
		OptionalInt floor = DelveChatParser.parseFloor(
			"Delve level: 7 duration: @mes_hl_red@1:35</col>. Personal best: @mes_hl_red@1:22</col>");
		assertTrue(floor.isPresent());
		assertEquals(7, floor.getAsInt());
	}

	@Test
	public void parseFloor_doesNotMatchRunSummary()
	{
		assertFalse(DelveChatParser.parseFloor(
			"Delve level 1 - 8 duration: 13:53. Personal best: 11:55").isPresent());
	}

	@Test
	public void parseFloor_doesNotMatchTotalDuration()
	{
		assertFalse(DelveChatParser.parseFloor("Total duration: 2:05").isPresent());
	}

	@Test
	public void parseDeepDelvesCompleted_plainText()
	{
		OptionalInt kc = DelveChatParser.parseDeepDelvesCompleted("Deep delves completed: 21");
		assertTrue(kc.isPresent());
		assertEquals(21, kc.getAsInt());
	}

	@Test
	public void parseDeepDelvesCompleted_withColorTags()
	{
		OptionalInt kc = DelveChatParser.parseDeepDelvesCompleted(
			"Deep delves completed: <col=ff0000>21</col>");
		assertTrue(kc.isPresent());
		assertEquals(21, kc.getAsInt());
	}

	@Test
	public void parseDeepDelvesCompleted_withColorMacro()
	{
		OptionalInt kc = DelveChatParser.parseDeepDelvesCompleted(
			"Deep delves completed: @mes_hl_red@43</col>");
		assertTrue(kc.isPresent());
		assertEquals(43, kc.getAsInt());
	}

	@Test
	public void parseDeepDelvesCompleted_doesNotMatchFloorLine()
	{
		assertFalse(DelveChatParser.parseDeepDelvesCompleted(
			"Delve level: 8 duration: 2:33. Personal best: 1:46").isPresent());
	}

	@Test
	public void parseDeepDelvesCompleted_doesNotMatchDeepDelveFloorLine()
	{
		assertFalse(DelveChatParser.parseDeepDelvesCompleted(
			"Delve level: 8+ (9) duration: 2:26. Personal best: 1:46").isPresent());
	}
}
