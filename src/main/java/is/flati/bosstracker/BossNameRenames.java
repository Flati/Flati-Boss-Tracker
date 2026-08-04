package is.flati.bosstracker;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class BossNameRenames
{
	static final Map<String, String> RENAMES;

	static
	{
		Map<String, String> map = new HashMap<>();
		map.put("Zul'rah", "Zulrah");
		map.put("Kre'ee", "Kree'arra");
		map.put("Kree'Arra", "Kree'arra");
		map.put("Kree'arra", "Kree'arra");
		map.put("Grotesque Guardian", "Grotesque Guardians");
		map.put("The Gauntlet", "Gauntlet");
		map.put("Gauntlet completion count", "Gauntlet");
		map.put("The Corrupted Gauntlet", "Corrupted Gauntlet");
		map.put("Corrupted Gauntlet completion count", "Corrupted Gauntlet");
		map.put("The Hueycoatl", "Hueycoatl");
		map.put("The Leviathan", "Leviathan");
		map.put("The Whisperer", "Whisperer");
		map.put("The Royal Titans", "Royal Titans");
		map.put("Royal Titan", "Royal Titans");
		map.put("The Nightmare", "Nightmare");
		map.put("Barrows Chests", "Barrows");
		map.put("Barrows Chests opened", "Barrows");
		map.put("Moons of Peril Chests", "Moons of Peril");
		map.put("Lunar Chests opened", "Moons of Peril");
		map.put("Chambers of Xeric completions", "Chambers of Xeric");
		map.put("Chambers of Xeric (CM) completions", "Chambers of Xeric: Challenge Mode");
		map.put("Theatre of Blood completions", "Theatre of Blood");
		map.put("Theatre of Blood Entry Mode", "Theatre of Blood: Entry Mode");
		map.put("Theatre of Blood (Entry) completions", "Theatre of Blood: Entry Mode");
		map.put("Theatre of Blood (Hard) completions", "Theatre of Blood: Hard Mode");
		map.put("Tombs of Amascut completions", "Tombs of Amascut");
		map.put("Tombs of Amascut Entry Mode", "Tombs of Amascut: Entry Mode");
		map.put("Tombs of Amascut (Entry) completions", "Tombs of Amascut: Entry Mode");
		map.put("Tombs of Amascut Expert Mode", "Tombs of Amascut: Expert Mode");
		map.put("Tombs of Amascut (Expert) completions", "Tombs of Amascut: Expert Mode");
		map.put("Demonic Brutus", "Brutus");
		map.put("Thermonuclear Smoke Devil", "Thermonuclear smoke devil");
		map.put("Last Man Standing", "Last Man Standing kills");
		map.put("Orders fulfilled", "Mastering Mixology orders");
		map.put("Rifts searches", "GOTR rewards");
		map.put("Rewards claimed", "Wintertodt rewards");
		map.put("Reward permits claimed", "Tempoross rewards");
		map.put("Rumours Completed", "Hunter rumours");
		map.put("Deep delves completed", "Deep delves");
		map.put("Kill count", "Kill count");
		RENAMES = Collections.unmodifiableMap(map);
	}

	private BossNameRenames()
	{
	}
}
