package is.flati.bosstracker;

import is.flati.bosstracker.model.KcSyncPayload;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class GraceLapSync
{
	private static final int GRACE_SYNC_RETRY_TICKS = 5;

	private final Client client;
	private final BossTrackerConfig config;
	private final ApiClient apiClient;

	private int graceLapsRetryTicks;

	@Inject
	public GraceLapSync(Client client, BossTrackerConfig config, ApiClient apiClient)
	{
		this.client = client;
		this.config = config;
		this.apiClient = apiClient;
	}

	public void markGraceLapsLoaded()
	{
		graceLapsRetryTicks = GRACE_SYNC_RETRY_TICKS;
		debug("Longscroll widget loaded; will attempt Grace lap sync for up to {} ticks", GRACE_SYNC_RETRY_TICKS);
	}

	public void onGameTick()
	{
		if (!config.enableExternalSync() || !config.syncKcFromGraceLaps())
		{
			return;
		}

		if (graceLapsRetryTicks <= 0)
		{
			return;
		}

		if (syncFromGraceLaps(null))
		{
			graceLapsRetryTicks = 0;
			return;
		}

		graceLapsRetryTicks--;
		debug("Grace lap sync attempt failed; {} tick(s) remaining", graceLapsRetryTicks);
	}

	public boolean syncFromGraceLaps(Runnable onSuccess)
	{
		debug("Grace lap sync: reading Longscroll widgets");

		Widget title = client.getWidget(InterfaceID.Longscroll.SCROLL_TITLE);
		Widget textWidget = client.getWidget(InterfaceID.Longscroll.SCROLL_TEXT);
		Widget textLayer = client.getWidget(InterfaceID.Longscroll.SCROLL_TEXT_LAYER);

		debug("Widgets title={} text={} textLayer={}",
			widgetSummary(title), widgetSummary(textWidget), widgetSummary(textLayer));

		if (title == null)
		{
			debug("Grace lap sync: SCROLL_TITLE widget is null");
			return false;
		}

		String headerText = Text.removeTags(title.getText());
		debug("Grace lap sync: header text={}", headerText);

		if (!isGraceLapHeader(headerText))
		{
			debug("Grace lap sync: header does not match Grace lap scroll");
			return false;
		}

		String body = collectScrollText(textWidget, textLayer);
		if (body.isEmpty())
		{
			debug("Grace lap sync: scroll body is empty");
			return false;
		}

		debug("Grace lap sync: scroll body ({} chars): {}", body.length(), truncate(body, 500));

		List<KcSyncPayload.KcEntry> entries = new ArrayList<>();
		for (AgilityCourses.CourseLap courseLap : AgilityCourses.parseGraceLapScroll(body))
		{
			debug("Grace lap sync: parsed {} = {}", courseLap.course, courseLap.laps);
			entries.add(KcSyncPayload.KcEntry.builder()
				.bossName(courseLap.course)
				.killCount(courseLap.laps)
				.build());
		}

		if (entries.isEmpty())
		{
			debug("Grace lap sync: no lap entries parsed from scroll body");
			return false;
		}

		String playerName = client.getLocalPlayer() != null ? client.getLocalPlayer().getName() : null;
		if (playerName == null)
		{
			debug("Grace lap sync: local player name unavailable");
			return false;
		}

		KcSyncPayload payload = KcSyncPayload.builder()
			.playerName(playerName)
			.source("grace_laps")
			.updatedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString())
			.entries(entries)
			.build();

		apiClient.sendKcSync(payload, onSuccess);
		log.info("Grace lap sync: queued {} courses for {}", entries.size(), playerName);
		debug("Grace lap sync: kc-sync payload sent with {} entries", entries.size());

		return true;
	}

	private static boolean isGraceLapHeader(String headerText)
	{
		if (headerText == null || headerText.isEmpty())
		{
			return false;
		}

		String trimmed = headerText.trim();
		return AgilityCourses.GRACE_LAPS_HEADER.equals(trimmed)
			|| trimmed.contains("Agility Course Lap Counts");
	}

	private static String collectScrollText(Widget textWidget, Widget textLayer)
	{
		StringBuilder builder = new StringBuilder();
		appendWidgetText(textWidget, builder);
		appendWidgetText(textLayer, builder);

		if (textLayer != null && textLayer.getChildren() != null)
		{
			for (Widget child : textLayer.getChildren())
			{
				appendWidgetText(child, builder);
			}
		}

		return builder.toString().trim();
	}

	private static void appendWidgetText(Widget widget, StringBuilder builder)
	{
		if (widget == null)
		{
			return;
		}

		String text = Text.removeTags(widget.getText());
		if (text == null || text.isEmpty())
		{
			return;
		}

		if (builder.length() > 0)
		{
			builder.append('\n');
		}
		builder.append(text.trim());
	}

	private static String widgetSummary(Widget widget)
	{
		if (widget == null)
		{
			return "null";
		}

		String text = Text.removeTags(widget.getText());
		if (text == null || text.isEmpty())
		{
			return "empty";
		}

		return truncate(text, 80);
	}

	private static String truncate(String value, int maxLen)
	{
		if (value.length() <= maxLen)
		{
			return value;
		}
		return value.substring(0, maxLen) + "...";
	}

	private void debug(String format, Object... args)
	{
		if (config.debugLogging())
		{
			log.debug(format, args);
		}
	}
}
