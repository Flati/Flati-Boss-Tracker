package is.flati.bosstracker;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.runelite.api.NPC;

public class BossRegistry
{
	private static final Map<Integer, String> NPC_TO_BOSS = new HashMap<>();

	static
	{
		register("Zulrah", 2042, 2043, 2044);
		register("Vorkath", 8061);
		register("General Graardor", 2215);
		register("Kree'arra", 3162);
		register("Commander Zilyana", 2205);
		register("K'ril Tsutsaroth", 3129);
		register("Dagannoth Prime", 2265);
		register("Dagannoth Supreme", 2266);
		register("Dagannoth Rex", 2267);
		register("King Black Dragon", 239);
		register("Corporeal Beast", 319);
		register("Kraken", 494);
		register("Cerberus", 5862, 5863, 5866);
		register("Abyssal Sire", 5886, 5887, 5888, 5889, 5890, 5908);
		register("Vet'ion", 6611, 6612);
		register("Callisto", 6609, 6610);
		register("Venenatis", 6619, 6620);
		register("Chaos Elemental", 6618);
		register("Chaos Fanatic", 6610);
		register("Scorpia", 6615);
		register("Sarachnis", 8713);
		register("Nex", 11278, 11279, 11280, 11281, 11282);
		register("Nightmare", 9416, 9423);
		register("Phosani's Nightmare", 9417);
		register("Grotesque Guardians", 7851, 7853, 7888, 7889, 7890);
		register("Alchemical Hydra", 8615, 8616, 8617, 8618, 8619, 8620, 8621, 8622);
		register("Duke Sucellus", 12191, 12192);
		register("Vardorvis", 12222, 12223);
		register("Whisperer", 12204, 12205);
		register("Leviathan", 12214, 12215);
		register("Araxxor", 13668, 13669);
		register("Skotizo", 6503);
		register("Deranged Archaeologist", 7286);
		register("TzTok-Jad", 8195);
		register("TzKal-Zuk", 10624);
		register("Kalphite Queen", 963);
		register("Thermonuclear smoke devil", 499);
		register("Giant Mole", 5779);
		register("Mimic", 7223);
		register("Hespori", 8633);
		register("Gauntlet", 9021, 9022, 9023, 9024);
		register("Corrupted Gauntlet", 9035, 9036, 9037, 9038);
		register("Zalcano", 9049, 9050, 9051);
		register("Amoxliatl", 13685, 13686);
		register("Brutus", 15626, 15627, 15628, 15629);
		register("Ahrim the Blighted", 203);
		register("Dharok the Wretched", 202);
		register("Guthan the Infested", 204);
		register("Karil the Tainted", 205);
		register("Torag the Corrupted", 206);
		register("Verac the Defiled", 207);
	}

	private static void register(String boss, int... ids)
	{
		for (int id : ids)
		{
			NPC_TO_BOSS.put(id, boss);
		}
	}

	public Optional<String> getBossName(NPC npc)
	{
		if (npc == null)
		{
			return Optional.empty();
		}
		return getBossName(npc.getId());
	}

	public Optional<String> getBossName(int npcId)
	{
		return Optional.ofNullable(NPC_TO_BOSS.get(npcId));
	}

	public static String normalizeBossName(String name)
	{
		if (name == null)
		{
			return "";
		}
		String trimmed = name.trim();
		String renamed = BossNameRenames.RENAMES.getOrDefault(trimmed, trimmed);
		if (renamed.endsWith(":"))
		{
			renamed = renamed.substring(0, renamed.length() - 1).trim();
		}
		// Boss Kill Log rows use labels like "Abyssal Sire kills"; collection log uses "Abyssal Sire".
		renamed = renamed.replaceAll("(?i)\\s+kills?$", "").trim();
		if (renamed.startsWith("The "))
		{
			renamed = renamed.substring(4);
		}
		return renamed.trim();
	}
}
