package is.flati.bosstracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.OptionalInt;
import org.junit.Test;

public class SepulchreChatParserTest
{
	@Test
	public void parseFloorCompletion_withColorTags()
	{
		OptionalInt floor = SepulchreChatParser.parseFloorCompletion(
			"You have completed Floor 4 of the Hallowed Sepulchre! Total completions: <col=ff0000>252</col>.");
		assertTrue(floor.isPresent());
		assertEquals(4, floor.getAsInt());
	}

	@Test
	public void parseFloorCompletion_plainText()
	{
		OptionalInt floor = SepulchreChatParser.parseFloorCompletion(
			"You have completed Floor 1 of the Hallowed Sepulchre! Total completions: 500.");
		assertTrue(floor.isPresent());
		assertEquals(1, floor.getAsInt());
	}

	@Test
	public void parseCoffinsOpened_withColorTags()
	{
		OptionalInt kc = SepulchreChatParser.parseCoffinsOpened(
			"You have opened the Grand Hallowed Coffin <col=ff0000>36</col> times!");
		assertTrue(kc.isPresent());
		assertEquals(36, kc.getAsInt());
	}

	@Test
	public void parseCoffinsOpened_singularTime()
	{
		OptionalInt kc = SepulchreChatParser.parseCoffinsOpened(
			"You have opened the Grand Hallowed Coffin <col=ff0000>1</col> time!");
		assertTrue(kc.isPresent());
		assertEquals(1, kc.getAsInt());
	}

	@Test
	public void parseCoffinsOpened_plainText()
	{
		OptionalInt kc = SepulchreChatParser.parseCoffinsOpened(
			"You have opened the Grand Hallowed Coffin 6 times!");
		assertTrue(kc.isPresent());
		assertEquals(6, kc.getAsInt());
	}

	@Test
	public void parseCoffinsOpened_withColorMacro()
	{
		OptionalInt kc = SepulchreChatParser.parseCoffinsOpened(
			"You have opened the Grand Hallowed Coffin @mes_hl_red@36</col> times!");
		assertTrue(kc.isPresent());
		assertEquals(36, kc.getAsInt());
	}

	@Test
	public void parseCoffinsOpened_doesNotMatchFloorCompletion()
	{
		assertFalse(SepulchreChatParser.parseCoffinsOpened(
			"You have completed Floor 5 of the Hallowed Sepulchre! Total completions: <col=ff0000>6</col>.").isPresent());
	}
}
