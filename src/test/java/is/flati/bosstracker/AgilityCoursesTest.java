package is.flati.bosstracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.OptionalInt;
import org.junit.Test;

public class AgilityCoursesTest
{
	private static final String GRACE_SCROLL_COLON_SAMPLE =
		"Rooftop Agility LapsDraynor Village: 17 LapsAl Kharid: 46 LapsVarrock: 55 LapsCanifis: 204 Laps"
			+ "Falador: 325 LapsSeers' Village: 670 LapsPollnivneach: 1 LapRellekka: 364 LapsArdougne: 1 Lap"
			+ "Miscellaneous Course: Agility Pyramid: 11 LapsBarbarian Outpost Agility: 1 Lap"
			+ "Brimhaven Agility Arena: 2 LapsDorgesh-Kaan Agility: 0 LapsGnome Stronghold Agility: 28 Laps"
			+ "Penguin Agility: 1 LapWerewolf Agility: 0 LapsSkullball: 3 LapsWilderness Agility: 4 Laps"
			+ "Shayzien Basic: 0 LapsShayzien Advanced: 0 LapsApe Atoll Agility: 3 LapsPrifddinas Agility: 1 Lap"
			+ "Colossal Wyrm Basic: 0 LapsColossal Wyrm Advanced: 0 Laps";

	private static final String GRACE_SCROLL_GLUE_MISC_SAMPLE =
		"Rooftop Agility LapsDraynor Village - 17 LapsAl Kharid - 46 LapsVarrock - 55 Laps"
			+ "Canifis - 204 LapsFalador - 325 LapsSeers' Village - 670 LapsPollnivneach - 1 Laps"
			+ "Rellekka - 364 LapsArdougne - 1 LapsMiscellaneous Course:Agility Pyramid - 11 Laps"
			+ "Barbarian Outpost Agility - 1 LapsBrimhaven Agility Arena- 2 LapsDorgesh-Kaan Agility - 0 Laps"
			+ "Gnome Stronghold Agility - 28 LapsPenguin Agility - 1 LapsWerewolf Agility - 0 Laps"
			+ "Skullball - 3 LapsWilderness Agility - 4 LapsShayzien Basic - 0 LapsShayzien Advanced - 0 Laps"
			+ "Ape Atoll Agility - 3 LapsPrifddinas Agility - 1 LapColossal Wyrm Basic - 0 Laps"
			+ "Colossal Wyrm Advanced - 12 Laps";

	private static final String GRACE_SCROLL_DASH_SAMPLE =
		"Rooftop Agility Laps\n"
			+ "Draynor Village - 17 Laps\n"
			+ "Al Kharid - 46 Laps\n"
			+ "Seers' Village - 670 Laps\n"
			+ "Miscellaneous Course:\n"
			+ "Agility Pyramid - 11 Laps\n"
			+ "Gnome Stronghold Agility - 28 Laps\n"
			+ "Colossal Wyrm Advanced - 12 Laps";

	@Test
	public void normalizeCourseName_stripsCourseSuffix()
	{
		assertEquals("Seers' Village Rooftop",
			AgilityCourses.normalizeCourseName("Seers' Village Rooftop Course"));
	}

	@Test
	public void normalizeCourseName_mapsGraceRooftopShortNames()
	{
		assertEquals("Seers' Village Rooftop",
			AgilityCourses.normalizeCourseName("Seers' Village"));
		assertEquals("Gnome Stronghold",
			AgilityCourses.normalizeCourseName("Gnome Stronghold Agility"));
	}

	@Test
	public void isAgilityCourse_rooftopSuffix()
	{
		assertTrue(AgilityCourses.isAgilityCourse("Seers' Village Rooftop"));
		assertFalse(AgilityCourses.isAgilityCourse("Zulrah"));
	}

	@Test
	public void parseGraceLapScroll_parsesConcatenatedColonText()
	{
		List<AgilityCourses.CourseLap> entries = AgilityCourses.parseGraceLapScroll(GRACE_SCROLL_COLON_SAMPLE);
		assertEquals(24, entries.size());
		assertEquals("Seers' Village Rooftop", entries.get(5).course);
		assertEquals(670, entries.get(5).laps);
		assertEquals("Agility Pyramid", entries.get(9).course);
		assertEquals(11, entries.get(9).laps);
		assertEquals("Dorgesh-Kaan Agility Course", entries.get(12).course);
		assertEquals(0, entries.get(12).laps);
	}

	@Test
	public void parseGraceLapScroll_parsesGluedMiscHeaderAndZeroLaps()
	{
		List<AgilityCourses.CourseLap> entries = AgilityCourses.parseGraceLapScroll(GRACE_SCROLL_GLUE_MISC_SAMPLE);
		assertEquals(24, entries.size());
		assertEquals("Agility Pyramid", entries.get(9).course);
		assertEquals(11, entries.get(9).laps);
		assertEquals("Brimhaven Agility Arena", entries.get(11).course);
		assertEquals(2, entries.get(11).laps);
		assertEquals("Dorgesh-Kaan Agility Course", entries.get(12).course);
		assertEquals(0, entries.get(12).laps);
		assertEquals("Colossal Wyrm Advanced", entries.get(23).course);
		assertEquals(12, entries.get(23).laps);
	}

	@Test
	public void parseGraceLapScroll_parsesDashFormattedScroll()
	{
		List<AgilityCourses.CourseLap> entries = AgilityCourses.parseGraceLapScroll(GRACE_SCROLL_DASH_SAMPLE);
		assertEquals(6, entries.size());
		assertEquals("Seers' Village Rooftop", entries.get(2).course);
		assertEquals(670, entries.get(2).laps);
		assertEquals("Colossal Wyrm Advanced", entries.get(5).course);
		assertEquals(12, entries.get(5).laps);
	}

	@Test
	public void parseGraceLapLine_matchesLegacyDashFormat()
	{
		OptionalInt laps = AgilityCourses.parseGraceLapLine("Seers' Village Rooftop - 668 Laps");
		assertTrue(laps.isPresent());
		assertEquals(668, laps.getAsInt());

		assertEquals("Seers' Village Rooftop",
			AgilityCourses.parseGraceCourseName("Seers' Village Rooftop - 1 Lap"));
	}

	@Test
	public void parseGraceLapLine_rejectsNonAgilityLine()
	{
		assertFalse(AgilityCourses.parseGraceLapLine("Your kill count is: 5").isPresent());
	}
}
