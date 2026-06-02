package com.soundtriggers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the trigger list to and from the JSON stored in RuneLite config.
 *
 * <p>Pure and free of {@code Client}/{@code ConfigManager}: strings in, triggers
 * out (and back). That keeps the parts most likely to break across plugin
 * versions — the schema-version envelope and the per-trigger fault isolation —
 * unit-testable without a live client. The plugin shell owns the actual
 * {@code ConfigManager} get/set and any recovery side effects.
 */
@Slf4j
final class TriggerStore
{
	/** Current on-disk schema version, written into every saved envelope. */
	static final int SCHEMA_VERSION = 1;

	private static final String KEY_VERSION = "version";
	private static final String KEY_TRIGGERS = "triggers";

	private TriggerStore()
	{
	}

	/** Serializes {@code triggers} into a versioned envelope JSON string. */
	static String serialize(Gson gson, List<SoundTrigger> triggers)
	{
		JsonObject root = new JsonObject();
		root.addProperty(KEY_VERSION, SCHEMA_VERSION);
		root.add(KEY_TRIGGERS, gson.toJsonTree(triggers));
		return gson.toJson(root);
	}

	/**
	 * Parses the stored config value (a {@code {"version":N,"triggers":[…]}}
	 * envelope) into a list of triggers. Each trigger is parsed in isolation: an
	 * element that fails to deserialize is skipped and logged, so one bad trigger
	 * can never take down the rest of the list.
	 *
	 * <p>A {@code null}/empty input, or a value that isn't a valid envelope,
	 * yields an empty list (the latter is logged).
	 */
	static List<SoundTrigger> parse(Gson gson, String json)
	{
		List<SoundTrigger> triggers = new ArrayList<>();
		if (json == null || json.isEmpty())
		{
			return triggers;
		}

		JsonArray array;
		try
		{
			array = new JsonParser().parse(json).getAsJsonObject().getAsJsonArray(KEY_TRIGGERS);
		}
		catch (Exception e)
		{
			log.error("Sound Triggers: stored triggers are not a valid envelope", e);
			return triggers;
		}

		if (array == null)
		{
			return triggers;
		}

		for (JsonElement element : array)
		{
			try
			{
				SoundTrigger trigger = gson.fromJson(element, SoundTrigger.class);
				if (trigger != null)
				{
					triggers.add(trigger);
				}
			}
			catch (Exception e)
			{
				log.warn("Sound Triggers: skipping unreadable trigger: {}", element, e);
			}
		}
		return triggers;
	}
}
