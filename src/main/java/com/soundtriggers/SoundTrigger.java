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

	// PLAYER_SEEN fields
	/** Player name to match; null means match any player. Interpreted per {@link #playerNameMatchMode}. */
	private String playerName = null;
	/** Whether {@link #playerName} is matched as a substring or exactly. */
	private MatchMode playerNameMatchMode = MatchMode.CONTAINS;

	// NPC_SEEN fields
	/** NPC name to match; null means match any NPC. Interpreted per {@link #npcNameMatchMode}. */
	private String npcName = null;
	/** Whether {@link #npcName} is matched as a substring or exactly. */
	private MatchMode npcNameMatchMode = MatchMode.CONTAINS;

	// STATUS_EFFECT fields
	private StatusEffectType statusEffectType = StatusEffectType.ANY;
	private StatusEffectCondition statusEffectCondition = StatusEffectCondition.GAINED;

	// PLAYER_STAT fields
	private PlayerStat playerStat = PlayerStat.HEALTH;
	private StatComparison statComparison = StatComparison.BELOW;
	/** Threshold the stat is compared against (absolute value, in the stat's own units). */
	private Integer statThreshold = null;
	/**
	 * Seconds between repeats while the condition holds. {@code null} or {@code 0}
	 * means play once when the value crosses the threshold (edge-triggered).
	 */
	private Integer statRepeatSeconds = null;
}
