package com.soundtriggers;

public enum StatusEffectType
{
	ANY("Any"),
	POISON("Poison"),
	VENOM("Venom");

	private final String displayName;

	StatusEffectType(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
