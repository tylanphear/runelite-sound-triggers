package com.soundtriggers;

public enum SoundSource
{
	FILE("File"),
	BUILTIN("Built-in"),
	CUSTOM("Custom");

	private final String displayName;

	SoundSource(String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public String toString()
	{
		return displayName;
	}
}
