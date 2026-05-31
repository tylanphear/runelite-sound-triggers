package com.soundtriggers;

public enum TriggerType
{
	HITSPLAT("Hitsplat"),
	ITEM_DROP("Item Drop"),
	CHAT_MESSAGE("Chat Message"),
	PLAYER_SPAWN("Player Appears"),
	NPC_SPAWN("NPC Spawns"),
	STATUS_EFFECT("Status Effect");

	private final String displayName;

	TriggerType(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
