package com.soundtriggers;

/**
 * A player resource whose current value can be compared against a threshold.
 *
 * <p>All values are normalised to the same scale the player sees in-game:
 * health and prayer are absolute points, while run energy and special attack
 * are 0&ndash;100.
 */
public enum PlayerStat
{
	HEALTH("Health"),
	PRAYER("Prayer"),
	RUN_ENERGY("Run Energy"),
	SPECIAL_ATTACK("Special Attack");

	private final String displayName;

	PlayerStat(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
