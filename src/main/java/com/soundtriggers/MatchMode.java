package com.soundtriggers;

/**
 * How a name filter is compared against an in-game name, for the
 * {@link TriggerType#PLAYER_SEEN} and {@link TriggerType#NPC_SEEN} triggers.
 */
public enum MatchMode
{
	/** The in-game name contains the filter (case-insensitive substring). */
	CONTAINS("Contains"),
	/** The in-game name equals the filter exactly (case-insensitive). */
	EXACT("Exact"),
        /** Any name will match the filter.*/
        ANY("Any");

	private final String displayName;

	MatchMode(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
