package com.soundtriggers;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SoundTriggersPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SoundTriggersPlugin.class);
		RuneLite.main(args);
	}
}
