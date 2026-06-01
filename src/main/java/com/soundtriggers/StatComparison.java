package com.soundtriggers;

/**
 * How a {@link PlayerStat}'s current value is compared against its threshold.
 */
public enum StatComparison
{
	BELOW("Drops below"),
	ABOVE("Rises above");

	private final String displayName;

	StatComparison(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
