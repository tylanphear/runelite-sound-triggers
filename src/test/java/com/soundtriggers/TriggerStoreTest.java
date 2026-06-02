package com.soundtriggers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Unit tests for {@link TriggerStore}, the plugin's persistence (de)serializer.
 * No live {@code Client} or {@code ConfigManager} is involved — only plain
 * {@link SoundTrigger} models and JSON strings.
 */
public class TriggerStoreTest
{
	private final Gson gson = new Gson();

	private static SoundTrigger trigger(String name, TriggerType type)
	{
		SoundTrigger t = new SoundTrigger();
		t.setName(name);
		t.setType(type);
		return t;
	}

	@Test
	public void roundTripPreservesTriggers()
	{
		List<SoundTrigger> original = Arrays.asList(
			trigger("a", TriggerType.HITSPLAT),
			trigger("b", TriggerType.CHAT_MESSAGE));

		List<SoundTrigger> parsed = TriggerStore.parse(gson, TriggerStore.serialize(gson, original));

		assertEquals(original, parsed);
	}

	@Test
	public void serializeWritesVersionedEnvelope()
	{
		String json = TriggerStore.serialize(gson, Collections.singletonList(
			trigger("a", TriggerType.HITSPLAT)));

		JsonObject root = new JsonParser().parse(json).getAsJsonObject();
		assertEquals(TriggerStore.SCHEMA_VERSION, root.get("version").getAsInt());
		assertEquals(1, root.getAsJsonArray("triggers").size());
	}

	@Test
	public void oneBadTriggerDoesNotSinkTheRest()
	{
		// Middle element has a non-numeric volume: it fails to bind while the
		// well-formed siblings on either side must still load.
		String json = "{\"version\":1,\"triggers\":["
			+ "{\"name\":\"good1\",\"type\":\"HITSPLAT\"},"
			+ "{\"name\":\"bad\",\"volume\":\"not-a-number\"},"
			+ "{\"name\":\"good2\",\"type\":\"CHAT_MESSAGE\"}"
			+ "]}";

		List<SoundTrigger> parsed = TriggerStore.parse(gson, json);

		assertEquals(2, parsed.size());
		assertEquals("good1", parsed.get(0).getName());
		assertEquals("good2", parsed.get(1).getName());
	}

	@Test
	public void nullAndEmptyInputYieldEmptyList()
	{
		assertTrue(TriggerStore.parse(gson, null).isEmpty());
		assertTrue(TriggerStore.parse(gson, "").isEmpty());
	}

	@Test
	public void garbageInputYieldsEmptyList()
	{
		assertTrue(TriggerStore.parse(gson, "this is not json").isEmpty());
		assertTrue(TriggerStore.parse(gson, "[1,2,3]").isEmpty());
	}
}
