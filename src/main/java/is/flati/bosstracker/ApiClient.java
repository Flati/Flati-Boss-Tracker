package is.flati.bosstracker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
import net.runelite.client.config.ConfigManager;
import is.flati.bosstracker.model.KcSyncPayload;
import is.flati.bosstracker.model.KcUpdatePayload;
import is.flati.bosstracker.model.KillPayload;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
@Singleton
public class ApiClient
{
	private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
	private static final String USER_AGENT = "FlatiBossTracker/1.0";
	private static final String CONFIG_GROUP = "flatibosstracker";
	private static final String LAST_BULK_SYNC_KEY = "lastBulkSyncAt";
	private static final String PLUGIN_DIR = "flati-boss-tracker";
	private static final String QUEUE_FILE = "queue.jsonl";

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ConfigManager configManager;
	private final BossTrackerConfig config;

	@Inject
	public ApiClient(OkHttpClient httpClient, Gson gson, ConfigManager configManager, BossTrackerConfig config)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.configManager = configManager;
		this.config = config;
	}

	public void shutdown()
	{
		httpClient.dispatcher().cancelAll();
	}

	public void sendKill(KillPayload payload)
	{
		enqueue(config.bossKillEndpoint(), payload, null);
	}

	public void sendKcUpdate(KcUpdatePayload payload)
	{
		enqueue(config.kcUpdateEndpoint(), payload, null);
	}

	public void sendKcSync(KcSyncPayload payload, Runnable onSuccess)
	{
		enqueue(config.kcSyncEndpoint(), payload, () -> {
			setLastBulkSyncAt(System.currentTimeMillis());
			if (onSuccess != null)
			{
				onSuccess.run();
			}
		});
	}

	public void flushRetryQueue()
	{
		Path path = retryQueuePath();
		if (!Files.exists(path))
		{
			return;
		}

		List<String> lines;
		try
		{
			lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			log.warn("Flati Boss Tracker: failed to read retry queue", e);
			return;
		}

		for (String line : lines)
		{
			if (line.isBlank())
			{
				continue;
			}
			try
			{
				QueuedRequest queued = gson.fromJson(line, QueuedRequest.class);
				postJsonAsync(queued.url, queued.body, null, false);
			}
			catch (JsonSyntaxException e)
			{
				log.warn("Flati Boss Tracker: skipping invalid queue entry", e);
			}
		}
	}

	private void enqueue(String url, Object payload, Runnable onSuccess)
	{
		if (!config.enableExternalSync())
		{
			return;
		}

		if (config.apiKey() == null || config.apiKey().isBlank())
		{
			log.debug("Flati Boss Tracker: API key not configured");
			return;
		}

		String body = gson.toJson(payload);
		postJsonAsync(url, body, onSuccess, true);
	}

	private void postJsonAsync(String url, String body, Runnable onSuccess, boolean queueOnFailure)
	{
		if (!config.enableExternalSync())
		{
			return;
		}

		if (config.apiKey() == null || config.apiKey().isBlank())
		{
			return;
		}

		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.header("Authorization", "Bearer " + config.apiKey())
			.post(RequestBody.create(JSON, body))
			.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Flati Boss Tracker: request error for {}", url, e);
				if (queueOnFailure)
				{
					queuePayload(url, body);
				}
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (Response res = response)
				{
					if (!res.isSuccessful())
					{
						log.warn("Flati Boss Tracker: request failed {} {}", res.code(), url);
						if (queueOnFailure)
						{
							queuePayload(url, body);
						}
						return;
					}

					if (config.debugLogging())
					{
						log.debug("Flati Boss Tracker: sent to {}", url);
					}

					if (onSuccess != null)
					{
						onSuccess.run();
					}
				}
			}
		});
	}

	private Path retryQueuePath()
	{
		return RuneLite.RUNELITE_DIR.toPath().resolve(PLUGIN_DIR).resolve(QUEUE_FILE);
	}

	private void queuePayload(String url, String body)
	{
		try
		{
			Path path = retryQueuePath();
			Files.createDirectories(path.getParent());
			String line = gson.toJson(new QueuedRequest(url, body)) + System.lineSeparator();
			Files.writeString(path, line, StandardCharsets.UTF_8,
				java.nio.file.StandardOpenOption.CREATE,
				java.nio.file.StandardOpenOption.APPEND);
		}
		catch (IOException e)
		{
			log.warn("Flati Boss Tracker: failed to queue payload", e);
		}
	}

	public long getLastBulkSyncAt()
	{
		Long value = configManager.getRSProfileConfiguration(CONFIG_GROUP, LAST_BULK_SYNC_KEY, Long.class);
		return value == null ? 0L : value;
	}

	public void setLastBulkSyncAt(long epochMillis)
	{
		configManager.setRSProfileConfiguration(CONFIG_GROUP, LAST_BULK_SYNC_KEY, epochMillis);
	}

	public boolean isSyncStale()
	{
		long last = getLastBulkSyncAt();
		if (last == 0)
		{
			return true;
		}
		long staleMs = (long) config.staleSyncDays() * 24L * 60L * 60L * 1000L;
		return System.currentTimeMillis() - last > staleMs;
	}

	private static class QueuedRequest
	{
		private String url;
		private String body;

		@SuppressWarnings("unused")
		private QueuedRequest()
		{
		}

		private QueuedRequest(String url, String body)
		{
			this.url = url;
			this.body = body;
		}
	}
}
