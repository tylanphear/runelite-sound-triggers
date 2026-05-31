package com.soundtriggers;

import lombok.Data;

import java.util.UUID;

@Data
public class SoundTrigger
{
	private String id = UUID.randomUUID().toString();
	private String name = "New Trigger";
	private boolean enabled = true;
	private TriggerType type = TriggerType.HITSPLAT;
	private String soundPath = "";
	private int volume = 100;

	// HITSPLAT fields
	/** Exact damage value to match; null means match any value. */
	private Integer hitsplatValue = null;
	private HitsplatTarget hitsplatTarget = HitsplatTarget.ANY;

	// ITEM_DROP fields
	/** Case-insensitive substring of item name to match; null means match any item. */
	private String itemName = null;

	// CHAT_MESSAGE fields
	/** Case-insensitive substring of chat message to match; null means match any message. */
	private String chatPattern = null;

	// PLAYER_SPAWN fields
	/** Case-insensitive substring of player name to match; null means match any player. */
	private String playerName = null;

	// NPC_SPAWN fields
	/** Case-insensitive substring of NPC name to match; null means match any NPC. */
	private String npcName = null;

	// STATUS_EFFECT fields
	private StatusEffectType statusEffectType = StatusEffectType.ANY;
}
