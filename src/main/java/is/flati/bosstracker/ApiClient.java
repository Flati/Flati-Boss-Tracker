package is.flati.bosstracker;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;
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
	private static final String PLUGIN_DIR = "flati-boss-tracker";
	private static final String QUEUE_FILE = "queue.jsonl";

	private final OkHttpClient httpClient;
	private final Gson gson;
	private final BossTrackerConfig config;
	private final ExecutorService queueExecutor;
	private Path queueDirectoryOverride;

	@Inject
	public ApiClient(OkHttpClient httpClient, Gson gson, BossTrackerConfig config)
	{
		this.httpClient = httpClient;
		this.gson = gson;
		this.config = config;
		this.queueExecutor = Executors.newSingleThreadExecutor(queueThreadFactory());
	}

	public void shutdown()
	{
		httpClient.dispatcher().cancelAll();
		queueExecutor.shutdownNow();
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
		enqueue(config.kcSyncEndpoint(), payload, onSuccess);
	}

	public void flushRetryQueue()
	{
		queueExecutor.execute(this::drainRetryQueue);
	}

	void setQueueDirectoryForTesting(Path directory)
	{
		queueDirectoryOverride = directory;
	}

	void awaitQueueIdle() throws InterruptedException
	{
		try
		{
			queueExecutor.submit(() -> {}).get();
		}
		catch (ExecutionException e)
		{
			throw new IllegalStateException("Queue executor failed", e.getCause());
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

		Request request = buildRequest(url, body);

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.warn("Flati Boss Tracker: request error for {}", url, e);
				if (queueOnFailure)
				{
					scheduleQueuePayload(url, body);
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
						if (queueOnFailure && shouldRetainFailedRequest(res.code()))
						{
							scheduleQueuePayload(url, body);
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

	private void drainRetryQueue()
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

			QueuedRequest queued;
			try
			{
				queued = gson.fromJson(line, QueuedRequest.class);
			}
			catch (JsonSyntaxException e)
			{
				log.warn("Flati Boss Tracker: dropping invalid queue entry", e);
				continue;
			}

			if (queued.url == null || queued.body == null)
			{
				log.warn("Flati Boss Tracker: dropping malformed queue entry");
				continue;
			}

			PostResult result = postJsonSync(queued.url, queued.body);
			if (result.success)
			{
				continue;
			}

			if (result.retainInQueue)
			{
				remaining.add(line);
			}
			else
			{
				log.warn("Flati Boss Tracker: dropping non-retryable queued request for {} ({})",
					queued.url, result.httpCode);
			}
		}

		writeQueueLines(path, remaining);
	}

	private PostResult postJsonSync(String url, String body)
	{
		if (!config.enableExternalSync() || config.apiKey() == null || config.apiKey().isBlank())
		{
			return PostResult.retain(0);
		}

		Request request = buildRequest(url, body);
		try (Response response = httpClient.newCall(request).execute())
		{
			if (response.isSuccessful())
			{
				if (config.debugLogging())
				{
					log.debug("Flati Boss Tracker: replayed queued request to {}", url);
				}
				return PostResult.success();
			}

			log.warn("Flati Boss Tracker: queued request failed {} {}", response.code(), url);
			return shouldRetainFailedRequest(response.code())
				? PostResult.retain(response.code())
				: PostResult.drop(response.code());
		}
		catch (IOException e)
		{
			log.warn("Flati Boss Tracker: queued request error for {}", url, e);
			return PostResult.retain(0);
		}
	}

	private Request buildRequest(String url, String body)
	{
		return new Request.Builder()
			.url(url)
			.header("User-Agent", USER_AGENT)
			.header("Authorization", "Bearer " + config.apiKey())
			.post(RequestBody.create(JSON, body))
			.build();
	}

	static boolean shouldRetainFailedRequest(int httpCode)
	{
		if (httpCode == 0)
		{
			return true;
		}

		if (httpCode == 403 || httpCode == 429 || httpCode >= 500)
		{
			return true;
		}

		return false;
	}

	private void scheduleQueuePayload(String url, String body)
	{
		queueExecutor.execute(() -> appendQueuePayload(url, body));
	}

	private void appendQueuePayload(String url, String body)
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

	private void writeQueueLines(Path path, List<String> remaining)
	{
		try
		{
			if (remaining.isEmpty())
			{
				Files.deleteIfExists(path);
				return;
			}

			Files.createDirectories(path.getParent());
			Files.write(path, remaining, StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			log.warn("Flati Boss Tracker: failed to update retry queue", e);
		}
	}

	private Path retryQueuePath()
	{
		Path base = queueDirectoryOverride != null
			? queueDirectoryOverride
			: RuneLite.RUNELITE_DIR.toPath().resolve(PLUGIN_DIR);
		return base.resolve(QUEUE_FILE);
	}

	private static ThreadFactory queueThreadFactory()
	{
		return runnable -> {
			Thread thread = new Thread(runnable, "flati-boss-tracker-queue");
			thread.setDaemon(true);
			return thread;
		};
	}

	private static final class PostResult
	{
		private final boolean success;
		private final boolean retainInQueue;
		private final int httpCode;

		private PostResult(boolean success, boolean retainInQueue, int httpCode)
		{
			this.success = success;
			this.retainInQueue = retainInQueue;
			this.httpCode = httpCode;
		}

		private static PostResult success()
		{
			return new PostResult(true, false, 0);
		}

		private static PostResult retain(int httpCode)
		{
			return new PostResult(false, true, httpCode);
		}

		private static PostResult drop(int httpCode)
		{
			return new PostResult(false, false, httpCode);
		}
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
