package is.flati.bosstracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.junit.Test;

public class ClueChatParserTest
{
	@Test
	public void parse_beginnerWithColorTags()
	{
		Optional<ClueChatParser.ClueCompletion> completion = ClueChatParser.parse(
			"You have completed <col=ff0000>255</col> beginner Treasure Trails.");
		assertTrue(completion.isPresent());
		assertEquals("Beginner clues completed", completion.get().getBossName());
		assertEquals(255, completion.get().getCount());
	}

	@Test
	public void parse_singularTreasureTrail()
	{
		Optional<ClueChatParser.ClueCompletion> completion = ClueChatParser.parse(
			"You have completed 1 beginner Treasure Trail.");
		assertTrue(completion.isPresent());
		assertEquals("Beginner clues completed", completion.get().getBossName());
		assertEquals(1, completion.get().getCount());
	}

	@Test
	public void parse_plainTextWithoutTags()
	{
		Optional<ClueChatParser.ClueCompletion> completion = ClueChatParser.parse(
			"You have completed 42 easy Treasure Trails.");
		assertTrue(completion.isPresent());
		assertEquals("Easy clues completed", completion.get().getBossName());
		assertEquals(42, completion.get().getCount());
	}

	@Test
	public void parse_masterTier()
	{
		Optional<ClueChatParser.ClueCompletion> completion = ClueChatParser.parse(
			"You have completed <col=ff0000>12</col> master Treasure Trails.");
		assertTrue(completion.isPresent());
		assertEquals("Master clues completed", completion.get().getBossName());
		assertEquals(12, completion.get().getCount());
	}

	@Test
	public void parse_doesNotMatchGenericCompletionMessage()
	{
		assertFalse(ClueChatParser.parse(
			"Well done, you've completed the Treasure Trail!").isPresent());
	}
}
