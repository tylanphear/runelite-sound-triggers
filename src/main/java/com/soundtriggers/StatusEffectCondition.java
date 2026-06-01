package com.soundtriggers;

public enum StatusEffectCondition
{
	GAINED("Gained"),
	LOST("Lost");

	private final String displayName;

	StatusEffectCondition(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
