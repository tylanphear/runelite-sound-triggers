package com.soundtriggers;

public enum HitsplatTarget
{
	ANY("Any"),
	SELF("Self"),
	OTHERS("Others");

	private final String displayName;

	HitsplatTarget(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
