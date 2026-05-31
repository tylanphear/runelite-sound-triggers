package com.soundtriggers;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(SoundTriggersPlugin.CONFIG_GROUP)
public interface SoundTriggersConfig extends Config
{
	@ConfigItem(
		keyName = SoundTriggersPlugin.CONFIG_KEY_TRIGGERS,
		name = "Triggers",
		description = "Serialized trigger list — managed by the Sound Triggers panel",
		hidden = true
	)
	default String triggers()
	{
		return "[]";
	}
}
