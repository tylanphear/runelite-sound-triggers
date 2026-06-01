package com.soundtriggers;

/**
 * Who <em>dealt</em> a hitsplat, as reported by {@link net.runelite.api.Hitsplat#isMine()}.
 * Independent of {@link HitsplatTarget}, which filters on <em>whom</em> the hitsplat lands.
 */
public enum HitsplatSource
{
	ANY("Any"),
	ME("Me"),
	OTHERS("Others");

	private final String displayName;

	HitsplatSource(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
