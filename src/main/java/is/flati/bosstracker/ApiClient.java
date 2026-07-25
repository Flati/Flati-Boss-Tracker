package is.flati.bosstracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import is.flati.bosstracker.model.KcSyncPayload;
import is.flati.bosstracker.model.KcUpdatePayload;
import is.flati.bosstracker.model.KillPayload;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
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
	private static final Gson GSON = new GsonBuilder().create();
	private static final String CONFIG_GROUP = "flatibosstracker";
	private static final String LAST_BULK_SYNC_KEY = "lastBulkSyncAt";

	private final OkHttpClient httpClient;
	private final ExecutorService executor;
	private final ConfigManager configManager;
	private final BossTrackerConfig config;

	@Inject
	public ApiClient(ConfigManager configManager, BossTrackerConfig config)
	{
		this.configManager = configManager;
		this.config = config;
		this.httpClient = new OkHttpClient();
		this.executor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "flati-boss-tracker-api");
			t.setDaemon(true);
			return t;
		});
	}

	public void sendKill(KillPayload payload)
	{
		enqueue(config.bossKillEndpoint(), payload);
	}

	public void sendKcUpdate(KcUpdatePayload payload)
	{
		enqueue(config.kcUpdateEndpoint(), payload);
	}

	public void sendKcSync(KcSyncPayload payload, Runnable onSuccess)
	{
		executor.execute(() -> {
			if (!postJson(config.kcSyncEndpoint(), payload))
			{
				queuePayload(config.kcSyncEndpoint(), payload);
				return;
			}
			setLastBulkSyncAt(System.currentTimeMillis());
			if (onSuccess != null)
			{
				onSuccess.run();
			}
		});
	}

	private void enqueue(String url, Object payload)
	{
		executor.execute(() -> {
			if (!postJson(url, payload))
			{
				queuePayload(url, payload);
			}
		});
	}

	public void flushRetryQueue()
	{
		executor.execute(this::processRetryQueue);
	}

	private boolean postJson(String url, Object payload)
	{
		if (config.apiKey() == null || config.apiKey().isBlank())
		{
			log.warn("Flati Boss Tracker: API key not configured");
			return false;
		}

		String body = GSON.toJson(payload);
		Request request = new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.header("Authorization", "Bearer " + config.apiKey())
			.post(RequestBody.create(JSON, body))
			.build();

		try (Response response = httpClient.newCall(request).execute())
		{
			if (!response.isSuccessful())
			{
				log.warn("Flati Boss Tracker: request failed {} {}", response.code(), url);
				return false;
			}
			if (config.debugLogging())
			{
				log.debug("Flati Boss Tracker: sent to {}", url);
			}
			return true;
		}
		catch (IOException e)
		{
			log.warn("Flati Boss Tracker: request error for {}", url, e);
			return false;
		}
	}

	private Path retryQueuePath()
	{
		return Path.of(System.getProperty("user.home"), ".runelite", "flati-boss-tracker-queue.jsonl");
	}

	private void queuePayload(String url, Object payload)
	{
		try
		{
			Files.createDirectories(retryQueuePath().getParent());
			String line = GSON.toJson(new QueuedRequest(url, payload)) + System.lineSeparator();
			Files.writeString(retryQueuePath(), line, StandardCharsets.UTF_8,
				java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
		}
		catch (IOException e)
		{
			log.warn("Flati Boss Tracker: failed to queue payload", e);
		}
	}

	private void processRetryQueue()
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

		List<String> remaining = new ArrayList<>();
		for (String line : lines)
		{
			if (line.isBlank())
			{
				continue;
			}
			QueuedRequest queued = GSON.fromJson(line, QueuedRequest.class);
			if (!postJson(queued.url, queued.payload))
			{
				remaining.add(line);
			}
		}

		try
		{
			if (remaining.isEmpty())
			{
				Files.deleteIfExists(path);
			}
			else
			{
				Files.write(path, remaining, StandardCharsets.UTF_8);
			}
		}
		catch (IOException e)
		{
			log.warn("Flati Boss Tracker: failed to update retry queue", e);
		}
	}

	public long getLastBulkSyncAt()
	{
		Long value = configManager.getRSProfileConfiguration(CONFIG_GROUP, LAST_BULK_SYNC_KEY, long.class);
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
		private final String url;
		private final Object payload;

		private QueuedRequest(String url, Object payload)
		{
			this.url = url;
			this.payload = payload;
		}
	}
}
