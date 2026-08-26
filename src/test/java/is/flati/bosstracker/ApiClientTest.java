package is.flati.bosstracker;

import com.google.gson.Gson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiClientTest
{
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private MockWebServer server;
	private ApiClient apiClient;
	private Path queueDirectory;
	private final Gson gson = new Gson();

	@Before
	public void setUp() throws IOException
	{
		server = new MockWebServer();
		server.start();
		queueDirectory = temporaryFolder.newFolder("queue").toPath();

		apiClient = new ApiClient(
			new OkHttpClient(),
			gson,
			null,
			new StubConfig("test-key")
		);
		apiClient.setQueueDirectoryForTesting(queueDirectory);
	}

	@After
	public void tearDown() throws IOException
	{
		apiClient.shutdown();
		server.shutdown();
	}

	@Test
	public void flushRetryQueueRemovesSuccessfulEntries() throws Exception
	{
		server.enqueue(new MockResponse().setResponseCode(201));
		writeQueueLine(server.url("/boss-kill").toString(), "{\"playerName\":\"Alice\"}");

		apiClient.flushRetryQueue();
		apiClient.awaitQueueIdle();

		assertEquals(1, server.getRequestCount());
		assertFalse(Files.exists(queueFile()));
	}

	@Test
	public void overlappingFlushCallsDoNotDuplicateSends() throws Exception
	{
		server.enqueue(new MockResponse().setResponseCode(201));
		writeQueueLine(server.url("/boss-kill").toString(), "{\"playerName\":\"Alice\"}");

		apiClient.flushRetryQueue();
		apiClient.flushRetryQueue();
		apiClient.awaitQueueIdle();

		assertEquals(1, server.getRequestCount());
		assertFalse(Files.exists(queueFile()));
	}

	@Test
	public void retryableFailureStaysQueued() throws Exception
	{
		server.enqueue(new MockResponse().setResponseCode(503));
		writeQueueLine(server.url("/boss-kill").toString(), "{\"playerName\":\"Alice\"}");

		apiClient.flushRetryQueue();
		apiClient.awaitQueueIdle();

		assertEquals(1, server.getRequestCount());
		assertTrue(Files.exists(queueFile()));
		assertEquals(1, Files.readAllLines(queueFile()).size());
	}

	@Test
	public void roster403StaysQueued() throws Exception
	{
		server.enqueue(new MockResponse().setResponseCode(403));
		writeQueueLine(server.url("/boss-kill").toString(), "{\"playerName\":\"Alice\"}");

		apiClient.flushRetryQueue();
		apiClient.awaitQueueIdle();

		assertEquals(1, server.getRequestCount());
		assertTrue(Files.exists(queueFile()));
	}

	@Test
	public void nonRetryable401DropsFromQueue() throws Exception
	{
		server.enqueue(new MockResponse().setResponseCode(401));
		writeQueueLine(server.url("/boss-kill").toString(), "{\"playerName\":\"Alice\"}");

		apiClient.flushRetryQueue();
		apiClient.awaitQueueIdle();

		assertEquals(1, server.getRequestCount());
		assertFalse(Files.exists(queueFile()));
	}

	@Test
	public void legacyQueueLinesStillLoad() throws Exception
	{
		server.enqueue(new MockResponse().setResponseCode(200));
		Path queueFile = queueFile();
		Files.createDirectories(queueFile.getParent());
		Files.writeString(queueFile,
			"{\"url\":\"" + server.url("/boss-kill") + "\",\"body\":\"{\\\"playerName\\\":\\\"Bob\\\"}\"}"
				+ System.lineSeparator(),
			StandardCharsets.UTF_8);

		apiClient.flushRetryQueue();
		apiClient.awaitQueueIdle();

		assertEquals(1, server.getRequestCount());
		assertFalse(Files.exists(queueFile));
	}

	@Test
	public void shouldRetainFailedRequest_classifiesRetryableStatuses()
	{
		assertTrue(ApiClient.shouldRetainFailedRequest(0));
		assertTrue(ApiClient.shouldRetainFailedRequest(403));
		assertTrue(ApiClient.shouldRetainFailedRequest(429));
		assertTrue(ApiClient.shouldRetainFailedRequest(500));
		assertFalse(ApiClient.shouldRetainFailedRequest(401));
		assertFalse(ApiClient.shouldRetainFailedRequest(400));
		assertFalse(ApiClient.shouldRetainFailedRequest(404));
	}

	private void writeQueueLine(String url, String body) throws IOException
	{
		Path queueFile = queueFile();
		Files.createDirectories(queueFile.getParent());
		String line = gson.toJson(new QueueLine(url, body)) + System.lineSeparator();
		Files.writeString(queueFile, line, StandardCharsets.UTF_8);
	}

	private Path queueFile()
	{
		return queueDirectory.resolve("queue.jsonl");
	}

	private static final class QueueLine
	{
		private final String url;
		private final String body;

		private QueueLine(String url, String body)
		{
			this.url = url;
			this.body = body;
		}
	}

	private static final class StubConfig implements BossTrackerConfig
	{
		private final String apiKey;

		private StubConfig(String apiKey)
		{
			this.apiKey = apiKey;
		}

		@Override
		public boolean enableExternalSync()
		{
			return true;
		}

		@Override
		public String apiKey()
		{
			return apiKey;
		}
	}
}
