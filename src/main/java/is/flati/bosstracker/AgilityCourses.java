package is.flati.bosstracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AgilityCourses
{
	static final String GRACE_LAPS_HEADER = "Agility Course Lap Counts";

	static final Pattern GRACE_LAP_ENTRY = Pattern.compile(
		"(?<course>.+?): (?<laps>\\d+) Laps?", Pattern.CASE_INSENSITIVE);

	static final Pattern GRACE_LAP_ENTRY_DASH = Pattern.compile(
		"(?<course>.+?)\\s*-\\s*(?<laps>\\d+) Laps?", Pattern.CASE_INSENSITIVE);

	private static final Pattern GRACE_LAP_LINE_DASH = Pattern.compile(
		"^(?<course>.+?)\\s*-\\s*(?<laps>\\d+) Laps?$", Pattern.CASE_INSENSITIVE);

	private static final Set<String> KNOWN_COURSES;
	private static final Map<String, String> ALIASES;
	private static final Map<String, String> GRACE_ROOFTOP_NAMES;
	private static final Set<String> GRACE_HEADER_SKIP;

	static
	{
		Set<String> courses = new HashSet<>();
		courses.add("Draynor Village Rooftop");
		courses.add("Al Kharid Rooftop");
		courses.add("Varrock Rooftop");
		courses.add("Canifis Rooftop");
		courses.add("Falador Rooftop");
		courses.add("Seers' Village Rooftop");
		courses.add("Pollnivneach Rooftop");
		courses.add("Rellekka Rooftop");
		courses.add("Ardougne Rooftop");
		courses.add("Gnome Stronghold");
		courses.add("Barbarian Outpost");
		courses.add("Wilderness Agility Course");
		courses.add("Ape Atoll");
		courses.add("Werewolf Agility Course");
		courses.add("Dorgesh-Kaan Agility Course");
		courses.add("Agility Pyramid");
		courses.add("Brimhaven Agility Arena");
		courses.add("Penguin Agility Course");
		courses.add("Hallowed Sepulchre");
		courses.add("Prifddinas Agility Course");
		courses.add("Colossal Wyrm Agility Course");
		courses.add("Shayzien Agility Course");
		courses.add("Shayzien Basic");
		courses.add("Shayzien Advanced");
		courses.add("Colossal Wyrm Basic");
		courses.add("Colossal Wyrm Advanced");
		courses.add("Skullball");
		KNOWN_COURSES = Collections.unmodifiableSet(courses);

		Map<String, String> aliases = new HashMap<>();
		aliases.put("Draynor Village Rooftop Course", "Draynor Village Rooftop");
		aliases.put("Al Kharid Rooftop Course", "Al Kharid Rooftop");
		aliases.put("Varrock Rooftop Course", "Varrock Rooftop");
		aliases.put("Canifis Rooftop Course", "Canifis Rooftop");
		aliases.put("Falador Rooftop Course", "Falador Rooftop");
		aliases.put("Seers' Village Rooftop Course", "Seers' Village Rooftop");
		aliases.put("Pollnivneach Rooftop Course", "Pollnivneach Rooftop");
		aliases.put("Rellekka Rooftop Course", "Rellekka Rooftop");
		aliases.put("Ardougne Rooftop Course", "Ardougne Rooftop");
		aliases.put("Gnome Stronghold Course", "Gnome Stronghold");
		aliases.put("Gnome Stronghold Agility", "Gnome Stronghold");
		aliases.put("Barbarian Outpost Course", "Barbarian Outpost");
		aliases.put("Barbarian Outpost Agility", "Barbarian Outpost");
		aliases.put("Ape Atoll Course", "Ape Atoll");
		aliases.put("Ape Atoll Agility", "Ape Atoll");
		aliases.put("Penguin Agility", "Penguin Agility Course");
		aliases.put("Werewolf Agility", "Werewolf Agility Course");
		aliases.put("Wilderness Agility", "Wilderness Agility Course");
		aliases.put("Dorgesh-Kaan Agility", "Dorgesh-Kaan Agility Course");
		aliases.put("Prifddinas Agility", "Prifddinas Agility Course");
		ALIASES = Collections.unmodifiableMap(aliases);

		Map<String, String> rooftops = new HashMap<>();
		rooftops.put("Draynor Village", "Draynor Village Rooftop");
		rooftops.put("Al Kharid", "Al Kharid Rooftop");
		rooftops.put("Varrock", "Varrock Rooftop");
		rooftops.put("Canifis", "Canifis Rooftop");
		rooftops.put("Falador", "Falador Rooftop");
		rooftops.put("Seers' Village", "Seers' Village Rooftop");
		rooftops.put("Pollnivneach", "Pollnivneach Rooftop");
		rooftops.put("Rellekka", "Rellekka Rooftop");
		rooftops.put("Ardougne", "Ardougne Rooftop");
		GRACE_ROOFTOP_NAMES = Collections.unmodifiableMap(rooftops);

		Set<String> skip = new HashSet<>();
		skip.add("Rooftop Agility");
		skip.add("Rooftop Agility Laps");
		skip.add("Miscellaneous Course");
		GRACE_HEADER_SKIP = Collections.unmodifiableSet(skip);
	}

	private AgilityCourses()
	{
	}

	static final class CourseLap
	{
		final String course;
		final int laps;

		CourseLap(String course, int laps)
		{
			this.course = course;
			this.laps = laps;
		}
	}

	static String normalizeCourseName(String name)
	{
		if (name == null)
		{
			return "";
		}

		String trimmed = name.trim();
		if (trimmed.endsWith(" Course"))
		{
			trimmed = trimmed.substring(0, trimmed.length() - " Course".length()).trim();
		}

		if (ALIASES.containsKey(trimmed))
		{
			return ALIASES.get(trimmed);
		}

		if (GRACE_ROOFTOP_NAMES.containsKey(trimmed))
		{
			return GRACE_ROOFTOP_NAMES.get(trimmed);
		}

		return trimmed;
	}

	static boolean isAgilityCourse(String name)
	{
		String normalized = normalizeCourseName(name);
		if (normalized.isEmpty())
		{
			return false;
		}
		if (normalized.endsWith(" Rooftop"))
		{
			return true;
		}
		return KNOWN_COURSES.contains(normalized);
	}

	static List<CourseLap> parseGraceLapScroll(String body)
	{
		if (body == null || body.isEmpty())
		{
			return new ArrayList<>();
		}

		String normalized = body.replace("<br>", "\n").replace('\u00A0', ' ').trim();
		List<CourseLap> entries = parseGraceEntries(normalized, GRACE_LAP_ENTRY_DASH);
		if (entries.isEmpty())
		{
			entries = parseGraceEntries(normalized, GRACE_LAP_ENTRY);
		}
		return entries;
	}

	private static List<CourseLap> parseGraceEntries(String body, Pattern pattern)
	{
		List<CourseLap> entries = new ArrayList<>();
		Matcher matcher = pattern.matcher(body);
		while (matcher.find())
		{
			String course = cleanGraceCourseRaw(matcher.group("course"));
			course = normalizeCourseName(course);
			if (course.isEmpty() || GRACE_HEADER_SKIP.contains(course))
			{
				continue;
			}

			int laps = Integer.parseInt(matcher.group("laps"));
			if (laps < 0)
			{
				continue;
			}

			entries.add(new CourseLap(course, laps));
		}
		return entries;
	}

	private static String cleanGraceCourseRaw(String raw)
	{
		if (raw == null)
		{
			return "";
		}

		String cleaned = raw.trim();
		cleaned = cleaned.replaceFirst("^Miscellaneous Course:\\s*", "");

		cleaned = cleaned.replaceAll(".*Laps(?=[A-Z'])", "");
		cleaned = cleaned.replaceFirst("^Rooftop Agility Laps?", "").trim();

		return cleaned;
	}

	static OptionalInt parseGraceLapLine(String line)
	{
		List<CourseLap> entries = parseGraceLapScroll(line);
		if (entries.size() == 1)
		{
			return OptionalInt.of(entries.get(0).laps);
		}

		if (line == null || line.isEmpty())
		{
			return OptionalInt.empty();
		}

		Matcher matcher = GRACE_LAP_LINE_DASH.matcher(line.trim());
		if (!matcher.matches())
		{
			return OptionalInt.empty();
		}

		return OptionalInt.of(Integer.parseInt(matcher.group("laps")));
	}

	static String parseGraceCourseName(String line)
	{
		List<CourseLap> entries = parseGraceLapScroll(line);
		if (entries.size() == 1)
		{
			return entries.get(0).course;
		}

		if (line == null || line.isEmpty())
		{
			return "";
		}

		Matcher matcher = GRACE_LAP_LINE_DASH.matcher(line.trim());
		if (!matcher.matches())
		{
			return "";
		}

		return normalizeCourseName(matcher.group("course"));
	}
}
