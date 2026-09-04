package is.flati.bosstracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;

public class HunterRumourChatParserTest
{
	@Test
	public void parse_withColorTags()
	{
		Optional<HunterRumourChatParser.RumourCompletion> completion = HunterRumourChatParser.parse(
			"You have completed <col=ff0000>163</col> rumours for the Hunter Guild.");
		assertTrue(completion.isPresent());
		assertEquals(163, completion.get().getCount());
	}

	@Test
	public void parse_plainTextWithoutTags()
	{
		Optional<HunterRumourChatParser.RumourCompletion> completion = HunterRumourChatParser.parse(
			"You have completed 42 rumours for the Hunter Guild.");
		assertTrue(completion.isPresent());
		assertEquals(42, completion.get().getCount());
	}

	@Test
	public void parse_commaSeparatedCount()
	{
		Optional<HunterRumourChatParser.RumourCompletion> completion = HunterRumourChatParser.parse(
			"You have completed <col=ff0000>1,234</col> rumours for the Hunter Guild.");
		assertTrue(completion.isPresent());
		assertEquals(1234, completion.get().getCount());
	}

	@Test
	public void parse_withColorMacro()
	{
		Optional<HunterRumourChatParser.RumourCompletion> completion = HunterRumourChatParser.parse(
			"You have completed @mes_hl_red@255</col> rumours for the Hunter Guild.");
		assertTrue(completion.isPresent());
		assertEquals(255, completion.get().getCount());
	}

	@Test
	public void parse_doesNotMatchRarePieceMessage()
	{
		assertFalse(HunterRumourChatParser.parse(
			"You find a rare piece of the creature! You should take it back to the Hunter Guild.").isPresent());
	}
}
