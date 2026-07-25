package is.flati.bosstracker.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KcSyncPayload
{
	private String playerName;
	private String source;
	private String updatedAt;
	private List<KcEntry> entries;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class KcEntry
	{
		private String bossName;
		private int killCount;
	}
}
