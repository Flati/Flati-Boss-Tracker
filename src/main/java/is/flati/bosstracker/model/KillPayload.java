package is.flati.bosstracker.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KillPayload
{
	private String playerName;
	private String bossName;
	private String killedAt;
	private Integer killCount;
}
