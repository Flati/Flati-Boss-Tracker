package is.flati.bosstracker;

import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class CollectionLogSync
{
	private final Client client;
	private final BossTrackerConfig config;
	private final KillCountParser killCountParser;

	@Inject
	public CollectionLogSync(Client client, BossTrackerConfig config, KillCountParser killCountParser)
	{
		this.client = client;
		this.config = config;
		this.killCountParser = killCountParser;
	}

	public void syncCurrentPage()
	{
		if (!config.syncKcFromCollectionLog())
		{
			return;
		}

		Widget pageHead = client.getWidget(WidgetInfo.COLLECTION_LOG_ENTRY_HEADER);
		if (pageHead == null || pageHead.getDynamicChildren() == null)
		{
			return;
		}

		Widget[] children = pageHead.getDynamicChildren();
		if (children.length < 3)
		{
			return;
		}

		Widget[] killCountWidgets = Arrays.copyOfRange(children, 2, children.length);
		for (Widget killCountWidget : killCountWidgets)
		{
			if (killCountWidget == null)
			{
				continue;
			}

			String killCountString = Text.removeTags(killCountWidget.getText());
			if (killCountString == null || !killCountString.contains(": "))
			{
				continue;
			}

			String[] parts = killCountString.split(": ", 2);
			if (parts.length != 2)
			{
				continue;
			}

			String label = BossRegistry.normalizeBossName(parts[0]);
			int amount;
			try
			{
				amount = Integer.parseInt(parts[1].replace(",", ""));
			}
			catch (NumberFormatException e)
			{
				continue;
			}

			String bossName = label;
			if ("Kill count".equalsIgnoreCase(label) && children[0] != null)
			{
				bossName = BossRegistry.normalizeBossName(Text.removeTags(children[0].getText()));
			}

			if (bossName.isEmpty() || amount < 0)
			{
				continue;
			}

			killCountParser.sendKcUpdate(bossName, amount);

			if (config.debugLogging())
			{
				log.debug("Collection log KC: {} = {}", bossName, amount);
			}
		}
	}
}
