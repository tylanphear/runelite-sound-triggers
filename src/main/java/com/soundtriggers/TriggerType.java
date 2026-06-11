package com.soundtriggers;

public enum TriggerType
{
	HITSPLAT("Hitsplat"),
	ITEM_DROP("Item Drop"),
	CHAT_MESSAGE("Chat Message"),
	NPC_SEEN("NPC Seen"),
	STATUS_EFFECT("Status Effect"),
	PLAYER_STAT("Player Stat");

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
